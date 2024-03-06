package contract.request.asset;

import model.DateFormatter;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UpdateEndDateRequest {

    private final String assetId;
    private final String newEndDate;

    public UpdateEndDateRequest(String assetId, String newEndDate) {
        this.assetId = assetId;
        this.newEndDate = newEndDate;
    }

    public UpdateEndDateRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("assetId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }

        assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("newEndDate");
        if (bytes == null) {
            throw new IllegalArgumentException("newEndDate cannot be null");
        }

        newEndDate = new String(bytes, StandardCharsets.UTF_8);
        DateFormatter.checkDateFormat(this.newEndDate);
    }

    public String getAssetId() {
        return assetId;
    }

    public String getNewEndDate() {
        return newEndDate;
    }
}
