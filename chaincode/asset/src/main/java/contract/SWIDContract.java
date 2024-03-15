package contract;

import contract.request.AssetIdAndAccountRequest;
import contract.request.AssetIdRequest;
import contract.request.SWIDRequest;
import contract.request.ReportSWIDRequest;
import model.Allocated;
import model.SWID;
import ngac.PDP;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

import static contract.AssetContract.*;

@Contract(
        name = "swid",
        info = @Info(
                title = "Blossom asset chaincode SWID contract",
                description = "Chaincode functions to manage SWIDs",
                version = "0.0.1"
        )
)
public class SWIDContract implements ContractInterface {

    public static final String SWID_PREFIX = "swid:";
    public static String swidKey(String licenseId) {
        return SWID_PREFIX + licenseId;
    }

    @Transaction
    public void ReportSWID(Context ctx) {
        ReportSWIDRequest req = new ReportSWIDRequest(ctx);

        PDP.canWriteSWID(ctx, req.getAccount());

        Allocated[] licensesForAccount = new OrderContract()
                .getAllocatedLicenses(
                        ctx,
                        req.getAccount(),
                        req.getAssetId()
                );

        // check the license has been checked out by the account
        boolean found = false;
        for (Allocated allocated : licensesForAccount) {
            if (allocated.getLicenseId().equals(req.getLicenseId())) {
                found = true;
                break;
            }
        }

        if (!found) {
            throw new ChaincodeException("cannot report SWID on license " + req.getLicenseId() + " for account " + req.getAccount());
        }

        // add to account's private data
        String id = ctx.getStub().getTxId();
        ctx.getStub().putPrivateData(
                accountIPDC(req.getAccount()),
                swidKey(req.getLicenseId()),
                new SWID(id, req.getPrimaryTag(), req.getXml(), req.getPrimaryTag(), req.getLicenseId()).toByteArray()
        );
    }

    @Transaction
    public void DeleteSWID(Context ctx) {
        SWIDRequest req = new SWIDRequest(ctx);

        // check user can delete swid
        // TODO NGAC

        String collection = accountIPDC(req.getAccount());

        // check if swid with id exists
        String key = swidKey(req.getLicenseId());
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("SWID with id " + req.getLicenseId() + " does not exist");
        }

        // delete from account's private data
        ctx.getStub().delPrivateData(collection, key);
    }

    @Transaction
    public SWID GetSWID(Context ctx) {
        SWIDRequest req = new SWIDRequest(ctx);

        PDP.canReadSWID(ctx, req.getAccount());

        SWID swid = getSWIDWithId(ctx, req.getAccount(), req.getLicenseId());
        if (swid == null) {
            throw new ChaincodeException("SWID with id " + req.getLicenseId() + " does not exist");
        }

        return swid;
    }

    SWID getSWIDWithId(Context ctx, String account, String licenseId) {
        String collection = accountIPDC(account);

        // check if swid with id exists
        String key = swidKey(licenseId);
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            return null;
        }

        return SWID.fromByteArray(bytes);
    }

    String[] getSWIDsAssociatedWithAsset(Context ctx, String assetId, String account) {
        String collection = accountIPDC(account);

        // iterate over all swids since they are stored by txid
        String key = swidKey("");
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(collection, key, key + "~")) {
            List<String> swids = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                SWID swid = SWID.fromByteArray(value);
                if (!swid.getAssetId().equals(assetId)) {
                    continue;
                }

                swids.add(swid.getPrimaryTag());
            }

            return swids.toArray(String[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }
}
