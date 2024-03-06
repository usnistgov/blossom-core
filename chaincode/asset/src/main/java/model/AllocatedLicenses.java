package model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


@DataType
public class AllocatedLicenses implements Serializable {

    public static final String ALLOCATED_PREFIX = "allocated:";

    @Property
    private String orderId;
    @Property
    private String assetId;
    @Property
    private String account;
    @Property
    private String expiration;
    @Property
    private Set<String> licenses;

    public AllocatedLicenses(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("orderId");
        if (bytes == null) {
            throw new IllegalArgumentException("orderId cannot be null");
        }
        orderId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("assetId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }
        assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }
        account = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("expiration");
        if (bytes == null) {
            throw new IllegalArgumentException("expiration cannot be null");
        }
        expiration = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("licenses");
        if (bytes == null) {
            throw new IllegalArgumentException("licenses cannot be null");
        }
        String s = new String(bytes, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = new Gson().fromJson(s, type);
        licenses = new HashSet<>(list);
    }

    public AllocatedLicenses(String orderId, String assetId, String account, String expiration, Set<String> licenses) {
        this.orderId = orderId;
        this.assetId = assetId;
        this.account = account;
        this.expiration = expiration;
        this.licenses = licenses;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public Set<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<String> licenses) {
        this.licenses = licenses;
    }

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    public static AllocatedLicenses fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    public static String allocatedKey(String assetId, String orderId) {
        return ALLOCATED_PREFIX + assetId + ":" + orderId;
    }
}