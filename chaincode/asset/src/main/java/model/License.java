package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class License implements Serializable {

    @Property
    private String id;
    @Property
    private String salt;
    @Property
    private Allocated allocated;

    public License(@JsonProperty String id, @JsonProperty String salt, @JsonProperty Allocated allocated) {
        this.id = id;
        this.salt = salt;
        this.allocated = allocated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public Allocated getAllocated() {
        return allocated;
    }

    public void setAllocated(Allocated allocated) {
        this.allocated = allocated;
    }

    public byte[] toByteArray() {
        return SerializationUtils.serialize(this);
    }

    @Override
    public String toString() {
        return "License{" +
                "id='" + id + '\'' +
                ", salt='" + salt + '\'' +
                ", allocated=" + allocated +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        License license = (License) o;
        return Objects.equals(id, license.id) && Objects.equals(allocated, license.allocated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, allocated);
    }

    public static License fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }
}
