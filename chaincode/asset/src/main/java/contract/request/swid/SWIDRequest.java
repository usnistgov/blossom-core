package contract.request.swid;

import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SWIDRequest {

    private final String account;
    private final String id;

    public SWIDRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }

        this.account = new String(bytes, StandardCharsets.UTF_8);

        bytes = t.get("id");
        if (bytes == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        this.id = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getAccount() {
        return account;
    }

    public String getId() {
        return id;
    }
}
