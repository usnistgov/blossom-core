package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType
public class LicenseWithExpiration {

    @Property
    private String licenseId;
    @Property
    private String expiration;

    public LicenseWithExpiration(@JsonProperty String licenseId, @JsonProperty String expiration) {
        this.licenseId = licenseId;
        this.expiration = expiration;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }
}
