package contract.request.order;

import com.google.common.primitives.Ints;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InitiateOrderRequest {

    private final String account;
    private final String orderId;
    private final String assetId;
    private final int amount;
    private final int duration;

    public InitiateOrderRequest(String account, String orderId, String assetId, int amount, int duration) {
        this.account = account;
        this.orderId = orderId;
        this.assetId = assetId;
        this.amount = amount;
        this.duration = duration;
    }

    public InitiateOrderRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                InitiateOrderRequest.class));
    }

    private InitiateOrderRequest(InitiateOrderRequest req) {
        this.orderId = Objects.requireNonNull(req.orderId, "orderId cannot be null");
        this.account = Objects.requireNonNull(req.account, "account cannot be null");
        this.assetId = Objects.requireNonNull(req.assetId, "assetId cannot be null");
        this.amount = req.amount;
        if (amount == 0) {
            throw new IllegalArgumentException("amount cannot be 0");
        }

        this.duration = req.duration;
        if (duration == 0) {
            throw new IllegalArgumentException("duration cannot be 0");
        }
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

    public String getOrderId() {
        return orderId;
    }
}
