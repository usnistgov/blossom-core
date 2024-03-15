package contract.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class SubmitFeedbackEvent implements Serializable {

    @Property
    private String targetAccountId;
    @Property
    private String commenterAccountId;

    public SubmitFeedbackEvent() {
    }

    public SubmitFeedbackEvent(@JsonProperty String targetAccountId, @JsonProperty String commenterAccountId) {
        this.commenterAccountId = commenterAccountId;
        this.targetAccountId = targetAccountId;
    }

    public String getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(String targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public String getCommenterAccountId() {
        return commenterAccountId;
    }

    public void setCommenterAccountId(String commenterAccountId) {
        this.commenterAccountId = commenterAccountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SubmitFeedbackEvent that = (SubmitFeedbackEvent) o;
        return Objects.equals(targetAccountId, that.targetAccountId) && Objects.equals(
                commenterAccountId, that.commenterAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetAccountId, commenterAccountId);
    }
}
