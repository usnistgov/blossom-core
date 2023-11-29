package contract;

import model.Account;
import model.Status;
import model.Vote;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static contract.AccountContract.accountKey;
import static model.Status.AUTHORIZED;

/**
 * Chaincode functions to support voting on account statuses.
 */
@Contract(
        name = "vote",
        info = @Info(
                title = "Blossom Authorization Vote Contract",
                description = "Chaincode functions to support voting on account statuses",
                version = "0.0.1"
        )
)
public class VoteContract implements ContractInterface {

    public static final String VOTE_PREFIX = "vote:";
    public static final String ONGOING_VOTE_KEY = "ongoing_vote";

    /**
     * Initiate a vote to change the status of a Blossom member. The initiating member and the ADMINMSP will be able
     * to certify the vote. Only one vote can be ongiong at a time. Votes on the ADMINMSP require a super majority
     * (> 2/3) to pass, while all other members require a simple majority (> 1/2) to pass.
     *
     * NGAC: Only users with the "Authorizing Official" attribute can initiate a vote. Successfully casting a vote also
     * depends on the vote configuration.
     *
     * event:
     *  - name: "InitiateVote"
     *  - payload: a serialized Vote object
     *
     * @param ctx Fabric context object.
     * @param targetAccountId The MSPID of the target of the vote.
     * @param statusChange A string representation of the intended status change if the vote is successful.
     * @param reason The reason for initiating the vote. Can be null or empty.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the target member does not exist.
     * @throws ChaincodeException If there is already an ongoing vote.
     */
    @Transaction
    public void InitiateVote(Context ctx, String targetAccountId,
                             String statusChange, String reason) {
        BlossomPDP pdp = new BlossomPDP();
        String initiatorAccountId = ctx.getClientIdentity().getMSPID();
        String adminmsp = pdp.getADMINMSP(ctx);
        String voteId = ctx.getStub().getTxId();
        String voteKey = voteKey(targetAccountId);

        // check that there is not an ongoing vote
        Vote ongoingVote = getOngoingVote(ctx);
        if (ongoingVote != null) {
            throw new ChaincodeException("there is an ongoing vote on account " + ongoingVote.getTargetAccountId());
        }

        // check that the target member exists
        checkTargetMemberExists(ctx, targetAccountId);

        // check user can initiate and initiate vote in policy
        pdp.initiateVote(ctx, targetAccountId);

        // validate the vote status change
        Status status = Status.fromString(statusChange);

        // determine the correct threshold for vote
        Vote.Threshold threshold = Vote.Threshold.MAJORITY;
        if (targetAccountId.equals(adminmsp)) {
            threshold = Vote.Threshold.SUPER_MAJORITY;
        }

        // identify voter pool -- all authorized users at initiate time
        List<Account> accounts = new AccountContract().GetAccounts(ctx);
        List<String> voters = new ArrayList<>(List.of(targetAccountId));
        for (Account account : accounts) {
            if (account.getStatus() != AUTHORIZED || account.getId().equals(targetAccountId)) {
                continue;
            }

            voters.add(account.getId());
        }

        // if there are no authorized voters, only votes on the blossom admin are allowed
        if (voters.size() == 1 && !targetAccountId.equals(adminmsp)) {
            throw new ChaincodeException("only votes on the ADMINMSP " + adminmsp + " are allowed when there are no authorized members");
        }

        Vote vote = new Vote(voteId, initiatorAccountId, targetAccountId, status,
                             reason, threshold, voters, new HashMap<>(), Vote.Result.ONGOING);

        // add vote and ongoing vote to ws
        byte[] bytes = SerializationUtils.serialize(vote);
        ctx.getStub().putState(voteKey, bytes);
        ctx.getStub().putStringState(ONGOING_VOTE_KEY, targetAccountId);

        // send event
        ctx.getStub().setEvent("InitiateVote", bytes);
    }

    /**
     * Vote as the requesting CID's MSPID for a vote with the given ID and target member. Members can overwrite their
     * votes as long as the vote has not been certified yet. How members vote will be available in the world state.
     *
     * NGAC: Only users with the "Authorizing Official" attribute can vote. Only authorized members at the time of
     * initiation are abel to vote.
     *
     * event:
     *  - name: "Vote"
     *  - payload: a serialized Vote object
     *
     * @param ctx Fabric context object.
     * @param value The requesting CID's vote. True is "yes" and false is "no".
     * @throws ChaincodeException If there is no ongoing vote.
     * @throws ChaincodeException If the requesting CID cannot vote.
     */
    @Transaction
    public void Vote(Context ctx, boolean value) {
        String voterId = ctx.getClientIdentity().getMSPID();
        Vote vote = GetOngoingVote(ctx);

        // check if the cid can vote
        new BlossomPDP().vote(ctx, vote.getTargetAccountId());

        // record the vote
        vote.submitVote(voterId, value);

        // update vote in ws
        byte[] bytes = SerializationUtils.serialize(vote);
        ctx.getStub().putState(voteKey(vote.getTargetAccountId()), bytes);

        // send event
        ctx.getStub().setEvent("Vote", bytes);
    }

