package mock;

import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.ServerError;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

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

    public void setTransientData(Map<String, String> data) {
        ((MockChaincodeStub) stub).setTransientData(data);
    }

    public void setATOTransientData(String memo, String artifacts) {
        HashMap<String, String> map = new HashMap<>();
        if (memo != null) {
            map.put("memo", memo);
        }

        if (artifacts != null) {
            map.put("artifacts", artifacts);
        }

        ((MockChaincodeStub) stub).setTransientData(map);
    }

    public void setFeedbackTransientData(String targetAccountId, String atoVersion, String comments) {
        HashMap<String, String> map = new HashMap<>();
        if (targetAccountId != null) {
            map.put("targetAccountId", targetAccountId);
        }

        if (atoVersion != null) {
            map.put("atoVersion", String.valueOf(atoVersion));
        }

        if (comments != null) {
            map.put("comments", comments);
        }

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
