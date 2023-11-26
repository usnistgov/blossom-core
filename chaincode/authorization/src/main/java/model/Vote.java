package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
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
    private String targetMember;

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
     * The current number of members that have cast their vote.
     */
    @Property
    private int count;

    /**
     * Result of the vote.
     */
    @Property
    private Result result;

    public Vote() {
    }

    public Vote(@JsonProperty String id, @JsonProperty String initiatingAccountId, @JsonProperty String targetMember,
                @JsonProperty Status statusChange, @JsonProperty String reason, @JsonProperty Threshold threshold,
                @JsonProperty int count, @JsonProperty Result result) {
        this.id = id;
        this.initiatingAccountId = initiatingAccountId;
        this.targetMember = targetMember;
        this.statusChange = statusChange;
        this.reason = reason;
        this.threshold = threshold;
        this.count = count;
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

    public String getTargetMember() {
        return targetMember;
    }

    public void setTargetMember(String targetMember) {
        this.targetMember = targetMember;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public static boolean passed(int yes, int total, Threshold threshold) {
        return ((double) yes / total) > threshold.value;
    }

    public static boolean failed(int yes, int no, int total, Threshold threshold) {
        int numVotes = yes + no;

        boolean isMajority = (double) yes / total > threshold.value;
        boolean majorityPossible = (double) (total-numVotes+yes)/total > threshold.value;

        return !isMajority && !majorityPossible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vote vote = (Vote) o;
        return count == vote.count && Objects.equals(id, vote.id) && Objects.equals(initiatingAccountId, vote.initiatingAccountId) && Objects.equals(targetMember, vote.targetMember) && statusChange == vote.statusChange && Objects.equals(reason, vote.reason) && threshold == vote.threshold && result == vote.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, initiatingAccountId, targetMember, statusChange, reason, threshold, count, result);
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id='" + id + '\'' +
                ", initiatingMSPID='" + initiatingAccountId + '\'' +
                ", targetMember='" + targetMember + '\'' +
                ", statusChange=" + statusChange +
                ", reason='" + reason + '\'' +
                ", threshold=" + threshold +
                ", count=" + count +
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
