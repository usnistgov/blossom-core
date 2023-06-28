package ngac;

import gov.nist.csd.pm.policy.events.PolicyEvent;

public class VoteEvent implements PolicyEvent {

    private String eventName;
    private String initiatingMSP;
    private String id;
    private String targetMember;
    private boolean passed;
    private String status;

    public VoteEvent(String eventName, String initiatingMSP, String id, String targetMember, boolean passed, String status) {
        this.eventName = eventName;
        this.initiatingMSP = initiatingMSP;
        this.id = id;
        this.targetMember = targetMember;
        this.passed = passed;
        this.status = status;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getInitiatingMSP() {
        return initiatingMSP;
    }

    public void setInitiatingMSP(String initiatingMSP) {
        this.initiatingMSP = initiatingMSP;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetMember() {
        return targetMember;
    }

    public void setTargetMember(String targetMember) {
        this.targetMember = targetMember;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
