package ngac;

import gov.nist.csd.pm.policy.events.PolicyEvent;

public class UpdateAccountStatusEvent implements PolicyEvent {

    private String accountName;
    private String status;

    public UpdateAccountStatusEvent(String accountName, String status) {
        this.accountName = accountName;
        this.status = status;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getEventName() {
        return "update_account_status";
    }
}
