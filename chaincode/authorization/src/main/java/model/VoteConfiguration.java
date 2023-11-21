package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.io.Serializable;
import java.util.Objects;

@DataType
public class VoteConfiguration implements Serializable {

    @Property
    private boolean voteOnSelf;

    @Property
    private boolean voteWhenNotAuthorized;

    @Property
    private boolean initiateVoteOnSelfWhenNotAuthorized;

    @Property
    private boolean certifyOrAbortVoteWhenNotAuthorized;

    public VoteConfiguration(@JsonProperty boolean voteOnSelf, @JsonProperty boolean voteWhenNotAuthorized,
                             @JsonProperty boolean initiateVoteOnSelfWhenNotAuthorized, @JsonProperty boolean certifyOrAbortVoteWhenNotAuthorized) {
        this.voteOnSelf = voteOnSelf;
        this.voteWhenNotAuthorized = voteWhenNotAuthorized;
        this.initiateVoteOnSelfWhenNotAuthorized = initiateVoteOnSelfWhenNotAuthorized;
        this.certifyOrAbortVoteWhenNotAuthorized = certifyOrAbortVoteWhenNotAuthorized;
    }

    public boolean isVoteOnSelf() {
        return voteOnSelf;
    }

    public void setVoteOnSelf(boolean voteOnSelf) {
        this.voteOnSelf = voteOnSelf;
    }

    public boolean isVoteWhenNotAuthorized() {
        return voteWhenNotAuthorized;
    }

    public void setVoteWhenNotAuthorized(boolean voteWhenNotAuthorized) {
        this.voteWhenNotAuthorized = voteWhenNotAuthorized;
    }

    public boolean isInitiateVoteOnSelfWhenNotAuthorized() {
        return initiateVoteOnSelfWhenNotAuthorized;
    }

    public void setInitiateVoteOnSelfWhenNotAuthorized(boolean initiateVoteOnSelfWhenNotAuthorized) {
        this.initiateVoteOnSelfWhenNotAuthorized = initiateVoteOnSelfWhenNotAuthorized;
    }

    public boolean isCertifyOrAbortVoteWhenNotAuthorized() {
        return certifyOrAbortVoteWhenNotAuthorized;
    }

    public void setCertifyOrAbortVoteWhenNotAuthorized(boolean certifyOrAbortVoteWhenNotAuthorized) {
        this.certifyOrAbortVoteWhenNotAuthorized = certifyOrAbortVoteWhenNotAuthorized;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        VoteConfiguration that = (VoteConfiguration) o;
        return voteOnSelf == that.voteOnSelf && voteWhenNotAuthorized == that.voteWhenNotAuthorized && initiateVoteOnSelfWhenNotAuthorized == that.initiateVoteOnSelfWhenNotAuthorized && certifyOrAbortVoteWhenNotAuthorized == that.certifyOrAbortVoteWhenNotAuthorized;
    }

    @Override
    public int hashCode() {
        return Objects.hash(voteOnSelf, voteWhenNotAuthorized, initiateVoteOnSelfWhenNotAuthorized,
                            certifyOrAbortVoteWhenNotAuthorized
        );
    }

    @Override
    public String toString() {
        return "VoteConfiguration{" +
                "voteOnSelf=" + voteOnSelf +
                ", voteWhenNotAuthorized=" + voteWhenNotAuthorized +
                ", initiateVoteOnSelfWhenNotAuthorized=" + initiateVoteOnSelfWhenNotAuthorized +
                ", certifyOrAbortVoteWhenNotAuthorized=" + certifyOrAbortVoteWhenNotAuthorized +
                '}';
    }
}
