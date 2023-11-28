package contract;

import mock.MockContext;
import mock.MockIdentity;
import model.Account;
import model.Status;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static contract.AccountContract.accountKey;
import static mock.MockOrgs.*;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapContractTest {

    @Test
    void testSuccess() {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        mockContext.setTimestamp(Instant.now());
        blossomContract.Bootstrap(mockContext);

        assertNotNull(mockContext.getStub().getState("policy"));
        assertNotNull(mockContext.getStub().getState(accountKey(ORG1_MSP)));

        Account account = new AccountContract().GetAccount(mockContext, ORG1_MSP);
        assertEquals(new Account(ORG1_MSP, Status.AUTHORIZED, 0, true), account);
    }

    @Test
    void testBootstrapTwice() {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        Instant now = Instant.now();
        mockContext.setTimestamp(now);
        mockContext.setTxId("123");
        blossomContract.Bootstrap(mockContext);
        assertThrows(ChaincodeException.class, () -> blossomContract.Bootstrap(mockContext));
    }
}