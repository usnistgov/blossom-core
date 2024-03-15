package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class OrderIdRequest {

    private final String orderId;

    public OrderIdRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                OrderIdRequest.class));
    }

    private OrderIdRequest(OrderIdRequest req) {
        this.orderId = Objects.requireNonNull(req.orderId, "orderId cannot be null");
    }

    public String getOrderId() {
        return orderId;
    }

}
