package model;

import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class Vote implements Serializable {

    private String id;
    private String initiatingMSP;
    private String targetMember;
    private Status statusChange;
    private String reason;
    private Threshold threshold;
    private int count;
    private Result result;

    public Vote(String id, String initiatingMSP, String targetMember, Status statusChange, String reason, Threshold threshold, int count, Result result) {
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

    public byte[] toBytes() {
        return SerializationUtils.serialize(this);
    }

    public static Vote fromBytes(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Vote fromJson(String json) {
        return new Gson().fromJson(json, Vote.class);
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
