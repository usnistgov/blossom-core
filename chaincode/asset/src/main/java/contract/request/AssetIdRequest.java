package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AssetIdRequest {

    private final String assetId;

    public AssetIdRequest(String assetId) {
        this.assetId = assetId;
    }

    public AssetIdRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AssetIdRequest.class));
    }

    private AssetIdRequest(AssetIdRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }
}
