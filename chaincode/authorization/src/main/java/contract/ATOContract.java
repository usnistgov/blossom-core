package contract;

import gov.nist.csd.pm.policy.exceptions.PMException;
import model.ATO;
import model.Account;
import model.Feedback;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.io.Serial;

import static contract.AccountContract.accountKey;

@Contract(
        name = "ato",
        info = @Info(
                title = "Blossom Authorization ATO Contract",
                description = "Functions supporting the Blossom ATO process",
                version = "0.0.1"
        )
)
public class ATOContract implements ContractInterface {

    private BlossomPDP pdp = new BlossomPDP();

    /**
     * Create a new ATO for the account the CID belongs to. This will create a new ID using the transaction
     * id, reset the version to 1, and remove feedback from the previous version.
     *
     * event:
     *  - name: "CreateATO"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @param memo The ATO memo value.
     * @param artifacts The ATO artifacts value.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void CreateATO(Context ctx, String memo, String artifacts) throws PMException {
        String mspid = ctx.getClientIdentity().getMSPID();

        // check the requesting cid can write an ATO
        pdp.writeATO(ctx, mspid);

        // deserialize the account object from the state and update the ATO value
        Account account = new AccountContract().GetAccount(ctx, mspid);
        ATO ato = ATO.createFromContext(ctx, memo, artifacts);
        account.setAto(ato);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(mspid), bytes);

        // set event
        ctx.getStub().setEvent("CreateATO", bytes);
    }

    /**
     * Update the ATO for the account the CID belongs to. This will increment the ATO version and update
     * the memo and artifacts fields. If either parameter is empty or null, the existing value will not be updated.
     *
     * event:
     *  - name: "UpdateATO"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @param memo The ATO memo value.
     * @param artifacts The ATO artifacts value.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void UpdateATO(Context ctx, String memo, String artifacts) throws PMException {
        String mspid = ctx.getClientIdentity().getMSPID();

        // check the requesting cid can write an ATO
        pdp.writeATO(ctx, mspid);

        // deserialize the account object from the state and update the ATO value
        Account account = new AccountContract().GetAccount(ctx, mspid);
        ATO ato = account.getAto();
        if (ato == null) {
            throw new ChaincodeException("account ato is null, must be created first using CreateATO");
        }

        account.updateATO(ato.getVersion() + 1, ctx.getStub().getTxTimestamp().toString(), memo, artifacts);
        account.setAto(ato);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(mspid), bytes);

        // set event
        ctx.getStub().setEvent("UpdateATO", bytes);
    }

    /**
     * Submit feedback on a member's ATO. The provided ATO version must match the member's current ATO version to ensure
     * the feedback is happening on the most recent version. The comments are stored in a string.
     *
     * event:
     *  - name: "SubmitFeedback"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @param targetOrg The org to provide feedback to.
     * @param atoVersion The ATO version the feedback is addressing.
     * @param comments The comments provided.
     * @throws PMException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    @Transaction
    public void SubmitFeedback(Context ctx, String targetOrg, int atoVersion, String comments) throws PMException {
        String mspid = ctx.getClientIdentity().getMSPID();

        // get the target account to retrieve the current ATO
        Account account = new AccountContract().GetAccount(ctx, targetOrg);
        if (account.getAto() == null) {
            throw new ChaincodeException("account " + targetOrg + " has not yet created an ATO");
        }

        // check that the ato version being commented on is the same as the current version for the account
        int currentATOVersion = account.getAto().getVersion();
        if (currentATOVersion != atoVersion) {
            throw new ChaincodeException("submitting feedback on incorrect ATO version: current version " +
                                                 currentATOVersion + ", got " + atoVersion);
        }

        // check that cid can submit feedback
        pdp.submitFeedback(ctx, targetOrg);

        Feedback feedback = new Feedback(atoVersion, mspid, comments);
        account.addATOFeedback(feedback);

        byte[] bytes = SerializationUtils.serialize(account);
        ctx.getStub().putState(accountKey(targetOrg), bytes);

        // set event
        ctx.getStub().setEvent("SubmitFeedback", bytes);
    }
}
