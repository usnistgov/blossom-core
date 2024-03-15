package contract.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.License;
import model.LicenseWithExpiration;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@DataType
public class AssetDetailResponse {

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
    @Property
    private int totalAmount;
    @Property
    private Set<String> availableLicenses;
    @Property
    private Map<String, Map<String, Set<LicenseWithExpiration>>> allocatedLicenses;

    public AssetDetailResponse(@JsonProperty String id,
                               @JsonProperty String name,
                               @JsonProperty int numAvailable,
                               @JsonProperty String startDate,
                               @JsonProperty String endDate,
                               @JsonProperty int totalAmount,
                               @JsonProperty Set<String> availableLicenses,
                               @JsonProperty Map<String, Map<String, Set<LicenseWithExpiration>>> allocatedLicenses) {
        this.id = id;
        this.name = name;
        this.numAvailable = numAvailable;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
        this.availableLicenses = availableLicenses;
        this.allocatedLicenses = allocatedLicenses;
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

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Set<String> getAvailableLicenses() {
        return availableLicenses;
    }

    public void setAvailableLicenses(Set<String> availableLicenses) {
        this.availableLicenses = availableLicenses;
    }

    public Map<String, Map<String, Set<LicenseWithExpiration>>> getAllocatedLicenses() {
        return allocatedLicenses;
    }

    public void setAllocatedLicenses(Map<String, Map<String, Set<LicenseWithExpiration>>> allocatedLicenses) {
        this.allocatedLicenses = allocatedLicenses;
    }

    @Override
    public String toString() {
        return "AssetDetailResponse{" +
                "totalAmount=" + totalAmount +
                ", availableLicenses=" + availableLicenses +
                ", allocatedLicenses=" + allocatedLicenses +
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
        if (!super.equals(o)) {
            return false;
        }

        AssetDetailResponse that = (AssetDetailResponse) o;
        return totalAmount == that.totalAmount && Objects.equals(
                availableLicenses,
                that.availableLicenses
        ) && Objects.equals(allocatedLicenses, that.allocatedLicenses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), totalAmount, availableLicenses, allocatedLicenses);
    }
}
