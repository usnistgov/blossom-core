package contract;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import mock.MockContext;
import mock.MockIdentity;
import model.Account;
import model.Status;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static contract.AccountContract.accountKey;
import static contract.MockContextUtil.buildTestMockContextWithAccounts;
import static mock.MockOrgs.*;
import static ngac.BlossomPDP.loadPolicy;
import static org.junit.jupiter.api.Assertions.*;

class AccountContractTest {

    private AccountContract contract = new AccountContract();

    static void updateAccountStatus(AccountContract contract, Context ctx, String mspid, String statusStr) throws Exception {
        updateAccountStatus(ctx, mspid, statusStr);

        // check the provided status is valid
        Status status = Status.fromString(statusStr);

        // retrieve the account and update the status
        Account account = contract.GetAccount(ctx, mspid);
        account.setStatus(status);

        // update the account
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }

    private static void updateAccountStatus(Context ctx, String account, String status) throws PMException {
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx);

        PAP pap = new PAP(memoryPolicyStore);
        String pml = String.format("updateAccountStatus('%s', '%s')", account, status);
        pap.executePML(new UserContext("blossom admin"), pml);

        ctx.getStub().putState("policy", memoryPolicyStore.serialize().toJSON().getBytes(StandardCharsets.UTF_8));
    }
    
    @Nested
    class RequestAccount {
        @Test
        void testNonSystemOwner() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_ADMIN);

            assertThrows(PMException.class, () -> contract.RequestAccount(mockCtx));
        }

        @Test
        void testSystemOwner() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);

            contract.RequestAccount(mockCtx);

            Account account = contract.GetAccount(mockCtx, ORG2_MSP);
            assertEquals(ORG2_MSP, account.getId());
            assertEquals("", account.getAto());
            assertEquals(Status.PENDING_APPROVAL, account.getStatus());
        }

        @Test
        void testAccountAlreadyExists() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);

            contract.RequestAccount(mockCtx);

            assertThrows(ChaincodeException.class, () -> {
                contract.RequestAccount(mockCtx);
            });
        }
    }

    @Nested
    class ApproveAccount {

        @Test
        void testAuthorizedUser() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);
            contract.RequestAccount(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            contract.ApproveAccount(mockCtx, ORG2_MSP);

            Account account = contract.GetAccount(mockCtx, ORG2_MSP);
            assertEquals(ORG2_MSP, account.getId());
            assertEquals("", account.getAto());
            assertEquals(Status.PENDING_ATO, account.getStatus());
        }

        @Test
        void testUnauthorizedUser() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);
            contract.RequestAccount(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            contract.ApproveAccount(mockCtx, ORG2_MSP);

            mockCtx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
            contract.RequestAccount(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> contract.ApproveAccount(mockCtx, ORG3_MSP));

            Account account = contract.GetAccount(mockCtx, ORG3_MSP);
            assertEquals(ORG3_MSP, account.getId());
            assertEquals("", account.getAto());
            assertEquals(Status.PENDING_APPROVAL, account.getStatus());
        }

        @Test
        void testAccountDoesNotExist() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG1_SYSTEM_OWNER);
            assertThrows(ChaincodeException.class, () -> contract.ApproveAccount(mockCtx, "non existing account"));
        }

        @Test
        void testAccountAlreadyApproved() throws PMException, IOException {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);
            contract.RequestAccount(mockCtx);

            mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
            contract.ApproveAccount(mockCtx, ORG2_MSP);

            assertThrows(ChaincodeException.class, () -> contract.ApproveAccount(mockCtx, ORG2_MSP));
        }
    }

    @Nested
    class UploadATO {

        @Test
        void testAuthorizedUser() throws Exception {
            MockContext mockCtx = buildTestMockContextWithAccounts();

            mockCtx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            contract.UploadATO(mockCtx, "test ato");

            Account account = contract.GetAccount(mockCtx, ORG2_MSP);
            assertEquals(ORG2_MSP, account.getId());
            assertEquals("test ato", account.getAto());
            assertEquals(Status.PENDING_ATO, account.getStatus());
        }

        @Test
        void testUnauthorizedUser() throws Exception {
            MockContext mockCtx = buildTestMockContextWithAccounts();

            mockCtx.setClientIdentity(MockIdentity.ORG2_SYSTEM_OWNER);
            assertThrows(PMException.class, () -> contract.UploadATO(mockCtx, "test ato"));
        }

        @Test
        void testPendingStatus() throws Exception {
            MockContext mockCtx = buildTestMockContextWithAccounts();

            updateAccountStatus(contract, mockCtx, ORG2_MSP, Status.UNAUTHORIZED_ATO.name());

            mockCtx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            contract.UploadATO(mockCtx, "test ato");

            Account account = contract.GetAccount(mockCtx, ORG2_MSP);
            assertEquals(ORG2_MSP, account.getId());
            assertEquals("test ato", account.getAto());
            assertEquals(Status.UNAUTHORIZED_ATO, account.getStatus());
        }

        @Test
        void testNoAtoProvided() throws Exception {
            MockContext mockCtx = buildTestMockContextWithAccounts();

            updateAccountStatus(contract, mockCtx, ORG2_MSP, Status.UNAUTHORIZED_ATO.name());

            mockCtx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            assertThrows(ChaincodeException.class, () -> contract.UploadATO(mockCtx, null));
            assertThrows(ChaincodeException.class, () -> contract.UploadATO(mockCtx, ""));
        }
    }

    @Nested
    class GetAccounts {

        @Test
        void testGetAccounts() throws Exception {
            MockContext ctx = buildTestMockContextWithAccounts();

            List<Account> accounts = contract.GetAccounts(ctx);
            assertEquals(3, accounts.size());
            assertTrue(accounts.contains(new Account(ORG1_MSP, Status.AUTHORIZED, "org1 test ato")));
            assertTrue(accounts.contains(new Account(ORG2_MSP, Status.PENDING_ATO, "")));
            assertTrue(accounts.contains(new Account(ORG3_MSP, Status.PENDING_ATO, "")));
        }

        @Test
        void testGetAccountsAdminOnly() throws Exception {
            MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);
            List<Account> accounts = contract.GetAccounts(mockCtx);
            assertEquals(1, accounts.size());
            assertTrue(accounts.contains(new Account(ORG1_MSP, Status.AUTHORIZED, "org1 test ato")));
        }
    }

    @Nested
    class GetAccount {

        @Test
        void testGetAccount() throws Exception {
            MockContext ctx = buildTestMockContextWithAccounts();

            Account account = contract.GetAccount(ctx, ORG2_MSP);
            assertEquals(account, new Account(ORG2_MSP, Status.PENDING_ATO, ""));

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            contract.UploadATO(ctx, "test ato");

            account = contract.GetAccount(ctx, ORG2_MSP);
            assertEquals(account, new Account(ORG2_MSP, Status.PENDING_ATO, "test ato"));
        }

        @Test
        void testGetAccountDoesNotExist() throws Exception {
            MockContext ctx = buildTestMockContextWithAccounts();
            assertThrows(ChaincodeException.class, () -> contract.GetAccount(ctx, "org4msp"));
        }
    }

    @Nested
    class GetAccountStatus {

        @Test
        void testGetAccountStatus() throws Exception {
            MockContext ctx = buildTestMockContextWithAccounts();

            Status status = contract.GetAccountStatus(ctx);
            assertEquals(Status.AUTHORIZED, status);

            ctx.setClientIdentity(MockIdentity.ORG2_SYSTEM_ADMIN);
            status = contract.GetAccountStatus(ctx);
            assertEquals(Status.PENDING_ATO, status);

            ctx.setClientIdentity(MockIdentity.ORG3_ACQ_SPEC);
            status = contract.GetAccountStatus(ctx);
            assertEquals(Status.PENDING_ATO, status);
        }

    }
}