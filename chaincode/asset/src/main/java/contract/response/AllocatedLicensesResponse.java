package contract.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.AllocatedLicenses;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
public class AllocatedLicensesResponse {

    @Property
    List<AllocatedLicenses> allocated;

    public AllocatedLicensesResponse(List<AllocatedLicenses> allocated) {
        this.allocated = allocated;
    }

    public List<AllocatedLicenses> getAllocated() {
        return allocated;
    }

    public void setAllocated(List<AllocatedLicenses> allocated) {
        this.allocated = allocated;
    }
}
