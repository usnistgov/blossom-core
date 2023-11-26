package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@DataType
public class ATO implements Serializable {

    @Property
    private String id;

    @Property
    private String creationTimestamp;

    @Property
    private String lastUpdatedTimestamp;

    @Property
    private int version;

    @Property
    private String memo;

    @Property
    private String artifacts;

    @Property
    private List<Feedback> feedback;

    public ATO() {

    }

    public ATO(@JsonProperty String id, @JsonProperty String creationTimestamp, @JsonProperty String lastUpdatedTimestamp,
               @JsonProperty int version, @JsonProperty String memo, @JsonProperty String artifacts,
               @JsonProperty List<Feedback> feedback) {
        this.id = id;
        this.creationTimestamp = creationTimestamp;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.version = version;
        this.memo = memo;
        this.artifacts = artifacts;
        this.feedback = feedback;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public void setLastUpdatedTimestamp(String lastUpdatedTimestamp) {
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(String artifacts) {
        this.artifacts = artifacts;
    }

    public List<Feedback> getFeedback() {
        return feedback;
    }

    public void setFeedback(List<Feedback> feedback) {
        this.feedback = feedback;
    }

    public void addFeedback(Feedback feedback) {
        this.feedback.add(feedback);
    }

    public void update(int version, String lastUpdatedTimestamp, String memo, String artifacts) {
        setVersion(version);
        setLastUpdatedTimestamp(lastUpdatedTimestamp);

        if (memo != null && !memo.isEmpty()) {
            setMemo(memo);
        }

        if (artifacts != null && !artifacts.isEmpty()) {
            setArtifacts(artifacts);
        }
    }

    public static ATO createFromContext(Context ctx, String memo, String artifacts) {
        return new ATO(
                ctx.getStub().getTxId(),
                ctx.getStub().getTxTimestamp().toString(),
                ctx.getStub().getTxTimestamp().toString(),
                1,
                memo,
                artifacts,
                new ArrayList<>()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ATO ato = (ATO) o;
        return version == ato.version && Objects.equals(id, ato.id) && Objects.equals(
                creationTimestamp, ato.creationTimestamp) && Objects.equals(
                lastUpdatedTimestamp, ato.lastUpdatedTimestamp) && Objects.equals(
                memo, ato.memo) && Objects.equals(artifacts, ato.artifacts) && Objects.equals(
                feedback, ato.feedback);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creationTimestamp, lastUpdatedTimestamp, version, memo, artifacts, feedback);
    }

    @Override
    public String toString() {
        return "ATO{" +
                "id='" + id + '\'' +
                ", creationTimestamp='" + creationTimestamp + '\'' +
                ", lastUpdatedTimestamp='" + lastUpdatedTimestamp + '\'' +
                ", version=" + version +
                ", memo='" + memo + '\'' +
                ", artifacts='" + artifacts + '\'' +
                ", feedback=" + feedback +
                '}';
    }
}
