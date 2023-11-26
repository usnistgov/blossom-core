package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import mock.MockContext;
import mock.MockIdentity;
import model.ATO;
import model.Account;
import model.Status;
import model.VoteConfiguration;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static contract.AccountContract.accountKey;
import static contract.VoteContract.VOTE_CONFIG_KEY;
import static mock.MockOrgs.*;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapContractTest {

    public static VoteConfiguration TEST_VOTE_CONFIG = new VoteConfiguration(true, true, true, false);

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

    @Test
    void testBootstrapSetsDefaultVoteConfig() {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        BootstrapContract bootstrapContract = new BootstrapContract();
        mockContext.setTimestamp(Instant.now());
        bootstrapContract.Bootstrap(mockContext);
        VoteConfiguration actual = new VoteContract().GetVoteConfiguration(mockContext);
        assertEquals(
                new VoteConfiguration(true, true, true, false),
                actual
        );
    }
}