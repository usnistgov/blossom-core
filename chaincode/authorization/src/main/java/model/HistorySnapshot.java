package model;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

@DataType
public class HistorySnapshot implements Serializable {

    @Property
    private String txID;

    @Property
    private String timestamp;

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
