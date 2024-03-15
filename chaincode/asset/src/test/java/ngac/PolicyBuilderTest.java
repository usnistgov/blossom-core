package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.reviewer.PolicyReviewer;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import mock.MockContext;
import mock.MockIdentity;
import model.Status;
import org.hyperledger.fabric.contract.Context;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static mock.MockContextUtil.newTestContext;
import static ngac.PolicyBuilder.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyBuilderTest {

    @Test
    void testInvalidRoles() {
        /*MockContext ctx = newTestContext(MockIdentity.ORG1_TPOC);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        ChaincodeException e = assertThrows(ChaincodeException.class, () -> {
            PolicyBuilder.buildWithUser(ctx);
        });
        assertEquals(e.getMessage(), "invalid role " + TPOC);
        ctx.setClientIdentity(MockIdentity.ORG1_SO);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildWithUser(ctx);
        });
        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildWithUser(ctx);
        });

        ctx.setClientIdentity(MockIdentity.ORG2_SO);
        e = assertThrows(ChaincodeException.class, () -> {
            PolicyBuilder.buildWithUser(ctx);
        });
        assertEquals(e.getMessage(), "invalid role " + SYS_OWNER);
        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildWithUser(ctx);
        });
        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        assertDoesNotThrow(() -> {
            PolicyBuilder.buildWithUser(ctx);
        });*/
    }

    @Test
    void testPrivilegesOnAssetDecision() throws PMException {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        PAP pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS, READ_ASSET_DETAIL, WRITE_ASSET));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(ALLOCATE_LICENSE, READ_ASSETS, READ_ASSET_DETAIL));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAssetDecision(ctx);
        test(ctx, pap, ASSET_TARGET, new AccessRightSet(READ_ASSETS));
    }

    @Test
    void testPrivilegesOnAccountDecision() throws PMException {
        MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
        ctx.getStub().setAccountStatus(Status.AUTHORIZED);
        PAP pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, READ_SWID));

        ctx.setClientIdentity(MockIdentity.ORG1_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, READ_SWID, READ_LICENSE));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(APPROVE_ORDER, READ_ORDER, READ_SWID, DENY_ORDER, READ_LICENSE));

        ctx.setClientIdentity(MockIdentity.ORG2_ACQ);
        pap = buildPolicyForAccountDecision(ctx, "Org3MSP");
        test(ctx, pap, accountTarget("Org3MSP"), new AccessRightSet());

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAccountDecision(ctx, "Org2MSP");
        test(ctx, pap, accountTarget("Org2MSP"), new AccessRightSet(READ_ORDER, RETURN_LICENSE, READ_SWID, INITIATE_ORDER, WRITE_SWID, READ_LICENSE));

        ctx.setClientIdentity(MockIdentity.ORG2_TPOC);
        pap = buildPolicyForAccountDecision(ctx, "Org3MSP");
        test(ctx, pap, accountTarget("Org3MSP"), new AccessRightSet());
    }

    private void test(Context ctx, PAP pap, String target, AccessRightSet arset) throws PMException {
        UserContext user = PolicyBuilder.getUserContextFromCID(ctx.getClientIdentity());
        Map<String, AccessRightSet> map = new PolicyReviewer(pap)
                .access()
                .buildCapabilityList(user);

        assertEquals(arset, map.get(target));
    }
}