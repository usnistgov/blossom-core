package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class Feedback implements Serializable {

    @Property
    private int atoVersion;

    @Property
    private String org;

    @Property
    private String comments;

    public Feedback() {
    }

    public Feedback(@JsonProperty int atoVersion, @JsonProperty String org, @JsonProperty String comments) {
        this.atoVersion = atoVersion;
        this.org = org;
        this.comments = comments;
    }

    public int getAtoVersion() {
        return atoVersion;
    }

    public void setAtoVersion(int atoVersion) {
        this.atoVersion = atoVersion;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Feedback feedback = (Feedback) o;
        return atoVersion == feedback.atoVersion && Objects.equals(org, feedback.org) && Objects.equals(
                comments, feedback.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atoVersion, org, comments);
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "atoVersion=" + atoVersion +
                ", org='" + org + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
