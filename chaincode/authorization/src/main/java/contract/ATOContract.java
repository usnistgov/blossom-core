package contract;

import contract.event.ATOEvent;
import contract.event.SubmitFeedbackEvent;
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

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
    
    public static String atoKey(String account) {
        return "ato:" + account;
    }

    /**
     * Create a new ATO for the account the CID belongs to. This will create a new ID using the transaction
     * id, reset the version to 1, and remove feedback from the previous version. The memo and artifacts must be
     * embedded in the transient field of the fabric context.
     *
     * Transient Data: {"memo": "memo text", "artifacts": "artifacts text"}
     *
     * event:
     *  - name: "CreateATO"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     */
    @Transaction
    public void CreateATO(Context ctx) {
        String accountId = ctx.getClientIdentity().getMSPID();

        ATORequest request = new ATORequest(ctx);

        // check the requesting cid can write an ATO
        new BlossomPDP().writeATO(ctx, accountId);

        ATO ato = ATO.createFromContext(ctx, request.memo, request.artifacts);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(ato);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId), bytes);

        // set event
        ctx.getStub().setEvent("CreateATO", SerializationUtils.serialize(new ATOEvent(accountId)));
    }

    /**
     * Update the ATO for the account the CID belongs to. This will increment the ATO version and update
     * the memo and artifacts fields. If either parameter is empty or null, the existing value will not be updated. The
     * memo and artifacts must be embedded in the transient field of the fabric context.
     *
     * Transient Data: {"memo": "memo text", "artifacts": "artifacts text"}
     *
     * event:
     *  - name: "UpdateATO"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the account's ATO has not been created yet.
     */
    @Transaction
    public void UpdateATO(Context ctx) {
        String accountId = ctx.getClientIdentity().getMSPID();

        ATORequest request = new ATORequest(ctx);

        // check the requesting cid can write an ATO
        new BlossomPDP().writeATO(ctx, accountId);

        // deserialize the account object from the state and update the ATO value
        ATO ato = GetATO(ctx, accountId);
        ato.update(ato.getVersion() + 1, ctx.getStub().getTxTimestamp().toString(), request.memo, request.artifacts);

        // serialize the account object with updated ATO and put to state
        byte[] bytes = SerializationUtils.serialize(ato);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId), bytes);

        // set event
        ctx.getStub().setEvent("UpdateATO", SerializationUtils.serialize(new ATOEvent(accountId)));
    }

    /**
     * Get teh ATO for the given member. It is not possible to get the history of the ATO since it is stored in the
     * members implicit private data collection which does not support key history.
     *
     * NGAC: CID must be authorized in order to view the ATO of another member. They do not need to be authorized to
     * see their own ATO.
     *
     * @param ctx Fabric context object.
     * @param accountId The member to get the ATO for.
     * @return The ATO of the given member.
     */
    @Transaction
    public ATO GetATO(Context ctx, String accountId) {
        new BlossomPDP().readATO(ctx, accountId);

        byte[] atoBytes = ctx.getStub().getPrivateData(accountImplicitDataCollection(accountId), atoKey(accountId));
        if (atoBytes.length == 0) {
            throw new ChaincodeException(accountId + " has not created an ATO yet");
        }

        return SerializationUtils.deserialize(atoBytes);
    }

    /**
     * Submit feedback on a member's ATO. The provided ATO version must match the member's current ATO version to ensure
     * the feedback is happening on the most recent version. The comments are stored in a string. The targetAccountId,
     * atoVersion, and comments must be embedded in the transient field of the fabric context.
     *
     * Transient Data: {"targetAccountId": "target id", "atoVersion": "ato version #", "comments": "comments text"}
     *
     * NGAC: All members can submit feedback on their own ATOs (i.e. respond directly to feedback from others). Only
     * authorized members can submit feedback to others.
     *
     * event:
     *  - name: "SubmitFeedback"
     *  - payload: a serialized Account object
     *
     * @param ctx Fabric context object.
     * @throws ChaincodeException If the cid is unauthorized or there is an error checking if the cid is unauthorized.
     * @throws ChaincodeException If the target account has not created an ATO yet.
     * @throws ChaincodeException If the feedback is targeting the wrong ATO version.
     */
    @Transaction
    public void SubmitFeedback(Context ctx) {
        String accountId = ctx.getClientIdentity().getMSPID();
        FeedbackRequest request = new FeedbackRequest(ctx);

        ATO targetATO = GetATO(ctx, request.targetAccountId);

        // check that the ato version being commented on is the same as the current version for the account
        int currentATOVersion = targetATO.getVersion();
        if (currentATOVersion != request.atoVersion) {
            throw new ChaincodeException("submitting feedback on incorrect ATO version: current version " +
                                                 currentATOVersion + ", got " + request.atoVersion);
        }

        // check that cid can submit feedback
        new BlossomPDP().submitFeedback(ctx, request.targetAccountId);

        Feedback feedback = new Feedback(request.atoVersion, accountId, request.comments);
        targetATO.addFeedback(feedback);

        byte[] bytes = SerializationUtils.serialize(targetATO);
        ctx.getStub().putPrivateData(accountImplicitDataCollection(request.targetAccountId), atoKey(request.targetAccountId), bytes);

        // set event
        ctx.getStub().setEvent("SubmitFeedback",
                               SerializationUtils.serialize(new SubmitFeedbackEvent(request.targetAccountId, accountId)));
    }

    static class ATORequest {
        final String memo;
        final String artifacts;

        ATORequest(Context ctx) {
            Map<String, byte[]> t = ctx.getStub().getTransient();
            System.out.println(t);

            byte[] memoBytes = t.get("memo");
            byte[] artBytes = t.get("artifacts");

            if (memoBytes == null) {
                memoBytes = new byte[]{};
            }

            if (artBytes == null) {
                artBytes = new byte[]{};
            }

            this.memo = new String(memoBytes, StandardCharsets.UTF_8);
            this.artifacts = new String(artBytes, StandardCharsets.UTF_8);
        }
    }

    static class FeedbackRequest {
        final String targetAccountId;
        final int atoVersion;
        final String comments;

        FeedbackRequest(Context ctx) {
            Map<String, byte[]> t = ctx.getStub().getTransient();

            byte[] targetBytes = t.get("targetAccountId");
            byte[] versionBytes = t.get("atoVersion");
            byte[] commentsBytes = t.get("comments");

            if (targetBytes == null || targetBytes.length == 0) {
                throw new ChaincodeException("targetAccountId cannot be null or empty");
            }

            if (versionBytes == null || versionBytes.length == 0) {
                throw new ChaincodeException("atoVersion cannot be null or empty");
            }

            if (commentsBytes == null || commentsBytes.length == 0) {
                throw new ChaincodeException("comments cannot be null or empty");
            }

            this.targetAccountId = new String(targetBytes, StandardCharsets.UTF_8);
            this.atoVersion = Integer.parseInt(new String(versionBytes, StandardCharsets.UTF_8));
            this.comments = new String(commentsBytes, StandardCharsets.UTF_8);
        }
    }
}
