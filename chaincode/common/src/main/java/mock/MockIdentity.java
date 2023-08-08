package mock;

import com.google.protobuf.ByteString;
import org.bouncycastle.operator.OperatorCreationException;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.protos.msp.Identities;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public enum MockIdentity {

    ORG1_SYSTEM_OWNER(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICdTCCAhugAwIBAgIUbfrU688TQ4/2lSy/lw33sZRitpkwCgYIKoZIzj0EAwIw\ncDELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMQ8wDQYDVQQH\nEwZEdXJoYW0xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjMwNzE3MjE0NjAwWhcNMjQwNzE2MjE1MTAw\nWjAlMQ8wDQYDVQQLEwZjbGllbnQxEjAQBgNVBAMTCWFkbWludXNlcjBZMBMGByqG\nSM49AgEGCCqGSM49AwEHA0IABM5ey81YO6LO4x1zmNZsjtapQQoNjJfLkXMXGV7p\n48iNWjP6wEdHYnW4HW5v47/rEG7laMYQBeSLD0PjH2IMgVOjgd0wgdowDgYDVR0P\nAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFJ17JDgnl/Pwmf/NFd8T\nKtI0EDeOMB8GA1UdIwQYMBaAFA9OzheTdIen28/ATQMscQHMBZp4MHoGCCoDBAUG\nBwgBBG57ImF0dHJzIjp7ImJsb3Nzb20ucm9sZSI6IlN5c3RlbSBPd25lciIsImhm\nLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoiYWRtaW51c2VyIiwi\naGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNIADBFAiEAx+MAx3H3WjAv\ndpKjzsaNBYR3FLaoluZGaleVua98tQ4CIHsHkqNUJxWQgTom0u50BR9f4qbnoV0g\n1JV73yL7mVmg\n-----END CERTIFICATE-----\n",
            "Org1MSP"
    )),
    ORG1_NON_ADMIN(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgzCCAiqgAwIBAgIUMovz0y5Qc0slkpwcvyfpLHg7mPYwCgYIKoZIzj0EAwIw\ncDELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMQ8wDQYDVQQH\nEwZEdXJoYW0xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjMwNzE3MjE0OTAwWhcNMjQwNzE2MjE1NDAw\nWjAoMQ8wDQYDVQQLEwZjbGllbnQxFTATBgNVBAMTDG5vbmFkbWludXNlcjBZMBMG\nByqGSM49AgEGCCqGSM49AwEHA0IABH/ugMOhA8rt/tQJnVufidImQRnFWifOnAkm\ncpL1ytmz7fvtbEuRQbFKQJh0dZNuDaqs7x4wtpoCZZ3a7ol6CuejgekwgeYwDgYD\nVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFNZ73fUKBp+fKGDJ\neBGF9VDNJ+hhMB8GA1UdIwQYMBaAFA9OzheTdIen28/ATQMscQHMBZp4MIGFBggq\nAwQFBgcIAQR5eyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJTeXN0ZW0gQWRtaW5p\nc3RyYXRvciIsImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoi\nbm9uYWRtaW51c2VyIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNH\nADBEAiB7tCaEjqGHNVvDW5OPQSYGO1Om4n9e09HsfSl5u4bXBAIgafJ+5lzrBIz4\nRtjFuk0NBej1rpUIajOK2GItU7LePbo=\n-----END CERTIFICATE-----\n",
            "Org1MSP"
    )),

    ORG2_SYSTEM_OWNER(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICejCCAiGgAwIBAgIUXOzKBOG/k5tjUBcOtVqZ4V9A8HQwCgYIKoZIzj0EAwIw\nbDELMAkGA1UEBhMCVUsxEjAQBgNVBAgTCUhhbXBzaGlyZTEQMA4GA1UEBxMHSHVy\nc2xleTEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eub3Jn\nMi5leGFtcGxlLmNvbTAeFw0yMzA3MTcyMTUyMDBaFw0yNDA3MTYyMTU3MDBaMCox\nDzANBgNVBAsTBmNsaWVudDEXMBUGA1UEAwwOb3JnMl9zeXNfb3duZXIwWTATBgcq\nhkjOPQIBBggqhkjOPQMBBwNCAARb2+cdPRk9uBXv6oooW6qdVnUJpTDdXDck741a\nGHH4PMk29Q03i1FwY06lACqCKASRnNVb7wAhfPVJIFtTYQqTo4HiMIHfMA4GA1Ud\nDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBRvdSBriWW92nuUWwIR\nP4Go2LK36TAfBgNVHSMEGDAWgBRstPpD188xsoL/1CXdq5EA29OrxjB/BggqAwQF\nBgcIAQRzeyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJTeXN0ZW0gT3duZXIiLCJo\nZi5BZmZpbGlhdGlvbiI6IiIsImhmLkVucm9sbG1lbnRJRCI6Im9yZzJfc3lzX293\nbmVyIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNHADBEAiB+SBeu\nSLzNQPcwC6O6tBv5kT85m33Gpy5y0YK2xbnMRgIgMRVp0cQPMqe6+9Ph9mK9MG2/\n55NQE4jgLQX2TiX+Umk=\n-----END CERTIFICATE-----\n",
            "Org2MSP"
    )),
    ORG2_SYSTEM_ADMIN(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgzCCAiqgAwIBAgIUafxVzEcdpNjEXvInGMtV4uEov2IwCgYIKoZIzj0EAwIw\nbDELMAkGA1UEBhMCVUsxEjAQBgNVBAgTCUhhbXBzaGlyZTEQMA4GA1UEBxMHSHVy\nc2xleTEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eub3Jn\nMi5leGFtcGxlLmNvbTAeFw0yMzA3MTcyMTUyMDBaFw0yNDA3MTYyMTU3MDBaMCox\nDzANBgNVBAsTBmNsaWVudDEXMBUGA1UEAwwOb3JnMl9zeXNfYWRtaW4wWTATBgcq\nhkjOPQIBBggqhkjOPQMBBwNCAASXxQmvJvAnbzv9IrHa+MKLd4lepS5kPqwi64lg\nkvIJ/SU5cqy1k+1ln72ZE8nz+llBq2wq7QfBrn/oDX9I4TNwo4HrMIHoMA4GA1Ud\nDwEB/wQEAwIHgDAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBSaQGMaIfDvyMVsfp7r\n6m5ROSUYGDAfBgNVHSMEGDAWgBRstPpD188xsoL/1CXdq5EA29OrxjCBhwYIKgME\nBQYHCAEEe3siYXR0cnMiOnsiYmxvc3NvbS5yb2xlIjoiU3lzdGVtIEFkbWluaXN0\ncmF0b3IiLCJoZi5BZmZpbGlhdGlvbiI6IiIsImhmLkVucm9sbG1lbnRJRCI6Im9y\nZzJfc3lzX2FkbWluIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNH\nADBEAiBuvs7uWbhQbil9FAFej99sOAo30JuPsVAp3BJHAfUiNQIgGtedRv95HQl8\nQP0f2+EIplZUe3RmL+cX1B4Y9DeiBng=\n-----END CERTIFICATE-----\n",
            "Org2MSP"
    )),
    ORG2_ACQ_SPEC(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgzCCAiqgAwIBAgIUGBUFhXByCOntIz5fWQLMqhzWQnwwCgYIKoZIzj0EAwIw\nbDELMAkGA1UEBhMCVUsxEjAQBgNVBAgTCUhhbXBzaGlyZTEQMA4GA1UEBxMHSHVy\nc2xleTEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eub3Jn\nMi5leGFtcGxlLmNvbTAeFw0yMzA3MTcyMTUyMDBaFw0yNDA3MTYyMTU3MDBaMCkx\nDzANBgNVBAsTBmNsaWVudDEWMBQGA1UEAwwNb3JnMl9hY3Ffc3BlYzBZMBMGByqG\nSM49AgEGCCqGSM49AwEHA0IABO47433Ir/tRN64QH5jHrtWgxHr5kms5n0KSioh1\neLaOcZKgRvOmSnsDMxeJOReurk24nemA0oJardMx6n9sDIKjgewwgekwDgYDVR0P\nAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFFi9PxRBoSK0BM2hvbdO\ne+WH/WRrMB8GA1UdIwQYMBaAFGy0+kPXzzGygv/UJd2rkQDb06vGMIGIBggqAwQF\nBgcIAQR8eyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJBY3F1aXNpdGlvbiBTcGVj\naWFsaXN0IiwiaGYuQWZmaWxpYXRpb24iOiIiLCJoZi5FbnJvbGxtZW50SUQiOiJv\ncmcyX2FjcV9zcGVjIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNH\nADBEAiAltdD8V1hqpMs6M/+3AnjVQf/mUwk7oKALoY31xCejQQIgArXCP3rxeG0R\nvUkUY6AbR4nhWaHwFxMdOWGt/+COjXM=\n-----END CERTIFICATE-----\n",
            "Org2MSP"
    )),

    ORG3_SYSTEM_OWNER(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgDCCAiagAwIBAgIUeMdRbog+o3uBaPY1MK+ylcLy7a4wCgYIKoZIzj0EAwIw\ncTELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRAwDgYDVQQH\nEwdSYWxlaWdoMRkwFwYDVQQKExBvcmczLmV4YW1wbGUuY29tMRwwGgYDVQQDExNj\nYS5vcmczLmV4YW1wbGUuY29tMB4XDTIzMDcxNzIxNTIwMFoXDTI0MDcxNjIxNTcw\nMFowKjEPMA0GA1UECxMGY2xpZW50MRcwFQYDVQQDDA5vcmczX3N5c19vd25lcjBZ\nMBMGByqGSM49AgEGCCqGSM49AwEHA0IABBruiUm5xF8IJnG2F8cUBm2XigBMokS1\nvUjzCi4MJtOiUIZ8jzEin6YCTgoQtOguj5eWaWPXFlUmFomXUVRejMmjgeIwgd8w\nDgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFAFSwoxj6XYX\ny7epvbE/vNQcqwelMB8GA1UdIwQYMBaAFDC7nbjSSHq5btH3ZLh6lUh1nenRMH8G\nCCoDBAUGBwgBBHN7ImF0dHJzIjp7ImJsb3Nzb20ucm9sZSI6IlN5c3RlbSBPd25l\nciIsImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoib3JnM19z\neXNfb3duZXIiLCJoZi5UeXBlIjoiY2xpZW50In19MAoGCCqGSM49BAMCA0gAMEUC\nIQD33tuxLh2suH8DT9Hqd8RBYwFbIRj30p8DyWZJsKlg4wIgVUhaaockegWSHiSu\nwjCUZMfF5RyiVpTA5qx+Z9HzUoI=\n-----END CERTIFICATE-----\n",
            "Org3MSP"
    )),
    ORG3_SYSTEM_ADMIN(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICiTCCAi+gAwIBAgIUR9dYqBlAjs0oI6c2rzPNlfTPQiUwCgYIKoZIzj0EAwIw\ncTELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRAwDgYDVQQH\nEwdSYWxlaWdoMRkwFwYDVQQKExBvcmczLmV4YW1wbGUuY29tMRwwGgYDVQQDExNj\nYS5vcmczLmV4YW1wbGUuY29tMB4XDTIzMDcxNzIxNTIwMFoXDTI0MDcxNjIxNTcw\nMFowKjEPMA0GA1UECxMGY2xpZW50MRcwFQYDVQQDDA5vcmczX3N5c19hZG1pbjBZ\nMBMGByqGSM49AgEGCCqGSM49AwEHA0IABIeJbcVWqZFDzctJJjoPeYnq5YHlOiNC\nKeu2T35QB1rpLEkZiFXKSQdXvP5u9v+37Zn6NCunQAoEuBCp0eKiaTKjgeswgegw\nDgYDVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFKGgZdbbkuwm\nXuZJiqWphN20J1dFMB8GA1UdIwQYMBaAFDC7nbjSSHq5btH3ZLh6lUh1nenRMIGH\nBggqAwQFBgcIAQR7eyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJTeXN0ZW0gQWRt\naW5pc3RyYXRvciIsImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElE\nIjoib3JnM19zeXNfYWRtaW4iLCJoZi5UeXBlIjoiY2xpZW50In19MAoGCCqGSM49\nBAMCA0gAMEUCIQCpikV58pRdc8oJSuK+TH1Z2JB1IMz9v7aTsZo/zOTVcwIgQjxq\nM+sgYXv9kKFxMYSDgprjCsu79E9kTjT69ClwcM4=\n-----END CERTIFICATE-----\n",
            "Org3MSP"
    )),
    ORG3_ACQ_SPEC(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICiDCCAi+gAwIBAgIUVImRh6zypeuQuw4pA2gvsosQhZ4wCgYIKoZIzj0EAwIw\ncTELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRAwDgYDVQQH\nEwdSYWxlaWdoMRkwFwYDVQQKExBvcmczLmV4YW1wbGUuY29tMRwwGgYDVQQDExNj\nYS5vcmczLmV4YW1wbGUuY29tMB4XDTIzMDcxNzIxNTIwMFoXDTI0MDcxNjIxNTcw\nMFowKTEPMA0GA1UECxMGY2xpZW50MRYwFAYDVQQDDA1vcmczX2FjcV9zcGVjMFkw\nEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEKId3sCIq3T17n1GLokOpkCTi0qXyUyh3\nABWFqP/3ssUFNgHNLpoBzx26rOXKdWn7Ixx43E906DsOXIbgM3TjoaOB7DCB6TAO\nBgNVHQ8BAf8EBAMCB4AwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUqiaTSnRryLO2\n16uC6G0EQuy7OkowHwYDVR0jBBgwFoAUMLuduNJIerlu0fdkuHqVSHWd6dEwgYgG\nCCoDBAUGBwgBBHx7ImF0dHJzIjp7ImJsb3Nzb20ucm9sZSI6IkFjcXVpc2l0aW9u\nIFNwZWNpYWxpc3QiLCJoZi5BZmZpbGlhdGlvbiI6IiIsImhmLkVucm9sbG1lbnRJ\nRCI6Im9yZzNfYWNxX3NwZWMiLCJoZi5UeXBlIjoiY2xpZW50In19MAoGCCqGSM49\nBAMCA0cAMEQCIAxShOYmSSh+OuN08iNIiD6FtM4rAxKMtZrmAXCWwrTKAiAqeGSQ\nIlzTMgOSrEzkbvVJTkpaAEloqHXJE0iLszLXIw==\n-----END CERTIFICATE-----\n",
            "Org3MSP"
    ));

    private final byte[] bytes;
    MockIdentity(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    private static byte[] buildSerializedIdentity(String certificate, String mspid) {
        final Identities.SerializedIdentity.Builder identity = Identities.SerializedIdentity.newBuilder();
        identity.setMspid(mspid);
        final byte[] decodedCert = certificate.getBytes();//Base64.getDecoder().decode(certificate);
        identity.setIdBytes(ByteString.copyFrom(decodedCert));
        final Identities.SerializedIdentity builtIdentity = identity.build();
        return builtIdentity.toByteArray();
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException {
        MockChaincodeStub mockChaincodeStub = new MockChaincodeStub(ORG1_SYSTEM_OWNER);
        ClientIdentity clientIdentity = new ClientIdentity(mockChaincodeStub);
        MockContext c = new MockContext(ORG2_SYSTEM_ADMIN);
        System.out.println(c.getClientIdentity().getMSPID());
    }

}
