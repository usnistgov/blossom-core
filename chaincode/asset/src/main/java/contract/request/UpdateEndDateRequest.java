package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class UpdateEndDateRequest {

    private final String assetId;
    private final String newEndDate;

    public UpdateEndDateRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                UpdateEndDateRequest.class));
    }

    private UpdateEndDateRequest(UpdateEndDateRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.newEndDate = Objects.requireNonNull(req.newEndDate, "newEndDate cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }

    public String getNewEndDate() {
        return newEndDate;
    }
}
