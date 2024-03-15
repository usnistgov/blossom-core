package contract.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;

@DataType
public class ATOEvent implements Serializable {

    @Property
    private String account;

    public ATOEvent() {
    }

    public ATOEvent(@JsonProperty String account) {
        this.account = account;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}