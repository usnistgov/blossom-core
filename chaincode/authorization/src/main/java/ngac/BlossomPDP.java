package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.PolicyReviewer;
import gov.nist.csd.pm.pdp.memory.MemoryPolicyReviewer;
import gov.nist.csd.pm.policy.Policy;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.model.expression.Value;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;

import javax.security.auth.x500.X500Principal;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Provides methods for checking access to Blossom resources and updating the policy as needed.
 */
public class BlossomPDP {

    private static final String BLOSSOM_SYSTEM = "blossom_system";
    private static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    private static final String SYSTEM_OWNER_UA = "System Owner";

    /**
     * Get the AdminMSP defined in the policy stored on the ledger.
     *
     * @param ctx Chaincode context.
     * @return The AdminMSP defined in the policy on ledger.
     * @throws PMException If there is an error retrieving the policy from the ledger.
     */
    public static String getAdminMSPID(Context ctx) throws PMException {
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        return getAdminMSPID(memoryPolicyStore);
    }

    /**
     * Get the AdminMSP defined in the in memory instance of the policy.
     *
     * @param policy An in memory policy representation.
     * @return The AdminMSP value stored in the policy as a PML constant.
     * @throws PMException If there is an error getting the AdminMSP value from the policy.
     */
    public static String getAdminMSPID(Policy policy) throws PMException {
        Value value = policy.userDefinedPML().getConstant("ADMINMSP");
        return value.getStringValue();
    }

    /**
     * Initialize the NGAC policy with the given PML string. The requesting user needs the "bootstrap" permission on
     * the blossom system.
     *
     * @param ctx Chaincode context.
     * @param pml The PML string to initialize an NGAC policy with.
     * @return A PAP object that stores an in memory policy from the given PML.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public PAP initNGAC(Context ctx, String pml) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        // create a new PAP object to compile and execute the PML
        PAP pap = new PAP(new MemoryPolicyStore());
        UserContext user = new UserContext(getNGACUserName(ctx));
        pap.deserialize().fromPML(user, pml);

        PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        if (!accessRights.contains("bootstrap")) {
            throw new PMException("user " + userCtx.getUser() + " cannot bootstrap");
        }

        return pap;
    }

    /**
     * Check if the given user has "request_account" on the blossom_system.
     *
     * @param ctx Chaincode context.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void requestAccount(Context ctx) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewerWithoutAccountUA(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        if (!accessRights.contains("request_account")) {
            throw new PMException("user " + userCtx.getUser() + " cannot request account");
        }
    }

    /**
     * Check if the requesting user has "approve_account" on blossom_system. If they do, call the approveAccount function
     * defined in policy.pml to create account in the policy.
     *
     * @param ctx Chaincode context.
     * @param account The ID of the account to approve.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void approveAccount(Context ctx, String account) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        boolean result = accessRights.contains("approve_account");
        if (!result) {
            throw new PMException("user " + userCtx.getUser() + " cannot approve account");
        }

        PAP pap = new PAP(memoryPolicyStore);
        String pml = String.format("approveAccount('%s')", account);
        pap.executePML(userCtx, pml);

        ctx.getStub().putState("policy", memoryPolicyStore.serialize().toJSON().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Check if the requesting user has "upload_ato" on the object representing the given account.
     *
     * @param ctx Chaincode context.
     * @param account
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void uploadATO(Context ctx, String account) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, accountObjectNodeName(account));

        if(!accessRights.contains("upload_ato")) {
            throw new PMException("user " + userCtx.getUser() + " cannot upload ato");
        }
    }

    /**
     * Check if the requesting user has "initiate_vote" on the object representing the target member of the vote. If they
     * do call the initiateVote function defined in policy.pml to create the vote object and give initiator permissions
     * on the vote object.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void initiateVote(Context ctx, String voteID, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, accountObjectNodeName(targetMember));

        boolean result = accessRights.contains("initiate_vote");
        if (!result) {
            throw new PMException("user " + userCtx.getUser() + " cannot initiate vote");
        }

        PAP pap = new PAP(memoryPolicyStore);
        String pml = String.format("initiateVote('%s', '%s', '%s')", ctx.getClientIdentity().getMSPID(), voteID, targetMember);
        pap.executePML(userCtx, pml);

        ctx.getStub().putState("policy", memoryPolicyStore.serialize().toJSON().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Check if the requesting user has "vote" on the vote object that represents the given vote.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void vote(Context ctx, String voteID, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, voteObj(targetMember, voteID));

        if(!accessRights.contains("vote")) {
            throw new PMException("user " + userCtx.getUser() + " cannot vote");
        }
    }

    /**
     * Check if the requesting user has "complete_vote" on the vote object that represents the given vote.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void completeVote(Context ctx, String voteID, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, voteObj(targetMember, voteID));

        if(!accessRights.contains("complete_vote")) {
            throw new PMException("user " + userCtx.getUser() + " cannot complete vote");
        }

        PAP pap = new PAP(memoryPolicyStore);
        String pml = String.format("completeVote('%s', '%s')", voteID, targetMember);
        pap.executePML(userCtx, pml);

        ctx.getStub().putState("policy", memoryPolicyStore.serialize().toJSON().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Check if the requesting user has "complete_vote" on the vote object that represents the given vote.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the requesting user cannot perform the action.
     */
    public void deleteVote(Context ctx, String voteID, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, voteObj(targetMember, voteID));

