package contract.request.asset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.DateFormatter;
import org.hyperledger.fabric.contract.Context;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddAssetRequest {

    private final String name;
    private final String endDate;
    private final Set<String> licenses;

    public AddAssetRequest(String name, String endDate, Set<String> licenses) {
        this.name = name;
        this.endDate = endDate;
        this.licenses = licenses;
    }

    public AddAssetRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("name");
        if (bytes == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        name = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("endDate");
        if (bytes == null) {
            throw new IllegalArgumentException("endDate cannot be null");
        }

        endDate = new String(bytes, StandardCharsets.UTF_8);
        DateFormatter.checkDateFormat(endDate);

        bytes = t.get("licenses");
        if (bytes == null) {
            throw new IllegalArgumentException("licenses cannot be null");
        }

        String s = new String(bytes, StandardCharsets.UTF_8);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = new Gson().fromJson(s, type);

        this.licenses = new HashSet<>(list);
    }

    public String getName() {
        return name;
    }

    public String getEndDate() {
        return endDate;
    }

    public Set<String> getLicenses() {
        return licenses;
    }
}
