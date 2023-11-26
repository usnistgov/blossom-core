package contract;

import contract.event.SignMOUEvent;
import gov.nist.csd.pm.policy.exceptions.PMException;
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

import java.util.ArrayList;
import java.util.List;

import static contract.AccountContract.accountKey;

/**
 * Chaincode functions to support the initial onboarding process and continuous management of the Blossom MOU.
 */
@Contract(
        name = "mou",
        info = @Info(
                title = "Blossom Authorization MOU Contract",
                description = "Functions supporting the Blossom MOU process",
                version = "0.0.1"
        )
)
public class MOUContract implements ContractInterface {

    /**
     * key used to identify the MOU text on the ledger
     */
    private static final String MOU_KEY = "mou";

    /**
     * BlossomPDP object to check privileges in NGAC system
     */
    private BlossomPDP pdp = new BlossomPDP();

    /**
     * Update the Blossom MOU.
     *
     * NGAC: Only an Authorizing Official in the ADMINMSP can call this function.
     *
     * event:
     *  - name: "UpdateMOU"
     *  - payload: a serialized MOU object
     *
     * @param ctx Fabric context object.
     * @param text The contents of the MOU.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
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

        ctx.getStub().setEvent("UpdateMOU", new byte[]{});
    }

    /**
     * Get the latest MOU.
     *
     * @param ctx Fabric context object.
     * @return an MOU object.
     * @throws ChaincodeException is the MOU has not been created yet.
     */
    @Transaction
    public MOU GetMOU(Context ctx) {
        byte[] bytes = ctx.getStub().getState(MOU_KEY);
        if (bytes == null) {
            throw new ChaincodeException("Blossom MOU has not yet been created");
        }

        return SerializationUtils.deserialize(bytes);
    }

    /**
     * Get the history of MOU updates.
     *
     * @param ctx Fabric context object.
     * @return a List of MOU objects representing the history of MOU updates.
     */
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

    /**
     * Sign the provided version of the MOU for the member in the cid. The act of signing the fabric transaction with
     * the provided version represents the signing process. The version must be the most recent.
     *
     * event:
     *  - name: "SignMOU"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @param version The version of the MOU the cid is signing.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the version being signed is not the same as the latest version.
     */
    @Transaction
    public void SignMOU(Context ctx, int version) {
        pdp.signMOU(ctx);

        MOU mou = GetMOU(ctx);

        if (version != mou.getVersion()) {
            throw new ChaincodeException("signing MOU version " + version + ", expected version " + mou.getVersion());
        }

        String accountId = ctx.getClientIdentity().getMSPID();

        // check that account exists, if not create it
        Account account;
        byte[] bytes = ctx.getStub().getState(accountKey(accountId));
        if (bytes == null) {
            account = new Account(accountId, Status.PENDING, version, false);
        } else {
            account = SerializationUtils.deserialize(bytes);
        }

        account.setMouVersion(version);

        bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(ctx.getClientIdentity().getMSPID()), bytes);

        ctx.getStub().setEvent("SignMOU", SerializationUtils.serialize(new SignMOUEvent(accountId, version)));
    }
}
