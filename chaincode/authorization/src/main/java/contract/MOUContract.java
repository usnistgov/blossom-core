package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import model.ATO;
import model.Account;
import model.MOU;
import model.Status;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import static contract.AccountContract.accountKey;

@Contract(
        name = "mou",
        info = @Info(
                title = "Blossom Authorization MOU Contract",
                description = "Functions supporting the Blossom MOU process",
                version = "0.0.1"
        )
)
public class MOUContract implements ContractInterface {

    private static final String MOU_KEY = "mou";

    private BlossomPDP pdp = new BlossomPDP();

    @Transaction
    public void UpdateMOU(Context ctx, String text) throws PMException {
        pdp.updateMOU(ctx);

        String timestamp = ctx.getStub().getTxTimestamp().toString();

        MOU mou;
        byte[] bytes = ctx.getStub().getState(MOU_KEY);
        if (bytes == null) {
            mou = new MOU(text, 1, timestamp);
        } else {
            mou = SerializationUtils.deserialize(bytes);
            mou.setText(text);
            mou.setVersion(mou.getVersion()+1);
            mou.setTimestamp(timestamp);
        }

        bytes = SerializationUtils.serialize(mou);

        ctx.getStub().putState(MOU_KEY, bytes);
        ctx.getStub().setEvent("UpdateMOU", bytes);
    }

    @Transaction
    public MOU GetMOU(Context ctx) {
        byte[] bytes = ctx.getStub().getState(MOU_KEY);
        if (bytes == null) {
            throw new ChaincodeException("Blossom MOU has not yet been created");
        }

        return SerializationUtils.deserialize(bytes);
    }

    @Transaction
    public List<MOU> GetMOUHistory(Context ctx) {
        QueryResultsIterator<KeyModification> historyForKey =
                ctx.getStub().getHistoryForKey(MOU_KEY);
        List<MOU> history = new ArrayList<>();

        for (KeyModification keyModification : historyForKey) {
            history.add(SerializationUtils.deserialize(keyModification.getValue()));
        }

        return history;
    }

    @Transaction
    public void SignMOU(Context ctx, int version) throws PMException {
        pdp.signMOU(ctx);

        MOU mou = GetMOU(ctx);

        if (version != mou.getVersion()) {
            throw new ChaincodeException("signing MOU version " + version + ", expected version " + mou.getVersion());
        }

        String mspid = ctx.getClientIdentity().getMSPID();

        // check that account exists, if not create it
        Account account;
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        if (bytes == null) {
            account = new Account(mspid, null, null, version);
        } else {
            account = SerializationUtils.deserialize(bytes);
        }

        account.setMouVersion(version);

        bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(ctx.getClientIdentity().getMSPID()), bytes);

        ctx.getStub().setEvent("SignMOU", bytes);
    }

    @Transaction
    public void Join(Context ctx) throws PMException {
        String mspid = ctx.getClientIdentity().getMSPID();

        Account account;
        try {
            account = new AccountContract().GetAccount(ctx, mspid);
        } catch (ChaincodeException e) {
            throw new ChaincodeException(mspid + " must sign the current MOU before joining");
        }

        if (account.getStatus() != null) {
            throw new ChaincodeException(mspid + " is already joined");
        }

        pdp.join(ctx, mspid);

        // update the account status to PENDING, which is the initial status after approval
        account.setStatus(Status.PENDING);

        // update the account
        byte[] bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(mspid), bytes);
        ctx.getStub().setEvent("Join", bytes);
    }

}
