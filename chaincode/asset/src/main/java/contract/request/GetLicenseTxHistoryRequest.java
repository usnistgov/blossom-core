package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class GetLicenseTxHistoryRequest {

    private final String assetId;
    private final String licenseId;

    public GetLicenseTxHistoryRequest(String assetId, String licenseId) {
        this.assetId = assetId;
        this.licenseId = licenseId;
    }

    public GetLicenseTxHistoryRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                GetLicenseTxHistoryRequest.class));
    }

    private GetLicenseTxHistoryRequest(GetLicenseTxHistoryRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.licenseId = Objects.requireNonNull(req.licenseId, "licenseId cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }

    public String getLicenseId() {
        return licenseId;
    }

}
