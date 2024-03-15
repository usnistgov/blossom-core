package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AssetIdAndAccountRequest {

    private final String assetId;
    private final String account;

    public AssetIdAndAccountRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AssetIdAndAccountRequest.class));
    }

    private AssetIdAndAccountRequest(AssetIdAndAccountRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.account = Objects.requireNonNull(req.account, "account cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAccount() {
        return account;
    }
}
