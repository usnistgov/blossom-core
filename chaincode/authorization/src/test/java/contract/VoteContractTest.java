package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.exceptions.UnauthorizedException;
import mock.MockContext;
import mock.MockEvent;
import mock.MockIdentity;
import model.Account;
import model.Status;
import model.Vote;
import model.VoteConfiguration;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static contract.MockContextUtil.newTestMockContextWithAccounts;
import static contract.MockContextUtil.updateAccountStatus;
import static mock.MockOrgs.*;
import static model.Status.*;
import static model.Vote.Result.*;
import static model.Vote.Threshold.MAJORITY;
import static org.junit.jupiter.api.Assertions.*;

class VoteContractTest {

    VoteContract voteContract = new VoteContract();
    AccountContract accountContract = new AccountContract();

    @Test
    void testGetVoteConfiguration() throws Exception {
        MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
        voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(true, true, true, false));
        VoteConfiguration voteConfiguration = voteContract.GetVoteConfiguration(ctx);
        assertEquals(
                new VoteConfiguration(true, true, true, false),
                voteConfiguration
        );
    }

    @Nested
    class UpdateVoteConfiguration {

        @Test
        void testAuthorized() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            VoteConfiguration expected = new VoteConfiguration(false, false, false, false);
            voteContract.UpdateVoteConfiguration(ctx, expected);
            VoteConfiguration actual = voteContract.GetVoteConfiguration(ctx);
            assertEquals(
                    expected,
                    actual
            );
        }

        @Test
        void testUserUnauthorized() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            VoteConfiguration expected = new VoteConfiguration(false, false, false, false);
            assertThrows(ChaincodeException.class, () -> voteContract.UpdateVoteConfiguration(ctx, expected));
        }

        @Test
        void testBlossomAdminAccountUnauthorized() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, "Org1MSP", PENDING.toString());
            assertThrows(ChaincodeException.class,
                         () -> voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false)));
        }

        @Test
        void testEvent() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            VoteConfiguration expected = new VoteConfiguration(true, true, false, false);
            voteContract.UpdateVoteConfiguration(ctx, expected);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("UpdateVoteConfiguration", mockEvent.getName());
            VoteConfiguration config = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new VoteConfiguration(true, true, false, false),
                    config
            );
        }
    }

    @Nested
    class InitiateVote {

        @Test
        void testTargetMemberDoesNotExistThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, "unknown account", Status.AUTHORIZED.toString(), "reason"));
        }

        @Test
        void testMemberHasOngoingVoteThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setTimestamp(Instant.now());
            ctx.setTxId(Instant.now().toString());
            updateAccountStatus(ctx, ORG2_MSP, Status.AUTHORIZED.name());
            voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED.name(), "reason");
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED.name(), "reason"));

        }

        @Test
        void testInitiateVoteInvalidStatus() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, "invalid status", "reason"));
        }

        @Test
        void testThreshold() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("1");
            voteContract.InitiateVote(ctx, "Org2MSP", AUTHORIZED.toString(), "reason");

            Vote vote = voteContract.GetVote(ctx, "1", "Org2MSP");
            assertEquals(new Vote(
                    "1", "Org1MSP", "Org2MSP", AUTHORIZED,
                    "reason", MAJORITY, 0, ONGOING
            ), vote);

            ctx.setTxId("2");
            voteContract.InitiateVote(ctx, "Org1MSP", AUTHORIZED.toString(), "reason");

            vote = voteContract.GetVote(ctx, "2", "Org1MSP");
            assertEquals(new Vote(
                    "2", "Org1MSP", "Org1MSP", AUTHORIZED,
                    "reason", Vote.Threshold.SUPER_MAJORITY, 0, ONGOING
            ), vote);
        }

        @Test
        void testEvent() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("1");
            voteContract.InitiateVote(ctx, "Org2MSP", AUTHORIZED.toString(), "reason");

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("InitiateVote", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("1", ORG1_MSP, ORG2_MSP, AUTHORIZED, "reason", MAJORITY, 0, ONGOING),
                    vote
            );
        }
    }

    @Nested
    class CertifyVote {

        @Test
        void testVoteDoesNotExistThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.CertifyVote(ctx, "123", ORG3_MSP));
        }

        @Test
        void testVoteHasAlreadyBeenCompletedThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyVote(ctx, id, ORG3_MSP);

            assertThrows(VoteContract.VoteHasAlreadyBeenCompletedException.class, () -> voteContract.CertifyVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testCompleteVoteFailed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote NO as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // vote NO as Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // complete vote failed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG3_MSP);
            assertFalse(result);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        @Test
        void testCompleteVotePassed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG3_MSP);
            assertTrue(result);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.UNAUTHORIZED, status);
        }

        @Test
        void testCompleteVoteNotEnoughForResult() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.CertifyVote(ctx, id, ORG3_MSP));

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        @Test
        void testCompleteVoteOnBlossomAdminRequiresSuperMajorityPassed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, true);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, true);

            // complete vote - passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG1_MSP);
            assertTrue(result);
        }

        @Test
        void testCompleteVoteOnBlossomAdminRequiresSuperMajorityFailed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG1_MSP, true);

            // complete vote - passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG1_MSP);
            assertFalse(result);
        }

        @Test
        void testWhenSelfVoteIsDisallowedAfterVoteButBeforeCertify() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG2_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote YES as Org2 (self)
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote yes Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, false);

            // update config to not allow self vote
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG2_MSP);
            assertFalse(result);
        }

        @Test
        void testEvent() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG2_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote YES as Org2 (self)
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote yes Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.CertifyVote(ctx, id, ORG2_MSP);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("CertifyVote", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("123", ORG2_MSP, ORG2_MSP, UNAUTHORIZED, "reason", MAJORITY, 3, PASSED),
                    vote
            );
        }
    }

    @Nested
    class AbortVote {

        @Test
        void testVoteDoesNotExistThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.AbortVote(ctx, "123", ORG3_MSP));

        }

        @Test
        void testVoteHasBeenCompletedThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG3_MSP);
            assertTrue(result);

            assertThrows(VoteContract.VoteHasAlreadyBeenCompletedException.class,
                         () -> voteContract.AbortVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testSuccess() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            voteContract.AbortVote(ctx, id, ORG3_MSP);

            assertThrows(VoteContract.VoteDoesNotExistException.class,
                         () -> voteContract.GetVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testEvent() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG2_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote YES as Org2 (self)
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            // vote yes Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.AbortVote(ctx, id, ORG2_MSP);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("AbortVote", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("123", ORG2_MSP, ORG2_MSP, UNAUTHORIZED, "reason", MAJORITY, 3, ABORTED),
                    vote
            );
        }
    }

    @Nested
    class GetVote {

        @Test
        void testGetUnknownVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.GetVote(ctx, "unknown", ORG3_MSP));
        }

        @Test
        void testGetVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 0, ONGOING
            ), vote);

            // add vote
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 1, ONGOING
            ), vote);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 2, ONGOING
            ), vote);

            // complete vote
            voteContract.CertifyVote(ctx, id, ORG3_MSP);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 2, Vote.Result.PASSED
            ), vote);

            // get vote with super
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED.name(), "reason");
            id = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP).getId();
            vote = voteContract.GetVote(ctx, id, ORG1_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG1_MSP, Status.UNAUTHORIZED, "reason", Vote.Threshold.SUPER_MAJORITY, 0, ONGOING
            ), vote);
        }

    }

    @Nested
    class GetVotes {

        @Test
        void testGetVotes() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // create another test vote
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED.name(), "reason");

            Vote vote = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP);
            String id2 = vote.getId();

            List<Vote> votes = voteContract.GetVotes(ctx);
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 0, ONGOING
            )));
            assertTrue(votes.contains(new Vote(
                    id2, ORG2_MSP, ORG1_MSP, Status.UNAUTHORIZED, "reason", Vote.Threshold.SUPER_MAJORITY, 0, ONGOING
            )));
        }

    }

    @Nested
    class GetOngoingVotes {

        @Test
        void testOngoingGetVotes() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // create another test vote
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED.name(), "reason");
            Vote vote = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP);
            String id2 = vote.getId();

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id2, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id2, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyVote(ctx, id2, ORG1_MSP);

            List<Vote> votes = voteContract.GetOngoingVotes(ctx);
            assertEquals(1, votes.size());
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 0, ONGOING
            )));
        }

    }

    @Nested
    class GetVotesForMember {

        @Test
        void testGetVotesForMember() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // create another test vote
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED.name(), "reason");

            List<Vote> votes = voteContract.GetVotesForMember(ctx, ORG3_MSP);
            assertEquals(1, votes.size());
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 0, ONGOING
            )));
        }
    }

    @Nested
    class GetOngoingVoteForMember {
        @Test
        void testGetVotesForMember() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);
            String id2 = initiateTestVote(ctx, ORG1_MSP);

            Vote vote = voteContract.GetOngoingVoteForMember(ctx, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY, 0, ONGOING
            ), vote);
        }
    }

    @Nested
    class VoteTest {

        @Test
        void testVoteDoesNotExistThrowsException() throws Exception {
            MockContext ctx = initializeCtx();

            ctx.setClientIdentity(MockIdentity.ORG2_NON_AO);
            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.Vote(ctx, "123", ORG3_MSP, true));
        }

        @Test
        void testVoteHasAlreadyBeenCompletedThrowsException() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(1, vote.getCount());

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // complete vote
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyVote(ctx, id, ORG3_MSP);

            // try voting again
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            assertThrows(VoteContract.VoteHasAlreadyBeenCompletedException.class,
                         () -> voteContract.Vote(ctx, id, ORG3_MSP, true));
        }

        @Test
        void testMembersCanNotVoteOnThemselvesThrowsException() throws Exception {
            MockContext ctx = initializeCtx();

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(false, false, false, false));

            String id = initiateTestVote(ctx, ORG2_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.Vote(ctx, id, ORG2_MSP, true));
        }

        @Test
        void testMemberUpdatesVotesDoesNotChangeCount() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(1, vote.getCount());

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, true);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // change vote for org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            assertEquals(3, voteContract.GetVote(ctx, id, ORG3_MSP).getCount());

            // complete vote - expect fail
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyVote(ctx, id, ORG3_MSP);
            assertFalse(result);
        }

        @Test
        void testEvent() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG2_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, id, ORG2_MSP, true);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("Vote", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("123", ORG2_MSP, ORG2_MSP, UNAUTHORIZED, "reason", MAJORITY, 1, ONGOING),
                    vote
            );
        }
    }

    private MockContext initializeCtx() throws Exception {
        MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

        updateAccountStatus(ctx, ORG2_MSP, Status.AUTHORIZED.name());
        updateAccountStatus(ctx, ORG3_MSP, Status.AUTHORIZED.name());

        voteContract.UpdateVoteConfiguration(ctx, new VoteConfiguration(true, true, true, false));

        ctx.setClientIdentity(MockIdentity.ORG2_AO);
        return ctx;
    }

    private String initiateTestVote(MockContext ctx, String targetMember) throws Exception {
        ctx.setClientIdentity(MockIdentity.ORG2_AO);
        ctx.setTxId("123");
        voteContract.InitiateVote(ctx, targetMember, Status.UNAUTHORIZED.name(), "reason");

        Vote vote = voteContract.GetOngoingVoteForMember(ctx, targetMember);
        return vote.getId();
    }
}