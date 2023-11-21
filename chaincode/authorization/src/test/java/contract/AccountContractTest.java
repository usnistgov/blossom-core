package contract;

import mock.MockContext;
import mock.MockIdentity;
import model.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static contract.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static org.junit.jupiter.api.Assertions.*;

class AccountContractTest {

    private AccountContract contract = new AccountContract();

    @Nested
    class GetAccounts {

        @Test
        void testGetAccounts() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG1_AO, now);

            List<Account> accounts = contract.GetAccounts(ctx);
            assertEquals(3, accounts.size());
            assertTrue(accounts.contains(new Account(ORG1_MSP, AUTHORIZED, new ATO(
                    "123",
                    now.toString(),
                    now.toString(),
                    1,
                    "org1 test ato",
                    "org1 artifacts",
                    List.of()
            ), 0)));
            assertTrue(accounts.contains(new Account(ORG2_MSP, Status.PENDING, new ATO(
                    "123",
                    now.toString(),
                    now.toString(),
                    1,
                    "memo",
                    "artifacts",
                    List.of(new Feedback(1, "Org1MSP", "comment1"))
            ), 1)));
            assertTrue(accounts.contains(new Account(ORG3_MSP, Status.PENDING, new ATO(
                    "123",
                    now.toString(),
                    now.toString(),
                    1,
                    "memo",
                    "artifacts",
                    List.of(new Feedback(1, "Org1MSP", "comment1"))
            ), 1)));
        }
    }

    @Nested
    class GetAccount {

        @Test
        void testGetAccount() throws Exception {
            ATOContract atoContract = new ATOContract();
            MockContext mockCtx = MockContextUtil.newTestMockContextWithAccounts(MockIdentity.ORG2_AO);

            Instant now = Instant.now();
            mockCtx.setTimestamp(now);
            mockCtx.setTxId("123");
            atoContract.CreateATO(mockCtx, "memo", "artifacts");

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED.toString());
            atoContract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1");
            atoContract.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment2");

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new Account(
                            "Org2MSP",
                            Status.PENDING,
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
                            1
                    ),
                    account
            );
        }

        @Test
        void testGetAccountDoesNotExist() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);
            assertThrows(ChaincodeException.class, () -> contract.GetAccount(ctx, "org4msp"));
        }
    }

    @Nested
    class GetAccountStatus {

        @Test
        void testGetAccountStatus() throws Exception {
            MockContext ctx = newTestMockContextWithAccounts(MockIdentity.ORG1_AO);

            Status status = contract.GetAccountStatus(ctx);
            assertEquals(AUTHORIZED, status);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            status = contract.GetAccountStatus(ctx);
            assertEquals(Status.PENDING, status);
        }

    }

    @Nested
    class GetAccountHistory {

        @Test
        void testGetAccountHistory() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG1_AO, now);

            List<AccountHistorySnapshot> history = contract.GetAccountHistory(ctx, ORG2_MSP);
            assertTrue(history.contains(new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z", new Account("Org2MSP", Status.PENDING, new ATO("123", now.toString(), now.toString(), 1, "memo", "artifacts", List.of(new Feedback(1, "Org1MSP", "comment1"))), 1))));
            assertTrue(history.contains(new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z", new Account("Org2MSP", Status.PENDING, new ATO("123", now.toString(), now.toString(), 1, "memo", "artifacts", List.of()), 1))));
            assertTrue(history.contains(new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z", new Account("Org2MSP", Status.PENDING, null, 1))));
            assertTrue(history.contains(new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z", new Account("Org2MSP", null, null, 1))));
        }
    }
}