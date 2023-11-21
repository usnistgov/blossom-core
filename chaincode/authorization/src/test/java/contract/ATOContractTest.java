package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockEvent;
import mock.MockIdentity;
import model.ATO;
import model.Account;
import model.Feedback;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static contract.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static model.Status.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ATOContractTest {

    ATOContract contract = new ATOContract();

    @Nested
    class CreateATOTest {

        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            new ArrayList<>()
                    ),
                    account.getAto()
            );
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("CreateATO", mockEvent.getName());
            Account account = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Account(ORG2_MSP, PENDING, new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of()
                    ), 1),
                    account
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_NON_AO);

            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.CreateATO(mockCtx, "memo", "artifacts"));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> contract.CreateATO(mockCtx, "memo", "artifacts")
            );
            assertEquals("account Org3MSP has not yet joined", e.getMessage());
        }

    }

    @Nested
    class UpdateATOTest {
        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            Instant now2 = Instant.now();
            mockCtx.setTimestamp(now2);
            contract.UpdateATO(mockCtx, "memo2", "artifacts2");

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now2.toString(),
                            2,
                            "memo2",
                            "artifacts2",
                            new ArrayList<>()
                    ),
                    account.getAto()
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG2_NON_AO);

            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.UpdateATO(mockCtx, "memo", "artifacts"));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> contract.CreateATO(mockCtx, "memo", "artifacts")
            );
            assertEquals("account Org3MSP has not yet joined", e.getMessage());
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            Instant now2 = Instant.now();
            mockCtx.setTimestamp(now2);
            contract.UpdateATO(mockCtx, "memo2", "artifacts2");

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("UpdateATO", mockEvent.getName());
            Account account = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Account(ORG2_MSP, PENDING, new ATO(
                            "123",
                            now.toString(),
                            now2.toString(),
                            2,
                            "memo2",
                            "artifacts2",
                            List.of()
                    ), 1),
                    account
            );
        }
    }

    @Nested
    class SubmitFeedback {

        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED.toString());
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED.toString());

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1");
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment2");

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of(
                                    new Feedback(1, "Org3MSP", "comment1"),
                                    new Feedback(1, "Org3MSP", "comment2")
                            )
                    ),
                    account.getAto()
            );
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED.toString());
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED.toString());

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1");
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment2");

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("SubmitFeedback", mockEvent.getName());
            Account account = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Account(ORG2_MSP, AUTHORIZED, new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of(
                                    new Feedback(1, "Org3MSP", "comment1"),
                                    new Feedback(1, "Org3MSP", "comment2")
                            )
                    ), 1),
                    account
            );
        }

        @Test
        void testCreateATOResetsFeedback() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED.toString());
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED.toString());

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1");
            contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment2");

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of(
                                    new Feedback(1, "Org3MSP", "comment1"),
                                    new Feedback(1, "Org3MSP", "comment2")
                            )
                    ),
                    account.getAto()
            );

            mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
            now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("1234");
            contract.CreateATO(mockCtx, "memo2", "artifacts2");
            account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new ATO(
                            "1234",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo2",
                            "artifacts2",
                            List.of()
                    ),
                    account.getAto()
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG2_NON_AO);

            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.SubmitFeedback(mockCtx, "Org1MSP", 1, ""));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testFeedbackOnOutdatedATOVersion() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            contract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            ChaincodeException e =
                    assertThrows(ChaincodeException.class,
                                                () -> contract.SubmitFeedback(mockCtx, "Org2MSP", 2, "comment1"));
            assertEquals("submitting feedback on incorrect ATO version: current version 1, got 2", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1")
            );
            assertEquals("account Org2MSP has not yet created an ATO", e.getMessage());
        }

        @Test
        void testBeforeTargetOrgJoins() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(mockCtx, "Org3MSP", 1, "comment1")
            );
            assertEquals("account Org3MSP has not yet created an ATO", e.getMessage());
        }
    }
}
