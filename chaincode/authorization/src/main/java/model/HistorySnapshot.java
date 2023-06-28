package model;

import com.google.gson.Gson;

import java.io.Serializable;

public class HistorySnapshot implements Serializable {

    private String txID;
    private String timestamp;
    private Account account;

    public HistorySnapshot(String txID, String timestamp, Account account) {
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

    public String toJson() {
        return new Gson().toJson(this);
    }
}
