package contract;

import contract.request.asset.AssetIdRequest;
import contract.request.swid.SWIDRequest;
import contract.request.swid.ReportSWIDRequest;
import model.AllocatedLicenses;
import model.SWID;
import ngac.SWIDPDP;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public static String swidKey(String id) {
        return SWID_PREFIX + id;
    }

    private SWIDPDP pdp = new SWIDPDP();

    @Transaction
    public void ReportSWID(Context ctx) {
        ReportSWIDRequest req = new ReportSWIDRequest(ctx);

        // check user can report swid
        // TODO NGAC

        AllocatedLicenses[] allocatedLicensesWithAsset = new OrderContract()
                .getAllocatedLicensesWithAsset(
                        ctx,
                        req.getAccount(),
                        req.getAssetId()
                );

        // check the license has been checked out by the account
        boolean found = false;
        for (AllocatedLicenses allocatedLicenses : allocatedLicensesWithAsset) {
            if (allocatedLicenses.getLicenses().contains(req.getLicenseId())) {
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
                swidKey(id),
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
        String key = swidKey(req.getId());
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("SWID with id " + req.getId() + " does not exist");
        }

        // delete from account's private data
        ctx.getStub().delPrivateData(collection, key);
    }

    @Transaction
    public SWID GetSWID(Context ctx) {
        SWIDRequest req = new SWIDRequest(ctx);

        // check can read swid for this account
        // TODO NGAC pdp.canReadSwID(ctx);

        SWID swid = getSWIDWithId(ctx, req.getAccount(), req.getId());
        if (swid == null) {
            throw new ChaincodeException("SWID with id " + req.getId() + " does not exist");
        }

        return swid;
    }

    @Transaction
    public String[] GetSWIDsAssociatedWithAsset(Context ctx) {
        AssetIdRequest req = new AssetIdRequest(ctx);

        return getSWIDsAssociatedWithAsset(ctx, req.getAssetId());
    }

    SWID getSWIDWithId(Context ctx, String account, String id) {
        String collection = accountIPDC(account);

        // check if swid with id exists
        String key = swidKey(id);
        byte[] bytes = ctx.getStub().getPrivateData(collection, key);
        if (bytes.length == 0) {
            return null;
        }

        return SWID.fromByteArray(bytes);
    }

    String[] getSWIDsAssociatedWithAsset(Context ctx, String assetId) {
        String account = ctx.getClientIdentity().getMSPID();
        // check can read swid for this account
        // TODO NGAC pdp.canReadSwID(ctx);

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
