package mock;

import com.google.protobuf.ByteString;
import org.bouncycastle.operator.OperatorCreationException;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.protos.msp.Identities;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public enum MockIdentity {

    ORG1_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICejCCAiCgAwIBAgIUHxnR5ldREJ142zX9B0/lN/nGrt4wCgYIKoZIzj0EAwIw\ncDELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMQ8wDQYDVQQH\nEwZEdXJoYW0xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjMxMTAzMTgzNTAwWhcNMjQxMTAyMTg0MDAw\nWjAjMQ8wDQYDVQQLEwZjbGllbnQxEDAOBgNVBAMMB29yZzFfYW8wWTATBgcqhkjO\nPQIBBggqhkjOPQMBBwNCAAQoidJ/IYlGBnvzQcWZt/zDMiZHrMJT55ZR8zewJBQ4\nVcm98RxdosgF8iGmkEpkFV+iP/NW4P8U43RA4n5JK1Y7o4HkMIHhMA4GA1UdDwEB\n/wQEAwIHgDAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBSiDOprjEHu6l2FCchwoUHB\neFPsIjAfBgNVHSMEGDAWgBQTHXz1u7kkTfOD+G1Uuq5c/Xc66TCBgAYIKgMEBQYH\nCAEEdHsiYXR0cnMiOnsiYmxvc3NvbS5yb2xlIjoiQXV0aG9yaXppbmcgT2ZmaWNp\nYWwiLCJoZi5BZmZpbGlhdGlvbiI6IiIsImhmLkVucm9sbG1lbnRJRCI6Im9yZzFf\nYW8iLCJoZi5UeXBlIjoiY2xpZW50In19MAoGCCqGSM49BAMCA0gAMEUCIQDlQI/g\nyu+4quYQz+chlr3XkmB8g4vttFUsSOeRCfQDYAIgNk+1WHYFZ8mRmOLw8afwEugg\n8vG5Dzt0lfIhce1Q8/I=\n-----END CERTIFICATE-----\n",
            "Org1MSP"
    )),
    ORG1_NON_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgTCCAiigAwIBAgIUA7FMoX7aXdOzGSA0WtFgG+XtC0gwCgYIKoZIzj0EAwIw\ncDELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMQ8wDQYDVQQH\nEwZEdXJoYW0xGTAXBgNVBAoTEG9yZzEuZXhhbXBsZS5jb20xHDAaBgNVBAMTE2Nh\nLm9yZzEuZXhhbXBsZS5jb20wHhcNMjMxMTAzMTgzNTAwWhcNMjQxMTAyMTg0MDAw\nWjAnMQ8wDQYDVQQLEwZjbGllbnQxFDASBgNVBAMMC29yZzFfbm9uX2FvMFkwEwYH\nKoZIzj0CAQYIKoZIzj0DAQcDQgAEqRncZZ7hshtN1F04dujD++ubaVffZGL8eM0Z\nW39/f1fIuiF8J9ZJ27GgEW/r88TkYOXuDCHyHIzM1OYKpGjZKKOB6DCB5TAOBgNV\nHQ8BAf8EBAMCB4AwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUEIu8fvpeJW9hPBKj\nhgYcbe/tyNEwHwYDVR0jBBgwFoAUEx189bu5JE3zg/htVLquXP13OukwgYQGCCoD\nBAUGBwgBBHh7ImF0dHJzIjp7ImJsb3Nzb20ucm9sZSI6IlN5c3RlbSBBZG1pbmlz\ndHJhdG9yIiwiaGYuQWZmaWxpYXRpb24iOiIiLCJoZi5FbnJvbGxtZW50SUQiOiJv\ncmcxX25vbl9hbyIsImhmLlR5cGUiOiJjbGllbnQifX0wCgYIKoZIzj0EAwIDRwAw\nRAIgdgxZK2HQYaBiAcH7b65y8/hYgvkYUhQ1F1of33gkz4cCIElHqxFrjOE7rNUY\naVOFgMYLfclbu0o+MHiz5yDe01c/\n-----END CERTIFICATE-----\n",
            "Org1MSP"
    )),

    ORG2_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICdjCCAhygAwIBAgIUSocPfp5rMgFG4O7+1wu8UrN3gfAwCgYIKoZIzj0EAwIw\nbDELMAkGA1UEBhMCVUsxEjAQBgNVBAgTCUhhbXBzaGlyZTEQMA4GA1UEBxMHSHVy\nc2xleTEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eub3Jn\nMi5leGFtcGxlLmNvbTAeFw0yMzExMDMxODM1MDBaFw0yNDExMDIxODQwMDBaMCMx\nDzANBgNVBAsTBmNsaWVudDEQMA4GA1UEAwwHb3JnMl9hbzBZMBMGByqGSM49AgEG\nCCqGSM49AwEHA0IABH4bLrsVgBXuwKscu4jkKTLpubqIJAzchMke6bW/rtFKYIT8\nHXJLqa+6buwXkxEx/TEROcv5kL7f+KJh2fm5h4qjgeQwgeEwDgYDVR0PAQH/BAQD\nAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFC3Pm/FnmS7akfp5UCgXkFcVVhFR\nMB8GA1UdIwQYMBaAFDK70kPfQL3Vc4F+FqmBw72KFtOqMIGABggqAwQFBgcIAQR0\neyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJBdXRob3JpemluZyBPZmZpY2lhbCIs\nImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoib3JnMl9hbyIs\nImhmLlR5cGUiOiJjbGllbnQifX0wCgYIKoZIzj0EAwIDSAAwRQIhAMA3uTeumBRT\n4f52W/8nE3l8hyI/y+vQlSa1aVEBKsv8AiBjvy18DlZwZqzXUKlgn31WyGrMeCiQ\nXuVfp+1gGtumcQ==\n-----END CERTIFICATE-----\n",
            "Org2MSP"
    )),
    ORG2_NON_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICfTCCAiSgAwIBAgIUbS5AqXVVnSlo8y0lbNkzlckTsGUwCgYIKoZIzj0EAwIw\nbDELMAkGA1UEBhMCVUsxEjAQBgNVBAgTCUhhbXBzaGlyZTEQMA4GA1UEBxMHSHVy\nc2xleTEZMBcGA1UEChMQb3JnMi5leGFtcGxlLmNvbTEcMBoGA1UEAxMTY2Eub3Jn\nMi5leGFtcGxlLmNvbTAeFw0yMzExMDMxODM1MDBaFw0yNDExMDIxODQwMDBaMCcx\nDzANBgNVBAsTBmNsaWVudDEUMBIGA1UEAwwLb3JnMl9ub25fYW8wWTATBgcqhkjO\nPQIBBggqhkjOPQMBBwNCAAS6QfCPRfNVC03l15AbZwjlIqLTM5mwKEsA8XwnOVL7\nlfAnnY8ypPWu33FbQesyutJPpkSNJARxYLs81D1+pmo2o4HoMIHlMA4GA1UdDwEB\n/wQEAwIHgDAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBQ8VDvgDLaYDu+jJN7XAW6F\nxfnhtDAfBgNVHSMEGDAWgBQyu9JD30C91XOBfhapgcO9ihbTqjCBhAYIKgMEBQYH\nCAEEeHsiYXR0cnMiOnsiYmxvc3NvbS5yb2xlIjoiU3lzdGVtIEFkbWluaXN0cmF0\nb3IiLCJoZi5BZmZpbGlhdGlvbiI6IiIsImhmLkVucm9sbG1lbnRJRCI6Im9yZzJf\nbm9uX2FvIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNHADBEAiB4\naWuw7tdaymFXU/gtrOmdRbDT9CaJ6r1TXa1hZdX4BAIgO1ISa7Yh89kCasBnoBb+\nAi+FKXs2FVJD1ZQ4FdU87FE=\n-----END CERTIFICATE-----\n",
            "Org2MSP"
    )),

    ORG3_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICezCCAiGgAwIBAgIUJ82zL+gX5r33IUn2fu0R2Q3A+PgwCgYIKoZIzj0EAwIw\ncTELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRAwDgYDVQQH\nEwdSYWxlaWdoMRkwFwYDVQQKExBvcmczLmV4YW1wbGUuY29tMRwwGgYDVQQDExNj\nYS5vcmczLmV4YW1wbGUuY29tMB4XDTIzMTEwMzE4MzUwMFoXDTI0MTEwMjE4NDAw\nMFowIzEPMA0GA1UECxMGY2xpZW50MRAwDgYDVQQDDAdvcmczX2FvMFkwEwYHKoZI\nzj0CAQYIKoZIzj0DAQcDQgAE4xkv0YMESaNPWF0hydhxM9mBImDemjXa9XAYEZgQ\np+N5hFy3ylqAyaXvNDeM1+XHF+iytKVm/FX4EQv+v0toFaOB5DCB4TAOBgNVHQ8B\nAf8EBAMCB4AwDAYDVR0TAQH/BAIwADAdBgNVHQ4EFgQUVR41aKUT9AOka6pp6kEQ\n7jAVTeMwHwYDVR0jBBgwFoAUsWfWu/x5B/ZBZ0ciVW3Ld/E2sn8wgYAGCCoDBAUG\nBwgBBHR7ImF0dHJzIjp7ImJsb3Nzb20ucm9sZSI6IkF1dGhvcml6aW5nIE9mZmlj\naWFsIiwiaGYuQWZmaWxpYXRpb24iOiIiLCJoZi5FbnJvbGxtZW50SUQiOiJvcmcz\nX2FvIiwiaGYuVHlwZSI6ImNsaWVudCJ9fTAKBggqhkjOPQQDAgNIADBFAiEAlPWH\n4EiOe4DWHZp/nSwf5BJjQquyCBFXZ/0c9WmPWIkCIGBKOrRH4SsWc4CMrEQr4rCI\nkpPC4pVWIzwaR0ofxdmE\n-----END CERTIFICATE-----\n",
            "Org3MSP"
    )),
    ORG3_NON_AO(buildSerializedIdentity(
            "-----BEGIN CERTIFICATE-----\nMIICgjCCAimgAwIBAgIUDwhQ361PAG2n0fhzXmO3hbT36k4wCgYIKoZIzj0EAwIw\ncTELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRAwDgYDVQQH\nEwdSYWxlaWdoMRkwFwYDVQQKExBvcmczLmV4YW1wbGUuY29tMRwwGgYDVQQDExNj\nYS5vcmczLmV4YW1wbGUuY29tMB4XDTIzMTEwMzE4MzUwMFoXDTI0MTEwMjE4NDAw\nMFowJzEPMA0GA1UECxMGY2xpZW50MRQwEgYDVQQDDAtvcmczX25vbl9hbzBZMBMG\nByqGSM49AgEGCCqGSM49AwEHA0IABODi4aYWF7C8hX5IN39MC3+Qpvzosq3IAo+K\n6SRfpDJXuvcBdkteXgsRkiXcMsZfj8urgaoWa8dLe8CklHISiKujgegwgeUwDgYD\nVR0PAQH/BAQDAgeAMAwGA1UdEwEB/wQCMAAwHQYDVR0OBBYEFAahtyvw/xqTmr7g\nNYY6aR0lNsdvMB8GA1UdIwQYMBaAFLFn1rv8eQf2QWdHIlVty3fxNrJ/MIGEBggq\nAwQFBgcIAQR4eyJhdHRycyI6eyJibG9zc29tLnJvbGUiOiJTeXN0ZW0gQWRtaW5p\nc3RyYXRvciIsImhmLkFmZmlsaWF0aW9uIjoiIiwiaGYuRW5yb2xsbWVudElEIjoi\nb3JnM19ub25fYW8iLCJoZi5UeXBlIjoiY2xpZW50In19MAoGCCqGSM49BAMCA0cA\nMEQCIEdYIFW7G8kZAks8ZqSRq4d6kqSCGfqCwptmllyooX36AiBx+rNqGvirsccx\nAAtfiGQ9wfXjgl+AtxTqfBlorjuZLQ==\n-----END CERTIFICATE-----\n",
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
        MockChaincodeStub mockChaincodeStub = new MockChaincodeStub(ORG1_AO);
        ClientIdentity clientIdentity = new ClientIdentity(mockChaincodeStub);
        MockContext c = new MockContext(ORG2_NON_AO);
        System.out.println(c.getClientIdentity().getMSPID());
    }

}
