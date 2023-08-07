package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import model.Account;
import model.Status;
import model.Vote;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static contract.AccountContract.accountKey;
import static ngac.BlossomPDP.getAdminMSPID;

@Contract(
        name = "vote",
        info = @Info(title = "Blossom Authorization Vote Contract", version = "0.0.1")
)
public class VoteContract {

    public static final String VOTE_PREFIX = "vote:";
    
    private BlossomPDP pdp = new BlossomPDP();

    /**
     * Initiate a vote to change the status of a Blossom member.
     *
     * NGAC: Only users with the "System Owner" attribute can initiate a vote. Any member can initiate a vote on another
     * member, including the Blossom Admin member, as long as they have an "AUTHORIZED" status. Members with pending or
     * unauthorized statuses will only be able to initiate a vote on themselves. Votes on the Blossom Admin require a
     * super majority (> 2/3) to pass, while all other members require a simple majority (> 1/2) to pass.
     *
     * The account that initiates the vote will be assigned to the vote initiator role for this vote. This will give them
     * the ability to complete or delete the vote.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param targetMSPID The MSPID of the target of the vote.
     * @param statusChange A string representation of the intended status change if the vote is successful.
     * @param reason The reason for initiating the vote. Can be null or empty.
     * @throws PMException If the requesting CID does not have permission to initiate a vote on the target member.
     * @throws ChaincodeException If the target member does not exist.
     * @throws ChaincodeException If there is already an ongoing vote for the target member.
     */
    @Transaction
    public void InitiateVote(Context ctx, String targetMSPID,
                             String statusChange, String reason) throws PMException {
        String initiatorMSPID = ctx.getClientIdentity().getMSPID();
        String adminmsp = getAdminMSPID(ctx);

        // check that the target member exists
        checkTargetMemberExists(ctx, targetMSPID);

        // check that there is not an ongoing vote for the target member
        String ongoingVoteID = getOngoingVoteForMember(ctx, targetMSPID);
        if (ongoingVoteID != null) {
            throw new ChaincodeException("member=" + targetMSPID + " already has an ongoing vote with id=" + ongoingVoteID);
        }

        // validate the vote status change
        Status status = Status.fromString(statusChange);

        // determine the correct threshold for vote
        Vote.Threshold threshold = Vote.Threshold.MAJORITY;
        if (targetMSPID.equals(adminmsp)) {
            threshold = Vote.Threshold.SUPER_MAJORITY;
        }

        // votes are identified by the initiating tx's txid
        String id = ctx.getStub().getTxId();

        // notify ngac of vote initiation, also checks the user has permission
        pdp.initiateVote(ctx, id, targetMSPID);

        Vote vote = new Vote(id, initiatorMSPID, targetMSPID, status,
                reason, threshold, 0, Vote.Result.ONGOING);

        ctx.getStub().putState(voteKey(id, targetMSPID), SerializationUtils.serialize(vote));
    }

    /**
     * Complete an ongoing vote. There are three possible outcomes:
     *      - Vote passes: There are enough "yes" votes and the target targetMember's status is updated and the vote is closed.
     *      - Vote fails: There are not enough "yes" votes and the target targetMember's status is unchanged and the vote is closed.
     *      - Not enough votes to decide: There are not enough "yes" or "no" votes to meet the needed threshold for the vote. The status is unchanged and the vote is still ongoing.
     *
     * NGAC: Only the Blossom Admin and vote initiator have the permission to complete a vote. However, in both cases
     * if the account is pending or unauthorized they will be unable to complete a vote.
     *
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param id The id of the vote to complete.
     * @param targetMember The target member of the vote.
     * @return true if the vote passed, false otherwise.
     * @throws VoteDoesNotExistException If the vote does not exist.
     * @throws VoteHasAlreadyBeenCompletedException If the vote does not exist.
     * @throws ChaincodeException If the vote does not have enough votes for a decision.
     * @throws PMException If the requesting CID cannot complete the vote.
     */
    @Transaction
    public boolean CompleteVote(Context ctx, String id, String targetMember) throws PMException {
        if (!voteExists(ctx, id, targetMember)) {
            throw new VoteDoesNotExistException(id, targetMember);
        } else if (voteCompleted(ctx, id, targetMember)) {
            throw new VoteHasAlreadyBeenCompletedException(id, targetMember);
        }

        // check the requesting cid can complete this vote
        pdp.completeVote(ctx, id, targetMember);

        // retrieve the vote
        Vote vote = GetVote(ctx, id, targetMember);

        // tally votes
        AccountContract accountContract = new AccountContract();
        List<Account> members = accountContract.GetAccounts(ctx);
        int total = members.size();
        int yes = 0;
        int no = 0;

        // Note: Member votes are stored in implicit PDC so other members won't have their vote
        // copied on their peer nodes, BUT they could still see the vote if the call getPrivateData
        // because implicitDataCollections are memberOnlyRead=false.
        // If execution has reached here then the requesting cid has permission to complete the vote
        // implying permission to see the vote in each members implicit PDC
        for (Account account : members) {
            String collection = getAccountImplicitDataCollection(account.getId());
            byte[] bytes = ctx.getStub().getPrivateData(collection, voteKey(id, targetMember));
            if (bytes == null) {
                continue;
            }

            String v = new String(bytes);
            if (v.equals("true")) {
                yes++;
            } else {
                no++;
            }
        }

        boolean passed = false;
        if (Vote.passed(yes, total, vote.getThreshold())) {
            passed = true;
            handlePassedVote(ctx, id, targetMember, vote.getStatusChange(), vote);
        } else if (Vote.failed(yes, no, total, vote.getThreshold())) {
            handleFailedVote(ctx, id, targetMember, vote);
        } else {
            throw new ChaincodeException("not enough votes for a result");
        }

        return passed;
    }