    /**
     * Certify a vote by checking if the cast ballots lead to a pass or fail. An exception will be thrown if there are not
     * enough votes to pass or fail.
     *
     * NGAC: Only the vote initiator or ADMINMSP can certify a vote. The ADMINMSP must be AUTHORIZED to certify.
     *
     * @param ctx Fabric context object.
     * @return true if the vote passed, false if it failed.
     * @throws ChaincodeException if there is no ongoing vote.
     * @throws ChaincodeException if there are not enough votes to certify.
     */
    @Transaction
    public boolean CertifyOngoingVote(Context ctx) {
        Vote ongoingVote = getOngoingVote(ctx);
        if (ongoingVote == null) {
            throw new ChaincodeException("there is no ongoing vote");
        }

        // check that the user can certify the vote
        new BlossomPDP().certifyVote(ctx, ongoingVote.getTargetAccountId());

        boolean passed;
        if (ongoingVote.passed()) {
            passed = true;
            handlePassedVote(ctx, ongoingVote);
        } else if (ongoingVote.failed()) {
            passed = false;
            handleFailedVote(ctx, ongoingVote);
        } else {
            throw new ChaincodeException("not enough votes to certify");
        }

        return passed;
    }

    /**
     * Get the ongoing vote.
     *
     * NGAC: none.
     *
     * @param ctx Fabric context object.
     * @return The vote with the given ID and target member.
     * @throws ChaincodeException If there is no ongoing vote.
     */
    @Transaction
    public Vote GetOngoingVote(Context ctx) {
        Vote ongoingVote = getOngoingVote(ctx);
        if (ongoingVote == null) {
            throw new ChaincodeException("there is no ongoing vote");
        }

        return ongoingVote;
    }

    /**
     * Get the history of votes in which the provided member was the target of.
     * @param ctx Fabric context object.
     * @param member The target member.
     * @return a list of Vote objects.
     */
    @Transaction
    public List<Vote> GetVoteHistory(Context ctx, String member) {
        List<Vote> votes = new ArrayList<>();

        QueryResultsIterator<KeyModification> history = ctx.getStub().getHistoryForKey(voteKey(member));
        for (KeyModification next : history) {
            byte[] value = next.getValue();

            votes.add(SerializationUtils.deserialize(value));
        }

        return votes;
    }

    private Vote getOngoingVote(Context ctx) {
        // ongoing_vote=target mspid
        byte[] bytes = ctx.getStub().getState(ONGOING_VOTE_KEY);
        if (bytes.length == 0) {
            return null;
        }

        String targetMember = new String(bytes);

        String voteKey = voteKey(targetMember);
        bytes = ctx.getStub().getState(voteKey);
        return SerializationUtils.deserialize(bytes);
    }


    private String voteKey(String targetMember) {
        return VOTE_PREFIX + targetMember;
    }

    private void handleFailedVote(Context ctx, Vote vote) {
        // update the vote
        vote.setResult(Vote.Result.FAILED);

        byte[] voteBytes = SerializationUtils.serialize(vote);
        ctx.getStub().putState(voteKey(vote.getTargetAccountId()), voteBytes);
        ctx.getStub().delState(ONGOING_VOTE_KEY);
        ctx.getStub().setEvent("VoteCompleted", voteBytes);
    }

    private void handlePassedVote(Context ctx, Vote vote) {
        String targetMember = vote.getTargetAccountId();

        // update the account
        Account account = new AccountContract().GetAccount(ctx, targetMember);
        account.setStatus(vote.getStatusChange());
        ctx.getStub().putState(accountKey(targetMember), SerializationUtils.serialize(account));

        // update the vote
        vote.setResult(Vote.Result.PASSED);
        byte[] voteBytes = SerializationUtils.serialize(vote);
        ctx.getStub().putState(voteKey(targetMember), voteBytes);
        ctx.getStub().delState(ONGOING_VOTE_KEY);
        ctx.getStub().setEvent("VoteCompleted", voteBytes);
    }

    private void checkTargetMemberExists(Context ctx, String mspid) {
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        if (bytes.length == 0) {
            throw new ChaincodeException("an account with id " + mspid + " does not exist");
        }
    }
}
