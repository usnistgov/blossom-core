package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import model.Vote;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static contract.AccountContractTest.updateAccountStatus;
import static mock.MockOrgs.*;
import static org.junit.jupiter.api.Assertions.*;

class VoteContractTest {

    VoteContract voteContract = new VoteContract();
    AccountContract accountContract = new AccountContract();

    @Nested
    class InitiateVote {

        @Test
        void testInitiateVoteInvalidStatus() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, "invalid status", "reason"));
        }

        @Test
        void testInitiateVoteOnUnknownAccount() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, "unknown account", Status.AUTHORIZED.toString(), "reason"));
        }

        @Test
        void testInitiateVoteUnauthorizedAccountStatus() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }

        @Test
        void testCanInitiateVoteOnSelfWhenStatusIsPending() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertDoesNotThrow(() -> voteContract.InitiateVote(ctx, ORG2_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }

        @Test
        void testCannotInitiateVoteOnOtherWhenStatusIsPending() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }

        @Test
        void testCannotInitiateVoteOnSelfWhenStatusIsPendingAndRoleIsUnauthorized() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            assertThrows(PMException.class, () -> voteContract.InitiateVote(ctx, ORG2_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }

        @Test
        void testInitiateVoteUnauthorizedUserRole() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.AUTHORIZED.name());
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            assertThrows(PMException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }

        @Test
        void testInitiateVoteAuthorized() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.AUTHORIZED.name());
            voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");
            List<Vote> votes = voteContract.GetVotesForMember(ctx, ORG3_MSP);
            assertEquals(1, votes.size());
        }

        @Test
        void testInitiateVoteForMemberWithOngoingVote() throws Exception {
            MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.AUTHORIZED.name());
            voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED_ATO.name(), "reason"));
        }
    }

    @Nested
    class CompleteVote {
        @Test
        void testUnauthorizedCompleteVote() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // try complete as org3 sys owner
            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));

            // try complete as org3 sys admin
            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_ADMIN);
            assertThrows(PMException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testCompleteVoteFailed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote NO as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // vote NO as Org3
            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // complete vote failed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertFalse(result);

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        @Test
        void testCompleteVotePassed() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertTrue(result);

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.UNAUTHORIZED_ATO, status);
        }

        @Test
        void testBlossomAdminCompleteVote() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertTrue(result);

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.UNAUTHORIZED_ATO, status);
        }

        @Test
        void testBlossomAdminCannotCompleteVoteWhenPending() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // change org1 status
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            updateAccountStatus(accountContract, ctx, ORG1_MSP, Status.PENDING_ATO.toString());

            // complete vote passed
            assertThrows(PMException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        // test initiating member is set to pending and cannot complete vote
        @Test
        void testInitiatingMemberCannotCompleteVoteWhenPending() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // change org2 status
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.PENDING_ATO.toString());

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        @Test
        void testCompleteVoteNotEnoughForResult() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(ChaincodeException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            Status status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);
        }

        @Test
        void testCompleteVoteOnUnknownVote() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            initiateTestVote(ctx, ORG3_MSP);

            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.CompleteVote(ctx, "123", ORG3_MSP));
        }

        @Test
        void testCompleteVoteThrowsExceptionWhenVoteAlreadyCompleted() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.CompleteVote(ctx, id, ORG3_MSP);

            assertThrows(VoteContract.VoteHasAlreadyBeenCompletedException.class, () -> voteContract.CompleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testCompleteVoteOnBlossomAdminRequiresSuperMajority() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG1_MSP, true);

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG1_MSP, true);

            // complete vote - passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG1_MSP);
            assertTrue(result);
        }
    }

    @Nested
    class DeleteVote {
        @Test
        void testDeleteVoteUnauthorizedRole() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            assertThrows(PMException.class, () -> voteContract.DeleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testDeleteVoteUnauthorizedStatus() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.PENDING_ATO.toString());

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.DeleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testBlossomAdminCannotDeleteVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> voteContract.DeleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testDeleteVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            voteContract.DeleteVote(ctx, id, ORG3_MSP);

            assertThrows(ChaincodeException.class, () -> voteContract.GetVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testDeleteVoteAfterCompleteThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertTrue(result);

            assertThrows(VoteContract.VoteHasAlreadyBeenCompletedException.class, () -> voteContract.DeleteVote(ctx, id, ORG3_MSP));
        }

        @Test
        void testDeleteVoteOnUnknownVote() throws Exception {
            // initiate vote
            MockContext ctx = initializeCtx();

            initiateTestVote(ctx, ORG3_MSP);

            assertThrows(VoteContract.VoteDoesNotExistException.class, () -> voteContract.DeleteVote(ctx, "123", ORG3_MSP));
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
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 0, Vote.Result.ONGOING
            ), vote);

            // add vote
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 1, Vote.Result.ONGOING
            ), vote);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 2, Vote.Result.ONGOING
            ), vote);

            // complete vote
            voteContract.CompleteVote(ctx, id, ORG3_MSP);

            vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 2, Vote.Result.PASSED
            ), vote);

            // get vote with super
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");
            id = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP).getId();
            vote = voteContract.GetVote(ctx, id, ORG1_MSP);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG1_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.SUPER_MAJORITY, 0, Vote.Result.ONGOING
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
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");

            Vote vote = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP);
            String id2 = vote.getId();

            List<Vote> votes = voteContract.GetVotes(ctx);
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 0, Vote.Result.ONGOING
            )));
            assertTrue(votes.contains(new Vote(
                    id2, ORG2_MSP, ORG1_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.SUPER_MAJORITY, 0, Vote.Result.ONGOING
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
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");
            Vote vote = voteContract.GetOngoingVoteForMember(ctx, ORG1_MSP);
            String id2 = vote.getId();

            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id2, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            voteContract.Vote(ctx, id2, ORG1_MSP, false);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.CompleteVote(ctx, id2, ORG1_MSP);

            List<Vote> votes = voteContract.GetOngoingVotes(ctx);
            assertEquals(1, votes.size());
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 0, Vote.Result.ONGOING
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
            voteContract.InitiateVote(ctx, ORG1_MSP, Status.UNAUTHORIZED_ATO.name(), "reason");

            List<Vote> votes = voteContract.GetVotesForMember(ctx, ORG3_MSP);
            assertEquals(1, votes.size());
            assertTrue(votes.contains(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 0, Vote.Result.ONGOING
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
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED_ATO, "reason", Vote.Threshold.MAJORITY, 0, Vote.Result.ONGOING
            ), vote);
        }
    }

    @Nested
    class VoteTests {

        @Test
        void testSystemAdminCannotVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            assertThrows(PMException.class, () -> voteContract.Vote(ctx, id, ORG3_MSP, true));
        }

        @Test
        void testSystemOwnerCanVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(1, vote.getCount());

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);
            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // complete vote - expect pass
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertTrue(result);
        }

        @Test
        void testSystemOwnerOfUnauthorizedAccountCanVote() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.UNAUTHORIZED_ATO.toString());

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(1, vote.getCount());
        }

        @Test
        void testChangeVoteBeforeCompleted() throws Exception {
            MockContext ctx = initializeCtx();

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);

            Vote vote = voteContract.GetVote(ctx, id, ORG3_MSP);
            assertEquals(1, vote.getCount());

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, true);
            ctx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            // change vote for org2
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            voteContract.Vote(ctx, id, ORG3_MSP, false);

            assertEquals(3, voteContract.GetVote(ctx, id, ORG3_MSP).getCount());

            // complete vote - expect fail
            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            boolean result = voteContract.CompleteVote(ctx, id, ORG3_MSP);
            assertFalse(result);
        }
    }

    private MockContext initializeCtx() throws Exception {
        MockContext ctx = MockContextUtil.buildTestMockContextWithAccounts();

        updateAccountStatus(accountContract, ctx, ORG2_MSP, Status.AUTHORIZED.name());
        updateAccountStatus(accountContract, ctx, ORG3_MSP, Status.AUTHORIZED.name());

        return ctx;
    }

    private String initiateTestVote(MockContext ctx, String targetMember) throws Exception {
        ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
        voteContract.InitiateVote(ctx, targetMember, Status.UNAUTHORIZED_ATO.name(), "reason");

        Vote vote = voteContract.GetOngoingVoteForMember(ctx, targetMember);
        return vote.getId();
    }
}