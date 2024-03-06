package model;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Allocated implements Serializable {

    @Property
    private String account;
    @Property
    private String expiration;
    @Property
    private String orderId;

    public Allocated(String account, String expiration, String orderId) {
        this.account = account;
        this.expiration = expiration;
        this.orderId = orderId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Allocated allocated = (Allocated) o;
        return Objects.equals(account, allocated.account) && Objects.equals(
                expiration,
                allocated.expiration
        ) && Objects.equals(orderId, allocated.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, expiration, orderId);
    }

    @Override
    public String toString() {
        return "Allocated{" +
                "account='" + account + '\'' +
                ", expiration='" + expiration + '\'' +
                ", orderId='" + orderId + '\'' +
                '}';
    }
}
