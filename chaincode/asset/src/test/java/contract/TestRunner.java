/*
package contract;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import mock.MockContext;
import mock.MockContextUtil;
import mock.MockIdentity;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static contract.TestInstance.toTestInstance;

public class TestRunner {

    public static void runTest(String name) throws IOException {
        InputStream resourceAsStream = TestRunner.class.getClassLoader().getResourceAsStream(name);
        if (resourceAsStream == null) {
            throw new ChaincodeException("could not read policy file");
        }

        String json = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        List<LinkedTreeMap<String, Object>> list = new Gson().fromJson(json, List.class);

        MockContext ctx = MockContextUtil.newTestContext(MockIdentity.ORG1_SO);

        int i = 0;
        for (LinkedTreeMap<String, Object> m : list) {
            TestInstance testInstance = toTestInstance(m);
            testInstance.run(ctx);
        }
    }


}
*/
