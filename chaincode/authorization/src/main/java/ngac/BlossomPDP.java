package ngac;

import gov.nist.csd.pm.epp.EPP;
import gov.nist.csd.pm.epp.EventContext;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.PolicyReviewer;
import gov.nist.csd.pm.pdp.memory.MemoryPDP;
import gov.nist.csd.pm.pdp.memory.MemoryPolicyReviewer;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.model.expression.Value;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Transaction;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

public class BlossomPDP {

    public static final String BLOSSOM_SYSTEM = "blossom_system";
    public static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    public static final String VOTES_OA = "votes";

    public String getAdminMSPID(Context ctx) throws PMException {
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        Value value = memoryPolicyStore.userDefinedPML().getConstant("ADMINMSP");
        return value.getStringValue();
    }

    public void requestAccount(Context ctx) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        if (!accessRights.contains("request_account")) {
            throw new PMException("user cannot request account");
        }
    }

    public void approveAccount(Context ctx, String account) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        boolean result = accessRights.contains("approve_account");
        if (!result) {
            throw new PMException("user cannot approve account");
        }

        new BlossomEPP().processApproveAccountEvent(memoryPolicyStore, userCtx, ctx.getClientIdentity().getMSPID());
    }

    public void uploadATO(Context ctx, String account) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, accountObjectNodeName(account));

        if(!accessRights.contains("upload_ato")) {
            throw new PMException("user cannot upload ato");
        }
    }

    public void updateAccountStatus(Context ctx, String account, String status) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, BLOSSOM_SYSTEM);

        boolean result = accessRights.contains("update_account_status");
        if (!result) {
            throw new PMException("user cannot update account status");
        }

        new BlossomEPP().processUpdateAccountStatusEvent(memoryPolicyStore, userCtx, ctx.getClientIdentity().getMSPID(), status);
    }

    public void initiateVote(Context ctx, String id, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, VOTES_OA);

        boolean result = accessRights.contains("initiate_vote");
        if (!result) {
            throw new PMException("user cannot initiate vote");
        }
           
        new BlossomEPP().processInitiateVoteEvent(memoryPolicyStore, userCtx, id, targetMember, ctx.getClientIdentity().getMSPID());
    }

    public void completeVote(Context ctx, String id, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, VOTES_OA);

        if(!accessRights.contains("complete_vote")) {
            throw new PMException("user cannot complete vote");
        }
        
        // event will be processed separately
    }

    public boolean deleteVote(Context ctx, String id, String targetMember) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);
        PolicyReviewer reviewer = loadPolicyReviewer(ctx, memoryPolicyStore);
        AccessRightSet accessRights = reviewer.getAccessRights(userCtx, voteObj(targetMember, id));

        boolean result = accessRights.contains("delete_vote");

        // If user has permission, process the event now. It will be rolled back if an error happens later in the transaction
        if (result) {
            new BlossomEPP().processDeleteVoteEvent(memoryPolicyStore, userCtx, id, targetMember, ctx.getClientIdentity().getMSPID());
        }

        return result;
    }

    static String accountUsersNodeName(String mspid) {
        return mspid + " users";
    }

    static String accountContainerNodeName(String mspid) {
        return mspid + " account attr";
    }

    static String accountObjectNodeName(String mspid) {
        return mspid + " account";
    }

    static String voteUA(String targetMember) {
        return "vote:" + targetMember + "_UA";
    }

    static String voteObj(String targetMember, String voteID) {
        return targetMember + "-" + voteID + " vote";
    }

    public static String getNGACUserName(Context ctx) {
        ClientIdentity clientIdentity = ctx.getClientIdentity();
        X509Certificate cert = clientIdentity.getX509Certificate();
        String user = cert.getSubjectX500Principal().getName();
        String mspid = ctx.getClientIdentity().getMSPID();

        return user + ":" + mspid;
    }

    static UserContext getUserCtxFromRequest(Context ctx) {
        return new UserContext(getNGACUserName(ctx));
    }

    static MemoryPolicyStore loadPolicy(Context ctx) throws PMException {
        byte[] policy = ctx.getStub().getState("policy");
        String json = new String(policy, StandardCharsets.UTF_8);

        MemoryPolicyStore memoryPolicyStore = new MemoryPolicyStore();
        memoryPolicyStore
                .deserialize()
                .fromJSON(json);

        return memoryPolicyStore;
    }

    static PolicyReviewer loadPolicyReviewer(Context ctx, MemoryPolicyStore memoryPolicyStore) throws PMException {
        String ngacUserName = getNGACUserName(ctx);
        String mspid = ctx.getClientIdentity().getMSPID();
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        // create the calling user in the graph and assign to appropriate attributes
        memoryPolicyStore.graph().createUser(ngacUserName, accountUsersNodeName(mspid), role);

        // check if user is blossom admin
        if (ctx.getClientIdentity().getAttributeValue("blossom.admin").equals("true")) {
             memoryPolicyStore.graph().assign(ngacUserName, "BlossomAdmin");
        }

        // create a new PolicyReviewer object to make a decision
        return new MemoryPolicyReviewer(memoryPolicyStore);
    }

}
