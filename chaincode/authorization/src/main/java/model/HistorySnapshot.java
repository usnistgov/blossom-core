package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

/**
 * Snapshot of Blossom account.
 */
@DataType
public class HistorySnapshot implements Serializable {

    /**
     * Fabric transaction ID.
     */
    @Property
    private String txID;

    /**
     * Fabric transaction timestamp.
     */
    @Property
    private String timestamp;

    /**
     * Account details for this snapshot.
     */
    @Property
    private Account account;

    public HistorySnapshot(@JsonProperty String txID, @JsonProperty String timestamp, @JsonProperty Account account) {
        this.txID = txID;
        this.timestamp = timestamp;
        this.account = account;
    }

    public String getTxID() {
        return txID;
    }

    public void setTxID(String txID) {
        this.txID = txID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

}
