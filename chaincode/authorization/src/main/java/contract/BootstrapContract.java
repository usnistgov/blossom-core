package contract;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.policy.exceptions.PMException;
import model.Account;
import model.Status;
import ngac.BlossomPDP;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static contract.AccountContract.accountKey;

@Contract(
        name = "bootstrap",
        info = @Info(title = "Blossom bootstrap contract", version = "0.0.1")
)
public class BootstrapContract {

    /**
     * Bootstrap the blossom authorization chaincode by initializing the NGAC policy and setting up the Blossom Admin
     * member account status. An ATO is required in the request in order to set the Blossom Admin account's status
     * to "AUTHORIZED". Once additional members are added to Blossom, those members may vote (using a super majority) to
     * rescind the Blossom Admin's authorized status if the ATO provided during bootstrapping is not satisfactory.
     * Only a users from the AdminMSP defined in policy.pml can call this method. This method can only be called once.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @param ato The ATO for the Blossom Admin member account.
     * @throws PMException If there is an error building the initial NGAC policy configuration.
     * @throws IOException If there is an error reading the policy file.
     */
    @Transaction()
    public void Bootstrap(Context ctx, String ato) throws PMException, IOException {
        if (ato == null || ato.isEmpty()) {
            throw new ChaincodeException("cannot bootstrap without ATO");
        }

        // check if this has been called already by checking if the policy has already been created
        if (ctx.getStub().getState("policy") != null) {
            throw new ChaincodeException("Bootstrap already called");
        }

        // get the policy file defined in PML
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("policy.pml");
        if (resourceAsStream == null) {
            throw new PMException("could not read policy file");
        }

        String pml = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

        PAP pap = new BlossomPDP().initNGAC(ctx, pml);

        // serialize to json to store on ledger, this will be faster to deserialize for subsequent
        // policy requests than storing in PML
        String json = pap.serialize().toJSON();

        // put policy on ledger
        ctx.getStub().putState("policy", json.getBytes(StandardCharsets.UTF_8));

        // put admin member account on ledger
        String mspid = ctx.getClientIdentity().getMSPID();
        Account account = new Account(mspid, Status.AUTHORIZED, ato);
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }

}
