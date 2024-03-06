package contract.request.order;

import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OrderIdAndAccountRequest {

    private final String orderId;
    private final String account;

    public OrderIdAndAccountRequest(String orderId, String account) {
        this.orderId = orderId;
        this.account = account;
    }

    public OrderIdAndAccountRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("orderId");
        if (bytes == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }

        this.orderId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }

        this.account = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getOrderId() {
        return orderId;
    }

    public String getAccount() {
        return account;
    }
}
