package contract;

import com.google.gson.Gson;
import gov.nist.csd.pm.policy.exceptions.PMException;
import model.Account;
import model.HistorySnapshot;
import model.Status;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AuthorizationContract {

    public static final String ACCOUNT_PREFIX = "account:";


    @Transaction()
    public void RequestAccount(Context ctx) throws Exception {
        new BlossomPDP().requestAccount(ctx);

        String mspid = ctx.getClientIdentity().getMSPID();
        String key = accountKey(mspid);

        byte[] bytes = ctx.getStub().getState(key);
        if (bytes != null) {
            throw new Exception("account already exists with the name: " + mspid);
        }

        Account account = new Account(mspid, Status.PENDING_APPROVAL, "");
        ctx.getStub().putState(key, account.toBytes());
    }

    @Transaction
    public void ApproveAccount(Context ctx, String mspid) throws PMException {
        new BlossomPDP().approveAccount(ctx, mspid);

        String accountJson = GetAccount(ctx, mspid);
        Account account = Account.fromJson(accountJson);
        account.setStatus(Status.PENDING_ATO);

        ctx.getStub().putState(accountKey(mspid), account.toBytes());
    }

    @Transaction
    public void UploadATO(Context ctx) throws Exception {
        Map<String, byte[]> t = ctx.getStub().getTransient();
        String ato = TransientATO.fromBytes(t.get("ato")).getAto();
        String mspid = ctx.getClientIdentity().getMSPID();

        new BlossomPDP().uploadATO(ctx, mspid);

        String accountJson = GetAccount(ctx, mspid);
        Account account = Account.fromJson(accountJson);
        account.setAto(ato);

        ctx.getStub().putState(accountKey(mspid), account.toBytes());
    }

    @Transaction
    public void UpdateAccountStatus(Context ctx, String mspid, String statusStr) throws Exception {
        new BlossomPDP().updateAccountStatus(ctx, mspid, statusStr);

        Status status = Status.fromString(statusStr);

        String accountJson = GetAccount(ctx, mspid);
        Account account = Account.fromJson(accountJson);
        account.setStatus(status);

        ctx.getStub().putState(accountKey(mspid), account.toBytes());
    }

    @Transaction
    public List<String> GetAccounts(Context ctx) throws Exception {
        List<String> accounts = new ArrayList<>();

        QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(ACCOUNT_PREFIX, ACCOUNT_PREFIX);
        for (KeyValue next : stateByRange) {
            byte[] value = next.getValue();
            accounts.add(Account.fromBytes(value).toJson());
        }

        stateByRange.close();

        return accounts;
    }

    @Transaction
    public String GetAccount(Context ctx, String mspid) {
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        return Account.fromBytes(bytes).toJson();
    }

    @Transaction
    public String GetAccountStatus(Context ctx, String mspid) {
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        return Account.fromBytes(bytes).getStatus().toString();
    }

    @Transaction
    public List<String> GetHistory(Context ctx, String mspid) {
        List<String> historySnapshots = new ArrayList<>();
        QueryResultsIterator<KeyModification> historyForKey = ctx.getStub().getHistoryForKey(accountKey(mspid));
        for (KeyModification next : historyForKey) {
            String txID = next.getTxId();
            Instant timestamp = next.getTimestamp();
            byte[] value = next.getValue();

            historySnapshots.add(new HistorySnapshot(txID, timestamp.toString(), Account.fromBytes(value)).toJson());
        }

        return historySnapshots;
    }

    static String accountKey(String mspid) {
        return ACCOUNT_PREFIX + mspid;
    }

    static class TransientATO {
        private String ato;

        public TransientATO(String ato) {
            this.ato = ato;
        }

        public String getAto() {
            return ato;
        }

        public void setAto(String ato) {
            this.ato = ato;
        }

        public static TransientATO fromBytes(byte[] bytes) throws Exception {
            if (bytes == null) {
                throw new Exception("received null value for ato");
            }

            String json = new String(bytes);
            return new Gson().fromJson(json, TransientATO.class);
        }
    }

}