    /**
     * Delete an ongoing vote. If a vote is deleted, the proposed status change will not take effect. Votes cannot be
     * deleted if they have already been completed using CompleteVote.
     *
     * NGAC: Only the member that initiated the vote can delete a vote. The Blossom Admin cannot delete a vote, unless
     * they initiated it.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param id The ID of the vote to delete.
     * @param targetMember The target of the vote.
     * @throws VoteDoesNotExistException If the vote does not exist.
     * @throws VoteHasAlreadyBeenCompletedException If the vote has already been completed.
     * @throws PMException If the requesting CID cannot delete the vote.
     */
    @Transaction
    public void DeleteVote(Context ctx, String id, String targetMember) throws PMException {
        if (!voteExists(ctx, id, targetMember)) {
            throw new VoteDoesNotExistException(id, targetMember);
        } else if (voteCompleted(ctx, id, targetMember)) {
            throw new VoteHasAlreadyBeenCompletedException(id, targetMember);
        }

        // check the requesting cid can delete the given vote
        pdp.deleteVote(ctx, id, targetMember);

        // delete vote
        ctx.getStub().delState(voteKey(id, targetMember));
    }

    /**
     * Vote as the requesting CID's MSPID for a vote with the given ID and target member. Members can overwrite their
     * votes as long as the vote has not been completed yet. Votes are stored in each member's implicit data collection.
     *
     * NGAC: Only users with the "System Owner" attribute can vote. All members can vote. Even members with pending or
     * unauthorized statuses.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param id The ID of the vote.
     * @param targetMember The target of the vote.
     * @param value The requesting CID's vote. True is "yes" and false is "no".
     * @throws VoteDoesNotExistException If the vote does not exist.
     * @throws VoteHasAlreadyBeenCompletedException If the vote has already been completed.
     * @throws PMException If the requesting CID cannot vote.
     */
    @Transaction
    public void Vote(Context ctx, String id, String targetMember, boolean value) throws PMException {
        //check vote exists and is not completed
        if (!voteExists(ctx, id, targetMember)) {
            throw new VoteDoesNotExistException(id, targetMember);
        } else if (voteCompleted(ctx, id, targetMember)) {
            throw new VoteHasAlreadyBeenCompletedException(id, targetMember);
        }

        pdp.vote(ctx, id, targetMember);

        // get the mspid from the requesting cid and corresponding implicit PDC
        String mspid = ctx.getClientIdentity().getMSPID();
        String collection = getAccountImplicitDataCollection(mspid);

        // check if this members has already voted
        // this doesn't prevent them from changing their vote, as long as the vote is still ongoing
        boolean voted = false;
        byte[] bytes = ctx.getStub().getPrivateData(collection, voteKey(id, targetMember));
        if (bytes != null) {
            voted = true;
        }

        // record vote in msp's private data collection
        String s = String.valueOf(value);
        ctx.getStub().putPrivateData(collection, voteKey(id, targetMember), s.getBytes(StandardCharsets.UTF_8));

        Vote vote = GetVote(ctx, id, targetMember);

        // only increment the vote count if the member hasn't voted yet
        if (!voted) {
            vote.setCount(vote.getCount()+1);
        }

        // update vote
        ctx.getStub().putState(voteKey(id, targetMember), SerializationUtils.serialize(vote));
    }

    /**
     * Get a vote with a given ID and target member. The "yes" and "no" tallies as well as how each member voted will
     * not be returned. Only the total vote count will be returned.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param id The ID of the vote.
     * @param member The target member.
     * @return The vote with the given ID and target member.
     * @throws VoteDoesNotExistException If the vote does not exist.
     */
    @Transaction
    public Vote GetVote(Context ctx, String id, String member) {
        if (!voteExists(ctx, id, member)) {
            throw new VoteDoesNotExistException(id, member);
        }

        byte[] state = ctx.getStub().getState(voteKey(id, member));

        return SerializationUtils.deserialize(state);
    }

