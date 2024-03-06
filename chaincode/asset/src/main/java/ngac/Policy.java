package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;

public class Policy {

    // used for account IPDC
    public static PAP getAccountPolicy() throws PMException {
        PAP pap = new PAP(new MemoryPolicyStore());

        // TODO load policy into memory using java code not pml

        return pap;
    }

    // used for adminmsp and license store
    public static PAP getADMINMSPPolicy() throws PMException {
        PAP pap = new PAP(new MemoryPolicyStore());

        // TODO load policy into memory using java code not pml

        return pap;
    }
}
