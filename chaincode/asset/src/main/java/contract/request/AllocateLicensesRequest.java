package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AllocateLicensesRequest {

    private final String orderId;
    private final String account;
    private final List<String> licenses;

    public AllocateLicensesRequest(String orderId, String account, List<String> licenses) {
        this.orderId = orderId;
        this.account = account;
        this.licenses = licenses;
    }

    public AllocateLicensesRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AllocateLicensesRequest.class));
    }

    private AllocateLicensesRequest(AllocateLicensesRequest req) {
        this.orderId = Objects.requireNonNull(req.orderId, "orderId cannot be null");
        this.account = Objects.requireNonNull(req.account, "account cannot be null");
        this.licenses = Objects.requireNonNull(req.licenses, "licenses cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccount() {
        return account;
    }

    public List<String> getLicenses() {
        return licenses;
    }
}
