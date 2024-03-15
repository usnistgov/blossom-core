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
    private String targetAccountId;

    @Property
    private String comments;

    public Feedback() {
    }

    public Feedback(@JsonProperty int atoVersion, @JsonProperty String targetAccountId, @JsonProperty String comments) {
        this.atoVersion = atoVersion;
        this.targetAccountId = targetAccountId;
        this.comments = comments;
    }

    public int getAtoVersion() {
        return atoVersion;
    }

    public void setAtoVersion(int atoVersion) {
        this.atoVersion = atoVersion;
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(String targetAccountId) {
        this.targetAccountId = targetAccountId;
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
        return atoVersion == feedback.atoVersion && Objects.equals(targetAccountId, feedback.targetAccountId) && Objects.equals(
                comments, feedback.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atoVersion, targetAccountId, comments);
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "atoVersion=" + atoVersion +
                ", org='" + targetAccountId + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}
