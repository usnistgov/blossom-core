package mock;

import org.hyperledger.fabric.shim.ledger.KeyModification;

import java.time.Instant;

public class MockKeyModification implements KeyModification {

    private MockKeyValue mockKeyValue;

    public MockKeyModification(MockKeyValue mockKeyValue) {
        this.mockKeyValue = mockKeyValue;
    }

    @Override
    public String getTxId() {
        return "123";
    }

    @Override
    public byte[] getValue() {
        return mockKeyValue.getValue();
    }

    @Override
    public String getStringValue() {
        return mockKeyValue.getStringValue();
    }

    @Override
    public Instant getTimestamp() {
        return Instant.ofEpochMilli(1);
    }

    @Override
    public boolean isDeleted() {
        return false;
    }
}
