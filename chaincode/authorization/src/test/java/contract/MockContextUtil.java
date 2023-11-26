package contract;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.pml.value.StringValue;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import mock.MockContext;
import mock.MockIdentity;
import model.Account;
import model.Status;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;

import static contract.AccountContract.accountKey;
import static mock.MockOrgs.*;
import static model.Status.AUTHORIZED;
import static ngac.BlossomPDP.getUserCtxFromRequest;
import static ngac.BlossomPDP.loadPolicy;

public class MockContextUtil {

    public static MockContext newTestContext(MockIdentity initialIdentity) throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_AO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract bootstrapContract = new BootstrapContract();
        mockContext.setTimestamp(Instant.now());
        bootstrapContract.Bootstrap(mockContext);

        // clear stub's transient
        mockContext.getStub().setTransientData(new HashMap<>());

        // update mou
        new MOUContract().UpdateMOU(mockContext, "mou text");

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }

    public static MockContext newTestMockContextWithAccounts(MockIdentity initialIdentity) throws Exception {
        MOUContract contract = new MOUContract();
        AccountContract accountContract = new AccountContract();

        MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
        contract.SignMOU(mockCtx, 1);
        updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
        accountContract.Join(mockCtx);

        mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
        contract.SignMOU(mockCtx, 1);
        updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);
        accountContract.Join(mockCtx);

        mockCtx.setClientIdentity(initialIdentity);

        return mockCtx;
    }

    public static MockContext newTestMockContextWithOneAccount(MockIdentity initialIdentity) throws Exception {
        MOUContract contract = new MOUContract();

        MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_AO);
        contract.SignMOU(mockCtx, 1);
        updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
        new AccountContract().Join(mockCtx);

        mockCtx.setClientIdentity(initialIdentity);

        return mockCtx;
    }

    public static MockContext newTestMockContextWithAccountsAndATOs(MockIdentity initialIdentity, Instant ts) throws Exception {
        MOUContract mou = new MOUContract();
        ATOContract ato = new ATOContract();
        AccountContract acct = new AccountContract();

        MockContext mockCtx = new MockContext(MockIdentity.ORG1_AO);
        mockCtx.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockCtx.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockCtx.getStub().addImplicitPrivateDataCollection(ORG3_MSP);
        mockCtx.setTimestamp(ts);
        mockCtx.setTxId("123");

        BootstrapContract bootstrapContract = new BootstrapContract();
        bootstrapContract.Bootstrap(mockCtx);

        ato.CreateATO(mockCtx, "org1 test ato", "org1 artifacts");

        // clear stub's transient
        mockCtx.getStub().setTransientData(new HashMap<>());

        // update mou
        new MOUContract().UpdateMOU(mockCtx, "mou text");

        mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
        mou.SignMOU(mockCtx, 1);
        updateAccountStatus(mockCtx, ORG2_MSP, AUTHORIZED);
        acct.Join(mockCtx);

        mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
        mou.SignMOU(mockCtx, 1);
        updateAccountStatus(mockCtx, ORG3_MSP, AUTHORIZED);
        acct.Join(mockCtx);

        mockCtx.setClientIdentity(MockIdentity.ORG2_AO);
        ato.CreateATO(mockCtx, "memo", "artifacts");

        mockCtx.setClientIdentity(MockIdentity.ORG3_AO);
        ato.CreateATO(mockCtx, "memo", "artifacts");

        mockCtx.setClientIdentity(MockIdentity.ORG1_AO);
        ato.SubmitFeedback(mockCtx, "Org2MSP", 1, "comment1");
        ato.SubmitFeedback(mockCtx, "Org3MSP", 1, "comment1");

        mockCtx.setClientIdentity(initialIdentity);

        return mockCtx;
    }

    public static void updateAccountStatus(Context ctx, String mspid, Status status) throws PMException {
        MemoryPolicyStore memoryPolicyStore = loadPolicy(ctx, getUserCtxFromRequest(ctx));

        PAP pap = new PAP(memoryPolicyStore);
        pap.executePMLFunction(new UserContext("blossom admin"), "updateAccountStatus",
                               new StringValue(mspid), new StringValue(status.toString())
        );

        ctx.getStub().putState("policy", memoryPolicyStore.serialize(new JSONSerializer()).getBytes(StandardCharsets.UTF_8));

        // retrieve the account and update the status
        Account account = new AccountContract().GetAccount(ctx, mspid);
        account.setStatus(status);

        // update the account
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }


}
