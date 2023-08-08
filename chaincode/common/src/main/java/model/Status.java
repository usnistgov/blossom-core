package model;

import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.Serializable;

public enum Status implements Serializable {
    PENDING_APPROVAL("waiting for approval"),
    PENDING_ATO("waiting for ATO"),
    AUTHORIZED("Authorized"),
    UNAUTHORIZED_DENIED("account request denied"),
    UNAUTHORIZED_ATO("ATO requires renewal"),
    UNAUTHORIZED_OPTOUT("opted out"),
    UNAUTHORIZED_SECURITY_RISK("security risk"),
    UNAUTHORIZED_ROB("breach in rules of behavior");

    private final String message;

    Status(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static Status fromString(String statusStr) {
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
                throw new ChaincodeException("unknown Status: " + statusStr);
        }
    }

}
