package contract.request.swid;

import model.SWID;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ReportSWIDRequest {

    private String account;
    private String primaryTag;
    private String xml;
    private String assetId;
    private String licenseId;

    public ReportSWIDRequest(Context ctx) {
        Map<String, byte[]> transientData = ctx.getStub().getTransient();

        byte[] bytes = transientData.get("account");
        if (bytes == null) {
            throw new ChaincodeException("account cannot be null");
        }
        this.account = new String(bytes, StandardCharsets.UTF_8);

        bytes = transientData.get("primaryTag");
        if (bytes == null) {
            throw new ChaincodeException("primaryTag cannot be null");
        }
        this.primaryTag = new String(bytes, StandardCharsets.UTF_8);

        bytes = transientData.get("xml");
        if (bytes == null) {
            throw new ChaincodeException("xml cannot be null");
        }
        this.xml = new String(bytes, StandardCharsets.UTF_8);

        bytes = transientData.get("assetId");
        if (bytes == null) {
            throw new ChaincodeException("assetId cannot be null");
        }
        this.assetId = new String(bytes, StandardCharsets.UTF_8);

        bytes = transientData.get("licenseId");
        if (bytes == null) {
            throw new ChaincodeException("licenseId cannot be null");
        }
        this.licenseId = new String(bytes, StandardCharsets.UTF_8);
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPrimaryTag() {
        return primaryTag;
    }

    public void setPrimaryTag(String primaryTag) {
        this.primaryTag = primaryTag;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }
}
