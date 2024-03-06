/*
package contract;

import contract.request.order.InitiateOrderRequest;
import mock.MockContext;
import mock.MockIdentity;
import model.Order;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;


import static model.DateFormatter.DATE_TIME_FORMATTER;
import static mock.MockContextUtil.newTestContext;
import static mock.MockContextUtil.newTestContextWithAsset;
import static org.junit.jupiter.api.Assertions.*;

class OrderContractTest {

    OrderContract contract = new OrderContract();

    @Nested
    class InitiateOrderTest {

        @Test
        void test_RenewalOfNonCompletedOrder_Throws_ChaincodeException() {

        }

        @Test
        void test_AssetDoesNotExist_Throws_ChaincodeException() {
            MockContext ctx = newTestContext(MockIdentity.ORG2_TPOC);
            ctx.setTransientData(new InitiateOrderRequest(null, "123", 2, 2));
            ChaincodeException e = assertThrows(ChaincodeException.class, () -> contract.InitiateOrder(ctx));
            assertEquals("", e.getMessage());
        }

        @Test
        void test_Unauthorized() {

        }


    }

    @Nested
    class ApproveOrderTest {

        @Test
        void test_OrderDoesNotExist_Throws_ChaincodeException() {

        }

        @Test
        void test_Unauthorized() {

        }

    }

    @Nested
    class CompleteOrderTest {
        @Test
        void test_OrderDoesNotExist_Throws_ChaincodeException() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

    @Nested
    class DeleteOrderTest {
        @Test
        void test_OrderDoesNotExist_Throws_ChaincodeException() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

    @Nested
    class GetOrderTest {
        @Test
        void test_OrderDoesNotExist_Throws_ChaincodeException() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

    @Nested
    class GetLicensesTest {
        @Test
        void test_Success() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

    @Nested
    class ReturnLicensesTest {
        @Test
        void test_LicenseDoesNotExist_Throws_ChaincodeException() {

        }

        @Test
        void test_NotAllocatedToAccount_Throws_ChaincodeException() {

        }
        @Test
        void test_Success() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

    @Nested
    class GetOrdersWithExpiredLicensesTest {
        @Test
        void test_Success() {

        }

        @Test
        void test_Unauthorized() {

        }
    }

}*/
