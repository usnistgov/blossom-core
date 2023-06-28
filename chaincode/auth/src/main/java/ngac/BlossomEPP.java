package ngac;

import gov.nist.csd.pm.epp.EPP;
import gov.nist.csd.pm.epp.EventContext;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.memory.MemoryPDP;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import model.Status;
import org.hyperledger.fabric.contract.Context;

import static ngac.BlossomPDP.getUserCtxFromRequest;
import static ngac.BlossomPDP.loadPolicy;

public class BlossomEPP {

    void processApproveAccountEvent(MemoryPolicyStore memoryPolicyStore, UserContext userCtx, String account) throws PMException {
        EPP epp = buildEPP(memoryPolicyStore);
        epp.handlePolicyEvent(new EventContext(userCtx,
                new ApproveAccountEvent(account)
        ));
    }

    void processUpdateAccountStatusEvent(MemoryPolicyStore memoryPolicyStore, UserContext userCtx, String account, String status) throws PMException {
        EPP epp = buildEPP(memoryPolicyStore);
        epp.handlePolicyEvent(new EventContext(userCtx,
                new UpdateAccountStatusEvent(account, status)
        ));
    }

    void processInitiateVoteEvent(MemoryPolicyStore memoryPolicyStore, UserContext userCtx, String id, String targetMember, String initiatorMSP) throws PMException {
        EPP epp = buildEPP(memoryPolicyStore);
        epp.handlePolicyEvent(new EventContext(userCtx,
                new VoteEvent("initiate_vote", initiatorMSP, id, targetMember, false, ""))
        );
    }

    public void processCompleteVote(Context ctx, String id, String targetMember, boolean passed, String status) throws PMException {
        UserContext userCtx = getUserCtxFromRequest(ctx);
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);

        EPP epp = buildEPP(memoryPolicyStore);
        epp.handlePolicyEvent(new EventContext(userCtx,
                new VoteEvent("complete_vote", ctx.getClientIdentity().getMSPID(), id, targetMember, passed, status))
        );
    }

    void processDeleteVoteEvent(MemoryPolicyStore memoryPolicyStore, UserContext userCtx, String id, String targetMember, String initiatorMSP) throws PMException {
        EPP epp = buildEPP(memoryPolicyStore);
        epp.handlePolicyEvent(new EventContext(userCtx,
                new VoteEvent("delete_vote", initiatorMSP, id, targetMember, false, ""))
        );
    }

    private EPP buildEPP(MemoryPolicyStore memoryPolicyStore) throws PMException {
        PAP pap = new PAP(memoryPolicyStore);
        PDP pdp = new MemoryPDP(pap, false);
        return new EPP(pdp, pap);
    }
}
