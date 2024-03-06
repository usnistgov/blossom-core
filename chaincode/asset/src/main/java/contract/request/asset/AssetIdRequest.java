package contract.request.asset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class AssetIdRequest {

    private final String assetId;

    public AssetIdRequest(String assetId) {
        this.assetId = assetId;
    }

    public AssetIdRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();
        byte[] bytes = t.get("assetId");

        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }

        this.assetId = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getAssetId() {
        return assetId;
    }
}
