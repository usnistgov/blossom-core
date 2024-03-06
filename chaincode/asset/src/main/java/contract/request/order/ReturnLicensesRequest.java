package contract.request.order;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Property;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReturnLicensesRequest {

    private String orderId;
    private String account;
    private String assetId;
    private Set<String> licenses;

    public ReturnLicensesRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("orderId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }
        this.orderId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }
        this.account = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("assetId");
        if (bytes == null) {
            throw new IllegalArgumentException("assetId cannot be null");
        }
        this.assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("licenses");
        if (bytes == null) {
            throw new IllegalArgumentException("licenses cannot be null");
        }
        String s = new String(bytes, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = new Gson().fromJson(s, type);

        this.licenses = new HashSet<>(list);
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public Set<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<String> licenses) {
        this.licenses = licenses;
    }
}
