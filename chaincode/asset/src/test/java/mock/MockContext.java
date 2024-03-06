package mock;

import contract.request.asset.AddAssetRequest;
import contract.request.asset.AssetIdRequest;
import contract.request.asset.LicensesRequest;
import contract.request.asset.UpdateEndDateRequest;
import contract.request.order.InitiateOrderRequest;
import contract.request.order.OrderIdAndAccountRequest;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.HashMap;

public class MockContext extends Context {

    private ClientIdentity clientIdentity;

    public MockContext(MockIdentity initialIdentity) {
        super(new MockChaincodeStub(initialIdentity));
        setClientIdentity(initialIdentity);
    }

    public void setClientIdentity(MockIdentity id) {
        try {
            ((MockChaincodeStub) stub).setCreator(id);
            this.clientIdentity = new ClientIdentity(getStub());
        } catch (CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTimestamp(Instant ts) {
        ((MockChaincodeStub) stub).setTimestamp(ts);
    }

    public void setTxId(String txId) {
        ((MockChaincodeStub) stub).setTxId(txId);
    }

    public void setTransientData(AddAssetRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("name", request.getName() == null ? null : SerializationUtils.serialize(request.getName()));
        map.put("endDate", request.getEndDate() == null ? null : SerializationUtils.serialize(request.getEndDate()));
        map.put("licenses", request.getLicenses() == null ? null : SerializationUtils.serialize(request.getLicenses().toArray(new String[]{})));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setTransientData(AssetIdRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("assetId", request.getAssetId() == null ? null : SerializationUtils.serialize(request.getAssetId()));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setTransientData(LicensesRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("assetId", request.getAssetId() == null ? null : SerializationUtils.serialize(request.getAssetId()));
        map.put("licenses", request.getLicenses() == null ? null : SerializationUtils.serialize(request.getLicenses().toArray(new String[]{})));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setTransientData(UpdateEndDateRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("assetId", request.getAssetId() == null ? null : SerializationUtils.serialize(request.getAssetId()));
        map.put("newEndDate", request.getNewEndDate() == null ? null : SerializationUtils.serialize(request.getNewEndDate()));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setTransientData(InitiateOrderRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("assetId", request.getAssetId() == null ? null : SerializationUtils.serialize(request.getAssetId()));
        map.put("orderId", request.getOrderId() == null ? null : SerializationUtils.serialize(request.getOrderId()));
        map.put("amount", SerializationUtils.serialize(request.getAmount()));
        map.put("duration", SerializationUtils.serialize(request.getDuration()));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setTransientData(OrderIdAndAccountRequest request) {
        HashMap<String, byte[]> map = new HashMap<>();
        map.put("orderId", request.getOrderId() == null ? null : SerializationUtils.serialize(request.getOrderId()));
        ((MockChaincodeStub) stub).setTransientData(map);
    }

    @Override
    public MockChaincodeStub getStub() {
        return (MockChaincodeStub) stub;
    }

    @Override
    public ClientIdentity getClientIdentity() {
        return clientIdentity;
    }
}
