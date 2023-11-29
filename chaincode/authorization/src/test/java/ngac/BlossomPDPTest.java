package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.value.StringValue;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static mock.MockContextUtil.*;
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

        ctx.getStub().putState(
                "policy",
                memoryPolicyStore.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    void testGetAdminMSPID() throws Exception {
        MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
        BlossomPDP pdp = new BlossomPDP();
        String actual = pdp.getADMINMSP(ctx);
        assertEquals(ORG1_MSP, actual);
        actual = pdp.getADMINMSP(ctx);
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

        @Test
        void testUnauthorizedBeforeSignMOU() throws Exception {
            MockContext ctx = newTestMockContextWithOneAccount(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.join(ctx, ORG3_MSP));
            assertEquals("account Org3MSP does not exist", e.getMessage());
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
        void testUnauthorizedWhenPending() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);

            assertThrows(ChaincodeException.class, () -> pdp.submitFeedback(ctx, ORG3_MSP));
        }

        @Test
        void testFeedbackOnSelfWhenPending() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG2_MSP));
        }

        @Test
        void testFeedbackOnSelfWhenAuthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG2_MSP));
        }
    }

    @Nested
    class InitiateVoteTest {

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG3_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org3MSP", e.getMessage());
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);
            e = assertThrows(ChaincodeException.class, () -> pdp.initiateVote(ctx, ORG2_MSP));
            assertEquals("cid is not authorized to initiate a vote on Org2MSP", e.getMessage());
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            e = assertThrows(ChaincodeException.class, () -> pdp.initiateVote(ctx, ORG1_MSP));
            assertEquals("cid is not authorized to initiate a vote on Org1MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG2_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org2MSP", e.getMessage());
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG2_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG3_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org3MSP", e.getMessage());
        }

        @Test
        void testADMINMSUnauthorizedSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG1_MSP)
            );
            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG1_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org1MSP", e.getMessage());
        }

        @Test
        void testADMINMSPAuthorizedOnSelfWhenNoOtherAuthorizedAccounts() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            assertThrows(ChaincodeException.class, () -> pdp.initiateVote(ctx, ORG1_MSP));

            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, ORG1_MSP));

            updateAccountStatus(ctx, ORG1_MSP, AUTHORIZED);
            assertThrows(ChaincodeException.class, () -> pdp.initiateVote(ctx, ORG1_MSP));
        }

        @Test
        void testUnauthorizedOnSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG2_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testAuthorizedOnOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, ORG3_MSP));
        }

        @Test
        void testUnauthorizedOnOtherWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG3_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org3MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedOnSelfWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.initiateVote(ctx, ORG2_MSP)
            );
            assertEquals("cid is not authorized to initiate a vote on Org2MSP", e.getMessage());
        }

        @Test
        void testInitiateVoteMultipleTimesOnSameMember() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);
            pdp.certifyVote(ctx, ORG3_MSP);
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, ORG3_MSP));
        }
    }

    @Nested
    class VoteTest {

        @Test
        void testVoteOnSelfWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            pdp.initiateVote(ctx, ORG3_MSP);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            assertDoesNotThrow(() -> pdp.vote(ctx, ORG3_MSP));
        }

        @Test
        void testVoteOnSelfWhenAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            assertDoesNotThrow(() -> pdp.vote(ctx, ORG3_MSP));
        }

        @Test
        void testVoteOnOthersWhenAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.vote(ctx, ORG3_MSP));
        }

        @Test
        void testVoteOnOthersWhenPendingThrowsException() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            pdp.initiateVote(ctx, ORG1_MSP);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.vote(ctx, ORG1_MSP)
            );
            assertEquals("cid is not authorized to vote on Org1MSP", e.getMessage());
        }

    }

    @Nested
    class CertifyVoteTest {
        @Test
        void testInitiatorAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            assertDoesNotThrow(() -> pdp.certifyVote(ctx, ORG3_MSP));
        }

        @Test
        void testADMINMSPAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, ORG3_MSP));
        }

        @Test
        void testADMINMSPUnauthorizedWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, ORG3_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org3MSP", e.getMessage());
        }

        @Test
        void testADMINMSPUnauthorizedWhenPendingAndTargetOfVote() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);

            pdp.initiateVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, ORG1_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org1MSP", e.getMessage());
        }

        @Test
        void testNotInitiatorUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> pdp.certifyVote(ctx, ORG3_MSP));
            assertEquals("cid is not authorized to certify a vote on Org3MSP", e.getMessage());
        }


        @Test
        void testInitiatorUnauthorizedOnNonTarget() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, ORG1_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org1MSP", e.getMessage());
        }

        @Test
        void testUnauthorizedAsSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> pdp.certifyVote(ctx, ORG3_MSP)
            );
            assertEquals("cid is not authorized to certify a vote on Org3MSP", e.getMessage());
        }

        @Test
        void testADMINMSPCanCertifyWhenNoAuthorizedMembersExist() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);
            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            pdp.initiateVote(ctx, ORG1_MSP);
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, ORG1_MSP));
        }
    }
}