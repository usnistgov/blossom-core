package contract;

import contract.event.SubmitFeedbackEvent;
import contract.event.ATOEvent;
import gov.nist.csd.pm.policy.exceptions.PMException;
import model.ATO;
import model.Feedback;
import ngac.BlossomPDP;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;

import static contract.AccountContract.accountImplicitDataCollection;

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

    public static String atoKey(String account) {
        return "ato:" + account;
    }

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
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    public void CreateATO(Context ctx, String memo, String artifacts) {
        String accountId = ctx.getClientIdentity().getMSPID();

        // check the requesting cid can write an ATO
        pdp.writeATO(ctx, accountId);

        ATO ato = ATO.createFromContext(ctx, memo, artifacts);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(ato);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId), bytes);

        // set event
        ctx.getStub().setEvent("CreateATO", SerializationUtils.serialize(new ATOEvent(accountId)));
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
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the account's ATO has not been created yet.
     */
    public void UpdateATO(Context ctx, String memo, String artifacts) {
        String accountId = ctx.getClientIdentity().getMSPID();

        // check the requesting cid can write an ATO
        pdp.writeATO(ctx, accountId);

        // deserialize the account object from the state and update the ATO value
        ATO ato = GetATO(ctx, accountId);
        ato.update(ato.getVersion() + 1, ctx.getStub().getTxTimestamp().toString(), memo, artifacts);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(ato);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId), bytes);

        // set event
        ctx.getStub().setEvent("UpdateATO", SerializationUtils.serialize(new ATOEvent(accountId)));
    }

    public ATO GetATO(Context ctx, String accountId) {
        pdp.readATO(ctx, accountId);

        byte[] atoBytes = ctx.getStub().getPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId));
        if (atoBytes == null || atoBytes.length == 0) {
            throw new ChaincodeException(accountId + " has not created an ATO yet");
        }

        return SerializationUtils.deserialize(atoBytes);
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
     * @param targetAccountId The org to provide feedback to.
     * @param atoVersion The target account's ATO version the feedback is addressing.
     * @param comments The comments provided.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the target account has not created an ATO yet.
     * @throws ChaincodeException If the feedback is targeting the wrong ATO version.
     */
    @Transaction
    public void SubmitFeedback(Context ctx, String targetAccountId, int atoVersion, String comments) {
        String accountId = ctx.getClientIdentity().getMSPID();

        ATO targetATO = GetATO(ctx, targetAccountId);

        // check that the ato version being commented on is the same as the current version for the account
        int currentATOVersion = targetATO.getVersion();
        if (currentATOVersion != atoVersion) {
            throw new ChaincodeException("submitting feedback on incorrect ATO version: current version " +
                                                 currentATOVersion + ", got " + atoVersion);
        }

        // check that cid can submit feedback
        pdp.submitFeedback(ctx, targetAccountId);

        Feedback feedback = new Feedback(atoVersion, accountId, comments);
        targetATO.addFeedback(feedback);

        byte[] bytes = SerializationUtils.serialize(targetATO);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(targetAccountId), atoKey(targetAccountId), bytes);

        // set event
        ctx.getStub().setEvent("SubmitFeedback",
                               SerializationUtils.serialize(new SubmitFeedbackEvent(targetAccountId, accountId)));
    }
}
