package contract.request.asset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.DateFormatter;
import org.bouncycastle.util.encoders.UTF8;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AddAssetRequest {

    private final String name;
    private final String endDate;
    private final Set<LicenseIdWithSaltRequest> licenses;

    public AddAssetRequest(String name, String endDate, Set<LicenseIdWithSaltRequest> licenses) {
        this.name = name;
        this.endDate = endDate;
        this.licenses = licenses;
    }

    public AddAssetRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AddAssetRequest.class));
    }

    private AddAssetRequest(AddAssetRequest req) {
        this.name = Objects.requireNonNull(req.name, "name cannot be null");
        this.endDate = Objects.requireNonNull(req.endDate, "endDate cannot be null");
        this.licenses = Objects.requireNonNull(req.licenses, "licenses cannot be null");
    }

    public static void main(String[] args) {
        /*JSONObject json = new JSONObject(new String(transientAssetJSON, UTF_8));
        Map<String, Object> tMap = json.toMap();

        type = (String) tMap.get("objectType");
        assetID = (String) tMap.get("assetID");
        color = (String) tMap.get("color");
        if (tMap.containsKey("size")) {
            size = (Integer) tMap.get("size");
        }
        if (tMap.containsKey("appraisedValue")) {
            appraisedValue = (Integer) tMap.get("appraisedValue");
        }*/


        byte[] bytes = new byte[]{
                123,10,32,32,32,32,34,110,97,109,101,34,58,32,34,97,115,115,101,116,49,50,51,34,44,10,
                32,32,32,32,34,101,110,100,68,97,116,101,34,58,32,34,50,48,51,48,45,48,49,45,48,49,34,44,10,32,32,32,32,
                34,108,105,99,101,110,115,101,115,34,58,32,91,10,32,32,32,32,32,32,32,32,123,10,32,32,32,32,32,32,32,32,
                32,32,32,32,34,105,100,34,58,32,34,49,34,44,10,32,32,32,32,32,32,32,32,32,32,32,32,34,115,97,108,116,34,
                58,32,34,49,34,10,32,32,32,32,32,32,32,32,125,44,10,32,32,32,32,32,32,32,32,123,10,32,32,32,32,32,32,32,
                32,32,32,32,32,34,105,100,34,58,32,34,50,34,44,10,32,32,32,32,32,32,32,32,32,32,32,32,34,115,97,108,116,
                34,58,32,34,50,34,10,32,32,32,32,32,32,32,32,125,44,10,32,32,32,32,32,32,32,32,123,10,32,32,32,32,32,32,
                32,32,32,32,32,32,34,105,100,34,58,32,34,51,34,44,10,32,32,32,32,32,32,32,32,32,32,32,32,34,115,97,108,
                116,34,58,32,34,51,34,10,32,32,32,32,32,32,32,32,125,10,32,32,32,32,93,10,125
        };

        AddAssetRequest map = new Gson().fromJson(new String(bytes, StandardCharsets.UTF_8), AddAssetRequest.class);
        System.out.println(map);
    }

    public String getName() {
        return name;
    }

    public String getEndDate() {
        return endDate;
    }

    public Set<LicenseIdWithSaltRequest> getLicenses() {
        return licenses;
    }
}
