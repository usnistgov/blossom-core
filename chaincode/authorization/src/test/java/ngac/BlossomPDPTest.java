package ngac;

import contract.VoteContract;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.value.StringValue;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static contract.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static model.Status.PENDING;
import static ngac.BlossomPDP.getUserCtxFromRequest;
import static ngac.BlossomPDP.loadPolicy;
import static org.junit.jupiter.api.Assertions.*;

class BlossomPDPTest {

    BlossomPDP pdp = new BlossomPDP();

    private static void updateAccountStatus(MockContext ctx, String mspid, Status status) throws PMException {
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, getUserCtxFromRequest(ctx));

        PAP pap = new PAP(memoryPolicyStore);
        pap.executePMLFunction(new UserContext("blossom admin"), "updateAccountStatus",
                               new StringValue(mspid), new StringValue(status.toString())
        );

        ctx.getStub().putState("policy", memoryPolicyStore.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testGetAdminMSPID() throws Exception {
        MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
        String actual = BlossomPDP.getAdminMSPID(ctx);
        assertEquals(ORG1_MSP, actual);
    }

    @Nested
    class UpdateMOUTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.updateMOU(ctx));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.updateMOU(ctx));
            assertEquals("cid is not authorized to update the Blossom MOU", e.getMessage());
        }
    }

    @Nested
    class SignMOUTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.signMOU(ctx));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_NON_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.signMOU(ctx));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }

        @Test
        void testSignAgainDoesNotThrowException() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.signMOU(ctx);
            assertDoesNotThrow(() -> pdp.signMOU(ctx));
        }
    }

    @Nested
    class JoinTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            pdp.signMOU(ctx);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.join(ctx, ORG2_MSP));
            assertEquals(
                    "cid is not authorized to join for account Org2MSP",
                    e.getMessage()
            );

            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            assertDoesNotThrow(() -> pdp.signMOU(ctx));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.join(ctx, ORG2_MSP));
            assertEquals("cid is not authorized to join for account Org2MSP", e.getMessage());
        }
    }

    @Nested
    class ReadATOTest {
        @Test
        void testPendingReadOwn() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            assertDoesNotThrow(() -> pdp.readATO(ctx, ORG2_MSP));
        }
        @Test
        void testPendingCannotReadOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.readATO(ctx, ORG3_MSP));
            assertEquals(
                    "cid is not authorized to read the ATO of account Org3MSP",
                    e.getMessage()
            );
        }
        @Test
        void testAuthorizedCanReadOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.readATO(ctx, ORG3_MSP));
        }
        @Test
        void testAuthorizedCanReadOwn() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.readATO(ctx, ORG2_MSP));
        }
    }

    @Nested
    class WriteATOTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.writeATO(ctx, ORG2_MSP));
        }

        @Test
        void testUnauthorizedOnAnotherMember() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> pdp.writeATO(ctx, ORG3_MSP));
        }

        @Test
        void testAuthorizedWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            assertDoesNotThrow(() -> pdp.writeATO(ctx, ORG2_MSP));
        }

        @Test
        void testOrg1AuthorizedWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            assertDoesNotThrow(() -> pdp.writeATO(ctx, ORG1_MSP));
        }
    }

    @Nested
    class SubmitFeedbackTest {
        @Test
        void testAuthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG3_MSP));
        }
        @Test
        void testUnauthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);

            assertThrows(ChaincodeException.class, () -> pdp.submitFeedback(ctx, ORG3_MSP));
        }

        @Test
        void testFeedbackOnSelf() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG2_MSP));
        }
    }

    @Nested
    class InitiateTest {

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, "123", ORG3_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org3MSP", e.getMessage());
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, "123", ORG2_MSP));
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            e = assertThrows(ChaincodeException.class, () -> pdp.initiateVote(ctx, "123", ORG1_MSP));
            assertEquals("cid is not authorized to initiate a vote on Org1MSP", e.getMessage());
        }

        @Test
        void testAuthorizedOnSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testAuthorizedOnOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, "123", ORG3_MSP));
        }
    }

    @Nested
    class CertifyVoteTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            assertDoesNotThrow(() -> pdp.certifyVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, "123", ORG2_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testAuthorizedAsAdmin() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testUnauthorizedAsAdminAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, "123", ORG2_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedAsSelfAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, "123", ORG2_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org2MSP", e.getMessage());
        }
    }

    @Nested
    class AbortVoteTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            assertDoesNotThrow(() -> pdp.abortVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.abortVote(ctx, "123", ORG2_MSP)
            );
            assertEquals("cid is not authorized to abort a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedAsAdmin() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.abortVote(ctx, "123", ORG2_MSP)
            );
            assertEquals(
                    "cid is not authorized to abort a vote on Org2MSP",
                    e.getMessage()
            );
        }

        @Test
        void testUnauthorizedAsAdminAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.abortVote(ctx, "123", ORG2_MSP)
            );
            assertEquals("cid is not authorized to abort a vote on Org2MSP", e.getMessage());
        }
    }
}