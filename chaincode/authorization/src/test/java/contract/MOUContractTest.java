package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import mock.MockContext;
import mock.MockEvent;
import mock.MockIdentity;
import model.Account;
import model.MOU;
import model.VoteConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static contract.BootstrapContractTest.TEST_VOTE_CONFIG;
import static contract.MockContextUtil.updateAccountStatus;
import static mock.MockOrgs.ORG2_MSP;
import static model.Status.AUTHORIZED;
import static model.Status.PENDING;
import static org.junit.jupiter.api.Assertions.*;

class MOUContractTest {

    MOUContract contract = new MOUContract();

    @Nested
    class UpdateMOUTest {

        @Test
        void testAuthorized() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);

            contract.UpdateMOU(mockCtx, "test mou");

            assertEquals(new MOU("test mou", 2, now.toString()), contract.GetMOU(mockCtx));
        }

        @Test
        void testUnauthorized() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);

            updateAccountStatus(mockCtx, "Org1MSP", PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> contract.UpdateMOU(mockCtx, "test mou"));

            updateAccountStatus(mockCtx, "Org1MSP", AUTHORIZED.toString());
            assertDoesNotThrow(() -> contract.UpdateMOU(mockCtx, "test mou"));
        }

        @Test
        void testEvent() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);

            contract.UpdateMOU(mockCtx, "test mou");

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("UpdateMOU", mockEvent.getName());
            MOU mou = SerializationUtils.deserialize(mockEvent.getPayload());

            assertEquals(
                    new MOU("test mou", 2, now.toString()),
                    mou
            );
        }
    }

    @Nested
    class GetMOUTest {

        @Test
        void testSuccess() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);
            MOU mou = contract.GetMOU(mockCtx);
            assertEquals("mou text", mou.getText());
            assertEquals(1, mou.getVersion());
        }

        @Test
        void testNotYetCreated() throws PMException, IOException {
            MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
            BootstrapContract bootstrapContract = new BootstrapContract();
            mockContext.setTimestamp(Instant.now());
            bootstrapContract.Bootstrap(mockContext, TEST_VOTE_CONFIG, "org1 test ato", "org1 artifacts");

            ChaincodeException e = assertThrows(ChaincodeException.class,
                                                () -> contract.GetMOU(mockContext));
            assertEquals("Blossom MOU has not yet been created", e.getMessage());
        }

    }

    @Test
    void testGetMOUHistory() throws PMException, IOException {
        MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);
        contract.UpdateMOU(mockCtx, "2");
        contract.UpdateMOU(mockCtx, "3");
        contract.UpdateMOU(mockCtx, "4");

        List<MOU> history = contract.GetMOUHistory(mockCtx);
        assertEquals(4, history.size());
    }

    @Nested
    class SignMOUTest {

        @Test
        void testSuccess() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 1);

            mockCtx.setClientIdentity(MockIdentity.ORG1_AO);
            contract.UpdateMOU(mockCtx, "updated mou");

            mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 2);
        }

        @Test
        void testSignOldVersion() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);
            contract.UpdateMOU(mockCtx, "updated mou");

            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> contract.SignMOU(mockCtx, 1)
            );

            assertEquals("signing MOU version 1, expected version 2", e.getMessage());
        }

        @Test
        void testSignBlossomAdmin() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_AO);
            contract.SignMOU(mockCtx, 1);

            Account account = new AccountContract()
                    .GetAccount(mockCtx, "Org1MSP");
            assertEquals(1, account.getMouVersion());

            contract.UpdateMOU(mockCtx, "updated mou");

            contract.SignMOU(mockCtx, 2);

            account = new AccountContract()
                    .GetAccount(mockCtx, "Org1MSP");
            assertEquals(2, account.getMouVersion());
        }

        @Test
        void testUnauthorized() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_NON_AO);
            assertThrows(PMException.class, () -> contract.SignMOU(mockCtx, 1));
        }

        @Test
        void testSignMouBeforeUpdated() throws PMException, IOException {
            MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);

            BootstrapContract bootstrapContract = new BootstrapContract();
            mockContext.setTimestamp(Instant.now());
            bootstrapContract.Bootstrap(mockContext, TEST_VOTE_CONFIG, "org1 test ato", "org1 artifacts");

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.SignMOU(mockContext, 0));
            assertEquals("Blossom MOU has not yet been created", e.getMessage());
        }

        @Test
        void testSignMOUWhenPending() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(mockCtx, "Org2MSP", PENDING.toString());

            mockCtx.setClientIdentity(MockIdentity.ORG1_AO);
            contract.UpdateMOU(mockCtx, "updated mou");

            mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 2);
        }

        @Test
        void testEvent() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 1);

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("SignMOU", mockEvent.getName());
            Account account = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(new Account(
                    ORG2_MSP,
                    null,
                    null,
                    1
            ), account);
        }
    }

    @Nested
    class JoinTest {

        @Test
        void testSuccess() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 1);
            contract.Join(mockCtx);

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(PENDING, account.getStatus());
        }

        @Test
        void testAlreadyJoined() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 1);
            contract.Join(mockCtx);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.Join(mockCtx));
            assertEquals("Org2MSP is already joined", e.getMessage());
        }

        @Test
        void testNotSignedMOU() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.Join(mockCtx));
            assertEquals("Org2MSP must sign the current MOU before joining", e.getMessage());
        }

        @Test
        void testEvent() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
            contract.SignMOU(mockCtx, 1);
            contract.Join(mockCtx);

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("Join", mockEvent.getName());
            Account account = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(new Account(
                    ORG2_MSP,
                    PENDING,
                    null,
                    1
            ), account);
        }
    }
}