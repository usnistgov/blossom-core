package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.reviewer.PolicyReviewer;
import gov.nist.csd.pm.policy.Policy;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.value.StringValue;
import gov.nist.csd.pm.policy.pml.value.Value;
import gov.nist.csd.pm.policy.serialization.json.JSONDeserializer;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import gov.nist.csd.pm.policy.serialization.pml.PMLDeserializer;
import model.VoteConfiguration;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Provides methods for checking access to Blossom resources and updating the policy as needed.
 */
public class BlossomPDP {

    private static final String BLOSSOM_TARGET = "blossom_target";
    private static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    private static final String AUTHORIZING_OFFICIAL = "Authorizing Official";

    /**
     * Get the AdminMSP defined in the policy stored on the ledger.
     *
     * @param ctx Chaincode context.
     * @return The AdminMSP defined in the policy on ledger.
     * @throws PMException If there is an error retrieving the policy from the ledger.
     */
    public static String getAdminMSPID(Context ctx) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);
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
     * Initialize the NGAC policy with the given PML string. The requesting user needs the "bootstrap" permission on the
     * blossom target.
     *
     * @param ctx               Chaincode context.
     * @param pml               The PML string to initialize an NGAC policy with.
     *
     * @return A PAP object that stores an in memory policy from the given PML.
     *
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public PAP bootstrap(Context ctx, String pml) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        // create a new PAP object to compile and execute the PML
        PAP pap = new PAP(new MemoryPolicyStore());
        UserContext user = new UserContext(getNGACUserName(ctx));
        pap.deserialize(user, pml, new PMLDeserializer());

        PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);
        AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, BLOSSOM_TARGET);

        if (!accessRights.contains("bootstrap")) {
            throw new UnauthorizedException(userCtx, BLOSSOM_TARGET, "bootstrap");
        }

        // delete user from policy before returning since the returned pap will be written to the world state
        pap.graph().deleteNode(user.getUser());

        return pap;
    }

    /**
     * Check if the cid has "update_mou" on BLOSSOM_TARGET
     * @param ctx The Fabric context.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void updateMOU(Context ctx) throws PMException {
        decide(ctx, BLOSSOM_TARGET, "update_mou");
    }

    /**
     * Check if the cid has "update_vote_config" on BLOSSOM_TARGET. If yes, invoke the updateVoteConfig function.
     * @param ctx The Fabric context.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void updateVoteConfig(Context ctx, VoteConfiguration voteConfiguration) throws PMException {
        decideAndRespond(ctx, BLOSSOM_TARGET, "update_vote_config",
                                "updateVoteConfig", Value.fromObject(voteConfiguration));
    }

    /**
     * Check if the cid has "sign_mou" on BLOSSOM_TARGET. Since this can be called before an account is called, do not
     * assign the user to the account user attribute, just the role.
     *
     * @param ctx The Fabric context.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void signMOU(Context ctx) throws PMException {
        decideWithoutAccountAttribute(ctx, BLOSSOM_TARGET, "sign_mou");
    }

    /**
     * Check if the cid has "sign_mou" on BLOSSOM_TARGET. Since this can be called before an account is called, do not
     * assign the user to the account user attribute, just the role. If yes, invoke the join function.
     *
     * @param ctx The Fabric context.
     * @param account The account id.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void join(Context ctx, String account) throws PMException {
        decideWithoutAccountAttributeAndRespond(ctx, BLOSSOM_TARGET, "join", "join", new StringValue(account));
    }

    /**
     * Check if the cid has "write_ato" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param account The account id.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void writeATO(Context ctx, String account) throws PMException {
        String target = accountObjectNodeName(account);
        decide(ctx, target, "write_ato");
    }

    /**
     * Check if the cid has "submit_feedback" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param targetMember The target of the feedback.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void submitFeedback(Context ctx, String targetMember) throws PMException {
        String target = accountObjectNodeName(targetMember);
        decide(ctx, target, "submit_feedback");
    }

    /**
     * Check if the cid has "initiate_vote" on <account> target. If yes, invoke the initiateVote function.
     *
     * @param ctx          Chaincode context.
     * @param voteID       The ID of the vote.
     * @param targetMember The target member of the vote.
     *
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void initiateVote(Context ctx, String voteID, String targetMember) throws PMException {
        String target = accountObjectNodeName(targetMember);
        decideAndRespond(ctx, target, "initiate_vote", "initiateVote",
                         new StringValue(ctx.getClientIdentity().getMSPID()),
                         new StringValue(voteID),
                         new StringValue(targetMember));
    }

    /**
     * Check if the cid has "vote" on the vote object.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void vote(Context ctx, String voteID, String targetMember) throws PMException {
        decide(ctx, voteObj(targetMember, voteID), "vote");
    }

    /**
     * Check if the cid has "certify_vote" on the vote object. if yes, invoke the endVote function.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void certifyVote(Context ctx, String voteID, String targetMember) throws PMException {
        String target = voteObj(targetMember, voteID);
        decideAndRespond(ctx, target, "certify_vote", "endVote",
                         new StringValue(voteID),
                         new StringValue(targetMember));
    }

    /**
     * Check if the cid has "abort_vote" on the vote object. if yes, invoke the endVote function.
     *
     * @param ctx Chaincode context.
     * @param voteID The ID of the vote.
     * @param targetMember The target member of the vote.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void abortVote(Context ctx, String voteID, String targetMember) throws PMException {
        String target = voteObj(targetMember, voteID);
        decideAndRespond(ctx, target, "abort_vote", "endVote",
                         new StringValue(voteID),
                         new StringValue(targetMember));
    }

    /**
     * Load the policy from the context into memory.
     * @param ctx The Fabric context.
     * @param userCtx The user context representing the cid.
     * @return The policy in memory.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public static MemoryPolicyStore loadPolicy(Context ctx, UserContext userCtx) throws PMException {
        byte[] policy = ctx.getStub().getState("policy");
        if (policy == null) {
            throw new PMException("ngac policy has not been initialized");
        }

        String json = new String(policy, StandardCharsets.UTF_8);

        MemoryPolicyStore memoryPolicyStore = new MemoryPolicyStore();
        memoryPolicyStore.deserialize(userCtx, json, new JSONDeserializer());

        return memoryPolicyStore;
    }

    private void decide(Context ctx, String target, String ar) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);
        PAP pap = new PAP(memoryPolicyStore);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);

        AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

        boolean result = accessRights.contains(ar);
        if (!result) {
            throw new UnauthorizedException(userCtx, target, ar);
        }
    }

    private void decideWithoutAccountAttribute(Context ctx, String target, String ar) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);
        PAP pap = new PAP(memoryPolicyStore);
        PolicyReviewer reviewer = loadPolicyReviewerWithoutAccountUA(ctx, pap);

        AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

        boolean result = accessRights.contains(ar);
        if (!result) {
            throw new UnauthorizedException(userCtx, target, ar);
        }
    }

    private void decideWithoutAccountAttributeAndRespond(Context ctx, String target, String ar, String function, Value ... args) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);
        PAP pap = new PAP(memoryPolicyStore);
        PolicyReviewer reviewer = loadPolicyReviewerWithoutAccountUA(ctx, pap);

        AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

        boolean result = accessRights.contains(ar);
        if (!result) {
            throw new UnauthorizedException(userCtx, target, ar);
        }

        pap.executePMLFunction(userCtx, function, args);

        // remove user before committing to the ledger
        memoryPolicyStore.graph().deleteNode(userCtx.getUser());

        ctx.getStub().putState("policy", memoryPolicyStore.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));
    }

    private void decideAndRespond(Context ctx, String target, String ar, String function, Value ... args) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);
        PAP pap = new PAP(memoryPolicyStore);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);

        AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

        boolean result = accessRights.contains(ar);
        if (!result) {
            throw new UnauthorizedException(userCtx, target, ar);
        }

        pap.executePMLFunction(userCtx, function, args);

        // remove user before committing to the ledger
        pap.graph().deleteNode(userCtx.getUser());

        // save policy updates
        ctx.getStub().putState("policy", pap.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));

    }

    private String accountUsersNodeName(String mspid) {
        return mspid + " users";
    }

    private String accountContainerNodeName(String mspid) {
        return mspid + " account";
    }

    private String accountObjectNodeName(String mspid) {
        return mspid + " target";
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
        String mspid = ctx.getClientIdentity().getMSPID();

        String user;
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

    public static UserContext getUserCtxFromRequest(Context ctx) {
        return new UserContext(getNGACUserName(ctx));
    }

    PolicyReviewer loadPolicyReviewer(Context ctx, PAP policy) throws PMException {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);
        String accountUA = accountUsersNodeName(mspid);

        if (!policy.graph().nodeExists(role)) {
            throw new ChaincodeException("unknown user role: " + role);
        } else if (!policy.graph().nodeExists(accountUA)) {
            throw new ChaincodeException("account " + mspid + " has not yet joined");
        }

        // create the calling user in the graph and assign to appropriate attributes
        try {
            policy.graph().createUser(ngacUserName, accountUA, role);

            // check if user is blossom admin
            if (getAdminMSPID(policy).equals(mspid) && role.equals(AUTHORIZING_OFFICIAL)) {
                policy.graph().assign(ngacUserName, "Blossom Admin");
            }
        } catch (PMException e) {
            throw new ChaincodeException(e.getMessage());
        }

        // create a new PolicyReviewer object to make a decision
        return new PolicyReviewer(policy);
    }

    private PolicyReviewer loadPolicyReviewerWithoutAccountUA(Context ctx, PAP policy) throws PMException {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        if (!policy.graph().nodeExists(role)) {
            throw new PMException("unknown user role: " + role);
        }

        // create the user in the policy
        policy.graph().createUser(ngacUserName, role);

        // a user is a Blossom Admin if 1) they are in the admin msp and 2) they are a Authorizing Official
        if (getAdminMSPID(policy).equals(mspid) && role.equals(AUTHORIZING_OFFICIAL)) {
            policy.graph().assign(ngacUserName, "Blossom Admin");
        }

        // create a new PolicyReviewer object to make a decision
        return new PolicyReviewer(policy);
    }
}
