package mock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MockIdentityTest {

    @Test
    void test() {
        MockIdentity org1SystemOwner = MockIdentity.ORG1_SYSTEM_OWNER;
        byte[] bytes = org1SystemOwner.getBytes();
        System.out.println(bytes);
    }

}