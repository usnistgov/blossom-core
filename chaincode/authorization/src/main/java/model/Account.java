package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

/**
 * Blossom account information.
 */
@DataType
public class Account implements Serializable {

    /**
     * Account ID (Fabric member MSPID)
     */
    @Property
    private String id;

    /**
     * Current account status
     */
    @Property
    private Status status;


    /**
     * The most recent version of the MOU that the account has signed.
     */
    @Property
    private int mouVersion;

    /**
     * Indicates the accounts commitment to join.
     */
    @Property
    private boolean joined;

    public Account() {
    }

    public Account(@JsonProperty String id, @JsonProperty Status status, @JsonProperty int mouVersion, @JsonProperty boolean joined) {
        this.id = id;
        this.status = status;
        this.mouVersion = mouVersion;
        this.joined = joined;
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

    public int getMouVersion() {
        return mouVersion;
    }

    public void setMouVersion(int mouVersion) {
        this.mouVersion = mouVersion;
    }

    public boolean isJoined() {
        return joined;
    }

    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Account account = (Account) o;
        return mouVersion == account.mouVersion && joined == account.joined && Objects.equals(
                id, account.id) && status == account.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, mouVersion, joined);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", mouVersion=" + mouVersion +
                ", joined=" + joined +
                '}';
    }
}
