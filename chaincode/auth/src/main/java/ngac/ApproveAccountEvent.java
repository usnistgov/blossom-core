package ngac;

import gov.nist.csd.pm.policy.events.PolicyEvent;

public class ApproveAccountEvent implements PolicyEvent {

    private String accountName;

    public ApproveAccountEvent(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String getEventName() {
        return "approve_account";
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }
}
