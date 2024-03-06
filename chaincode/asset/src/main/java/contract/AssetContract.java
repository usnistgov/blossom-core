package contract;

import contract.request.asset.*;
import contract.response.AssetDetailResponse;
import contract.response.AssetResponse;
import model.*;
import ngac.AssetPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.*;

import static model.AllocatedLicenses.allocatedKey;
import static ngac.PDP.ADMINMSP;

@Contract(
        name = "asset",
        info = @Info(
                title = "Blossom asset chaincode license contract",
                description = "Chaincode functions to manage assets and licenses",
                version = "0.0.1"
        )
)
public class AssetContract implements ContractInterface {

    public static final String  ADMINMSP_IPDC = accountIPDC(ADMINMSP);
    public static final String ASSET_PREFIX = "asset:";
    public static final String LICENSE_PREFIX = "license:";

    public static String accountIPDC(String account) {
        return "_implicit_org_" + account;
    }

    public static String assetKey(String assetId) {
        return ASSET_PREFIX + assetId;
    }

    public static String licenseKey(String assetId, String licenseId) {
        return LICENSE_PREFIX + assetId + ":" + licenseId;
    }

    public static String licenseSearchKey(String assetId) {
        return LICENSE_PREFIX + assetId;
    }

    private AssetPDP assetPDP = new AssetPDP();

    // use transient for license ids
    @Transaction
    public String AddAsset(Context ctx) {
        // TODO NGAC

        AddAssetRequest req = new AddAssetRequest(ctx);

        String id = ctx.getStub().getTxId();
        String startDate = DateFormatter.getFormattedTimestamp(ctx);

        // build the asset object and write to ADMINMSP_IPDC
        Asset asset = new Asset(id, req.getName(), startDate, req.getEndDate(), req.getLicenses());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(id), asset.toByteArray());

