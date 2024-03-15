package contract;

import mock.MockContext;
import mock.MockContextUtil;
import mock.MockEvent;
import mock.MockIdentity;
import model.Status;
import model.Vote;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static mock.MockContextUtil.updateAccountStatus;
import static mock.MockOrgs.*;
import static model.Status.*;
import static model.Vote.Result.*;
import static model.Vote.Threshold.MAJORITY;
import static model.Vote.Threshold.SUPER_MAJORITY;
import static org.junit.jupiter.api.Assertions.*;

class VoteContractTest {

    VoteContract voteContract = new VoteContract();
    AccountContract accountContract = new AccountContract();

    @Nested
    class InitiateVote {

        @Test
        void testSuccess() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setTxId("1");
            voteContract.InitiateVote(ctx, ORG3_MSP, AUTHORIZED.toString(), "reason");

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(
                    new Vote(
                            "1", "Org2MSP", "Org3MSP", AUTHORIZED,
                            "reason", MAJORITY, List.of(ORG3_MSP, ORG2_MSP, ORG1_MSP), Map.of(), ONGOING
                    ),
                    vote
            );
        }

        @Test
        void testTargetMemberDoesNotExistThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, "unknown account", Status.AUTHORIZED.toString(), "reason"));
        }

        @Test
        void testOngoingVoteThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setTimestamp(Instant.now());
            ctx.setTxId(Instant.now().toString());
            updateAccountStatus(ctx, ORG2_MSP, Status.AUTHORIZED);
            voteContract.InitiateVote(ctx, ORG3_MSP, Status.UNAUTHORIZED.toString(), "reason");
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx,
                                                    ORG3_MSP,
                                                    UNAUTHORIZED.toString(),
                                                    "reason"
                    )
            );
            assertEquals("there is an ongoing vote on account Org3MSP", e.getMessage());
        }

        @Test
        void testInitiateVoteInvalidStatus() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.InitiateVote(ctx, ORG3_MSP, "invalid status", "reason"));
        }

        @Test
        void testThresholdADMINMSP() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);
            ctx.setTxId("1");
            voteContract.InitiateVote(ctx, "Org1MSP", AUTHORIZED.toString(), "reason");

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    "1", "Org2MSP", "Org1MSP", AUTHORIZED,
                    "reason", Vote.Threshold.SUPER_MAJORITY, List.of(ORG1_MSP, ORG2_MSP, ORG3_MSP), Map.of(), ONGOING
            ), vote);
        }

        @Test
        void testThresholdRegularMember() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            ctx.setTxId("1");
            voteContract.InitiateVote(ctx, "Org2MSP", AUTHORIZED.toString(), "reason");

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    "1", "Org1MSP", "Org2MSP", AUTHORIZED,
                    "reason", MAJORITY, List.of(ORG2_MSP, ORG3_MSP, ORG1_MSP), Map.of(), ONGOING
            ), vote);
        }

        @Test
        void testSetVoters() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);

            ctx.setTxId("2");

            voteContract.InitiateVote(ctx, "Org2MSP", AUTHORIZED.toString(), "reason");

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    "2", "Org1MSP", "Org2MSP", AUTHORIZED,
                    "reason", MAJORITY, List.of(ORG2_MSP, ORG1_MSP), Map.of(), ONGOING
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
                    new Vote("1", ORG1_MSP, ORG2_MSP, AUTHORIZED, "reason", MAJORITY, List.of(ORG2_MSP, ORG3_MSP, ORG1_MSP),
                             Map.of(), ONGOING),
                    vote
            );
        }

        @Test
        void testUnauthorized() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), "reason")
            );

            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), "reason")
            );
        }

        @Test
        void testADMINMSPCanInitiateOnSelfWhenNoOtherAuthorizedAccountsExist() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), "reason")
            );

            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);

            assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), "reason")
            );

            updateAccountStatus(ctx, ORG1_MSP, PENDING);

            assertDoesNotThrow(() -> voteContract.InitiateVote(ctx, ORG1_MSP, AUTHORIZED.toString(), "reason"));
        }

        @Test
        void testOnlyVotesOnADMINMSPAllowedWhenNoAuthorizedAccountsExist() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            updateAccountStatus(ctx, ORG2_MSP, PENDING);
            updateAccountStatus(ctx, ORG3_MSP, PENDING);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.InitiateVote(ctx, ORG2_MSP, AUTHORIZED.toString(), "reason")
            );
            assertEquals(
                    "only votes on the ADMINMSP Org1MSP are allowed when there are no authorized members",
                    e.getMessage()
            );
        }
    }


    @Nested
    class VoteTest {

        @Test
        void testNoOngoingVoteThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.Vote(ctx, true)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

        @Test
        void testVoteHasAlreadyBeenCompletedThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);

            // complete vote
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyOngoingVote(ctx);

            // try voting again
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.Vote(ctx, true)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

        @Test
        void testMemberCanVoteTwice() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // vote as others
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);

            // change vote for org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            assertDoesNotThrow(() -> voteContract.Vote(ctx, false));

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(Map.of(ORG1_MSP, true, ORG2_MSP, false, ORG3_MSP, false), vote.getSubmittedVotes());
        }

        @Test
        void testEvent() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("Vote", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("123", ORG2_MSP, ORG3_MSP, UNAUTHORIZED, "reason", MAJORITY,
                             List.of(ORG3_MSP, ORG2_MSP, ORG1_MSP), Map.of(ORG1_MSP, true), ONGOING),
                    vote
            );
        }

        @Test
        void testVoteAsPendingThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            updateAccountStatus(ctx, ORG1_MSP, PENDING);

            initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            assertThrows(ChaincodeException.class, () -> voteContract.Vote(ctx, true));
        }

        @Test
        void testVoteAsPendingOnSelf() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            updateAccountStatus(ctx, ORG3_MSP, PENDING);

            String id = initiateTestVote(ctx, ORG3_MSP);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            assertDoesNotThrow(() -> voteContract.Vote(ctx, true));
        }

        @Test
        void testVoteAsPendingOnOtherThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            updateAccountStatus(ctx, ORG3_MSP, PENDING);

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.Vote(ctx, true)
            );
            assertEquals("cid is not authorized to vote on Org1MSP", e.getMessage());
        }

        @Test
        void testVoteAsAuthorizedOnOther() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            assertDoesNotThrow(() -> voteContract.Vote(ctx, true));
        }
    }

    @Nested
    class CertifyVote {

        @Test
        void testVoteDoesNotExistThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.CertifyOngoingVote(ctx)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

        @Test
        void testVoteHasAlreadyBeenCompletedThrowsException() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx,true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyOngoingVote(ctx);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.CertifyOngoingVote(ctx)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

        @Test
        void testCompleteVoteFailed() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote NO as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, false);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, false);

            // vote NO as Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);

            // complete vote failed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyOngoingVote(ctx);
            assertFalse(result);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            String status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED.toString(), status);
        }

        @Test
        void testCompleteVotePassed() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyOngoingVote(ctx);
            assertTrue(result);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            String status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.UNAUTHORIZED.toString(), status);
        }

        @Test
        void testCompleteVoteNotEnoughForResult() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            // vote YES as Org2
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // complete vote passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            ChaincodeException e = assertThrows(
                    ChaincodeException.class, () -> voteContract.CertifyOngoingVote(ctx));
            assertEquals("not enough votes to certify", e.getMessage());

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            String status = accountContract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED.toString(), status);
        }

        @Test
        void testCompleteVoteOnBlossomAdminRequiresSuperMajorityPassed() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, false);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, true);

            // complete vote - passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyOngoingVote(ctx);
            assertTrue(result);
        }

        @Test
        void testCompleteVoteOnBlossomAdminRequiresSuperMajorityFailed() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG1_MSP);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, false);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, false);

            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, true);

            // complete vote - passed
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            boolean result = voteContract.CertifyOngoingVote(ctx);
            assertFalse(result);
        }

        @Test
        void testEvent() throws Exception {
            // initiate vote
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG1_MSP);

            // vote YES as Org1
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);

            // vote YES as Org2 (self)
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            // vote yes Org3
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);

            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.CertifyOngoingVote(ctx);

            MockEvent mockEvent = ctx.getStub().getMockEvent();
            assertEquals("VoteCompleted", mockEvent.getName());
            Vote vote = SerializationUtils.deserialize(mockEvent.getPayload());
            assertEquals(
                    new Vote("123", ORG2_MSP, ORG1_MSP, UNAUTHORIZED, "reason", SUPER_MAJORITY,
                             List.of(ORG1_MSP, ORG2_MSP, ORG3_MSP), Map.of(ORG1_MSP, true, ORG2_MSP, true, ORG3_MSP, false), PASSED),
                    vote
            );
        }
    }

    @Nested
    class GetOngoingVoteTest {

        @Test
        void testNoOngoingVoteThrowsException() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.GetOngoingVote(ctx)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

        @Test
        void testGetOngoingVote() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id = initiateTestVote(ctx, ORG3_MSP);

            Vote vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY,
                    List.of(ORG3_MSP, ORG2_MSP, ORG1_MSP), Map.of(), ONGOING
            ), vote);

            // add vote
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);

            vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY,
                    List.of(ORG3_MSP, ORG2_MSP, ORG1_MSP), Map.of(ORG1_MSP, true), ONGOING
            ), vote);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.Vote(ctx, true);

            vote = voteContract.GetOngoingVote(ctx);
            assertEquals(new Vote(
                    id, ORG2_MSP, ORG3_MSP, Status.UNAUTHORIZED, "reason", MAJORITY,
                    List.of(ORG3_MSP, ORG2_MSP, ORG1_MSP), Map.of(ORG1_MSP, true, ORG2_MSP, true), ONGOING
            ), vote);

            // complete vote
            voteContract.CertifyOngoingVote(ctx);

            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> voteContract.GetOngoingVote(ctx)
            );
            assertEquals("there is no ongoing vote", e.getMessage());
        }

    }

    @Nested
    class GetVoteHistoryTest {

        @Test
        void testGetVoteHistoryEmpty() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            Vote[] votes = voteContract.GetVoteHistory(ctx, ORG2_MSP);
            assertEquals(0, votes.length);
        }

        @Test
        void testGetVoteHistory() throws Exception {
            MockContext ctx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            String id1 = initiateTestVote(ctx, ORG3_MSP);
            voteContract.Vote(ctx, true);
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, true);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyOngoingVote(ctx);

            String id2 = initiateTestVote(ctx, ORG3_MSP);
            voteContract.Vote(ctx, true);
            ctx.setClientIdentity(MockIdentity.ORG1_AO);
            voteContract.Vote(ctx, false);
            ctx.setClientIdentity(MockIdentity.ORG3_AO);
            voteContract.Vote(ctx, false);
            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            voteContract.CertifyOngoingVote(ctx);

            Vote[] votes = voteContract.GetVoteHistory(ctx, ORG3_MSP);
            assertEquals(10, votes.length);
        }
    }

    private String initiateTestVote(MockContext ctx, String targetMember) {
        ctx.setClientIdentity(MockIdentity.ORG2_AO);
        ctx.setTxId("123");
        voteContract.InitiateVote(ctx, targetMember, Status.UNAUTHORIZED.toString(), "reason");

        Vote vote = voteContract.GetOngoingVote(ctx);
        return vote.getId();
    }
}