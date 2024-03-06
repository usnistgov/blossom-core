package contract.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Objects;

@DataType
public class AssetResponse {

    @Property
    private String id;
    @Property
    private String name;
    @Property
    private int numAvailable;
    @Property
    private String startDate;
    @Property
    private String endDate;

    public AssetResponse(@JsonProperty String id, @JsonProperty String name, @JsonProperty int numAvailable, @JsonProperty String startDate, @JsonProperty String endDate) {
        this.id = id;
        this.name = name;
        this.numAvailable = numAvailable;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public int getNumAvailable() {
        return numAvailable;
    }

    public void setNumAvailable(int numAvailable) {
        this.numAvailable = numAvailable;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssetResponse that = (AssetResponse) o;
        return numAvailable == that.numAvailable && Objects.equals(id, that.id) && Objects.equals(
                name,
                that.name
        ) && Objects.equals(startDate, that.startDate) && Objects.equals(endDate, that.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, numAvailable, startDate, endDate);
    }

    @Override
    public String toString() {
        return "AssetResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", numAvailable=" + numAvailable +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                '}';
    }
}
