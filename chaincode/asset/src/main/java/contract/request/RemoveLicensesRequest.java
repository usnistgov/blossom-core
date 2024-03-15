package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RemoveLicensesRequest {

    private final String assetId;
    private final Set<String> licenses;

    public RemoveLicensesRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                RemoveLicensesRequest.class));
    }

    private RemoveLicensesRequest(RemoveLicensesRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.licenses = Objects.requireNonNull(req.licenses, "licenses cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }

    public Set<String> getLicenses() {
        return licenses;
    }
}
