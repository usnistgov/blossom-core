package contract.request;

import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@DataType
public class LicensesRequest implements Serializable {

    public static String allocateRequestKey(ACTION action, String orderId) {
        return action + ":" + orderId;
    }

    public static LicensesRequest fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    @Property
    private String account;
    @Property
    private String assetId;
    @Property
    private String orderId;
    @Property
    private String expiration;
    @Property
    private List<String> licenses;

    public LicensesRequest(String account, String assetId, String orderId, String expiration, List<String> licenses) {
        this.account = account;
        this.assetId = assetId;
        this.orderId = orderId;
        this.expiration = expiration;
        this.licenses = licenses;
    }

    public LicensesRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                LicensesRequest.class));
    }

    private LicensesRequest(LicensesRequest req) {
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.assetId = Objects.requireNonNull(req.getAssetId(), "assetId cannot be null");
        this.orderId = Objects.requireNonNull(req.getOrderId(), "orderId cannot be null");
        this.expiration = Objects.requireNonNull(req.getExpiration(), "expiration cannot be null");
        this.licenses = Objects.requireNonNull(req.getLicenses(), "licenses cannot be null");
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

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public List<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<String> licenses) {
        this.licenses = licenses;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicensesRequest that = (LicensesRequest) o;
        return Objects.equals(orderId, that.orderId) && Objects.equals(
                expiration,
                that.expiration
        ) && Objects.equals(licenses, that.licenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, expiration, licenses);
    }

    public enum ACTION {
        ALLOCATE,
        DEALLOCATE
    }
}