        return id;
    }

    @Transaction
    public void AddLicenses(Context ctx) {
        // TODO NGAC

        // get asset info from transient data field
        LicensesRequest req = new LicensesRequest(ctx);

        // get the asset
        Asset asset = getAsset(ctx, req.getAssetId());

        // check that the licenses are not duplicates
        for (String licenseId : req.getLicenses()) {
            if (licenseExists(ctx, asset, licenseId)) {
                throw new ChaincodeException("license " + licenseId + " already exists");
            }

            asset.addAvailableLicense(licenseId);
        }

        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(asset.getId()), asset.toByteArray());
    }

    @Transaction
    public void RemoveLicenses(Context ctx) {
        // TODO NGAC

        // get asset info from transient data field
        LicensesRequest req = new LicensesRequest(ctx);

        // get asset
        Asset asset = getAsset(ctx, req.getAssetId());

        // remove licenses from adminmsp ipdc as long as they exist and are not allocated
        for (String licenseId : req.getLicenses()) {
            if (!asset.getAvailableLicenses().contains(licenseId)) {
                throw new ChaincodeException("license " + licenseId + " does not exist");
            }

            String account = getLicenseAllocatedTo(ctx, asset.getId(), licenseId);
            if (account != null){
                throw new ChaincodeException("license " + licenseId + " is allocated to " + account);
            }

            asset.removeAvailableLicense(licenseId);
        }

        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(asset.getId()), asset.toByteArray());
    }

    @Transaction
    public void UpdateEndDate(Context ctx) {
        // TODO NGAC

        UpdateEndDateRequest req = new UpdateEndDateRequest(ctx);

        // read asset from ledger
        Asset asset = getAsset(ctx, req.getAssetId());

        // update asset object
        asset.setEndDate(req.getNewEndDate());

        // update asset state
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, assetKey(req.getAssetId()), asset.toByteArray());
    }

    @Transaction
    public void RemoveAsset(Context ctx) {
        // TODO NGAC

        AssetIdRequest req = new AssetIdRequest(ctx);

        // check asset exists
        getAsset(ctx, req.getAssetId());

        // delete asset state
        ctx.getStub().delPrivateData(ADMINMSP_IPDC, assetKey(req.getAssetId()));
    }

    @Transaction
    public AssetResponse[] GetAssets(Context ctx) {
        // TODO NGAC

        List<AssetResponse> assets = new ArrayList<>();

        try(QueryResultsIterator<KeyValue> stateByRange = getAssetQueryIterator(ctx, ASSET_PREFIX)) {
            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                Asset asset = Asset.fromByteArray(value);
                AssetResponse assetResponse = new AssetResponse(
                        asset.getId(),
                        asset.getName(),
                        asset.getAvailableLicenses().size(),
                        asset.getStartDate(),
                        asset.getEndDate()
                );

                assets.add(assetResponse);
            }
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }

        return assets.toArray(AssetResponse[]::new);
    }

    @Transaction
    public AssetDetailResponse GetAsset(Context ctx) {
        // TODO NGAC

        AssetIdRequest req = new AssetIdRequest(ctx);

        Asset asset = getAsset(ctx, req.getAssetId());

        AssetDetailResponse response;
        if (false) {// TODO NGAC Check
            response = new AssetDetailResponse(
                    asset.getId(),
                    asset.getName(),
                    asset.getAvailableLicenses().size(),
                    asset.getStartDate(),
                    asset.getEndDate(),
                    0,
                    null,
                    null
            );
        } else {
            Map<String, Map<String, License>> allocatedLicenses = getAllocatedLicensesMap(ctx, req.getAssetId());
            int total = 0;
            for (Map<String, License> licenses : allocatedLicenses.values()) {
                total += licenses.size();
            }

            response = new AssetDetailResponse(
                    asset.getId(),
                    asset.getName(),
                    asset.getAvailableLicenses().size(),
                    asset.getStartDate(),
                    asset.getEndDate(),
                    total,
                    asset.getAvailableLicenses(),
                    allocatedLicenses
            );
        }

        return response;
    }

    QueryResultsIterator<KeyValue> getAssetQueryIterator(Context ctx, String key) {
        return ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~");
    }

    Asset getAsset(Context ctx, String assetId) {
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, assetKey(assetId));
        if (bytes.length == 0) {
            throw new ChaincodeException("asset with id " + assetId + " does not exist");
        }

        return Asset.fromByteArray(bytes);
    }

    private boolean licenseExists(Context ctx, Asset asset, String licenseId) {
        if (asset.getAvailableLicenses().contains(licenseId)) {
            return true;
        }

        // check allocated
        String key = allocatedKey(asset.getId(), "");
        try(QueryResultsIterator<KeyValue> stateByRange = getAssetQueryIterator(ctx, key)) {
            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(value);
                if (allocatedLicenses.getLicenses().contains(licenseId)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    private String getLicenseAllocatedTo(Context ctx, String assetId, String licenseId) {
        try(QueryResultsIterator<KeyValue> stateByRange = getAssetQueryIterator(ctx, allocatedKey(assetId, ""))) {
            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(value);
                if (allocatedLicenses.getLicenses().contains(licenseId)) {
                    return allocatedLicenses.getAccount();
                }
            }

            return null;
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    private Map<String, Map<String, License>> getAllocatedLicensesMap(Context ctx, String assetId) {
        try(QueryResultsIterator<KeyValue> stateByRange = getAssetQueryIterator(ctx, allocatedKey(assetId, ""))) {
            // account -> order -> licenses
            Map<String, Map<String, License>> map = new HashMap<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(value);
                Map<String, License> licenses = map.getOrDefault(allocatedLicenses.getAccount(), new HashMap<>());
                for (String licenseId : allocatedLicenses.getLicenses()) {
                    licenses.put(
                            allocatedLicenses.getOrderId(),
                            new License(
                                    licenseId,
                                    new Allocated(allocatedLicenses.getAccount(), allocatedLicenses.getExpiration(), allocatedLicenses.getOrderId())
                            )
                    );
                }

                map.put(allocatedLicenses.getAccount(), licenses);
            }

            return map;
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }
}
