package contract;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.pdp.memory.MemoryPolicyReviewer;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static ngac.BlossomPDP.BLOSSOM_SYSTEM;
import static ngac.BlossomPDP.getNGACUserName;

@Contract(
        name = "ngac",
        info = @Info(title = "NGAC contract", version = "0.0.1")
)
@Default
public class NGACContract implements ContractInterface {

    public NGACContract() {
    }

    @Transaction()
    public void Init(Context ctx) throws PMException, IOException {
        // get the policy file defined in PML
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("policy.pml");
        if (resourceAsStream == null) {
            throw new PMException("could not read policy file");
        }

        String pml = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        System.out.println("pml: " + pml);

        // create a new PAP object to compile and execute the PML
        PAP pap = new PAP(new MemoryPolicyStore());
        UserContext user = new UserContext(getNGACUserName(ctx));
        pap.deserialize().fromPML(user, pml);

        // serialize to json to store on ledger, this will be faster to deserialize for subsequent
        // policy requests than storing in PML
        String json = pap.serialize().toJSON();

        MemoryPolicyReviewer reviewer = new MemoryPolicyReviewer(pap);
        AccessRightSet accessRights = reviewer.getAccessRights(user, BLOSSOM_SYSTEM);
        if (accessRights.contains("bootstrap")) {
            throw new PMException("user does not have permission to bootstrap");
        }

        ctx.getStub().putState("policy", json.getBytes(StandardCharsets.UTF_8));
    }
}
