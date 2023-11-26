package contract.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class SignMOUEvent implements Serializable {

    @Property
    private String account;
    @Property
    private int version;

    public SignMOUEvent() {
    }

    public SignMOUEvent(@JsonProperty String account, @JsonProperty int version) {
        this.account = account;
        this.version = version;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SignMOUEvent that = (SignMOUEvent) o;
        return version == that.version && Objects.equals(account, that.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, version);
    }
}
