package ngac;

import model.Status;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.Chaincode;

public class PDP {

    public static final String ADMINMSP = "Org1MSP";

    public static final String AUTH_CHAINCODE_NAME = "authorization";
    public static final String AUTH_CHANNEL_NAME = "authorization";

    public Status getAccountStatus(Context ctx) {
        // invoke the ATO channel chaincode to get the status of the requesting account using GetAccountStatus
        // the MSPID to check is embedded in the request context
        Chaincode.Response response = ctx.getStub()
                                         .invokeChaincodeWithStringArgs(
                                                 AUTH_CHAINCODE_NAME,
                                                 "GetAccountStatus",
                                                 AUTH_CHANNEL_NAME
                                         );
        return Status.fromString(response.getStringPayload());
    }
}
