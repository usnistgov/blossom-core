package contract;

import contract.event.ATOEvent;
import contract.event.SubmitFeedbackEvent;
import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockContextUtil;
import mock.MockEvent;
import mock.MockIdentity;
import model.ATO;
import model.Account;
import model.Feedback;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static mock.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static model.Status.PENDING;
import static org.junit.jupiter.api.Assertions.*;

public class ATOContractTest {

    ATOContract contract = new ATOContract();

    @Nested
    class CreateATOTest {

        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            ATO ato = contract.GetATO(mockCtx, ORG2_MSP);
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
                    ato
            );
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            MockEvent actual = mockCtx.getStub().getMockEvent();
            assertEquals(
                    new MockEvent("CreateATO", SerializationUtils.serialize(new ATOEvent(ORG2_MSP))),
                    actual
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_NON_AO);

            mockCtx.setATOTransientData("memo", "artifacts");
            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.CreateATO(mockCtx));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            mockCtx.setATOTransientData("memo", "artifacts");
            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> contract.CreateATO(mockCtx)
            );
            assertEquals("account Org3MSP does not exist", e.getMessage());
        }

        @Test
        void testBlossomAdmin() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG1_AO);

            Instant now = Instant.now();
            ctx.setTxId("123");
            ctx.setTimestamp(now);

            ctx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(ctx);
            ATO ato = contract.GetATO(ctx, ORG1_MSP);
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
                    ato
            );
        }

        @Test
        void testWhenPending() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            ctx.setATOTransientData("memo", "artifacts");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
        }

        @Test
        void testNullOrEmptyInputs() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            ctx.setATOTransientData(null, null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("", null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("memo", null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData(null, "");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData(null, "art");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("mem", "art");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
        }
    }

    @Nested
    class UpdateATOTest {
        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            Instant now2 = Instant.now();
            mockCtx.setTimestamp(now2);
            mockCtx.setATOTransientData("memo2", "artifacts2");
            contract.UpdateATO(mockCtx);

            ATO actual = contract.GetATO(mockCtx, ORG2_MSP);
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
                    actual
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG2_NON_AO);

            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.UpdateATO(mockCtx));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            mockCtx.setATOTransientData("memo", "artifacts");
            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> contract.CreateATO(mockCtx)
            );
            assertEquals("account Org3MSP does not exist", e.getMessage());
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            Instant now2 = Instant.now();
            mockCtx.setTimestamp(now2);
            mockCtx.setATOTransientData("memo2", "artifacts2");
            contract.UpdateATO(mockCtx);

            MockEvent actual = mockCtx.getStub().getMockEvent();
            assertEquals(
                    new MockEvent("UpdateATO", SerializationUtils.serialize(new ATOEvent(ORG2_MSP))),
                    actual
            );
        }

        @Test
        void testWhenPending() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            ctx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(ctx);
            assertDoesNotThrow(() -> contract.UpdateATO(ctx));
        }

        @Test
        void testNullOrEmptyInputs() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            ctx.setATOTransientData(null, null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("", null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("memo", null);
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData(null, "");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData(null, "art");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
            ctx.setATOTransientData("mem", "art");
            assertDoesNotThrow(() -> contract.CreateATO(ctx));
        }
    }

    @Nested
    class GetATOTest {

        @Test
        void testNotYetJoinedCannotGetATO() throws Exception {
            MockContext ctx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.GetATO(ctx, ORG2_MSP));
            assertEquals("account Org3MSP does not exist", e.getMessage());
        }

        @Test
        void testATONotCreatedYet() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.GetATO(ctx, ORG3_MSP));
            assertEquals("Org3MSP has not created an ATO yet", e.getMessage());
        }

        @Test
        void testSuccess() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG3_AO, now);
            updateAccountStatus(ctx, ORG3_MSP, AUTHORIZED);

            ATO expected = new ATO(
                    "123",
                    now.toString(),
                    now.toString(),
                    1,
                    "memo",
                    "artifacts",
                    List.of(
                            new Feedback(1, ORG1_MSP, "comment1")
                    )
            );

            ATO ato = contract.GetATO(ctx, ORG2_MSP);
            assertEquals(expected, ato);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            ato = contract.GetATO(ctx, ORG2_MSP);
            assertEquals(expected, ato);
        }

        @Test
        void testUnauthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG3_AO, now);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.GetATO(ctx, ORG2_MSP)
            );
            assertEquals("cid is not authorized to read the ATO of account Org2MSP", e.getMessage());

            assertDoesNotThrow(() -> contract.GetATO(ctx, ORG3_MSP));
        }

    }

    @Nested
    class SubmitFeedback {

        @Test
        void testSuccess() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment1");
            contract.SubmitFeedback(mockCtx);
            mockCtx.setFeedbackTransientData( ORG2_MSP, "1", "comment2");
            contract.SubmitFeedback(mockCtx);

            ATO actual = contract.GetATO(mockCtx, ORG2_MSP);
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of(
                                    new Feedback(1, ORG3_MSP, "comment1"),
                                    new Feedback(1, ORG3_MSP, "comment2")
                            )
                    ),
                    actual
            );
        }

        @Test
        void testEvent() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment1");
            contract.SubmitFeedback(mockCtx);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment2");
            contract.SubmitFeedback(mockCtx);

            MockEvent mockEvent = mockCtx.getStub().getMockEvent();
            assertEquals("SubmitFeedback", mockEvent.getName());
            SubmitFeedbackEvent actual = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new SubmitFeedbackEvent(ORG2_MSP, ORG3_MSP),
                    actual
            );
        }

        @Test
        void testCreateATOResetsFeedback() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment1");
            contract.SubmitFeedback(mockCtx);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment2");
            contract.SubmitFeedback(mockCtx);

            ATO actual = contract.GetATO(mockCtx, ORG2_MSP);
            assertEquals(
                    new ATO(
                            "123",
                            now.toString(),
                            now.toString(),
                            1,
                            "memo",
                            "artifacts",
                            List.of(
                                    new Feedback(1, ORG3_MSP, "comment1"),
                                    new Feedback(1, ORG3_MSP, "comment2")
                            )
                    ),
                    actual
            );

            mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
            now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("1234");
            mockCtx.setATOTransientData("memo2", "artifacts2");
            contract.CreateATO(mockCtx);
            actual = contract.GetATO(mockCtx, ORG2_MSP);
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
                    actual
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG2_NON_AO);

            mockCtx.setFeedbackTransientData("Org1MSP", "1", "comments");
            ChaincodeException e =
                    assertThrows(ChaincodeException.class, () -> contract.SubmitFeedback(mockCtx));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testFeedbackOnOutdatedATOVersion() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            mockCtx.setATOTransientData("memo", "artifacts");
            contract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "2", "comment1");
            ChaincodeException e =
                    assertThrows(ChaincodeException.class,
                                                () -> contract.SubmitFeedback(mockCtx));
            assertEquals("submitting feedback on incorrect ATO version: current version 1, got 2", e.getMessage());
        }

        @Test
        void testBeforeJoinThrowsException() throws Exception {
            MockContext mockCtx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            mockCtx.setFeedbackTransientData(ORG2_MSP, "1", "comment1");
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(mockCtx)
            );
            assertEquals("account Org3MSP does not exist", e.getMessage());
        }

        @Test
        void testBeforeTargetOrgJoins() throws Exception {
            MockContext mockCtx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
            mockCtx.setFeedbackTransientData(ORG3_MSP, "1", "comment1");
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(mockCtx)
            );
            assertEquals("Org3MSP has not created an ATO yet", e.getMessage());
        }

        @Test
        void testNullOrEmptyInputs() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setFeedbackTransientData(null, null, null);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("targetAccountId cannot be null or empty", e.getMessage());

            ctx.setFeedbackTransientData("", null, null);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("targetAccountId cannot be null or empty", e.getMessage());

            ctx.setFeedbackTransientData(ORG1_MSP, null, null);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("atoVersion cannot be null or empty", e.getMessage());

            ctx.setFeedbackTransientData(ORG1_MSP, "", null);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("atoVersion cannot be null or empty", e.getMessage());

            ctx.setFeedbackTransientData(ORG1_MSP, "1", null);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("comments cannot be null or empty", e.getMessage());

            ctx.setFeedbackTransientData(ORG1_MSP, "1", "");
            e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.SubmitFeedback(ctx)
            );
            assertEquals("comments cannot be null or empty", e.getMessage());

        }
    }
}
