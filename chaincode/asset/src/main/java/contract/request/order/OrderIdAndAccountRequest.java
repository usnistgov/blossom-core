package contract.request.order;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class OrderIdAndAccountRequest {

    private final String orderId;
    private final String account;

    public OrderIdAndAccountRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                OrderIdAndAccountRequest.class));
    }

    private OrderIdAndAccountRequest(OrderIdAndAccountRequest req) {
        this.orderId = Objects.requireNonNull(req.orderId, "orderId cannot be null");
        this.account = Objects.requireNonNull(req.account, "account cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccount() {
        return account;
    }
}
