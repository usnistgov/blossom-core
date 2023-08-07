package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockIdentity;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static contract.AccountContract.accountKey;
import static mock.MockOrgs.*;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapContractTest {

    @Test
    void testBootstrapWithATO() throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SYSTEM_OWNER);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        blossomContract.Bootstrap(mockContext, "org1 test ato");

        assertNotNull(mockContext.getStub().getState("policy"));
        assertNotNull(mockContext.getStub().getState(accountKey(ORG1_MSP)));
    }

    @Test
    void testBootstrapTwice() throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SYSTEM_OWNER);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        blossomContract.Bootstrap(mockContext, "org1 test ato");
        assertThrows(ChaincodeException.class, () -> blossomContract.Bootstrap(mockContext, "org1 test ato2"));

        assertEquals("org1 test ato", new AccountContract().GetAccount(mockContext, ORG1_MSP).getAto());
    }

    @Test
    void testBootstrapWithoutATO() throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SYSTEM_OWNER);
        BootstrapContract blossomContract = new BootstrapContract();
        assertThrows(ChaincodeException.class, () -> blossomContract.Bootstrap(mockContext, null));
        assertThrows(ChaincodeException.class, () -> blossomContract.Bootstrap(mockContext, ""));
    }
}