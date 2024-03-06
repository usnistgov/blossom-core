package contract.request.asset;

import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AssetIdAndAccountRequest {

    private final String assetId;
    private final String account;

    public AssetIdAndAccountRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("assetId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }

        this.assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }

        this.account = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getAssetId() {
        return assetId;
    }

    public String getAccount() {
        return account;
    }
}
