package model;

import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Allocated implements Serializable {

    public static Allocated fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    @Property
    private String licenseId;
    @Property
    private String account;
    @Property
    private String expiration;
    @Property
    private String orderId;

    public Allocated(String licenseId, String account, String expiration, String orderId) {
        this.licenseId = licenseId;
        this.account = account;
        this.expiration = expiration;
        this.orderId = orderId;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
        Allocated allocated = (Allocated) o;
        return Objects.equals(licenseId, allocated.licenseId) && Objects.equals(
                account,
                allocated.account
        ) && Objects.equals(expiration, allocated.expiration) && Objects.equals(
                orderId,
                allocated.orderId
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenseId, account, expiration, orderId);
    }

    @Override
    public String toString() {
        return "Allocated{" +
                "licenseId='" + licenseId + '\'' +
                ", account='" + account + '\'' +
                ", expiration='" + expiration + '\'' +
                ", orderId='" + orderId + '\'' +
                '}';
    }
}
