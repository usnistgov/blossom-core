package mock;

import java.util.Arrays;
import java.util.Objects;

public class MockEvent {

    private String name;
    private byte[] payload;

    public MockEvent(String name, byte[] payload) {
        this.name = name;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MockEvent mockEvent = (MockEvent) o;
        return Objects.equals(name, mockEvent.name) && Arrays.equals(payload, mockEvent.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
