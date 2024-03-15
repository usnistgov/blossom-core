package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class QuoteRequest {

    private String orderId;
    private final String account;
    private String assetId;
    private int amount;
    private int duration;
    private double price;

    public QuoteRequest(Context ctx, boolean isRequest) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                QuoteRequest.class), isRequest);
    }

    private QuoteRequest(QuoteRequest req, boolean isRequest) {
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");

        if (isRequest) {
            this.assetId = Objects.requireNonNull(req.getAssetId(), "assetId cannot be null");
            this.amount = req.getAmount();
            if (amount == 0) {
                throw new IllegalArgumentException("amount cannot be 0");
            }

            this.duration = req.getDuration();
            if (duration == 0) {
                throw new IllegalArgumentException("duration cannot be 0");
            }
        } else {
            this.orderId = Objects.requireNonNull(req.getOrderId(), "orderId cannot be null");
            this.price = req.getPrice();
            if (price == 0) {
                throw new IllegalArgumentException("price cannot be 0");
            }
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
