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
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("orderId");
        if (bytes != null && bytes.length > 0) {
            this.orderId = new String(bytes, StandardCharsets.UTF_8);
        } else {
            this.orderId = null;
        }

        bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }
        account = new String(bytes, StandardCharsets.UTF_8);

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
