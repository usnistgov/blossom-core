package model;

import java.io.Serializable;

public enum Status implements Serializable {
    PENDING_APPROVAL,
    PENDING_ATO,
    AUTHORIZED,
    UNAUTHORIZED_DENIED,
    UNAUTHORIZED_ATO,
    UNAUTHORIZED_OPTOUT,
    UNAUTHORIZED_SECURITY_RISK,
    UNAUTHORIZED_ROB;

    public static Status fromString(String statusStr) throws Exception {
        switch (statusStr) {
            case "PENDING_APPROVAL":
                return PENDING_APPROVAL;
            case "PENDING_ATO":
                return PENDING_ATO;
            case "AUTHORIZED":
                return AUTHORIZED;
            case "UNAUTHORIZED_DENIED":
                return UNAUTHORIZED_DENIED;
            case "UNAUTHORIZED_ATO":
                return UNAUTHORIZED_ATO;
            case "UNAUTHORIZED_OPTOUT":
                return UNAUTHORIZED_OPTOUT;
            case "UNAUTHORIZED_SECURITY_RISK":
                return UNAUTHORIZED_SECURITY_RISK;
            case "UNAUTHORIZED_ROB":
                return UNAUTHORIZED_ROB;
            default:
                throw new Exception("unknown Status: " + statusStr);
        }
    }

    public String getMessage() {
        switch (this) {
            case PENDING_APPROVAL:
                return "Pending: waiting for approval";
            case PENDING_ATO:
                return "Pending: waiting for ATO";
            case AUTHORIZED:
                return "Authorized";
            case UNAUTHORIZED_DENIED:
                return "Unauthorized: request denied";
            case UNAUTHORIZED_ATO:
                return "Unauthorized: waiting for ATO renewal";
            case UNAUTHORIZED_OPTOUT:
                return "Unauthorized: opted out";
            case UNAUTHORIZED_SECURITY_RISK:
                return "Unauthorized: security risk";
            case UNAUTHORIZED_ROB:
                return "Unauthorized: breach in rules of behavior";
            default:
                return "";
        }
    }
}
