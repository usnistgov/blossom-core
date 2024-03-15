package contract;

import contract.request.*;
import contract.response.AllocateLicensesResponse;
import contract.response.IdResponse;
import model.*;
import ngac.PDP;
import ngac.UnauthorizedException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static contract.AssetContract.*;
import static contract.SWIDContract.swidKey;
import static model.DateFormatter.DATE_TIME_FORMATTER;
import static java.time.ZoneOffset.UTC;
import static contract.request.LicensesRequest.allocateRequestKey;
import static model.DateFormatter.isExpired;
import static model.Order.Status.*;
import static model.SHA256.EMPTY_HASH_BYTES;

@Contract(
        name = "order",
        info = @Info(
                title = "Blossom asset chaincode order contract",
                description = "Chaincode functions to manage orders and licenses",
                version = "0.0.1"
        )
)
public class OrderContract implements ContractInterface {

    public static String ORDER_PREFIX = "order:";

    public String orderKey(String account, String orderId) {
        return ORDER_PREFIX + account + orderId;
    }

    private AssetContract assetContract = new AssetContract();

    @Transaction
    public IdResponse GetQuote(Context ctx) {
        QuoteRequest req = new QuoteRequest(ctx, true);

        PDP.canInitiateOrder(ctx, req.getAccount());

        String account = ctx.getClientIdentity().getMSPID();
        String orderId = req.getOrderId();

        Order order;
        if (orderId != null) {
            // renewal order
            order = getOrder(ctx, req.getOrderId(), account);
            order.setStatus(RENEWAL_QUOTE_REQUESTED);
        } else {
            // new order
            orderId = ctx.getStub().getTxId();
            order = new Order(orderId, account,
                              QUOTE_REQUESTED,
                              "",
                              "",
                              "",
                              "",
                              req.getAssetId(),
                              req.getAmount(),
                              req.getDuration(),
                              0,
                              null,
                              null
            );
        }

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return new IdResponse(orderId);
    }

    @Transaction
    public IdResponse SendQuote(Context ctx) {
        QuoteRequest req = new QuoteRequest(ctx, false);

        PDP.canAllocateLicense(ctx);

        String orderId = req.getOrderId();
        Order order = getOrder(ctx, orderId, req.getAccount());
        order.setPrice(req.getPrice());

        if (order.getStatus().equals(QUOTE_REQUESTED)) {
            order.setStatus(QUOTE_RECEIVED);
        } else if (order.getStatus().equals(RENEWAL_QUOTE_REQUESTED)) {
            order.setStatus(RENEWAL_QUOTE_RECEIVED);
        } else {
            throw new ChaincodeException("a quote request for order " + orderId + " does not exist");
        }

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return new IdResponse(orderId);
    }

    @Transaction
    public IdResponse InitiateOrder(Context ctx) {
        OrderIdAndAccountRequest request = new OrderIdAndAccountRequest(ctx);

        PDP.canInitiateOrder(ctx, request.getAccount());

        String account = request.getAccount();
        Order order = getOrder(ctx, request.getOrderId(), account);

        // an order can only be initiated if a quote has been received
        if (!(order.getStatus().equals(RENEWAL_QUOTE_RECEIVED) || order.getStatus().equals(QUOTE_RECEIVED))) {
            throw new ChaincodeException("order " + order.getId() + " has not received a quote");
        }

        if (order.getStatus().equals(RENEWAL_QUOTE_RECEIVED)) {
            order.setStatus(RENEWAL_INITIATED);
        } else {
            order.setInitiationDate(DATE_TIME_FORMATTER.format(ctx.getStub().getTxTimestamp()));
            order.setStatus(INITIATED);
        }

        // write order to the ADMIN IPDC
        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return new IdResponse(order.getId());
    }

    @Transaction
    public IdResponse ApproveOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canApproveOrder(ctx, req.getAccount());

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        if (order.getStatus().equals(RENEWAL_INITIATED)) {
            order.setStatus(RENEWAL_APPROVED);
        } else if (order.getStatus().equals(INITIATED)) {
            order.setStatus(APPROVED);
        } else {
            throw new ChaincodeException("order " + order.getId() + " is not up for approval");
        }

