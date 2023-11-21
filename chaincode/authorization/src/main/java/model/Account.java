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
     * Account ATO
     */
    @Property
    private ATO ato;

    /**
     * The most recent version of the MOU that the account has signed.
     */
    @Property
    private int mouVersion;

    public Account(@JsonProperty String id, @JsonProperty Status status,
                   @JsonProperty ATO ato, @JsonProperty int mouVersion) {
        this.id = id;
        this.status = status;
        this.ato = ato;
        this.mouVersion = mouVersion;
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

    public ATO getAto() {
        return ato;
    }

    public void setAto(ATO ato) {
        this.ato = ato;
    }

    public int getMouVersion() {
        return mouVersion;
    }

    public void setMouVersion(int mouVersion) {
        this.mouVersion = mouVersion;
    }

    public void updateATO(int version, String lastUpdatedTimestamp, String memo, String artifacts) {
        ato.setVersion(version);
        ato.setLastUpdatedTimestamp(lastUpdatedTimestamp);

        if (memo != null && !memo.isEmpty()) {
            ato.setMemo(memo);
        }

        if (artifacts != null && !artifacts.isEmpty()) {
            ato.setArtifacts(artifacts);
        }
    }

    public void addATOFeedback(Feedback feedback) {
        this.ato.addFeedback(feedback);
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
        return mouVersion == account.mouVersion && Objects.equals(
                id, account.id) && status == account.status && Objects.equals(ato, account.ato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, status, ato, mouVersion);
    }

    @Override
    public String toString() {
        return "Account{" +
                "id='" + id + '\'' +
                ", status=" + status +
                ", ato='" + ato + '\'' +
                ", mouVersion=" + mouVersion +
                '}';
    }
}
