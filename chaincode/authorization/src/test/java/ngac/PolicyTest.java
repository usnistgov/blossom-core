package ngac;

import gov.nist.csd.pm.epp.EPP;
import gov.nist.csd.pm.epp.EventContext;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pdp.memory.MemoryPDP;
import gov.nist.csd.pm.pdp.memory.MemoryPolicyReviewer;
import gov.nist.csd.pm.policy.events.PolicyEvent;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import ngac.ApproveAccountEvent;
import ngac.UpdateAccountStatusEvent;
import ngac.VoteEvent;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static contract.NGACContract.*;
import static gov.nist.csd.pm.pap.SuperPolicy.SUPER_USER;
import static ngac.BlossomPDP.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyTest {

    public static final String SAMS = "Org1MSP";
    public static final String ORG2 = "Org2MSP";
    public static final String ORG3 = "Org3MSP";
    public static final TestUser BLOSSOM_SYSTEM_OWNER = new TestUser(new UserContext("blossom_sys_owner"), SAMS, "System Owner");
    public static final TestUser BLOSSOM_SYSTEM_ADMIN = new TestUser(new UserContext("blossom_sys_admin"), SAMS, "System Administrator");
    public static final TestUser ORG2_SYSTEM_OWNER = new TestUser(new UserContext("org2_sys_owner"), ORG2, "System Owner");
    public static final TestUser ORG2_SYSTEM_ADMIN = new TestUser(new UserContext("org2_sys_admin"), ORG2, "System Administrator");
    public static final TestUser ORG3_SYSTEM_OWNER = new TestUser(new UserContext("org3_sys_owner"), ORG3, "System Owner");
    public static final TestUser ORG3_SYSTEM_ADMIN = new TestUser(new UserContext("org3_sys_admin"), ORG3, "System Administrator");

    private PAP getPAP() throws PMException, IOException {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("policy.pml");
        if (resourceAsStream == null) {
            throw new PMException("could not read policy file");
        }

        String pml = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

        // create a new PAP object to compile and execute the PML
        PAP pap = new PAP(new MemoryPolicyStore());
        UserContext userCtx = new UserContext(SUPER_USER);
        pap.deserialize().fromPML(userCtx, pml);

        return pap;
    }

    private void addUser(PAP pap, TestUser user) throws PMException {
        String acctUA = accountUsersNodeName(user.getAccount());
        pap.graph().createUser(user.getUser().getUser(), user.getRole());
        if (pap.graph().nodeExists(acctUA)) {
            pap.graph().assign(user.user.getUser(), acctUA);
        }
    }

    private void cleanupUser(PAP pap, TestUser user) throws PMException {
        pap.graph().deleteNode(user.user.getUser());
    }

    @Test
    void test() throws PMException, IOException {
        PAP pap = testInitialize();
        testApproveAccount1(pap);
        testUpdateAccount1StatusToAUTHORIZED(pap);
        testApproveAccount2(pap);
        testUpdateAccount2StatusToAUTHORIZED(pap);
        testInitiateVoteOnAccount1(pap);
        testCompleteVoteOnAccount1SetStatusToUNAUTHORIZED(pap);


        /*PAP pap = getPAP(BLOSSOM_SYSTEM_OWNER, "Org1MSP", "System Owner");
        MemoryPolicyReviewer rev = new MemoryPolicyReviewer(pap);
        AccessRightSet accessRights = rev.getAccessRights(new UserContext(BLOSSOM_SYSTEM_OWNER), BLOSSOM_SYSTEM);
        System.out.println(accessRights);

        PDP pdp = new MemoryPDP(pap, false);
        EPP epp = new EPP(pdp, pap);
        epp.handlePolicyEvent(new EventContext(
                new UserContext(BLOSSOM_SYSTEM_OWNER),
                new UpdateAccountStatusEvent("Org1MSP", "NOT_AUTHORIZED")
        ));

        accessRights = rev.getAccessRights(new UserContext(BLOSSOM_SYSTEM_OWNER), BLOSSOM_SYSTEM);
        System.out.println(accessRights);

        epp.handlePolicyEvent(new EventContext(
                new UserContext(BLOSSOM_SYSTEM_OWNER),
                new UpdateAccountStatusEvent("Org1MSP", "AUTHORIZED")
        ));

        accessRights = rev.getAccessRights(new UserContext(BLOSSOM_SYSTEM_OWNER), BLOSSOM_SYSTEM);
        System.out.println(accessRights);*/


    }

    private void processEvent(PAP pap, TestUser testUser, PolicyEvent event) throws PMException {
        PDP pdp = new MemoryPDP(pap, false);
        EPP epp = new EPP(pdp, pap);
        epp.handlePolicyEvent(new EventContext(
                testUser.user,
                event
        ));
    }

    private void checkAccessRights(PAP pap, TestUser subject, String target, AccessRightSet expected) throws PMException {
        MemoryPolicyReviewer reviewer = new MemoryPolicyReviewer(pap);
        assertEquals(expected, reviewer.getAccessRights(subject.user, target));
    }

    private PAP testInitialize() throws PMException, IOException {
        PAP pap = getPAP();

        addUser(pap, BLOSSOM_SYSTEM_OWNER);

        checkAccessRights(pap, BLOSSOM_SYSTEM_OWNER, BLOSSOM_SYSTEM, new AccessRightSet("update_account_status", "request_account", "initiate_vote", "approve_account", "bootstrap"));

        cleanupUser(pap, BLOSSOM_SYSTEM_OWNER);

        return pap;
    }

    private void testApproveAccount1(PAP pap) throws PMException, IOException {
        addUser(pap, ORG2_SYSTEM_OWNER);

        // test user can request account
        checkAccessRights(pap, ORG2_SYSTEM_OWNER, BLOSSOM_SYSTEM, new AccessRightSet("request_account"));
        cleanupUser(pap, ORG2_SYSTEM_OWNER);

        processEvent(pap, BLOSSOM_SYSTEM_OWNER, new ApproveAccountEvent(ORG2));

        addUser(pap, ORG2_SYSTEM_OWNER);
        checkAccessRights(pap, ORG2_SYSTEM_OWNER, BLOSSOM_SYSTEM, new AccessRightSet("request_account", "initiate_vote"));
        cleanupUser(pap, ORG2_SYSTEM_OWNER);
    }

    private void testUpdateAccount1StatusToAUTHORIZED(PAP pap) throws PMException {
        addUser(pap, BLOSSOM_SYSTEM_OWNER);
        processEvent(pap, BLOSSOM_SYSTEM_OWNER, new UpdateAccountStatusEvent(ORG2, "AUTHORIZED"));
        cleanupUser(pap, BLOSSOM_SYSTEM_OWNER);

        addUser(pap, ORG2_SYSTEM_ADMIN);
        checkAccessRights(pap, ORG2_SYSTEM_ADMIN, accountObjectNodeName(ORG2), new AccessRightSet("upload_ato"));
        cleanupUser(pap, ORG2_SYSTEM_ADMIN);
    }

    private void testApproveAccount2(PAP pap) throws PMException {
        addUser(pap, ORG3_SYSTEM_OWNER);

        // test user can request account
        checkAccessRights(pap, ORG3_SYSTEM_OWNER, BLOSSOM_SYSTEM, new AccessRightSet("request_account"));
        cleanupUser(pap, ORG3_SYSTEM_OWNER);

        processEvent(pap, BLOSSOM_SYSTEM_OWNER, new ApproveAccountEvent(ORG3));

        addUser(pap, ORG3_SYSTEM_OWNER);
        checkAccessRights(pap, ORG3_SYSTEM_OWNER, BLOSSOM_SYSTEM, new AccessRightSet("request_account", "initiate_vote"));
        cleanupUser(pap, ORG3_SYSTEM_OWNER);
    }

    private void testUpdateAccount2StatusToAUTHORIZED(PAP pap) throws PMException {
        addUser(pap, BLOSSOM_SYSTEM_OWNER);
        processEvent(pap, BLOSSOM_SYSTEM_OWNER, new UpdateAccountStatusEvent(ORG3, "AUTHORIZED"));
        cleanupUser(pap, BLOSSOM_SYSTEM_OWNER);

        addUser(pap, ORG3_SYSTEM_ADMIN);
        checkAccessRights(pap, ORG3_SYSTEM_ADMIN, accountObjectNodeName(ORG3), new AccessRightSet("upload_ato"));
        cleanupUser(pap, ORG3_SYSTEM_ADMIN);
    }

    private void testInitiateVoteOnAccount1(PAP pap) throws PMException {
        addUser(pap, ORG2_SYSTEM_OWNER);
        processEvent(pap, ORG2_SYSTEM_OWNER, new VoteEvent("initiate_vote", ORG2, "123", ORG3, false, ""));
        cleanupUser(pap, ORG2_SYSTEM_OWNER);
    }

    private void testCompleteVoteOnAccount1SetStatusToUNAUTHORIZED(PAP pap) throws PMException {
        addUser(pap, ORG3_SYSTEM_OWNER);
        checkAccessRights(pap, ORG3_SYSTEM_OWNER, voteObj(ORG3, "123"), new AccessRightSet());
        cleanupUser(pap, ORG3_SYSTEM_OWNER);

        addUser(pap, ORG2_SYSTEM_ADMIN);
        checkAccessRights(pap, ORG2_SYSTEM_ADMIN, voteObj(ORG3, "123"), new AccessRightSet());
        cleanupUser(pap, ORG2_SYSTEM_ADMIN);

        addUser(pap, ORG2_SYSTEM_OWNER);
        checkAccessRights(pap, ORG2_SYSTEM_OWNER, voteObj(ORG3, "123"), new AccessRightSet("complete_vote", "delete_vote"));
        cleanupUser(pap, ORG2_SYSTEM_OWNER);
    }

    static class TestUser {

        private UserContext user;
        private String account;
        private String role;

        public TestUser(UserContext user, String account, String role) {
            this.user = user;
            this.account = account;
            this.role = role;
        }

        public UserContext getUser() {
            return user;
        }

        public String getAccount() {
            return account;
        }

        public String getRole() {
            return role;
        }
    }
}