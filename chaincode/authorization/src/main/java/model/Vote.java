package model;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Vote implements Serializable {

    @Property
    private String id;

    @Property
    private String initiatingMSP;

    @Property
    private String targetMember;

    @Property
    private Status statusChange;

    @Property
    private String reason;

    @Property
    private Threshold threshold;

    @Property
    private int count;

    @Property
    private Result result;

    public Vote(@JsonProperty String id, @JsonProperty String initiatingMSP, @JsonProperty String targetMember,
                @JsonProperty Status statusChange, @JsonProperty String reason, @JsonProperty Threshold threshold,
                @JsonProperty int count, @JsonProperty Result result) {
        this.id = id;
        this.initiatingMSP = initiatingMSP;
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

    public String getInitiatingMSP() {
        return initiatingMSP;
    }

    public void setInitiatingMSP(String initiatingMSP) {
        this.initiatingMSP = initiatingMSP;
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
        return count == vote.count && Objects.equals(id, vote.id) && Objects.equals(initiatingMSP, vote.initiatingMSP) && Objects.equals(targetMember, vote.targetMember) && statusChange == vote.statusChange && Objects.equals(reason, vote.reason) && threshold == vote.threshold && result == vote.result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, initiatingMSP, targetMember, statusChange, reason, threshold, count, result);
    }

    public enum Threshold {
        MAJORITY(.5),
        SUPER_MAJORITY(.66);

        private double value;

        Threshold(double value) {
            this.value = value;
        }
    }

    public enum Result {
        ONGOING,
        PASSED,
        FAILED
    }

    public enum Option {
        YES,
        NO
    }
}
