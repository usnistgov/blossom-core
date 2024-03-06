package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

import static contract.OrderContract.ORDER_PREFIX;

@DataType
public class Order implements Serializable {

    @Property
    private String id;
    @Property
    private String account;
    @Property
    private Status status;
    @Property
    private String initiationDate;
    @Property
    private String approvalDate;
    @Property
    private String allocatedDate;
    @Property
    private String latestRenewalDate;
    @Property
    private String assetId;
    @Property
    private int amount;
    @Property
    private int duration;
    @Property
    private double price;

    public Order(@JsonProperty String id,
                 @JsonProperty String account,
                 @JsonProperty Status status,
                 @JsonProperty String initiationDate,
                 @JsonProperty String approvalDate,
                 @JsonProperty String allocatedDate,
                 @JsonProperty String latestRenewalDate,
                 @JsonProperty String assetId,
                 @JsonProperty int amount,
                 @JsonProperty int duration,
                 @JsonProperty double price) {
        this.id = id;
        this.account = account;
        this.status = status;
        this.initiationDate = initiationDate;
        this.approvalDate = approvalDate;
        this.allocatedDate = allocatedDate;
        this.latestRenewalDate = latestRenewalDate;
        this.assetId = assetId;
        this.amount = amount;
        this.duration = duration;
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getInitiationDate() {
        return initiationDate;
    }

    public void setInitiationDate(String initiationDate) {
        this.initiationDate = initiationDate;
    }

    public String getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(String approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getAllocatedDate() {
        return allocatedDate;
    }

    public void setAllocatedDate(String completedDate) {
        this.allocatedDate = completedDate;
    }

    public String getLatestRenewalDate() {
        return latestRenewalDate;
    }

    public void setLatestRenewalDate(String latestRenewalDate) {
        this.latestRenewalDate = latestRenewalDate;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
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
        Order order = (Order) o;
        return amount == order.amount && duration == order.duration && Objects.equals(
                id,
                order.id
        ) && Objects.equals(account, order.account) && status == order.status && Objects.equals(
                initiationDate,
                order.initiationDate
        ) && Objects.equals(approvalDate, order.approvalDate) && Objects.equals(
                allocatedDate,
                order.allocatedDate
        ) && Objects.equals(latestRenewalDate, order.latestRenewalDate) && Objects.equals(
                assetId,
                order.assetId
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                account,
                status,
                initiationDate,
                approvalDate,
                allocatedDate,
                latestRenewalDate,
                assetId,
                amount,
                duration
        );
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", account='" + account + '\'' +
                ", status=" + status +
                ", initiationDate='" + initiationDate + '\'' +
                ", approvalDate='" + approvalDate + '\'' +
                ", completedDate='" + allocatedDate + '\'' +
                ", latestRenewalDate='" + latestRenewalDate + '\'' +
                ", assetId='" + assetId + '\'' +
                ", amount=" + amount +
                ", duration=" + duration +
                '}';
    }

    public static String orderKey(String orderId, String account) {
        return ORDER_PREFIX + orderId + ":" + account;
    }

    public static Order fromByteArray(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    @DataType
    public enum Status {
        QUOTE_REQUESTED,
        QUOTE_RECEIVED,
        INITIATED,
        APPROVED,
        DENIED,
        ALLOCATED,// -> final stage for new and renewal
        RENEWAL_QUOTE_REQUESTED,
        RENEWAL_QUOTE_RECEIVED,
        RENEWAL_INITIATED,
        RENEWAL_APPROVED,
        RENEWAL_DENIED,
    }
}
