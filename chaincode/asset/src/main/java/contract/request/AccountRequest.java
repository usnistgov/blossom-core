package contract.request;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AccountRequest {

    private String account;

    public AccountRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                AccountRequest.class));
    }

    private AccountRequest(AccountRequest req) {
        this.account = Objects.requireNonNull(req.account, "account cannot be null");
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
