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

        assertTrue(mockContext.getStub().getState("policy").length > 0);
        assertTrue(mockContext.getStub().getState(accountKey(ORG1_MSP)).length > 0);

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

    @Test
    void testUnauthorized() {
        MockContext ctx = new MockContext(MockIdentity.ORG2_AO);
        assertThrows(ChaincodeException.class, () -> new BootstrapContract().Bootstrap(ctx));
    }
}