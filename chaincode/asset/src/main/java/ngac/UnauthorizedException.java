package ngac;

import org.hyperledger.fabric.shim.ChaincodeException;

public class UnauthorizedException extends ChaincodeException {

    public UnauthorizedException() {
        super("unauthorized");
    }
}