        boolean result = accessRights.contains("delete_vote");
        if (!result) {
            throw new PMException("user " + userCtx.getUser() + " cannot delete vote");
        }

        PAP pap = new PAP(memoryPolicyStore);
        String pml = String.format("deleteVote('%s', '%s', '%s')", ctx.getClientIdentity().getMSPID(), voteID, targetMember);
        pap.executePML(userCtx, pml);

        ctx.getStub().putState("policy", memoryPolicyStore.serialize().toJSON().getBytes(StandardCharsets.UTF_8));
    }

    public static MemoryPolicyStore loadPolicy(Context ctx) throws PMException {
        byte[] policy = ctx.getStub().getState("policy");
        if (policy == null) {
            throw new PMException("ngac policy has not been initialized");
        }

        String json = new String(policy, StandardCharsets.UTF_8);

        MemoryPolicyStore memoryPolicyStore = new MemoryPolicyStore();
        memoryPolicyStore
                .deserialize()
                .fromJSON(json);

        return memoryPolicyStore;
    }

    private String accountUsersNodeName(String mspid) {
        return mspid + " users";
    }

    private String accountContainerNodeName(String mspid) {
        return mspid + " account attr";
    }

    private String accountObjectNodeName(String mspid) {
        return mspid + " account";
    }

    private String voteUA(String targetMember) {
        return "vote:" + targetMember + "_UA";
    }

    private String voteObj(String targetMember, String voteID) {
        return targetMember + "-" + voteID + " vote";
    }

    private static String getNGACUserName(Context ctx) {
        ClientIdentity clientIdentity = ctx.getClientIdentity();
        X509Certificate cert = clientIdentity.getX509Certificate();
        X500Principal principal = cert.getSubjectX500Principal();
        String mspid = ctx.getClientIdentity().getMSPID();

        String user = "";
        try {
            JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(cert);
            X500Name subject = jcaX509CertificateHolder.getSubject();
            RDN cnRDN = subject.getRDNs(BCStyle.CN)[0];
            AttributeTypeAndValue first = cnRDN.getFirst();
            user = first.getValue().toString();
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }

        return user + ":" + mspid;
    }

    private UserContext getUserCtxFromRequest(Context ctx) {
        return new UserContext(getNGACUserName(ctx));
    }

    private PolicyReviewer loadPolicyReviewer(Context ctx, Policy policy) throws PMException {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        // create the calling user in the graph and assign to appropriate attributes
        policy.graph().createUser(ngacUserName, accountUsersNodeName(mspid), role);

        // check if user is blossom admin
        if (getAdminMSPID(policy).equals(mspid) && role.equals(SYSTEM_OWNER_UA)) {
             policy.graph().assign(ngacUserName, "Blossom Admin");
        }

        // create a new PolicyReviewer object to make a decision
        return new MemoryPolicyReviewer(policy);
    }

    private PolicyReviewer loadPolicyReviewerWithoutAccountUA(Context ctx, Policy policy) throws PMException {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        // create the calling user in the graph and assign to appropriate attributes
        policy.graph().createUser(ngacUserName, role);

        // a user is a Blossom Admin if 1) they are in the admin msp and 2) they are a system owner
        if (getAdminMSPID(policy).equals(mspid) && role.equals(SYSTEM_OWNER_UA)) {
             policy.graph().assign(ngacUserName, "Blossom Admin");
        }

        // create a new PolicyReviewer object to make a decision
        return new MemoryPolicyReviewer(policy);
    }

}
