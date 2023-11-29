package contract;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.serialization.json.JSONSerializer;
import model.Account;
import model.Status;
import ngac.BlossomPDP;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static contract.AccountContract.accountKey;

/**
 * Chaincode functions to bootstrap the Blossom system.
 */
@Contract(
        name = "bootstrap",
        info = @Info(
                title = "Blossom bootstrap contract",
                description = "Chaincode functions to bootstrap the Blossom system",
                version = "0.0.1"
        )
)
public class BootstrapContract implements ContractInterface {

    /**
     * Bootstrap the Blossom authorization chaincode by initializing the NGAC policy. The account identified by the ADMINMSP
     * constant in the policy.pml file will automatically be set to AUTHORIZED. Once additional members are added to the
     * network, those members may vote (using a super majority) to rescind the Blossom Admin's authorized status if deemed
     * necessary.
     *
     * NGAC: Only an Authorizing Official from the ADMINMSP can call this function.
     *
     * @param ctx Chaincode context which stores the requesting CID and exposes world state functions.
     * @throws ChaincodeException If the Bootstrap method has already been called.
     * @throws ChaincodeException If there is an error building the initial NGAC policy configuration.
     * @throws ChaincodeException If there is an error reading the policy file.
     */
    @Transaction()
    public void Bootstrap(Context ctx) {
        // check if this has been called already by checking if the policy has already been created
        byte[] policyBytes = ctx.getStub().getState("policy");
        if (policyBytes.length > 0) {
            throw new ChaincodeException("Bootstrap already called");
        }

        // get the policy file defined in PML
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("policy.pml");
        if (resourceAsStream == null) {
            throw new ChaincodeException("could not read policy file");
        }

        String json;
        try {
            String pml = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);

            PAP pap = new BlossomPDP().bootstrap(ctx, pml);

            // serialize to json to store on ledger, this will be faster to deserialize for subsequent
            // policy requests than storing in PML
            json = pap.serialize(new JSONSerializer());
        } catch (IOException | PMException e) {
            throw new ChaincodeException(e);
        }

        // put policy on ledger
        ctx.getStub().putState("policy", json.getBytes(StandardCharsets.UTF_8));

        // put admin member account on ledger
        String mspid = ctx.getClientIdentity().getMSPID();
        Account account = new Account(mspid, Status.AUTHORIZED, 0, true);
        ctx.getStub().putState(accountKey(mspid), SerializationUtils.serialize(account));
    }

    @Transaction
    public String Test(Context ctx) {
        return "hello blossom";
    }
}
