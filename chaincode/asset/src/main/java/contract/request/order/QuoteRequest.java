package contract.request.order;

import model.Quote;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class QuoteRequest {

    private String orderId;
    private String account;
    private String assetId;
    private int amount;
    private int duration;
    private double price;

    public QuoteRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        // a quote request is either a renewal -- orderId and optionally price if sending quote
        // or a new order -- with params
        byte[] bytes = t.get("orderId");
        if (bytes != null && bytes.length > 0) {
            orderId = new String(bytes, StandardCharsets.UTF_8);

            bytes = t.get("account");
            if (bytes != null && bytes.length > 0) {
                throw new IllegalArgumentException("account cannot be null");
            }

            bytes = t.get("price");
            if (bytes != null && bytes.length > 0) {
                price = Double.parseDouble(new String(bytes, StandardCharsets.UTF_8));
            }
        } else {
            bytes = t.get("account");
            if (bytes == null) {
                throw new IllegalArgumentException("account cannot be null");
            }
            account = new String(bytes, StandardCharsets.UTF_8);

            // if new order, params cannot be null
            bytes = t.get("assetId");
            if (bytes == null) {
                throw new IllegalArgumentException("assetId cannot be null");
            }
            assetId = new String(bytes, StandardCharsets.UTF_8);

            bytes = t.get("amount");
            if (bytes == null) {
                throw new IllegalArgumentException("amount cannot be null");
            }
            amount = Integer.parseInt(new String(bytes, StandardCharsets.UTF_8));

            bytes = t.get("duration");
            if (bytes == null) {
                throw new IllegalArgumentException("duration cannot be null");
            }
            duration = Integer.parseInt(new String(bytes, StandardCharsets.UTF_8));
        }
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

    public int getAmount() {
        return amount;
    }

    public int getDuration() {
        return duration;
    }

    public double getPrice() {
        return price;
    }
}
