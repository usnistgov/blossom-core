package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddLicensesRequest {

    private final String assetId;
    private final Set<LicenseIdWithSaltRequest> licenses;

    public AddLicensesRequest(String assetId, Set<LicenseIdWithSaltRequest> licenses) {
        this.assetId = assetId;
        this.licenses = licenses;
    }

    public AddLicensesRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AddLicensesRequest.class));
    }

    private AddLicensesRequest(AddLicensesRequest req) {
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.licenses = Objects.requireNonNull(req.licenses, "licenses cannot be null");
    }

    public String getAssetId() {
        return assetId;
    }

    public Set<LicenseIdWithSaltRequest> getLicenses() {
        return licenses;
    }
}
