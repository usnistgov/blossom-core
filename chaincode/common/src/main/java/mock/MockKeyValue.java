package mock;

import org.hyperledger.fabric.shim.ledger.KeyValue;

public class MockKeyValue implements KeyValue {

    private final String key;
    private final byte[] value;

    public MockKeyValue(String key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public String getStringValue() {
        return new String(value);
    }
}
