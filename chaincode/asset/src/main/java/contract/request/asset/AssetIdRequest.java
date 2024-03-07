package contract.request.asset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
