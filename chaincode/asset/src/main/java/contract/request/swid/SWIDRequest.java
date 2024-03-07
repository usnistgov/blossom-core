package contract.request.swid;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class SWIDRequest {

    private final String account;
    private final String licenseId;

    public SWIDRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                SWIDRequest.class));
    }

    private SWIDRequest(SWIDRequest req) {
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.licenseId = Objects.requireNonNull(req.getLicenseId(), "licenseId cannot be null");
    }

    public String getAccount() {
        return account;
    }

    public String getLicenseId() {
        return licenseId;
    }
}
