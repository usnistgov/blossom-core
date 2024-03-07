package contract.request.order;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Property;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReturnLicensesRequest {

    private final String orderId;
    private final String account;
    private final String assetId;
    private final Set<String> licenses;

    public ReturnLicensesRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                ReturnLicensesRequest.class));
    }

    private ReturnLicensesRequest(ReturnLicensesRequest req) {
        this.orderId = Objects.requireNonNull(req.getOrderId(), "orderId cannot be null");
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.assetId = Objects.requireNonNull(req.getAssetId(), "assetId cannot be null");
        this.licenses = Objects.requireNonNull(req.getLicenses(), "licenses cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccount() {
        return account;
    }

    public String getAssetId() {
        return assetId;
    }

    public Set<String> getLicenses() {
        return licenses;
    }
}
