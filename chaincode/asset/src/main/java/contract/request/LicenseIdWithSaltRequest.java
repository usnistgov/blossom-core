package contract.request;

public class LicenseIdWithSaltRequest {

    private String id;
    private String salt;

    public LicenseIdWithSaltRequest(String id, String salt) {
        this.id = id;
        this.salt = salt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

}
