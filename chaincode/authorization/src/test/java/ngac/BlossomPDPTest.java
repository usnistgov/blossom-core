package ngac;

import contract.VoteContract;
import gov.nist.csd.pm.policy.exceptions.NodeDoesNotExistException;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import mock.MockContext;
import mock.MockIdentity;
import model.VoteConfiguration;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static contract.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static model.Status.PENDING;
import static org.junit.jupiter.api.Assertions.*;

class BlossomPDPTest {

    BlossomPDP pdp = new BlossomPDP();

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
            assertThrows(UnauthorizedException.class, () -> pdp.updateMOU(ctx));
        }
    }

    @Nested
    class UpdateVoteConfigTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.updateVoteConfig(ctx, new VoteConfiguration(false, false, false, false)));
        }

        @Test
        void testUnauthorizedAsAdminAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> pdp.updateVoteConfig(ctx, new VoteConfiguration(false, false, false, false)));
            updateAccountStatus(ctx, ORG1_MSP, AUTHORIZED.toString());
            assertDoesNotThrow(() -> pdp.updateVoteConfig(ctx, new VoteConfiguration(false, false, false, false)));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class, () -> pdp.updateVoteConfig(ctx, new VoteConfiguration(false, false, false, false)));
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
            PMException e = assertThrows(PMException.class, () -> pdp.signMOU(ctx));
            assertEquals("unknown user role: System Administrator", e.getMessage());
        }
    }

    @Nested
    class JoinTest {
        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.join(ctx, ORG2_MSP));
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_NON_AO);
            PMException e = assertThrows(PMException.class, () -> pdp.join(ctx, ORG2_MSP));
            assertEquals("unknown user role: System Administrator", e.getMessage());
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
            assertThrows(UnauthorizedException.class, () -> pdp.writeATO(ctx, ORG3_MSP));
        }

        @Test
        void testAuthorizedWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());
            assertDoesNotThrow(() -> pdp.writeATO(ctx, ORG2_MSP));
        }

        @Test
        void testOrg1AuthorizedWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            assertDoesNotThrow(() -> pdp.writeATO(ctx, ORG1_MSP));
        }
    }

    @Nested
    class SubmitFeedbackTest {
        @Test
        void testAuthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG3_MSP));
        }
        @Test
        void testUnauthorized() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());

            assertThrows(UnauthorizedException.class, () -> pdp.submitFeedback(ctx, ORG3_MSP));
        }

        @Test
        void testFeedbackOnSelf() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG2_AO, now);
            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());

            assertDoesNotThrow(() -> pdp.submitFeedback(ctx, ORG2_MSP));
        }
    }

    @Nested
    class InitiateTest {

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class, () -> pdp.initiateVote(ctx, "123", ORG3_MSP));
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, "123", ORG2_MSP));
            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> pdp.initiateVote(ctx, "123", ORG1_MSP));
        }

        @Test
        void testAuthorizedOnSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());
            assertDoesNotThrow(() -> pdp.initiateVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testAuthorizedOnOther() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());
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
            assertThrows(UnauthorizedException.class, () -> pdp.certifyVote(ctx, "123", ORG2_MSP));
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
            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> pdp.certifyVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testUnauthorizedAsSelfAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            assertThrows(UnauthorizedException.class, () -> pdp.certifyVote(ctx, "123", ORG2_MSP));
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
            assertThrows(UnauthorizedException.class, () -> pdp.abortVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testAuthorizedAsAdmin() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            assertDoesNotThrow(() -> pdp.abortVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testUnauthorizedAsAdminAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> pdp.abortVote(ctx, "123", ORG2_MSP));
        }
    }

    @Nested
    class VoteConfigurationTest {

        VoteContract voteContract = new VoteContract();

        @Test
        void testVoteOnSelf() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.Vote(ctx, "123", ORG2_MSP, true));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(true, false, false, false));
            assertDoesNotThrow(() -> voteContract.Vote(ctx, "123", ORG2_MSP, true));
        }

        @Test
        void testVoteWhenNotAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));
            voteContract.InitiateVote(ctx, ORG3_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);

            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class,
                         () -> voteContract.Vote(ctx, "123", ORG3_MSP, true));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, true, false, false));
            assertDoesNotThrow(() -> voteContract.Vote(ctx, "123", ORG3_MSP, true));
        }

        @Test
        void testVoteOnSelfAndVoteWhenNotAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(true, true, false, false));
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());
            assertDoesNotThrow(() -> voteContract.Vote(ctx, "123", ORG2_MSP, true));
        }

        @Test
        void testInitiateVoteOnSelfWhenNotAuthorized() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class, () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), ""));
            assertThrows(UnauthorizedException.class, () -> voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), ""));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, true, false));

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class, () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), ""));
            assertDoesNotThrow(() -> voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), ""));
        }

        @Test
        void testCertify() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class,
                         () -> pdp.certifyVote(ctx, "123", ORG2_MSP));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, true));

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testAbortAsAdmin() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class,
                         () -> pdp.abortVote(ctx, "123", ORG2_MSP));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, true));
        }

        @Test
        void testAbortAsInitiator() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(UnauthorizedException.class,
                         () -> pdp.abortVote(ctx, "123", ORG2_MSP));

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, true));
        }

        @Test
        void testVoteConfigChangeAfterInitiate() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");

            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, true, true));

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            ctx.setTxId("123");
            voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "");
            ctx.setTxId("123");
            voteContract.InitiateVote(ctx, ORG3_MSP, AUTHORIZED.toString(), "");

            // update config
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));

            updateAccountStatus(ctx, ORG2_MSP, PENDING.toString());

            ctx.setClientIdentity(MockIdentity.ORG2_AO);

            // try certifying and aborting
            assertThrows(UnauthorizedException.class,
                         () -> pdp.certifyVote(ctx, "123", ORG2_MSP));
            assertThrows(UnauthorizedException.class,
                         () -> pdp.abortVote(ctx, "123", ORG2_MSP));

            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED.toString());

            // try certifying and aborting
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, "123", ORG2_MSP));
            // this call will throw NodeDoesNotExist because the above call will delete the vote, however reaching the point
            // where this exception is thrown means the decision passed
            assertThrows(NodeDoesNotExistException.class, () -> pdp.abortVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testCannotCertifyWhenAdminIsInitiatorAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            assertThrows(UnauthorizedException.class, () -> pdp.certifyVote(ctx, "123", ORG2_MSP));

            updateAccountStatus(ctx, ORG1_MSP, AUTHORIZED.toString());
            assertDoesNotThrow(() -> pdp.certifyVote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testVoteForSelfWhenInitiatorAndPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setTxId("123");
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            assertDoesNotThrow(() -> pdp.vote(ctx, "123", ORG2_MSP));
        }

        @Test
        void testVoteForSelfWhenInitiatorAndPendingAndAdmin() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");
            updateAccountStatus(ctx, ORG1_MSP, PENDING.toString());
            pdp.initiateVote(ctx, "123", ORG1_MSP);
            assertDoesNotThrow(() -> pdp.vote(ctx, "123", ORG1_MSP));
        }

        @Test
        void testVoteOnSelfWhenPending() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("123");
            pdp.initiateVote(ctx, "123", ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> pdp.vote(ctx, "123", ORG2_MSP));
        }
    }

}