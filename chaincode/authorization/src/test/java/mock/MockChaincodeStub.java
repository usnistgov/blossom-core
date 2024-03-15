package mock;

import model.Status;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.protos.peer.ChaincodeEventPackage;
import org.hyperledger.fabric.protos.peer.ProposalPackage;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.*;
import org.mockito.Mock;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class MockChaincodeStub implements ChaincodeStub {

    private byte[] creator;
    private Map<String, byte[]> transientData;
    private Map<String, List<byte[]>> ledger;
    private Chaincode.Response getAccountStatusResponse;
    private Map<String, Map<String, byte[]>> privateData;
    private Instant timestamp;
    private String txId;
    private MockEvent mockEvent;

    public MockChaincodeStub(MockIdentity initialIdentity) {
        setCreator(initialIdentity);
        this.ledger = new HashMap<>();
        this.transientData = new HashMap<>();
        this.privateData = new HashMap<>();
    }

    public void addImplicitPrivateDataCollection(String org) {
        privateData.put("_implicit_org_" + org, new HashMap<>());
    }

    public void addPrivateDataCollection(String collectionName) {
        privateData.put(collectionName, new HashMap<>());
    }

    public void setCreator(MockIdentity mockIdentity) {
        this.creator = mockIdentity.getBytes();
    }

    public void setCreator(byte [] creator) {
        this.creator = creator;
    }

    public void setTransientData(Map<String, String> transientData) {
        this.transientData.clear();
        for (Map.Entry<String, String> e : transientData.entrySet()){
            this.transientData.put(e.getKey(), e.getValue().getBytes(StandardCharsets.UTF_8));
        }
    }

    public void setTimestamp(Instant instant) {
        this.timestamp = instant;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public void setAccountStatus(Status status) {
        getAccountStatusResponse = new Chaincode.Response(
                200,
                "success",
                SerializationUtils.serialize(status)
        );
    }

    @Override
    public List<byte[]> getArgs() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getStringArgs() {
        return new ArrayList<>();
    }

    @Override
    public String getFunction() {
        return "";
    }

    @Override
    public List<String> getParameters() {
        return getStringArgs();
    }

    @Override
    public String getTxId() {
        return txId;
    }

    @Override
    public String getChannelId() {
        return "";
    }

    @Override
    public Chaincode.Response invokeChaincode(String chaincodeName, List<byte[]> args, String channel) {
        return Objects.requireNonNullElseGet(getAccountStatusResponse, () -> new Chaincode.Response(
                400,
                "mock invokeChaincode response not set",
                new byte[]{}
        ));

    }

    @Override
    public byte[] getState(String key) {
        if (!ledger.containsKey(key)) {
            return new byte[]{};
        }

        return ledger.get(key).get(0);
    }

    @Override
    public byte[] getStateValidationParameter(String key) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void putState(String key, byte[] value) {
        List<byte[]> keyHistory = ledger.getOrDefault(key, new ArrayList<>());
        keyHistory.add(0, value);
        ledger.put(key, keyHistory);
    }

    @Override
    public void setStateValidationParameter(String key, byte[] value) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void delState(String key) {
        ledger.remove(key);
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByRange(String startKey, String endKey) {
        List<KeyValue> keyValues = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> e : ledger.entrySet()) {
            if (!(e.getKey().contains(startKey) && e.getKey().contains(endKey))) {
                continue;
            }

            keyValues.add(new MockKeyValue(e.getKey(), e.getValue().get(0)));
        }

        return new MockQueryResultsIterator(keyValues);
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(String startKey, String endKey, int pageSize, String bookmark) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String compositeKey) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(String objectType, String... attributes) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(CompositeKey compositeKey) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(CompositeKey compositeKey, int pageSize, String bookmark) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public CompositeKey createCompositeKey(String objectType, String... attributes) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public CompositeKey splitCompositeKey(String compositeKey) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getQueryResult(String query) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(String query, int pageSize, String bookmark) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyModification> getHistoryForKey(String key) {
        if (!ledger.containsKey(key)) {
            return new MockQueryResultsIterator<>(new ArrayList<>());
        }

        List<byte[]> history = ledger.get(key);
        List<KeyModification> keyModifications = new ArrayList<>();
        for (byte[] bytes : history) {
            keyModifications.add(new MockKeyModification(new MockKeyValue(key, bytes)));
        }

        return new MockQueryResultsIterator<>(keyModifications);
    }

    @Override
    public byte[] getPrivateData(String collection, String key) {
        if (!privateData.containsKey(collection)) {
            return new byte[]{};
        }

        Map<String, byte[]> data = privateData.get(collection);
        if (!data.containsKey(key)) {
            return new byte[]{};
        }

        return data.get(key);
    }

    @Override
    public byte[] getPrivateDataHash(String collection, String key) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public byte[] getPrivateDataValidationParameter(String collection, String key) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void putPrivateData(String collection, String key, byte[] value) {
        Map<String, byte[]> pdc = privateData.get(collection);
        if (pdc == null) {
            return;
        }

        pdc.put(key, value);
    }

    @Override
    public void setPrivateDataValidationParameter(String collection, String key, byte[] value) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void delPrivateData(String collection, String key) {
        Map<String, byte[]> pdc = privateData.get(collection);
        if (pdc == null) {
            return;
        }

        pdc.remove(key);
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByRange(String collection, String startKey, String endKey) {
        return null;
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String compositeKey) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, CompositeKey compositeKey) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(String collection, String objectType, String... attributes) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(String collection, String query) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void setEvent(String name, byte[] payload) {
        this.mockEvent = new MockEvent(name, payload);
    }

    public MockEvent getMockEvent() {
        return mockEvent;
    }

    @Override
    public ChaincodeEventPackage.ChaincodeEvent getEvent() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public ProposalPackage.SignedProposal getSignedProposal() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public Instant getTxTimestamp() {
        return timestamp;
    }

    @Override
    public byte[] getCreator() {
        if (creator == null) {
            throw new ChaincodeException("mock chaincode creator not set");
        }

        return creator;
    }

    @Override
    public Map<String, byte[]> getTransient() {
        return transientData;
    }

    @Override
    public byte[] getBinding() {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public String getMspId() {
        throw new RuntimeException("not yet implemented");
    }
}