    /**
     * Get all votes. The "yes" and "no" tallies as well as how each member voted will
     * not be returned. Only the total vote count will be returned.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @return A list of all votes.
     * @throws ChaincodeException If there is an issue with iterating over the votes from the world state.
     */
    @Transaction
    public List<Vote> GetVotes(Context ctx) {
        List<Vote> votes = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(VOTE_PREFIX, VOTE_PREFIX)) {
            for (KeyValue kv : stateByRange) {
                byte[] value = kv.getValue();
                votes.add(SerializationUtils.deserialize(value));
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return votes;
    }

    /**
     * Get all ongoing votes. The "yes" and "no" tallies as well as how each member voted will
     * not be returned. Only the total vote count will be returned.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @return A list of all ongoing votes.
     * @throws ChaincodeException If there is an issue with iterating over the votes from the world state.
     */
    @Transaction
    public List<Vote> GetOngoingVotes(Context ctx) {
        List<Vote> votes = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(VOTE_PREFIX, VOTE_PREFIX)) {
            for (KeyValue kv : stateByRange) {
                byte[] value = kv.getValue();
                Vote vote = SerializationUtils.deserialize(value);
                if (vote.getResult() != Vote.Result.ONGOING) {
                    continue;
                }

                votes.add(vote);
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return votes;
    }

    /**
     * Get all votes for a given member that have ever occurred. The "yes" and "no" tallies as well as how each member voted will
     * not be returned. Only the total vote count will be returned.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param mspid The MSPID to get all votes for.
     * @return A list of all votes that have ever occurred for the given member.
     * @throws ChaincodeException If there is an issue with iterating over the votes from the world state.
     */
    @Transaction
    public List<Vote> GetVotesForMember(Context ctx, String mspid) {
        checkTargetMemberExists(ctx, mspid);

        List<Vote> votes = new ArrayList<>();

        String key = voteKey("", mspid);
        try (QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(key, key)) {
            for (KeyValue kv : stateByRange) {
                byte[] value = kv.getValue();
                votes.add(SerializationUtils.deserialize(value));
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return votes;
    }

    /**
     * Get the ongoing vote for the given member. Throws exception if no ongoing vote is found. The "yes" and "no" tallies
     * as well as how each member voted will not be returned. Only the total vote count will be returned.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param mspid
     * @return The ongoing vote fot the target member
     * @throws ChaincodeException If there is no ongoing vote for the given member.
     */
    @Transaction
    public Vote GetOngoingVoteForMember(Context ctx, String mspid) {
        checkTargetMemberExists(ctx, mspid);

        List<Vote> votes = GetVotesForMember(ctx, mspid);
        for (Vote vote : votes) {
            if (vote.getResult() == Vote.Result.ONGOING) {
                return vote;
            }
        }

        throw new ChaincodeException("there is no ongoing vote for member=" + mspid);
    }

    private String voteKey(String id, String targetMember) {
        return VOTE_PREFIX + (targetMember != null && !targetMember.isEmpty() ? targetMember + ":" : "") + id;
    }

    private String getAccountImplicitDataCollection(String mspid) {
        return "_implicit_org_" + mspid;
    }

    private void handleFailedVote(Context ctx, String id, String targetMember, Vote vote) {
        // update the vote
        vote.setResult(Vote.Result.FAILED);
        ctx.getStub().putState(voteKey(id, targetMember), SerializationUtils.serialize(vote));
    }

    private void handlePassedVote(Context ctx, String id, String targetMember, Status status, Vote vote) {
        AccountContract accountContract = new AccountContract();

        // update the account
        Account account = accountContract.GetAccount(ctx, targetMember);
        account.setStatus(status);

        ctx.getStub().putState(accountKey(targetMember), SerializationUtils.serialize(account));

        // update the vote
        vote.setResult(Vote.Result.PASSED);
        ctx.getStub().putState(voteKey(id, targetMember), SerializationUtils.serialize(vote));
    }

    private void checkTargetMemberExists(Context ctx, String mspid) {
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        if (bytes == null) {
            throw new ChaincodeException("an account with id " + mspid + " does not exist");
        }
    }

    private String getOngoingVoteForMember(Context ctx, String mspid) {
        try {
            Vote vote = GetOngoingVoteForMember(ctx, mspid);

            // reaching here means there is an active vote
            return vote.getId();
        } catch (ChaincodeException e) {
            return null;
        }
    }

    private boolean voteExists(Context ctx, String id, String member) {
        return ctx.getStub().getState(voteKey(id, member)) != null;
    }

    private boolean voteCompleted(Context ctx, String id, String member) {
        Vote vote = GetVote(ctx, id, member);
        return vote.getResult() != Vote.Result.ONGOING;
    }

    static class VoteDoesNotExistException extends ChaincodeException {
        public VoteDoesNotExistException(String id, String member) {
            super("a vote with id=" + id + " and targetMember=" + member + " does not exist");
        }
    }

    static class VoteHasAlreadyBeenCompletedException extends ChaincodeException {
        public VoteHasAlreadyBeenCompletedException(String id, String member) {
            super("a vote with id=" + id + " and targetMember=" + member + " has already been completed");
        }
    }

}