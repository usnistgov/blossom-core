package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

@DataType
public class SWID implements Serializable {

    @Property
    private String id;
    @Property
    private String primaryTag;
    @Property
    private String xml;
    @Property
    private String assetId;
    @Property
    private String licenseId;

    public SWID(@JsonProperty String id,
                @JsonProperty String primaryTag,
                @JsonProperty String xml,
                @JsonProperty String assetId,
                @JsonProperty String licenseId) {
        this.id = id;
        this.primaryTag = primaryTag;
        this.xml = xml;
        this.assetId = assetId;
        this.licenseId = licenseId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    public static SWID fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
