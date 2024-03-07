package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.Serializable;
import java.util.Set;
import java.util.Map;
import java.util.Objects;

@DataType
public class Asset implements Serializable {

    @Property
    private String id;
    @Property
    private String name;
    @Property
    private String startDate;
    @Property
    private String endDate;

    public Asset(@JsonProperty String id,
                 @JsonProperty String name,
                 @JsonProperty String startDate,
                 @JsonProperty String endDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Asset() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Asset asset = (Asset) o;
        return Objects.equals(id, asset.id) && Objects.equals(
                name,
                asset.name
        ) && Objects.equals(startDate, asset.startDate) && Objects.equals(endDate, asset.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, startDate, endDate);
    }

    public static Asset fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
