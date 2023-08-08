package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import model.Account;
import model.HistorySnapshot;
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
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Chaincode functions to manage Blossom accounts.
 */
@Contract(
        name = "account",
        info = @Info(
                title = "Blossom Authorization Account Contract",
                description = "Chaincode functions to manage Blossom accounts",
                version = "0.0.1"
        )
)
public class AccountContract implements ContractInterface {

    /**
     * Prefix used when writing accounts to the Fabric ledger
     */
    private static final String ACCOUNT_PREFIX = "account:";

    private BlossomPDP pdp = new BlossomPDP();

    @Transaction
    public Account TestGet(Context ctx) {
        return new Account("123", Status.AUTHORIZED, "atooo");
    }

    /**
     * Request a blossom account. The account name will be the MSPID extracted from the CID in the Context. When an account
     * is requested and written to the ledger, the default status will be PENDING_APPROVAL. The default ATO value will
     * be an empty string.
     *
     * NGAC: The requesting CID must have the "System Owner" attribute embedded in their X509 certificate.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @throws PMException if the requesting identity does not have permission to request an account.
     * @throws ChaincodeException if an account already exists with the MSPID.
     */
    @Transaction()
    public void RequestAccount(Context ctx) throws PMException {
        // check the requesting cid can request an account
        pdp.requestAccount(ctx);

        // mspid is the account name, the key will identify the account on the ledger
        String mspid = ctx.getClientIdentity().getMSPID();
        String key = accountKey(mspid);

        // check an account with the same mspid doesn't already exist
        byte[] bytes = ctx.getStub().getState(key);
        if (bytes != null) {
            throw new ChaincodeException("account already exists with the name: " + mspid);
        }

        // initialize the account, and put on ledger
        Account account = new Account(mspid, Status.PENDING_APPROVAL, "");

        ctx.getStub().putState(key, SerializationUtils.serialize(account));
    }

    /**
     * Approve an account request. A request must have previously been made. Once approved the status is set to
     * PENDING_ATO.
     *
     * NGAC: the requesting CID must have approve_account permissions on the blossom system. This permission is given
     * to users of the Blossom Admin member which is defined in the policy.pml file.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param mspid the MSPID of the account to approve.
     * @throws PMException if the requesting CID does not have the approve_account permission.
     * @throws ChaincodeException if the account has already been approved.
     */
    @Transaction
    public void ApproveAccount(Context ctx, String mspid) throws PMException {
        // check that a request exists
        Account account = GetAccount(ctx, mspid);
        if (account.getStatus() != Status.PENDING_APPROVAL) {
            throw new ChaincodeException("account " + mspid + " has already been approved");
        }

        // check that the requesting cid can approve an account
        pdp.approveAccount(ctx, mspid);

        // update the account status to PENDING_ATO, which is the initial status after approval
        account.setStatus(Status.PENDING_ATO);

        // update the account
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }

    /**
     * Upload an ATO attestation for the account embedded in the requesting user's X509 certificate. The ATO value must be
     * present.
     *
     * NGAC: Only users with the "System Administrator" can upload an ATO.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param ato The ATO value to upload.
     * @throws PMException if the requesting CID does not have the upload_ato permission on the account.
     * @throws ChaincodeException if no ato has been provided in the transient field of the Context.
     */
    @Transaction
    public void UploadATO(Context ctx, String ato) throws PMException {
        if (ato == null || ato.isEmpty()) {
            throw new ChaincodeException("no ATO was provided");
        }

        // check the requesting cid can upload an ATO
        String mspid = ctx.getClientIdentity().getMSPID();
        pdp.uploadATO(ctx, mspid);

        // deserialize the account object from the state and update the ATO value
        Account account = GetAccount(ctx, mspid);
        account.setAto(ato);

        // serialize the account object with updated ATO and put to state
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }

    /**
     * Get all accounts in blossom.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @return A list of Accounts.
     * @throws ChaincodeException if an error occurs iterating through accounts on the ledger.
     */
    @Transaction
    public List<Account> GetAccounts(Context ctx) {
        List<Account> accounts = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getStateByRange(ACCOUNT_PREFIX, ACCOUNT_PREFIX)) {
            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();
                accounts.add(SerializationUtils.deserialize(value));
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return accounts;
    }

    /**
     * Get the account with the given name.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param mspid The account to return.
     * @return The account information of the account with the given MSPID.
     * @throws ChaincodeException if an account with the given MSPID does not exist.
     */
    @Transaction
    public Account GetAccount(Context ctx, String mspid) {
        byte[] bytes = ctx.getStub().getState(accountKey(mspid));
        if (bytes == null) {
            throw new ChaincodeException("an account with id " + mspid + " does not exist");
        }

        return SerializationUtils.deserialize(bytes);
    }

    /**
     * Get the status of the account that the requesting CID belongs to.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @return The status of the account.
     * @throws ChaincodeException If the account in the CID does not exist.
     */
    @Transaction
    public Status GetAccountStatus(Context ctx) {
        String mspid = ctx.getClientIdentity().getMSPID();
        Account account = GetAccount(ctx, mspid);

        return account.getStatus();
    }

    /**
     * Get the history for a given account identified by the mspid. Each historySnapshot will contain the TxID and timestamp
     * of the update and the account information at the point in time.
     *
     * NGAC: none.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param mspid The MSPID of the account to get the history for.
     * @return A list of HistorySnapshots containing the history of the given account.
     */
    @Transaction
    public List<HistorySnapshot> GetHistory(Context ctx, String mspid) {
        List<HistorySnapshot> historySnapshots = new ArrayList<>();
        QueryResultsIterator<KeyModification> historyForKey = ctx.getStub().getHistoryForKey(accountKey(mspid));
        for (KeyModification next : historyForKey) {
            String txID = next.getTxId();
            Instant timestamp = next.getTimestamp();
            byte[] value = next.getValue();

            historySnapshots.add(
                    new HistorySnapshot(txID, timestamp.toString(), SerializationUtils.deserialize(value))
            );
        }

        return historySnapshots;
    }

    /**
     * builds the key for writing the account to the ledger
     */
    static String accountKey(String mspid) {
        return ACCOUNT_PREFIX + mspid;
    }
}
