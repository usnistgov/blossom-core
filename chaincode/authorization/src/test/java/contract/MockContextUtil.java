package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import mock.MockContext;
import mock.MockIdentity;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static mock.MockOrgs.*;

public class MockContextUtil {

    public static MockContext newTestContext(MockIdentity initialIdentity) throws PMException, IOException {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SYSTEM_OWNER);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        BootstrapContract blossomContract = new BootstrapContract();
        blossomContract.Bootstrap(mockContext, "org1 test ato");

        // clear stub's transient
        mockContext.getStub().setTransientData(new HashMap<>());

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }

    public static MockContext buildTestMockContextWithAccounts() throws Exception {
        AccountContract contract = new AccountContract();

        MockContext mockCtx = MockContextUtil.newTestContext(MockIdentity.ORG2_SYSTEM_OWNER);
        contract.RequestAccount(mockCtx);

        mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
        contract.ApproveAccount(mockCtx, ORG2_MSP);

        mockCtx.setClientIdentity(MockIdentity.ORG3_SYSTEM_OWNER);
        contract.RequestAccount(mockCtx);

        mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);
        contract.ApproveAccount(mockCtx, ORG3_MSP);

        mockCtx.setClientIdentity(MockIdentity.ORG1_SYSTEM_OWNER);

        return mockCtx;
    }

}
