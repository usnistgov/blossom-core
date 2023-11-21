package contract;

import model.Account;
import model.AccountHistorySnapshot;
import model.Status;
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
                description = "Functions to retrieve information about Blossom accounts",
                version = "0.0.1"
        )
)
public class AccountContract implements ContractInterface {

    /**
     * Prefix used when writing accounts to the Fabric ledger
     */
    private static final String ACCOUNT_PREFIX = "account:";


    /**
     * builds the key for writing the account to the ledger
     */
    public static String accountKey(String mspid) {
        return ACCOUNT_PREFIX + mspid;
    }

    /**
     * Get all accounts in Blossom.
     *
     * NGAC: none.
     *
     * @param ctx Fabric context object.
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
     * @param ctx Fabric context object.
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
     * @param ctx Fabric context object.
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
     * @param ctx Fabric context object.
     * @param mspid The MSPID of the account to get the history for.
     * @return A list of HistorySnapshots containing the history of the given account.
     */
    @Transaction
    public List<AccountHistorySnapshot> GetAccountHistory(Context ctx, String mspid) {
        List<AccountHistorySnapshot> accountHistorySnapshots = new ArrayList<>();
        QueryResultsIterator<KeyModification> historyForKey = ctx.getStub().getHistoryForKey(accountKey(mspid));
        for (KeyModification next : historyForKey) {
            String txID = next.getTxId();
            Instant timestamp = next.getTimestamp();
            byte[] value = next.getValue();

            accountHistorySnapshots.add(
                    new AccountHistorySnapshot(txID, timestamp.toString(), SerializationUtils.deserialize(value))
            );
        }

        return accountHistorySnapshots;
    }
}
