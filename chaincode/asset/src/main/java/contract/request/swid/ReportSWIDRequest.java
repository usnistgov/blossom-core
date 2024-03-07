package contract.request.swid;

import com.google.gson.Gson;
import contract.request.order.ReturnLicensesRequest;
import model.SWID;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class ReportSWIDRequest {

    private final String account;
    private final String primaryTag;
    private final String xml;
    private final String assetId;
    private final String licenseId;

    public ReportSWIDRequest(Context ctx) {
        this(new Gson().fromJson(
                new String(ctx.getStub().getTransient().get("request"), StandardCharsets.UTF_8),
                ReportSWIDRequest.class));
    }

    private ReportSWIDRequest(ReportSWIDRequest req) {
        this.account = Objects.requireNonNull(req.getAccount(), "account cannot be null");
        this.primaryTag = Objects.requireNonNull(req.getPrimaryTag(), "primaryTag cannot be null");
        this.xml = Objects.requireNonNull(req.getXml(), "xml cannot be null");
        this.assetId = Objects.requireNonNull(req.getAssetId(), "assetId cannot be null");
        this.licenseId = Objects.requireNonNull(req.getLicenseId(), "licenseId cannot be null");
    }

    public String getAccount() {
        return account;
    }

    public String getPrimaryTag() {
        return primaryTag;
    }

    public String getXml() {
        return xml;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getLicenseId() {
        return licenseId;
    }
}
