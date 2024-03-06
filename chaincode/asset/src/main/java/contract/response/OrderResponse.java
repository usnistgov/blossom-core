package contract.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import model.AllocatedLicenses;
import model.License;
import model.Order;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType
public class OrderResponse {

    @Property
    private Order order;
    @Property
    private AllocatedLicenses licenses;

    public OrderResponse(@JsonProperty Order order, @JsonProperty AllocatedLicenses licenses) {
        this.order = order;
        this.licenses = licenses;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public AllocatedLicenses getLicenses() {
        return licenses;
    }

    public void setLicenses(AllocatedLicenses licenses) {
        this.licenses = licenses;
    }
}
