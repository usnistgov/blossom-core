package mock;

import contract.AssetContract;
import contract.request.asset.AddAssetRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static mock.MockOrgs.*;

public class MockContextUtil {

    public static MockContext newTestContext(MockIdentity initialIdentity) {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }

    public static MockContext newTestContextWithAsset(MockIdentity initialIdentity) {
        MockContext mockContext = new MockContext(MockIdentity.ORG1_SO);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG1_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG2_MSP);
        mockContext.getStub().addImplicitPrivateDataCollection(ORG3_MSP);

        mockContext.setClientIdentity(MockIdentity.ORG1_SO);
        mockContext.setTxId("123");
        mockContext.setTimestamp(Instant.now());
        mockContext.setTransientData(new AddAssetRequest("123", "2000-01-01", Set.of("l1", "l2")));
        new AssetContract().AddAsset(mockContext);

        mockContext.setClientIdentity(initialIdentity);

        return mockContext;
    }
}
