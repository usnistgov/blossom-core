/*
package contract;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import contract.request.asset.AddAssetRequest;
import contract.request.asset.AssetIdRequest;
import contract.request.asset.LicensesRequest;
import contract.request.asset.UpdateEndDateRequest;
import contract.response.AssetDetailResponse;
import contract.response.AssetResponse;
import mock.MockContext;
import mock.MockContextUtil;
import mock.MockIdentity;
import model.Allocated;
import model.License;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static contract.AssetContract.ADMINMSP_IPDC;
import static contract.AssetContract.licenseKey;
import static contract.TestInstance.toTestInstance;
import static mock.MockContextUtil.newTestContext;
import static mock.MockContextUtil.newTestContextWithAsset;
import static org.junit.jupiter.api.Assertions.*;

class AssetContractTest {

    public static void allocateLicense(MockContext ctx, String assetId, String licenseId, String account) {
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, licenseKey(assetId, licenseId), new License(
                licenseId,
                new Allocated(
                        account,
                        "",
                        "",
                        ""
                )
        ).toByteArray());

    }

    private AssetContract contract = new AssetContract();

    @Test
    void test_Success() throws IOException {
        TestRunner.runTest("asset_contract_test.json");
    }

    @Nested
    class AddAssetTest {

        @Test
        void testInvalidDateFormat_Throws_ChaincodeException () {
            MockContext ctx = newTestContext(MockIdentity.ORG1_SO);

            assertThrows(ChaincodeException.class, () -> {
                ctx.setTransientData(new AddAssetRequest("asset1", "01/01/2000", Set.of()));
                contract.AddAsset(ctx);
            });
            assertThrows(ChaincodeException.class, () -> {
                ctx.setTransientData(new AddAssetRequest("asset1", "", Set.of()));
                contract.AddAsset(ctx);
            });
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContext(MockIdentity.ORG2_SO);

            ctx.setTxId("123");
            ctx.setTransientData(new AddAssetRequest("asset1", "2000-01-01", Set.of("l1", "l2")));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.AddAsset(ctx)
            );
            assertEquals(
                    "",
                    e.getMessage()
            );
        }
    }

    @Nested
    class AddLicensesTest {

        @Test
        void test_LicenseAlreadyExists() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTransientData(new LicensesRequest("123", Set.of("l1")));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.AddLicenses(ctx)
            );
            assertEquals(
                    "license l1 already exists",
                    e.getMessage()
            );
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTimestamp(Instant.now());
            ctx.setTransientData(new AddAssetRequest("123", "2000-01-01", Set.of("l1", "l2")));
            String id = contract.AddAsset(ctx);

            ctx.setClientIdentity(MockIdentity.ORG2_SO);
            ctx.setTransientData(new LicensesRequest("123", Set.of("l1", "l2")));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.AddLicenses(ctx)
            );
            assertEquals(
                    "",
                    e.getMessage()
            );
        }

    }

    @Nested
    class RemoveLicensesTest {

        @Test
        void test_LicenseDoesNotExist() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTransientData(new LicensesRequest("123", Set.of("l1")));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.RemoveLicenses(ctx));
            assertEquals("", e.getMessage());
        }

        @Test
        void test_LicenseIsAllocated() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTransientData(new LicensesRequest("123", Set.of("l1")));

            allocateLicense(ctx, "123", "l1", "Org2MSP");

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.RemoveLicenses(ctx));
            assertEquals("license l1 is allocated to Org2MSP", e.getMessage());
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTxId("123");
            ctx.setTransientData(new LicensesRequest("123", Set.of("l1")));

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.RemoveLicenses(ctx));
            assertEquals("", e.getMessage());
        }
    }

    @Nested
    class UpdateEndDateTest {

        @Test
        void test_InvalidDateFormat_Throws_ChaincodeException() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTransientData(new UpdateEndDateRequest("123", "2000-01-01T23:22:44.249975Z"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.UpdateEndDate(ctx)
            );
            assertEquals("expected format YYYY-MM-DD, received 2000-01-01T23:22:44.249975Z", e.getMessage());
        }

        @Test
        void test_AssetDoesNotExist_Throws_ChaincodeException() {
            MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
            ctx.setTransientData(new UpdateEndDateRequest("123", "2000-01-01"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.UpdateEndDate(ctx)
            );
            assertEquals("asset with id 123 does not exist", e.getMessage());
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTransientData(new UpdateEndDateRequest("123", "2000-01-01"));
            ChaincodeException e = assertThrows(
                    ChaincodeException.class,
                    () -> contract.UpdateEndDate(ctx)
            );
            assertEquals("", e.getMessage());
        }

    }

    @Nested
    class RemoveAssetTest {

        @Test
        void test_AssetHasAllocatedLicenses_Throws_ChaincodeException() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            allocateLicense(ctx, "123", "l1", "Org2MSP");
            ctx.setTransientData(new AssetIdRequest("123"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.RemoveAsset(ctx));
            assertEquals("cannot remove asset 123 as it has allocated licenses", e.getMessage());
        }

        @Test
        void test_AssetDoesNotExist_Throws_ChaincodeException() {
            MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
            ctx.setTransientData(new AssetIdRequest("123"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.RemoveAsset(ctx));
            assertEquals("asset with id 123 does not exist", e.getMessage());
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG2_SO);
            ctx.setTransientData(new AssetIdRequest("123"));
            assertThrows(ChaincodeException.class, () ->contract.RemoveAsset(ctx));
        }
    }

    @Nested
    class GetAssetsTest {

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG2_SO);

            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.GetAssets(ctx));
            assertEquals("", e.getMessage());
        }
    }

    @Nested
    class GetAssetTest {
        
        @Test
        void test_AssetDoesNotExist_Throws_ChaincodeException() {
            MockContext ctx = newTestContext(MockIdentity.ORG1_SO);
            ctx.setTransientData(new AssetIdRequest("123"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.GetAsset(ctx));
            assertEquals("", e.getMessage());
        }

        @Test
        void test_Unauthorized() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTransientData(new AssetIdRequest("123"));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.GetAsset(ctx));
            assertEquals("", e.getMessage());
        }

        @Test
        void test_PublicFieldsOnly() {
            MockContext ctx = newTestContextWithAsset(MockIdentity.ORG1_SO);
            ctx.setTransientData(new AssetIdRequest("123"));
            AssetResponse assetResponse = contract.GetAsset(ctx);
            assertFalse(assetResponse instanceof AssetDetailResponse);

            assertEquals(
                    new AssetResponse(
                            "123",
                            "asset1",
                            2,
                            "2000-01-01",
                            "2000-01-01"
                    ),
                    assetResponse
            );
        }
    }
}*/
