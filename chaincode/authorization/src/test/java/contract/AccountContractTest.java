package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockContextUtil;
import mock.MockEvent;
import mock.MockIdentity;
import model.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static mock.MockContextUtil.*;
import static mock.MockOrgs.*;
import static model.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class AccountContractTest {

    private AccountContract contract = new AccountContract();

    @Nested
    class JoinTest {

        @Test
        void testJoinedFieldIsTrue() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            Account account = contract.GetAccount(ctx, ORG2_MSP);
            assertFalse(account.isJoined());

            contract.Join(ctx);

            account = contract.GetAccount(ctx, ORG2_MSP);
            assertTrue(account.isJoined());
        }

        @Test
        void testEvent() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            contract.Join(ctx);

            assertEquals(
                    new MockEvent("Join", new byte[]{}),
                    ctx.getStub().getMockEvent()
            );
        }

        @Test
        void testAlreadyJoined() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);

            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            Account account = contract.GetAccount(ctx, ORG2_MSP);
            assertFalse(account.isJoined());

            contract.Join(ctx);

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.Join(ctx));
            assertEquals("Org2MSP has already joined", e.getMessage());
        }

        @Test
        void testUnauthorized() throws PMException, IOException {
            MockContext ctx = newTestContext(MockIdentity.ORG2_AO);
            new MOUContract().SignMOU(ctx, 1);

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.Join(ctx));
            assertEquals(
                    "cid is not authorized to join for account Org2MSP",
                    e.getMessage()
            );
        }

    }

    @Nested
    class GetAccounts {

        @Test
        void testGetAccounts() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG1_AO, now);

            List<Account> accounts = List.of(contract.GetAccounts(ctx));
            assertEquals(3, accounts.size());
            assertTrue(accounts.contains(new Account(ORG1_MSP, AUTHORIZED, 0, true)));
            assertTrue(accounts.contains(new Account(ORG2_MSP, Status.AUTHORIZED, 1, true)));
            assertTrue(accounts.contains(new Account(ORG3_MSP, Status.AUTHORIZED, 1, true)));
        }

        @Test
        void testGetAccountsEmpty() throws Exception {
            MockContext ctx = new MockContext(MockIdentity.ORG1_AO);

            List<Account> accounts = List.of(contract.GetAccounts(ctx));
            assertTrue(accounts.isEmpty());
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
            mockCtx.setATOTransientData("memo", "artifacts");
            atoContract.CreateATO(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
            updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);

            Account account = new AccountContract().GetAccount(mockCtx, "Org2MSP");
            assertEquals(
                    new Account(
                            "Org2MSP",
                            AUTHORIZED,
                            1,
                            true
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

            String status = contract.GetAccountStatus(ctx);
            assertEquals(AUTHORIZED.toString(), status);

            ctx.setClientIdentity(MockIdentity.ORG2_AO);
            updateAccountStatus(ctx, ORG2_MSP, UNAUTHORIZED);
            status = contract.GetAccountStatus(ctx);
            assertEquals(Status.UNAUTHORIZED.toString(), status);
        }

    }

    @Nested
    class GetAccountHistory {

        @Test
        void testGetAccountHistory() throws Exception {
            Instant now = Instant.now();
            MockContext ctx = newTestMockContextWithAccountsAndATOs(MockIdentity.ORG1_AO, now);

            updateAccountStatus(ctx, ORG2_MSP, UNAUTHORIZED);
            updateAccountStatus(ctx, ORG2_MSP, AUTHORIZED);

            AccountHistorySnapshot[] history = contract.GetAccountHistory(ctx, ORG2_MSP);
            assertEquals(5, history.length);
            assertEquals(
                    new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z",
                                               new Account("Org2MSP", AUTHORIZED, 1, true)),
                    history[0]
            );
            assertEquals(
                    new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z",
                                               new Account("Org2MSP", UNAUTHORIZED, 1, true)),
                    history[1]
            );
            assertEquals(
                    new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z",
                                               new Account("Org2MSP", AUTHORIZED, 1, true)),
                    history[2]
            );
            assertEquals(
                    new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z",
                                               new Account("Org2MSP", AUTHORIZED, 1, false)),
                    history[3]
            );
            assertEquals(
                    new AccountHistorySnapshot("123", "1970-01-01T00:00:00.001Z",
                                               new Account("Org2MSP", PENDING, 1, false)),
                    history[4]
            );
        }
    }
}