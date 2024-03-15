package mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockLedger {

    private Map<String, Map<String, List<byte[]>>> channels;

    public MockLedger() {
        this.channels = new HashMap<>();
    }

    public void createChannel(String name) {
        channels.put(name, new HashMap<>());
    }

    public byte[] getState(String channel, String key) {
        Map<String, List<byte[]>> channelState = channels.get(channel);
        if (channelState == null) {
            throw new RuntimeException("channel " + channel + " does not exist");
        }

        List<byte[]> bytes = channelState.get(key);
        if (bytes == null) {
            return null;
        }

        return bytes.get(0);
    }

    public void putState(String channel, String key, byte[] value) {
        Map<String, List<byte[]>> channelState = channels.get(channel);
        if (channelState == null) {
            throw new RuntimeException("channel " + channel + " does not exist");
        }


        List<byte[]> keyHistory = channelState.getOrDefault(key, new ArrayList<>());
        keyHistory.add(0, value);
        channelState.put(key, keyHistory);
        channels.put(channel, channelState);
    }

    public void delState(String channel, String key) {
        Map<String, List<byte[]>> channelState = channels.get(channel);
        if (channelState == null) {
            throw new RuntimeException("channel " + channel + " does not exist");
        }


        List<byte[]> keyHistory = channelState.getOrDefault(key, new ArrayList<>());
        keyHistory.add(0, null);
        channelState.put(key, keyHistory);
        channels.put(channel, channelState);
    }

    public void putStringState(String channel, String key, String value) {
        putState(channel, key, value.getBytes());
    }

    public Map<String, List<byte[]>> getChannelState(String channel) {
        return new HashMap<>(channels.get(channel));
    }

}
