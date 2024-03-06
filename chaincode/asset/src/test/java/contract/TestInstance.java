/*
package contract;

import com.google.gson.internal.LinkedTreeMap;
import contract.request.asset.AddAssetRequest;
import contract.request.asset.AssetIdRequest;
import contract.request.asset.LicensesRequest;
import contract.request.asset.UpdateEndDateRequest;
import contract.response.AssetDetailResponse;
import contract.response.AssetResponse;
import mock.MockContext;
import mock.MockIdentity;
import model.Allocated;
import model.License;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static model.DateFormatter.DATE_TIME_FORMATTER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestInstance {

    public static TestInstance toTestInstance(LinkedTreeMap<String, Object> map) {
        String name = (String) map.get("name");
        String user = (String) map.get("user");
        String channel = (String) map.get("channel");
        String chaincode = (String) map.get("chaincode");
        String contract = (String) map.get("contract");
        String ts = (String) map.get("ts");
        String function = (String) map.get("function");
        List params = (List) map.get("params");
        Map<String, Object> transientData = (Map<String, Object>) map.get("transientData");
        List<String> endorsingOrgs = (List) map.get("endorsingOrgs");
        Object expectedOutput = map.get("expectedOutput");
        String expectedErrorMessage = (String) map.get("expectedErrorMessage");

        List<TestInstance> setup = new ArrayList<>();
        List l = (List)map.get("setup");
        if (l != null) {
            for (Object o : l) {
                setup.add(toTestInstance((LinkedTreeMap<String, Object>) o));
            }
        }

        List<TestInstance> check = new ArrayList<>();
        l = (List)map.get("check");
        if (l != null) {
            for (Object o : l) {
                check.add(toTestInstance((LinkedTreeMap<String, Object>) o));
            }
        }

        return new TestInstance(
                setup,
                name,
                user,
                channel,
                chaincode,
                contract,
                ts,
                function,
                params,
                transientData,
                endorsingOrgs,
                expectedOutput,
                expectedErrorMessage,
                check
        );
    }

    private List<TestInstance> setup;
    private String name;
    private String user;
    private String channel;
    private String chaincode;
    private String contract;
    private String ts;
    private String function;
    private List params;
    private Map<String, Object> transientData;
    private List<String> endorsingOrgs;
    private Object expectedOutput;
    private String expectedErrorMessage;
    private List<TestInstance> check;

    public TestInstance(List<TestInstance> setup, String name, String user, String channel, String chaincode,
                        String contract, String ts, String function, List params, Map<String, Object> transientData,
                        List<String> endorsingOrgs, Object expectedOutput, String expectedErrorMessage,
                        List<TestInstance> check) {
        this.setup = setup;
        this.name = name;
        this.user = user;
        this.channel = channel;
        this.chaincode = chaincode;
        this.contract = contract;
        this.ts = ts;
        this.function = function;
        this.params = params;
        this.transientData = transientData;
        this.endorsingOrgs = endorsingOrgs;
        this.expectedOutput = expectedOutput;
        this.expectedErrorMessage = expectedErrorMessage;
        this.check = check;
    }

    @Override
    public String toString() {
        return "TestInstance{" +
                "name='" + name + '\'' +
                ", channel='" + channel + '\'' +
                ", chaincode='" + chaincode + '\'' +
                ", contract='" + contract + '\'' +
                ", ts='" + ts + '\'' +
                ", function='" + function + '\'' +
                ", params=" + params +
                ", transientData=" + transientData +
                ", endorsingOrgs=" + endorsingOrgs +
                ", expectedOutput=" + expectedOutput +
                ", expectedErrorMessage='" + expectedErrorMessage + '\'' +
                '}';
    }

    public void run(MockContext ctx) {
        ctx.setClientIdentity(getClientIdentity());

        try {
            // do setup
            for (TestInstance t : setup) {
                t.run(ctx);
            }

            Object actual = invoke(ctx);
            Object expected = expectedToObject();

            if (actual instanceof List && expected instanceof List) {
                List actualList = (List) actual;
                List expectedList = (List) expected;
                assertTrue(actualList.containsAll(expectedList));
            } else {
                assertEquals(expected, actual, name);
            }

            // do checks
            for (TestInstance t : check) {
                t.run(ctx);
            }
        } catch (Exception e) {
            if (expectedErrorMessage == null || expectedErrorMessage.isEmpty()) {
                throw e;
            }

            assertEquals(expectedErrorMessage, e.getMessage());
        }
    }

    private Object expectedToObject() {
        switch (function) {
            case "AddAsset": {
                return expectedOutput;
            }
            case "AddLicenses": {
                return null;
            }
            case "RemoveLicenses": {
                return null;
            }
            case "UpdateEndDate": {
                return null;
            }
            case "RemoveAsset": {
                return null;
            }
            case "GetAssets": {
                List<AssetResponse> response = new ArrayList<>();
                List list = (List) expectedOutput;
                for (Object o : list) {
                    Map map = (Map)o;
                    response.add(mapToAssetResponse(map));
                }

                return response;
            }
            case "GetAsset": {
                Map map = (Map)expectedOutput;
                return mapToAssetResponse(map);
            }
            default: {
                return null;
            }
        }
    }

    private AssetResponse mapToAssetResponse(Map m) {
        if (m.containsKey("totalAmount")) {
            return new AssetDetailResponse(
                    (String)m.get("id"),
                    (String)m.get("name"),
                    (int)((double)m.get("numAvailable")),
                    (String)m.get("startDate"),
                    (String)m.get("endDate"),
                    (int)((double)m.get("totalAmount")),
                    new HashSet<>((List)m.get("availableLicenses")),
                    mapToAllocatedLicenses((Map) m.get("allocatedLicenses"))
            );
        } else {
            return new AssetResponse(
                    (String)m.get("id"),
                    (String)m.get("name"),
                    (int)((double)m.get("numAvailable")),
                    (String)m.get("startDate"),
                    (String)m.get("endDate")
            );
        }
    }

    private Map<String, Set<License>> mapToAllocatedLicenses(Map<String, Object> allocatedLicenses) {
        Map<String, Set<License>> allocated = new HashMap<>();
        for (Map.Entry<String, Object> e : allocatedLicenses.entrySet()) {
            List orgLicenses = (List) e.getValue();

            Set<License> licenses = new HashSet<>();
            for (Object licenseObj : orgLicenses) {
                Map map = (Map) licenseObj;

                String id = (String) map.get("id");
                Map allocatedMap = (Map) map.get("allocated");
                License license = new License(
                        id,
                        new Allocated(
                                (String) allocatedMap.get("account"),
                                (String) allocatedMap.get("expiration"),
                                (String) allocatedMap.get("orderId"),
                                (String) allocatedMap.get("swid")
                        )
                );

                licenses.add(license);
            }

            allocated.put(e.getKey(), licenses);
        }

        return allocated;
    }

    private Object invoke(MockContext ctx) {
        AssetContract assetContract = new AssetContract();
        OrderContract orderContract = new OrderContract();
        SWIDContract swidContract = new SWIDContract();

        ctx.setTxId(this.name);
        ctx.setTimestamp(tsToInstant());

        switch (function) {
            // asset
            case "AddAsset": {
                Object name = transientData.get("name");
                Object endDate = transientData.get("endDate");
                Object licenses = transientData.get("licenses");
                ctx.setTransientData(new AddAssetRequest(
                        name != null ? (String) name : null,
                        endDate != null ? (String) endDate : null,
                        licenses != null ? new HashSet<>((List<String>) licenses) : null
                ));
                return assetContract.AddAsset(ctx);
            }
            case "AddLicenses": {
                Object assetId = transientData.get("assetId");
                Object licenses = transientData.get("licenses");
                ctx.setTransientData(new LicensesRequest(
                        assetId != null ? (String) assetId : null,
                        licenses != null ? new HashSet<>((List<String>) licenses) : null
                ));
                assetContract.AddLicenses(ctx);
                return null;
            }
            case "RemoveLicenses": {
                Object assetId = transientData.get("assetId");
                Object licenses = transientData.get("licenses");
                ctx.setTransientData(new LicensesRequest(
                        assetId != null ? (String) assetId : null,
                        licenses != null ? new HashSet<>((List<String>) licenses) : null
                ));
                assetContract.RemoveLicenses(ctx);
                return null;
            }
            case "UpdateEndDate": {
                Object assetId = transientData.get("assetId");
                Object newEndDate = transientData.get("newEndDate");
                ctx.setTransientData(new UpdateEndDateRequest(
                        assetId != null ? (String) assetId : null,
                        newEndDate != null ? (String) newEndDate : null
                ));
                assetContract.UpdateEndDate(ctx);
                return null;
            }
            case "RemoveAsset": {
                Object assetId = transientData.get("assetId");
                ctx.setTransientData(new AssetIdRequest(
                        assetId != null ? (String) assetId : null
                ));
                assetContract.RemoveAsset(ctx);
                return null;
            }
            case "GetAssets": {
                return List.of(assetContract.GetAssets(ctx));
            }
            case "GetAsset": {
                Object assetId = transientData.get("assetId");
                ctx.setTransientData(new AssetIdRequest(
                        assetId != null ? (String) assetId : null
                ));
                return assetContract.GetAsset(ctx);
            }
            // order
            case "InitiateOrder": {
                return orderContract.InitiateOrder(ctx);
            }
            case "ApproveOrder": {
                orderContract.ApproveOrder(ctx);
                return null;
            }
            case "DenyOrder": {
                orderContract.DenyOrder(ctx);
                return null;
            }
            case "CompleteOrder": {
                orderContract.CompleteOrder(ctx);
                return null;
            }
            case "DeleteOrder": {
                orderContract.DeleteOrder(ctx);
                return null;
            }
            case "ReturnLicenses": {
                orderContract.ReturnLicenses(ctx);
                return null;
            }
            case "GetOrder": {
                return orderContract.GetOrder(ctx);
            }
            case "GetOrders": {
                return orderContract.GetOrders(ctx);
            }
            case "GetAssetLicenses": {
                return orderContract.GetAssetLicenses(ctx);
            }
            case "GetOrdersWithExpiredLicenses": {
                return orderContract.GetOrdersWithExpiredLicenses(ctx);
            }
            // swid
            case "ReportSWID": {
                swidContract.ReportSWID(ctx);
                return null;
            }
            case "DeleteSWID": {
                swidContract.DeleteSWID(ctx);
                return null;
            }
            case "GetSwID": {
                return swidContract.GetSWID(ctx);
            }
            case "GetSWIDsAssociatedWithAsset": {
                return swidContract.GetSWIDsAssociatedWithAsset(ctx);
            }
            default: {
                return null;
            }
        }
    }

    private Instant tsToInstant() {
        LocalDate localDate = LocalDate.parse(ts, DATE_TIME_FORMATTER);
        return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private MockIdentity getClientIdentity() {
        switch (user) {
            case "org1_ACQ": {
                return MockIdentity.ORG1_ACQ;
            }
            case "org1_TPOC": {
                return MockIdentity.ORG1_TPOC;
            }
            case "org1_SO": {
                return MockIdentity.ORG1_SO;
            }
            case "org1_SA": {
                return MockIdentity.ORG1_SA;
            }
            case "org2_ACQ": {
                return MockIdentity.ORG2_ACQ;
            }
            case "org2_TPOC": {
                return MockIdentity.ORG2_TPOC;
            }
            case "org2_SO": {
                return MockIdentity.ORG2_SO;
            }
            case "org2_SA": {
                return MockIdentity.ORG2_SA;
            }
            case "org3_ACQ": {
                return MockIdentity.ORG3_ACQ;
            }
            case "org3_TPOC": {
                return MockIdentity.ORG3_TPOC;
            }
            case "org3_SO": {
                return MockIdentity.ORG3_SO;
            }
            case "org3_SA": {
                return MockIdentity.ORG3_SA;
            }
        }

        return null;
    }
}
*/
