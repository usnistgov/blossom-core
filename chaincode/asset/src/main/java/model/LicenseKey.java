package model;

public class LicenseKey {

    public static final String LICENSE_PREFIX = "license:";

    private String assetId;
    private String licenseId;
    private String salt;

    public LicenseKey(String assetId, String licenseId, String salt) {
        this.assetId = assetId;
        this.licenseId = licenseId;
        this.salt = salt;
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

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String toKey() {
        return LICENSE_PREFIX + assetId + ":" +  licenseId;
    }

    public String toHashKey() {
        return SHA256.hashStrToStr(LICENSE_PREFIX + salt + licenseId + assetId);
    }
}
