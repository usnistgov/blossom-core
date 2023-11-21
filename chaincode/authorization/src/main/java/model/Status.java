package model;

import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.Serializable;

public enum Status implements Serializable {

    AUTHORIZED,
    PENDING,
    UNAUTHORIZED;

    public static Status fromString(String statusStr) {
        switch (statusStr) {
            case "AUTHORIZED":
                return AUTHORIZED;
            case "PENDING":
                return PENDING;
            case "UNAUTHORIZED":
                return UNAUTHORIZED;
            default:
                throw new ChaincodeException("unknown Status: " + statusStr);
        }
    }
}
