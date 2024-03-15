package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.reviewer.PolicyReviewer;
import gov.nist.csd.pm.policy.Policy;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.value.StringValue;
import gov.nist.csd.pm.policy.pml.value.Value;
import gov.nist.csd.pm.policy.serialization.json.JSONDeserializer;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import gov.nist.csd.pm.policy.serialization.pml.PMLDeserializer;
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

    private String adminMSP;

    public BlossomPDP() {

    }

    /**
     * Get the AdminMSP defined in the policy stored on the ledger.
     *
     * @param ctx Chaincode context.
     * @return The AdminMSP defined in the policy on ledger.
     * @throws ChaincodeException If there is an error retrieving the policy from the ledger.
     */
    public String getADMINMSP(Context ctx) {
        if (adminMSP != null) {
            return adminMSP;
        }

        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);

        return getADMINMSP(memoryPolicyStore);
    }

    private String getADMINMSP(Policy policy) {
        try {
            adminMSP = policy.userDefinedPML().getConstant("ADMINMSP").getStringValue();
            return adminMSP;
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
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
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public PAP bootstrap(Context ctx, String pml) {
        UserContext userCtx = getUserCtxFromRequest(ctx);

        try {
            // create a new PAP object to compile and execute the PML
            PAP pap = new PAP(new MemoryPolicyStore());
            UserContext user = new UserContext(getNGACUserName(ctx));
            pap.deserialize(user, pml, new PMLDeserializer());

            PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);
            AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, BLOSSOM_TARGET);

            if (!accessRights.contains("bootstrap")) {
                throw new ChaincodeException("cid is not authorized to bootstrap Blossom");
            }

            // delete user from policy before returning since the returned pap will be written to the world state
            pap.graph().deleteNode(user.getUser());

            return pap;
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    public void join(Context ctx, String account) {
        decide(ctx, accountObjectNodeName(account), "join", "cid is not authorized to join for account " + account);
    }

    public void readATO(Context ctx, String account) {
        decide(ctx, accountObjectNodeName(account), "read_ato", "cid is not authorized to read the ATO of account " + account);
    }

    /**
     * Check if the cid has "update_mou" on BLOSSOM_TARGET
     * @param ctx The Fabric context.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void updateMOU(Context ctx) {
        decide(ctx, BLOSSOM_TARGET, "update_mou", "cid is not authorized to update the Blossom MOU");
    }

    /**
     * Check if the cid has "sign_mou" on BLOSSOM_TARGET. Since this can be called before an account is called, do not
     * assign the user to the account user attribute, just the role.
     *
     * @param ctx The Fabric context.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void signMOU(Context ctx) {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);

        try {
            PAP pap = new PAP(memoryPolicyStore);
            PolicyReviewer reviewer = loadPolicyReviewerWithoutAccountUA(ctx, pap);

            AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, BLOSSOM_TARGET);

            boolean result = accessRights.contains("sign_mou");
            if (!result) {
                throw new ChaincodeException("cid is not authorized to sign the MOU");
            }

            pap.executePMLFunction(userCtx, "signMOU",
                                   new StringValue(ctx.getClientIdentity().getMSPID())
            );

            // remove user before committing to the ledger
            memoryPolicyStore.graph().deleteNode(userCtx.getUser());

            ctx.getStub().putState("policy", memoryPolicyStore.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    /**
     * Check if the cid has "write_ato" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param account The account id.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void writeATO(Context ctx, String account) {
        String target = accountObjectNodeName(account);
        decide(ctx, target, "write_ato", "cid is not authorized to write the ATO for account " + account);
    }

    /**
     * Check if the cid has "submit_feedback" on <account> target.
     *
     * @param ctx The Fabric context.
     * @param targetMember The target of the feedback.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void submitFeedback(Context ctx, String targetMember) {
        String target = accountObjectNodeName(targetMember);
        decide(ctx, target, "submit_feedback", "cid is not authorized to submit feedback on " + targetMember);
    }

    /**
     * Check if the cid has "initiate_vote" on <account> target. If yes, invoke the initiateVote function.
     *
     * @param ctx          Chaincode context.
     * @param targetMember The target member of the vote.
     *
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void initiateVote(Context ctx, String targetMember) {
        String target = accountObjectNodeName(targetMember);
        decideAndRespond(ctx, target, "initiate_vote", "cid is not authorized to initiate a vote on " + targetMember,
                         "initiateVote",
                         new StringValue(ctx.getClientIdentity().getMSPID()),
                         new StringValue(targetMember));
    }

    /**
     * Check if the cid has "vote" on the vote object.
     *
     * @param ctx Chaincode context.
     * @param targetMember The target member of the vote.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void vote(Context ctx, String targetMember) {
        decide(ctx, voteObj(targetMember), "vote",
               "cid is not authorized to vote on " + targetMember);
    }

    /**
     * Check if the cid has "certify_vote" on the vote object. if yes, invoke the endVote function.
     *
     * @param ctx Chaincode context.
     * @param targetMember The target member of the vote.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void certifyVote(Context ctx, String targetMember) {
        String target = voteObj(targetMember);
        decideAndRespond(ctx, target, "certify_vote", "cid is not authorized to certify a vote on " + targetMember,
                         "certifyVote",
                         new StringValue(targetMember));
    }

    /**
     * Load the policy from the context into memory.
     * @param ctx The Fabric context.
     * @param userCtx The user context representing the cid.
     * @return The policy in memory.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public static MemoryPolicyStore loadPolicy(Context ctx, UserContext userCtx) {
        byte[] policy = ctx.getStub().getState("policy");
        if (policy.length == 0) {
            throw new ChaincodeException("ngac policy has not been initialized");
        }

        String json = new String(policy, StandardCharsets.UTF_8);

        try {
            MemoryPolicyStore memoryPolicyStore = new MemoryPolicyStore();
            memoryPolicyStore.deserialize(userCtx, json, new JSONDeserializer());

            return memoryPolicyStore;
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private void decide(Context ctx, String target, String ar, String unauthMessage) {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);

        try {
            PAP pap = new PAP(memoryPolicyStore);
            PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);

            AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

            boolean result = accessRights.contains(ar);
            if (!result) {
                throw new ChaincodeException(unauthMessage);
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private void decideAndRespond(Context ctx, String target, String ar, String unauthMessage, String function, Value ... args) {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, userCtx);

        try {
            PAP pap = new PAP(memoryPolicyStore);
            PolicyReviewer reviewer = loadPolicyReviewer(ctx, pap);

            AccessRightSet accessRights = reviewer.access().computePrivileges(userCtx, target);

            boolean result = accessRights.contains(ar);
            if (!result) {
                throw new ChaincodeException(unauthMessage);
            }

            pap.executePMLFunction(userCtx, function, args);

            // remove user before committing to the ledger
            pap.graph().deleteNode(userCtx.getUser());

            // save policy updates
            ctx.getStub().putState("policy", pap.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private String accountUsersNodeName(String mspid) {
        return mspid + " users";
    }

    private String accountObjectNodeName(String mspid) {
        return mspid + " target";
    }

    private String voteObj(String targetMember) {
        return targetMember + " vote";
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
            throw new ChaincodeException(e);
        }

        return user + ":" + mspid;
    }

    public static UserContext getUserCtxFromRequest(Context ctx) {
        return new UserContext(getNGACUserName(ctx));
    }

    private PolicyReviewer loadPolicyReviewer(Context ctx, PAP policy) {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);
        String accountUA = accountUsersNodeName(mspid);

        try {
            if (!policy.graph().nodeExists(role)) {
                throw new ChaincodeException("unknown user role: " + role);
            } else if (!policy.graph().nodeExists(accountUA)) {
                throw new ChaincodeException("account " + mspid + " does not exist");
            }

            // create the calling user in the graph and assign to appropriate attributes
            policy.graph().createUser(ngacUserName, accountUA, role);

            // check if user is blossom admin
            if (getADMINMSP(policy).equals(mspid) && role.equals(AUTHORIZING_OFFICIAL)) {
                policy.graph().assign(ngacUserName, "Blossom Admin");
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }

        // create a new PolicyReviewer object to make a decision
        return new PolicyReviewer(policy);
    }

    private PolicyReviewer loadPolicyReviewerWithoutAccountUA(Context ctx, PAP policy) {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        try {
            if (!policy.graph().nodeExists(role)) {
                throw new ChaincodeException("unknown user role: " + role);
            }

            // create the user in the policy
            policy.graph().createUser(ngacUserName, role);

            // a user is a Blossom Admin if 1) they are in the admin msp and 2) they are a Authorizing Official
            if (getADMINMSP(policy).equals(mspid) && role.equals(AUTHORIZING_OFFICIAL)) {
                policy.graph().assign(ngacUserName, "Blossom Admin");
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }

        // create a new PolicyReviewer object to make a decision
        return new PolicyReviewer(policy);
    }
}
