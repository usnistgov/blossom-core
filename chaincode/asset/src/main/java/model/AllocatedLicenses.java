package model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import contract.request.order.ReturnLicensesRequest;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;


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

    public AllocatedLicenses(String orderId, String assetId, String account, String expiration, Set<String> licenses) {
        this.orderId = orderId;
        this.assetId = assetId;
        this.account = account;
        this.expiration = expiration;
        this.licenses = licenses;
    }

    public AllocatedLicenses(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AllocatedLicenses.class));
    }

    private AllocatedLicenses(AllocatedLicenses req) {
        this.orderId = Objects.requireNonNull(req.getOrderId());
        this.account = Objects.requireNonNull(req.getAccount());
        this.assetId = Objects.requireNonNull(req.getAssetId());
        this.expiration = Objects.requireNonNull(req.getExpiration());
        this.licenses = Objects.requireNonNull(req.getLicenses());
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