        order.setApprovalDate(DateFormatter.getFormattedTimestamp(ctx));

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return new IdResponse(order.getId());
    }

    @Transaction
    public void DenyOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canDenyOrder(ctx, req.getAccount());

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        if (order.getStatus().equals(RENEWAL_INITIATED)) {
            order.setStatus(RENEWAL_DENIED);
        } else if (order.getStatus().equals(INITIATED)) {
            order.setStatus(DENIED);
        } else {
            throw new ChaincodeException("order " + order.getId() + " is not up for approval");
        }

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());
    }

    @Transaction
    public AllocateLicensesResponse GetLicensesToAllocateForOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canAllocateLicense(ctx);

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());
        Order.Status status = order.getStatus();
        if (status.equals(RENEWAL_APPROVED)) {
            throw new ChaincodeException("cannot get licenses to allocate for an order that is being renewed");
        } else if (!status.equals(APPROVED)) {
            throw new ChaincodeException("cannot get licenses to allocate for an order that has not been approved");
        }

        List<License> availableLicenses = new AssetContract().getAvailableLicenses(ctx, order.getAssetId());
        if (availableLicenses.size() < order.getAmount()) {
            throw new ChaincodeException("not enough available licenses to complete order " + order.getId());
        }

        List<String> licensesToAllocate = new ArrayList<>();
        for (int i = 0; i < order.getAmount(); i++) {
            licensesToAllocate.add(availableLicenses.get(i).getId());
        }

        return new AllocateLicensesResponse(req.getOrderId(), req.getAccount(), licensesToAllocate);
    }

    @Transaction
    public LicensesRequest AllocateLicenses(Context ctx) {
        AllocateLicensesRequest req = new AllocateLicensesRequest(ctx);

        PDP.canAllocateLicense(ctx);
        
        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        // check that the order is ready to be allocated
        Order.Status status = order.getStatus();
        if (!(status.equals(RENEWAL_APPROVED) || status.equals(APPROVED))) {
            throw new ChaincodeException("cannot allocate licenses for an order that has not been approved");
        }

        // compute expiration date
        int duration = order.getDuration();
        String expiration = DATE_TIME_FORMATTER.format(
                ctx.getStub()
                   .getTxTimestamp()
                   .atZone(ZoneId.from(UTC))
                   .plusYears(duration)
                   .toInstant()
        );

        // update order to reflect allocation
        // mark order as completed and set date, expiration, and licenses
        String ts = DateFormatter.getFormattedTimestamp(ctx);
        order.setAllocatedDate(ts);
        order.setLatestRenewalDate(ts);
        order.setStatus(Order.Status.ALLOCATED);
        order.setExpiration(expiration);

        // allocate new licenses or update expiration for renewal
        // if renewal the licenses will be in the Order object not in the request
        if (status.equals(RENEWAL_APPROVED)) {
             allocateRenewalLicenses(ctx, order);
        } else {
             allocateLicenses(ctx, order, req.getLicenses());
             order.setLicenses(req.getLicenses());
        }

        // update order
        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                orderKey(order.getAccount(), order.getId()),
                order.toByteArray()
        );

        // create allocate request in ADMINMSP
        LicensesRequest licensesRequest = new LicensesRequest(order.getAccount(), order.getAssetId(), order.getId(), order.getExpiration(), order.getLicenses());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.ALLOCATE, order.getId()), licensesRequest.toByteArray());

        // return the licenses request as that will be the input to the send licenses method
        return licensesRequest;
    }

    @Transaction
    public LicensesRequest GetAllocateRequestForOrder(Context ctx) {
        OrderIdRequest req = new OrderIdRequest(ctx);

        PDP.canAllocateLicense(ctx);

        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.ALLOCATE, req.getOrderId()));
        if (bytes.length == 0) {
            throw new ChaincodeException("no allocate request exists for order " + req.getOrderId());
        }

        return LicensesRequest.fromByteArray(bytes);
    }

    @Transaction
    public void SendLicenses(Context ctx) {
        LicensesRequest req = new LicensesRequest(ctx);

        PDP.canAllocateLicense(ctx);
        
        // check hash vs private data on adminmsp ipdc to verify the licenses being sent were the ones allocated
        byte[] pvtDataHash = ctx.getStub().getPrivateDataHash(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.ALLOCATE, req.getOrderId()));
        byte[] hash = SHA256.hashBytesToBytes(req.toByteArray());
        if (!Arrays.equals(pvtDataHash, hash)) {
            throw new ChaincodeException("provided licenses to send do not match the licenses allocated");
        }

        // delete request
        ctx.getStub().delPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.ALLOCATE, req.getOrderId()));

        // put in ORG
        String collection = accountIPDC(req.getAccount());
        for (String licenseId : req.getLicenses()) {
            LicenseKey lk = new LicenseKey(req.getAssetId(), licenseId, "");
            Allocated allocated = new Allocated(licenseId, req.getAccount(), req.getExpiration(), req.getOrderId());
            ctx.getStub().putPrivateData(collection, lk.toKey(), allocated.toByteArray());
        }
    }

    @Transaction
    public void InitiateReturn(Context ctx) {
        LicensesRequest req = new LicensesRequest(ctx);

        PDP.canReturnLicense(ctx, req.getAccount());

        // TODO NGAC

        // check that a request for this order is not already active
        String key = allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, req.getOrderId());
        byte[] hash = ctx.getStub().getPrivateDataHash(accountIPDC(req.getAccount()), key);
        if (Arrays.equals(hash, EMPTY_HASH_BYTES)) {
            throw new ChaincodeException("a request to return licenses for order " + req.getOrderId() + " is already active");
        }

        // check that each licenses exists in ORG using hash
        for (String licenseId : req.getLicenses()) {
            LicenseKey lk = new LicenseKey(req.getAssetId(), licenseId, "");
            hash = ctx.getStub().getPrivateDataHash(accountIPDC(req.getAccount()), lk.toKey());
            if (Arrays.equals(hash, EMPTY_HASH_BYTES)) {
                // it's ok to provide a specific error because only users of the org will get here
                throw new ChaincodeException("license " + licenseId + " is not checked out by " + req.getAccount());
            }
        }

        // submit deallocate request to ADMINMSP,
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, req.toByteArray());
    }

    @Transaction
    public LicensesRequest GetInitiatedReturnForOrder(Context ctx) {
        OrderIdRequest req = new OrderIdRequest(ctx);

        PDP.canAllocateLicense(ctx);

        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, req.getOrderId()));
        if (bytes.length == 0) {
            throw new ChaincodeException("no deallocate request exists for order " + req.getOrderId());
        }

        return LicensesRequest.fromByteArray(bytes);
    }

    @Transaction
    public void DeallocateLicensesFromAccount(Context ctx) throws Exception {
        LicensesRequest req = new LicensesRequest(ctx);

        PDP.canAllocateLicense(ctx);

        // check deallocate request exists and equals the one provided
        String key = allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, req.getOrderId());
        byte[] requestHash = ctx.getStub().getPrivateDataHash(ADMINMSP_IPDC, key);
        byte[] paramsHash = SHA256.hashBytesToBytes(req.toByteArray());
        if (!Arrays.equals(requestHash, paramsHash)) {
            throw new ChaincodeException("provided deallocation request does not match the one initiated");
        }

        // delete licenses from ORG
        String collection = accountIPDC(req.getAccount());
        for (String license : req.getLicenses()) {
            LicenseKey lk = new LicenseKey(req.getOrderId(), license, "");

            // delete license from ORG
            ctx.getStub().delPrivateData(collection, lk.toKey());
        }
    }

    @Transaction
    public void DeallocateLicensesFromSP(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canAllocateLicense(ctx);

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        // check request exists
        String key = allocateRequestKey(LicensesRequest.ACTION.DEALLOCATE, req.getOrderId());
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, key);
        if (bytes.length == 0) {
            throw new ChaincodeException("a deallocation request does not exist for order " + req.getOrderId());
        }

        LicensesRequest licensesRequest = LicensesRequest.fromByteArray(bytes);

        // update each license to remove allocated and track on ledger
        for (String licenseId : licensesRequest.getLicenses()) {
            LicenseKey lk = new LicenseKey(order.getAssetId(), licenseId, "");
            bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, lk.toKey());
            if (bytes.length == 0) {
                throw new ChaincodeException("license " + lk.getLicenseId() + " does not exist");
            }

            License license = License.fromByteArray(bytes);
            license.setAllocated(null);

            // write to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, lk.toKey(), license.toByteArray());

            // write to ledger to track tx
            ctx.getStub().putState(lk.toKey(), new byte[]{0});
        }

        // update the order
        order.getLicenses().removeAll(licensesRequest.getLicenses());
        order.setAmount(order.getAmount() - licensesRequest.getLicenses().size());
        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                orderKey(order.getAccount(), order.getId()),
                order.toByteArray()
        );

        // delete request
        ctx.getStub().delPrivateData(ADMINMSP_IPDC, key);
    }

    @Transaction
    public Order GetOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canReadOrder(ctx, req.getAccount());

        return getOrder(ctx, req.getOrderId(), req.getAccount());
    }

    @Transaction
    public Order[] GetOrdersByAccount(Context ctx) {
        AccountRequest req = new AccountRequest(ctx);

        PDP.canReadOrder(ctx, req.getAccount());

        String key = orderKey(req.getAccount(), "");
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~")) {
            List<Order> orders = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                Order order = Order.fromByteArray(value);
                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    @Transaction
    public Order[] GetOrdersByAccountAndAsset(Context ctx) {
        AssetIdAndAccountRequest req = new AssetIdAndAccountRequest(ctx);

        PDP.canReadOrder(ctx, req.getAccount());

        String key = orderKey(req.getAccount(), "");
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~")) {
            List<Order> orders = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                Order order = Order.fromByteArray(value);
                if (!order.getAssetId().equals(req.getAssetId())) {
                    continue;
                }

                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    @Transaction
    public Order[] GetOrdersByAsset(Context ctx) {
        AssetIdRequest req = new AssetIdRequest(ctx);

        String key = ORDER_PREFIX;
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~")) {
            List<Order> orders = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                Order order = Order.fromByteArray(value);
                if (!order.getAssetId().equals(req.getAssetId())) {
                    continue;
                }

                // ngac check on order account
                try {
                    PDP.canReadOrder(ctx, order.getAccount());
                } catch (UnauthorizedException e) {
                    // if unauthorized do not add to the return set
                    continue;
                }

                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    @Transaction
    public String[] GetAvailableLicenses(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canReadLicense(ctx, req.getAccount());

        return getAvailableLicenses(ctx, req.getAccount(), req.getOrderId());
    }

    @Transaction
    public String[] GetLicensesWithSWIDs(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        PDP.canReadLicense(ctx, req.getAccount());

        return getLicensesWithSWIDs(ctx, req.getAccount(), req.getOrderId());
    }

    @Transaction
    public Order[] GetExpiredOrders(Context ctx) {
        Instant txTs = ctx.getStub().getTxTimestamp();

        String key = ORDER_PREFIX;
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~")) {
            List<Order> orders = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                Order order = Order.fromByteArray(value);

                // skip orders that are not expired or that the cid does not have access to
                if (!isExpired(txTs, order.getExpiration())) {
                    continue;
                }

                try {
                    PDP.canReadOrder(ctx, order.getAccount());
                } catch (UnauthorizedException e) {
                    continue;
                }

                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    String[] getAvailableLicenses(Context ctx, String account, String orderId) {
        // get order
        Order order = getOrder(ctx, orderId, account);

        // collect only licenses in order that do not have a swid:id entry
        List<String> availableLicenses = new ArrayList<>();
        for (String licenseId : order.getLicenses()) {
            String key = swidKey(licenseId);
            byte[] bytes = ctx.getStub().getPrivateData(accountIPDC(account), key);
            if (bytes.length > 0) {
                continue;
            }

            availableLicenses.add(licenseId);
        }

        return availableLicenses.toArray(String[]::new);
    }

    String[] getLicensesWithSWIDs(Context ctx, String account, String orderId) {
        // get order
        Order order = getOrder(ctx, orderId, account);

        // collect only licenses in order that do not have a swid:id entry
        List<String> licensesWithSWIDs = new ArrayList<>();
        for (String licenseId : order.getLicenses()) {
            String key = swidKey(licenseId);
            byte[] bytes = ctx.getStub().getPrivateData(accountIPDC(account), key);
            if (bytes.length == 0) {
                continue;
            }

            licensesWithSWIDs.add(licenseId);
        }

        return licensesWithSWIDs.toArray(String[]::new);
    }


    /*Allocated[] getAllocatedLicensesForAsset(Context ctx, String account, String assetId){
        String collection = accountIPDC(account);
        String key = new LicenseKey(assetId, "", "").toKey();
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(collection, key, key + "~")) {
            List<Allocated> licenses = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                licenses.add(Allocated.fromByteArray(value));
            }

            return licenses.toArray(Allocated[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }*/

    private Order getOrder(Context ctx, String orderId, String account) {
        // get order from IPDC
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, orderKey(account, orderId));
        if (bytes.length == 0) {
            throw new ChaincodeException("order with ID " + orderId + " and account " + account + " does not exist");
        }

        return Order.fromByteArray(bytes);
    }

    private void allocateRenewalLicenses(Context ctx, Order order) {
        // update each license on ADMINMSP to reflect the new expiration
        for (String licenseId : order.getLicenses()) {
            LicenseKey lk = new LicenseKey(order.getAssetId(), licenseId, "");
            byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, lk.toKey());
            if (bytes.length == 0) {
                throw new ChaincodeException("allocated license " + lk.getLicenseId() + " does not exist for asset " + order.getAssetId());
            }

            License license = License.fromByteArray(bytes);
            license.setAllocated(new Allocated(licenseId, order.getAccount(), order.getExpiration(), order.getId()));

            // write license update to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, lk.toKey(), license.toByteArray());

            // add salt to license key
            lk.setSalt(license.getSalt());

            // write license update to ledger
            ctx.getStub().putState(lk.toHashKey(), new byte[]{0});
        }
    }

    private void allocateLicenses(Context ctx, Order order, List<String> licenses) {
        // check that there are no duplicate licenses
        HashSet<String> licensesSet = new HashSet<>(licenses);
        if (licensesSet.size() != licenses.size()) {
            throw new ChaincodeException("duplicate licenses are not allowed");
        }

        // check that each license exists and is not allocated already
        // it's assumed to get here an ngac check was passed so detailed error messages are ok
        for (String licenseId : licenses) {
            LicenseKey licenseKey = new LicenseKey(order.getAssetId(), licenseId, "");
            byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, licenseKey.toKey());
            if (bytes.length == 0) {
                throw new ChaincodeException("license " + licenseId + " does not exist for asset " + order.getAssetId());
            }

            License license = License.fromByteArray(bytes);
            if (license.getAllocated() != null) {
                throw new ChaincodeException("license " + licenseId + " is already allocated");
            }

            // update license allocated
            Allocated allocated = new Allocated(licenseId, order.getAccount(), order.getExpiration(), order.getId());
            license.setAllocated(allocated);

            // add salt to license key
            licenseKey.setSalt(license.getSalt());

            // write license update to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, licenseKey.toKey(), license.toByteArray());

            // write license update to ledger
            ctx.getStub().putState(licenseKey.toHashKey(), new byte[]{0});
        }
    }
}
