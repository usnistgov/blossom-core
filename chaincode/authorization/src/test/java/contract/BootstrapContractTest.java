package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import mock.MockContext;
import mock.MockIdentity;
import model.ATO;
import model.VoteConfiguration;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static contract.AccountContract.accountKey;
import static mock.MockOrgs.*;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapContractTest {

    public static VoteConfiguration TEST_VOTE_CONFIG = new VoteConfiguration(true, true, true, false);

    @Test
    void testBootstrapWithATO() throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        mockContext.setTimestamp(Instant.now());
        blossomContract.Bootstrap(mockContext,"org1 test ato", "artifacts");

        assertNotNull(mockContext.getStub().getState("policy"));
        assertNotNull(mockContext.getStub().getState(accountKey(ORG1_MSP)));
    }

    @Test
    void testBootstrapTwice() throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        Instant now = Instant.now();
        mockContext.setTimestamp(now);
        mockContext.setTxId("123");
        blossomContract.Bootstrap(mockContext, "org1 test ato", "artifacts");
        assertThrows(ChaincodeException.class, () -> blossomContract.Bootstrap(mockContext, "org1 test ato2", "artifacts"));

        assertEquals(new ATO(
                "123", now.toString(), now.toString(), 1, "org1 test ato", "artifacts", List.of()
        ), new AccountContract().GetAccount(mockContext, ORG1_MSP).getAto());
    }
}