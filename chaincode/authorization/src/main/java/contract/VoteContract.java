package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import model.Account;
import model.Status;
import model.Vote;
import ngac.BlossomEPP;
import ngac.BlossomPDP;
import org.bouncycastle.asn1.cmc.GetCert;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static contract.AuthorizationContract.ACCOUNT_PREFIX;
import static contract.AuthorizationContract.accountKey;

public class VoteContract {

    public static final String VOTE_PREFIX = "vote:";

    @Transaction
    public void InitiateVote(Context ctx, String member,
                             String statusChangeStr, String reason) throws Exception {
        Status status = Status.fromString(statusChangeStr);
        String mspid = ctx.getClientIdentity().getMSPID();
        BlossomPDP pdp = new BlossomPDP();

        Vote.Threshold threshold = Vote.Threshold.MAJORITY;
        String adminmsp = pdp.getAdminMSPID(ctx);
        if (mspid.equals(adminmsp)) {
            threshold = Vote.Threshold.SUPER_MAJORITY;
        }

        String id = ctx.getStub().getTxId();
        new BlossomPDP().initiateVote(ctx, id, member);

        Vote vote = new Vote(id, mspid, member, status,
                reason, threshold, 0, Vote.Result.ONGOING);

        List<String> votesForMember = GetVotesForMember(ctx, member);
        if (!votesForMember.isEmpty()) {
            throw new Exception("a vote already exists for member " + member);
        }

        ctx.getStub().putState(voteKey(id, member), vote.toBytes());
    }

    @Transaction
    public boolean CompleteVote(Context ctx, String id, String member) throws Exception {
        new BlossomPDP().completeVote(ctx, id, member);

        String voteJson = GetVote(ctx, id, member);
        Vote vote = Vote.fromJson(voteJson);

        AuthorizationContract authorizationContract = new AuthorizationContract();
        List<String> members = authorizationContract.GetAccounts(ctx);
        int total = members.size();
        int yes = 0;
        int no = 0;

        for (String accountStr : members) {
            Account account = Account.fromJson(accountStr);
            String collection = getAccountImplicitDataCollection(account.getName());

            byte[] bytes = ctx.getStub().getPrivateData(collection, voteKey(id, member));
            String v = new String(bytes);
            if (v.equals("yes")) {
                yes++;
            } else {
                no++;
            }
        }

        boolean passed = false;
        if (Vote.passed(yes, total, vote.getThreshold())) {
            passed = true;
            handlePassedVote(ctx, id, member, vote.getStatusChange());
        } else if (Vote.failed(yes, no, total, vote.getThreshold())) {
            handleFailedVote(ctx, id, member);
        } else {
            throw new Exception("not enough votes for a result");
        }

        return passed;
    }

    @Transaction
    public void DeleteVote(Context ctx, String id, String member) throws PMException {
        new BlossomPDP().deleteVote(ctx, id, member);

        ctx.getStub().delState(voteKey(id, member));
    }

    @Transaction
    public String GetVote(Context ctx, String id, String member) throws Exception {
        byte[] bytes = ctx.getStub().getState(voteKey(id, member));
        if (bytes == null) {
            throw new Exception("a vote with id=" + id + " and targetMember=" + member + " does not exist");
        }

        return Vote.fromBytes(bytes).toJson();
    }

    @Transaction
    public List<String> GetVotes(Context ctx) throws Exception {
        List<String> votes = new ArrayList<>();

        QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(VOTE_PREFIX, VOTE_PREFIX);
        for (KeyValue kv : stateByRange) {
            byte[] value = kv.getValue();
            votes.add(Vote.fromBytes(value).toJson());
        }

        stateByRange.close();

        return votes;
    }

    @Transaction
    public List<String> GetVotesForMember(Context ctx, String mspid) throws Exception {
        List<String> votes = new ArrayList<>();

        String key = voteKey(mspid, "");
        QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(key, key);
        for (KeyValue kv : stateByRange) {
            byte[] value = kv.getValue();
            votes.add(Vote.fromBytes(value).toJson());
        }

        stateByRange.close();

        return votes;
    }

    @Transaction
    public void Vote(Context ctx, String id, String targetMember, boolean value) throws Exception {
        String mspid = ctx.getClientIdentity().getMSPID();
        String collection = getAccountImplicitDataCollection(mspid);

        boolean voted = false;
        byte[] bytes = ctx.getStub().getPrivateData(collection, voteKey(id, targetMember));
        if (bytes != null) {
            voted = true;
        }

        // record vote in msp's private data collection
        String s = String.valueOf(value);
        ctx.getStub().putPrivateData(collection, voteKey(id, targetMember), s.getBytes(StandardCharsets.UTF_8));

        String voteJson = GetVote(ctx, id, targetMember);
        Vote vote = Vote.fromJson(voteJson);

        if (!voted) {
            vote.setCount(vote.getCount()+1);
        }

        ctx.getStub().putState(voteKey(id, targetMember), vote.toBytes());
    }

    private String voteKey(String id, String targetMember) {
        return VOTE_PREFIX + targetMember + ":" + id;
    }

    private String getAccountImplicitDataCollection(String mspid) {
        return "_implicit_org_" + mspid;
    }

    private void handleFailedVote(Context ctx, String id, String targetMember) throws Exception {
        String voteJson = GetVote(ctx, id, targetMember);
        Vote vote = Vote.fromJson(voteJson);

        vote.setResult(Vote.Result.FAILED);
        ctx.getStub().putState(voteKey(id, targetMember), vote.toBytes());

        new BlossomEPP().processCompleteVote(ctx, id, targetMember, false, "");
    }

    private void handlePassedVote(Context ctx, String id, String targetMember, Status status) throws Exception {
        AuthorizationContract authorizationContract = new AuthorizationContract();

        // update the account
        String acctJson = authorizationContract.GetAccount(ctx, targetMember);
        Account account = Account.fromJson(acctJson);
        account.setStatus(status);

        ctx.getStub().putState(accountKey(targetMember), account.toBytes());

        // update the vote
        String voteJson = GetVote(ctx, id, targetMember);
        Vote vote = Vote.fromJson(voteJson);
        vote.setResult(Vote.Result.PASSED);

        ctx.getStub().putState(voteKey(id, targetMember), vote.toBytes());

        new BlossomEPP().processCompleteVote(ctx, id, targetMember, true, status.toString());
    }
}
