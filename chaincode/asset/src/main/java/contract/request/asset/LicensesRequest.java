package contract.request.asset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LicensesRequest {

    private final String assetId;
    private final Set<String> licenses;

    public LicensesRequest(String assetId, Set<String> licenses) {
        this.assetId = assetId;
        this.licenses = licenses;
    }

    public LicensesRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("assetId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }

        this.assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("licenses");
        if (bytes == null) {
            throw new IllegalArgumentException("licenses cannot be null");
        }

        String s = new String(bytes, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = new Gson().fromJson(s, type);

        this.licenses = new HashSet<>(list);
    }

    public String getAssetId() {
        return assetId;
    }

    public Set<String> getLicenses() {
        return licenses;
    }
}
