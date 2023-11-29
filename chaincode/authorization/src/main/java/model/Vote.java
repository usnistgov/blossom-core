package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Blossom status change vote.
 */
@DataType
public class Vote implements Serializable {

    /**
     * The ID of the vote.
     */
    @Property
    private String id;

    /**
     * The MSPID that initiated the vote.
     */
    @Property
    private String initiatingAccountId;

    /**
     * The target member of the vote.
     */
    @Property
    private String targetAccountId;

    /**
     * The intended status change if the vote passes.
     */
    @Property
    private Status statusChange;

    /**
     * The reason for the status change.
     */
    @Property
    private String reason;

    /**
     * The threshold required for the vote to pass.
     */
    @Property
    private Threshold threshold;

    /**
     * The accounts that can vote.
     */
    @Property
    private List<String> voters;

    /**
     * A record of the votes already cast.
     */
    private Map<String, Boolean> submittedVotes;

    /**
     * Result of the vote.
     */
    @Property
    private Result result;

    public Vote() {
    }

    public Vote(@JsonProperty String id,
                @JsonProperty String initiatingAccountId,
                @JsonProperty String targetAccountId,
                @JsonProperty Status statusChange,
                @JsonProperty String reason,
                @JsonProperty Threshold threshold,
                @JsonProperty List<String> voters,
                @JsonProperty Map<String, Boolean> submittedVotes,
                @JsonProperty Result result) {
        this.id = id;
        this.initiatingAccountId = initiatingAccountId;
        this.targetAccountId = targetAccountId;
        this.statusChange = statusChange;
        this.reason = reason;
        this.threshold = threshold;
        this.voters = voters;
        this.submittedVotes = submittedVotes;
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInitiatingAccountId() {
        return initiatingAccountId;
    }

    public void setInitiatingAccountId(String initiatingAccountId) {
        this.initiatingAccountId = initiatingAccountId;
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(String targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public Status getStatusChange() {
        return statusChange;
    }

    public void setStatusChange(Status statusChange) {
        this.statusChange = statusChange;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Threshold getThreshold() {
        return threshold;
    }

    public void setThreshold(Threshold threshold) {
        this.threshold = threshold;
    }

    public List<String> getVoters() {
        return voters;
    }

    public void setVoters(List<String> voters) {
        this.voters = voters;
    }

    public Map<String, Boolean> getSubmittedVotes() {
        return submittedVotes;
    }

    public void setSubmittedVotes(Map<String, Boolean> submittedVotes) {
        this.submittedVotes = submittedVotes;
    }

    public void submitVote(String accountId, boolean value) {
        this.submittedVotes.put(accountId, value);
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public boolean passed() {
        int total = voters.size();
        int yes = calcNumYesVotes();

        return ((double) yes / total) > threshold.value;
    }

    public boolean failed() {
        int total = voters.size();
        int numVotes = submittedVotes.size();
        int yes = calcNumYesVotes();

        boolean isMajority = (double) yes / total > threshold.value;
        boolean majorityPossible = (double) (total-numVotes+yes)/total > threshold.value;

        return !isMajority && !majorityPossible;
    }

    private int calcNumYesVotes() {
        int yes = 0;
        for (Boolean vote : submittedVotes.values()) {
            if (vote) {
                yes++;
            }
        }

        return yes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Vote vote = (Vote) o;
        return Objects.equals(id, vote.id) && Objects.equals(
                initiatingAccountId,
                vote.initiatingAccountId
        ) && Objects.equals(
                targetAccountId,
                vote.targetAccountId
        ) && statusChange == vote.statusChange && Objects.equals(
                reason,
                vote.reason
        ) && threshold == vote.threshold && Objects.equals(voters, vote.voters) && Objects.equals(
                submittedVotes,
                vote.submittedVotes
        ) && result == vote.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                initiatingAccountId,
                targetAccountId,
                statusChange,
                reason,
                threshold,
                voters,
                submittedVotes,
                result
        );
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id='" + id + '\'' +
                ", initiatingAccountId='" + initiatingAccountId + '\'' +
                ", targetAccountId='" + targetAccountId + '\'' +
                ", statusChange=" + statusChange +
                ", reason='" + reason + '\'' +
                ", threshold=" + threshold +
                ", voters=" + voters +
                ", submittedVotes=" + submittedVotes +
                ", result=" + result +
                '}';
    }

    /**
     * Threshold for a vote
     */
    public enum Threshold {
        /**
         * Majority requires > 50%
         */
        MAJORITY(.5),

        /**
         * Super majority requires > 66%
         */
        SUPER_MAJORITY(.66);

        private double value;

        Threshold(double value) {
            this.value = value;
        }
    }

    /**
     * Possible results of votes
     */
    public enum Result {
        /**
         * The vote has not yet been completed
         */
        ONGOING,
        /**
         * The vote passed
         */
        PASSED,
        /**
         * The vote failed
         */
        FAILED,
        /**
         * The vote was aborted before certification
         */
        ABORTED
    }
}
