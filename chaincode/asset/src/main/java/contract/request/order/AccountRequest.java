package contract.request.order;

import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AccountRequest {

    private String account;

    public AccountRequest(Context ctx) {
        Map<String, byte[]> t = ctx.getStub().getTransient();

        byte[] bytes = t.get("account");
        if (bytes == null) {
            throw new IllegalArgumentException("account cannot be null");
        }

        this.account = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
