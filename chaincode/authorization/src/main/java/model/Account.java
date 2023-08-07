package model;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Account implements Serializable {

    @Property
    private String id;

    @Property
    private Status status;

    @Property
    private String ato;

    public Account(@JsonProperty String id, @JsonProperty Status status, @JsonProperty String ato) {
        this.id = id;
        this.status = status;
        this.ato = ato;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAto() {
        return ato;
    }

    public void setAto(String ato) {
        this.ato = ato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(id, account.id) && status == account.status && Objects.equals(ato, account.ato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, ato);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", ato='" + ato + '\'' +
                '}';
    }
}
