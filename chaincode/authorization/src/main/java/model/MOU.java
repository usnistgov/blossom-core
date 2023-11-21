package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class MOU implements Serializable {

    @Property
    private String text;

    @Property
    private int version;

    @Property
    private String timestamp;

    public MOU() {
    }

    public MOU(@JsonProperty String text, @JsonProperty int version, @JsonProperty String timestamp) {
        this.text = text;
        this.version = version;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MOU mou = (MOU) o;
        return Objects.equals(text, mou.text) && Objects.equals(
                version, mou.version) && Objects.equals(timestamp, mou.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, version, timestamp);
    }

    @Override
    public String toString() {
        return "MOU{" +
                "text='" + text + '\'' +
                ", version=" + version +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
