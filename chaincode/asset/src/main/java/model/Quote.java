package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType
public class Quote {

    @Property
    private String id;
    @Property
    private String orderId;
    @Property
    private String timestamp;
    @Property
    private String assetId;
    @Property
    private int amount;
    @Property
    private int duration;
    @Property
    private double price;

    public Quote(@JsonProperty String id,
                 @JsonProperty String orderId,
                 @JsonProperty String timestamp,
                 @JsonProperty String assetId,
                 @JsonProperty int amount,
                 @JsonProperty int duration,
                 @JsonProperty double price) {
        this.id = id;
        this.orderId = orderId;
        this.timestamp = timestamp;
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
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

    @Override
    public String toString() {
        return "Quote{" +
                "id='" + id + '\'' +
                ", orderId='" + orderId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", assetId='" + assetId + '\'' +
                ", amount='" + amount + '\'' +
                ", duration='" + duration + '\'' +
                ", price=" + price +
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
        Quote quote = (Quote) o;
        return Double.compare(price, quote.price) == 0 && Objects.equals(
                id,
                quote.id
        ) && Objects.equals(orderId, quote.orderId) && Objects.equals(
                timestamp,
                quote.timestamp
        ) && Objects.equals(assetId, quote.assetId) && Objects.equals(
                amount,
                quote.amount
        ) && Objects.equals(duration, quote.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderId, timestamp, assetId, amount, duration, price);
    }
}
