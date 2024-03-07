package contract;

import contract.request.asset.AssetIdAndAccountRequest;
import contract.request.asset.AssetIdRequest;
import contract.request.order.*;
import contract.response.OrderResponse;
import model.*;
import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static contract.AssetContract.*;
import static contract.SWIDContract.swidKey;
import static model.AllocatedLicenses.ALLOCATED_PREFIX;
import static model.AllocatedLicenses.allocatedKey;
import static model.DateFormatter.DATE_TIME_FORMATTER;
import static java.time.ZoneOffset.UTC;
import static model.Order.Status.*;

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
    public String GetQuote(Context ctx) {
        QuoteRequest req = new QuoteRequest(ctx, true);

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
                              0,
                              0,
                              0
            );
        }

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return orderId;
    }

    @Transaction
    public void SendQuote(Context ctx) {
        QuoteRequest req = new QuoteRequest(ctx, false);

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
    }

    @Transaction
    public String InitiateOrder(Context ctx) {
        InitiateOrderRequest request = new InitiateOrderRequest(ctx);

        // TODO NGAC

        String account = request.getAccount();
        Order order = getOrder(ctx, request.getOrderId(), account);

        // an order can only be initiated if a quote has been received
        if (!(order.getStatus().equals(RENEWAL_QUOTE_RECEIVED) || order.getStatus().equals(QUOTE_RECEIVED))) {
            throw new ChaincodeException("order " + order.getId() + " has not received a quote");
        }

        // renew the order only if this existing order is in a renewal stage, otherwise initiate it
        if (order.getStatus().equals(RENEWAL_QUOTE_RECEIVED)) {
            order = renewOrder(ctx, account, request);
        } else {
            order = initiateOrder(ctx, request);
        }

        // write order to the ADMIN IPDC
        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());

        return order.getId();
    }

    @Transaction
    public void ApproveOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        // TODO NGAC

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
    }

    @Transaction
    public void DenyOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        // TODO NGAC

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());
        order.setStatus(Order.Status.DENIED);

        String key = orderKey(order.getAccount(), order.getId());
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, order.toByteArray());
    }

    @Transaction
    public String[] GetLicensesToAllocateForOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

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

        return licensesToAllocate.toArray(String[]::new);
    }

    @Transaction
    public AllocatedLicenses AllocateLicenses(Context ctx) {
        AllocateLicensesRequest req = new AllocateLicensesRequest(ctx);

        // TODO NGAC

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        // check that the order is ready to be allocated
        Order.Status status = order.getStatus();
        if (!(status.equals(RENEWAL_APPROVED) || status.equals(APPROVED))) {
            throw new ChaincodeException("cannot allocate licenses for an order that has not been approved");
        }

        AllocatedLicenses allocatedLicenses;
        if (status.equals(RENEWAL_APPROVED)) {
            allocatedLicenses = allocateRenewalLicenses(ctx, order);
        } else {
            allocatedLicenses = allocateLicenses(ctx, order, req.getLicenses());
        }

        // mark order as completed and set date
        order.setStatus(Order.Status.ALLOCATED);

        String ts = DateFormatter.getFormattedTimestamp(ctx);
        order.setAllocatedDate(ts);
        order.setLatestRenewalDate(ts);

        // update order
        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                orderKey(order.getAccount(), order.getId()),
                order.toByteArray()
        );

        return allocatedLicenses;
    }

    @Transaction
    public void SendLicenses(Context ctx) {
        AllocatedLicenses req = new AllocatedLicenses(ctx);

        // todo NGAC

        // check hash vs private data on adminmsp ipdc to verify the licenses being sent were the ones allocated
        byte[] pvtDataHash = ctx.getStub().getPrivateDataHash(ADMINMSP_IPDC, allocatedKey(req.getAssetId(), req.getOrderId()));
        byte[] hash = SHA256.hashBytesToBytes(SerializationUtils.serialize(req));
        if (Arrays.equals(pvtDataHash, hash)) {
            throw new ChaincodeException("provided licenses to send do not match the licenses allocated");
        }

        // save the licenses for this order in the accounts IPDC
        ctx.getStub().putPrivateData(
                accountIPDC(req.getAccount()),
                allocatedKey(req.getAssetId(), req.getOrderId()),
                SerializationUtils.serialize(req)
        );
    }

    @Transaction
    public void DeleteOrder(Context ctx) {
        OrderIdAndAccountRequest request = new OrderIdAndAccountRequest(ctx);

        // TODO NGAC

        Order order = getOrder(ctx, request.getOrderId(), request.getAccount());

        // orders can only be deleted when all allocated licenses are returned
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, allocatedKey(order.getAssetId(), order.getId()));
        if (bytes.length > 0) {
            AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(bytes);
            if (!allocatedLicenses.getLicenses().isEmpty()) {
                throw new ChaincodeException("all licenses must be returned before an order is deleted");
            }
        }

        // delete order
        ctx.getStub().delPrivateData(ADMINMSP_IPDC, orderKey(order.getAccount(), request.getOrderId()));
    }

    @Transaction
    public void ReturnLicenses(Context ctx) {
        ReturnLicensesRequest req = new ReturnLicensesRequest(ctx);

        // todo NGAC

        String allocatedAccount = req.getAccount();
        String collection = accountIPDC(allocatedAccount);

        // get allocated from account IPDC to check for swids
        String allocatedKey = allocatedKey(req.getAssetId(), req.getOrderId());
        byte[] bytes = ctx.getStub().getPrivateData(collection, allocatedKey);
        AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(bytes);

        // delete a swid associated with this license if there is one
        for (String license : req.getLicenses()) {
            ctx.getStub().delPrivateData(collection, swidKey(license));
        }

        // remove licenses from allocated
        Set<String> licenses = allocatedLicenses.getLicenses();
        licenses.removeAll(req.getLicenses());
        allocatedLicenses.setLicenses(licenses);

        // save allocated to ledger
        ctx.getStub().putPrivateData(collection, allocatedKey, allocatedLicenses.toByteArray());
    }

    @Transaction
    public void DeallocateLicenses(Context ctx) throws Exception {
        ReturnLicensesRequest req = new ReturnLicensesRequest(ctx);

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());
        String key = allocatedKey(order.getAssetId(), order.getId());

        // get the hash of the AllocatedLicenses on the account's IPDC and compare with what is about to be applied
        // to the ADMINMSP, if they don't match then return an exception
        byte[] accountHash = ctx.getStub().getPrivateDataHash(accountIPDC(req.getAccount()), key);

        // get allocated from ADMINMSP and remove the licenses that are being returned
        // what's left should be the same as the AllocatedLicenses on the account IPDC
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, key);
        AllocatedLicenses adminmspAllocatedLicenses = AllocatedLicenses.fromByteArray(bytes);
        adminmspAllocatedLicenses.getLicenses().removeAll(req.getLicenses());

        byte[] adminmspHash = SHA256.hashBytesToBytes(adminmspAllocatedLicenses.toByteArray());
        if (Arrays.equals(accountHash, adminmspHash)) {
            throw new ChaincodeException("provided licenses to deallocate do not match the licenses returned from account");
        }

        // update the allocated licenses in the adminmsp
        ctx.getStub().putPrivateData(ADMINMSP_IPDC, key, adminmspAllocatedLicenses.toByteArray());

        // update the order by decreasing the amount by the number of licenses returned
        order.setAmount(order.getAmount() - req.getLicenses().size());
        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                orderKey(order.getAccount(), order.getId()),
                order.toByteArray()
        );

        // update each license to remove allocated and track on ledger
        for (String licenseId : req.getLicenses()) {
            LicenseKey lk = new LicenseKey(order.getAssetId(), licenseId, "");
            bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, lk.toKey());
            if (bytes.length == 0) {
                throw new Exception("license " + lk.getLicenseId() + " does not exist");
            }

            License license = License.fromByteArray(bytes);
            license.setAllocated(null);

            // write to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, lk.toKey(), license.toByteArray());

            // write to ledger
            ctx.getStub().putState(lk.toKey(), new byte[]{0});
        }
    }

    @Transaction
    public OrderResponse GetOrder(Context ctx) {
        OrderIdAndAccountRequest req = new OrderIdAndAccountRequest(ctx);

        Order order = getOrder(ctx, req.getOrderId(), req.getAccount());

        // get licenses from allocated order
        byte[] bytes = ctx.getStub().getPrivateData(
                ADMINMSP_IPDC,
                allocatedKey(order.getAssetId(), order.getId())
        );

        AllocatedLicenses licenses = null;
        if (bytes.length != 0) {
            licenses = SerializationUtils.deserialize(bytes);
        }

        return new OrderResponse(order, licenses);
    }

    @Transaction
    public Order[] GetOrdersByAccount(Context ctx) {
        AccountRequest req = new AccountRequest(ctx);

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

                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    @Transaction
    public AllocatedLicenses[] GetAssetLicenses(Context ctx) {
        AssetIdAndAccountRequest req = new AssetIdAndAccountRequest(ctx);

        // TODO NGAC

        return getAllocatedLicensesWithAsset(ctx, req.getAccount(), req.getAssetId());
    }

    @Transaction
    public Order[] GetOrdersWithExpiredLicenses(Context ctx) {
        AccountRequest req = new AccountRequest(ctx);

        // TODO NGAC

        Instant txTs = ctx.getStub().getTxTimestamp();

        String key = ALLOCATED_PREFIX;
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(ADMINMSP_IPDC, key, key + "~")) {
            List<Order> orders = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                AllocatedLicenses allocatedLicenses = AllocatedLicenses.fromByteArray(value);
                if (!isExpired(txTs, allocatedLicenses.getExpiration())) {
                    continue;
                }

                Order order = getOrder(ctx, allocatedLicenses.getOrderId(), allocatedLicenses.getAccount());
                orders.add(order);
            }

            return orders.toArray(Order[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    protected AllocatedLicenses[] getAllocatedLicensesWithAsset(Context ctx, String account, String assetId){
        String key = allocatedKey(assetId, "");
        try(QueryResultsIterator<KeyValue> stateByRange = ctx.getStub().getPrivateDataByRange(accountIPDC(account), key, key + "~")) {
            List<AllocatedLicenses> licenses = new ArrayList<>();

            for (KeyValue next : stateByRange) {
                byte[] value = next.getValue();

                AllocatedLicenses allocatedLicenses = AllocatedLicenses.fromByteArray(value);
                licenses.add(allocatedLicenses);
            }

            return licenses.toArray(AllocatedLicenses[]::new);
        } catch (Exception e) {
            throw new ChaincodeException(e);
        }
    }

    private Order getOrder(Context ctx, String orderId, String account) {
        // get order from IPDC
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, orderKey(account, orderId));
        if (bytes.length == 0) {
            throw new ChaincodeException("order with ID " + orderId + " and account " + account + " does not exist");
        }

        return Order.fromByteArray(bytes);
    }

    private Order renewOrder(Context ctx, String account, InitiateOrderRequest request) {
        // get order, checking that it exists
        Order order = getOrder(ctx, request.getOrderId(), account);

        // order must have a status of ALLOCATED, RENEWAL_QUOTE_RECEIVED, or RENEWAL_QUOTE_REQUESTED
        if (!order.getStatus().equals(Order.Status.ALLOCATED) || !order.getStatus().equals(RENEWAL_QUOTE_RECEIVED) || !order.getStatus().equals(RENEWAL_QUOTE_REQUESTED)) {
            throw new ChaincodeException("cannot renew an order that has not been allocated yet");
        }

        order.setStatus(Order.Status.RENEWAL_INITIATED);
        order.setDuration(request.getDuration());

        return order;
    }

    private Order initiateOrder(Context ctx, InitiateOrderRequest request) {
        String account = ctx.getClientIdentity().getMSPID();
        String orderId = ctx.getStub().getTxId();
        String initiationDate = DateFormatter.getFormattedTimestamp(ctx);

        return new Order(
                orderId,
                account,
                Order.Status.INITIATED,
                initiationDate,
                null,
                null,
                null,
                request.getAssetId(),
                request.getAmount(),
                request.getDuration(),
                0
        );
    }

    private boolean isExpired(Instant txTs, String expiration) {
        Instant exp = Instant.from(DATE_TIME_FORMATTER.parse(expiration));
        return txTs.isAfter(exp);
    }

    private AllocatedLicenses allocateRenewalLicenses(Context ctx, Order order) {
        int duration = order.getDuration();
        String expiration = DATE_TIME_FORMATTER.format(
                ctx.getStub()
                   .getTxTimestamp()
                   .atZone(ZoneId.from(UTC))
                   .plusYears(duration)
                   .toInstant()
        );

        // get licenses from allocation entry
        String key = allocatedKey(order.getId(), order.getAssetId());
        byte[] bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, key);
        AllocatedLicenses allocatedLicenses = SerializationUtils.deserialize(bytes);
        allocatedLicenses.setExpiration(expiration);

        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                allocatedKey(order.getId(), order.getAssetId()),
                SerializationUtils.serialize(allocatedLicenses)
        );

        // update each license on ADMINMSP to reflect the new expiration
        for (String licenseId : allocatedLicenses.getLicenses()) {
            LicenseKey lk = new LicenseKey(order.getAssetId(), licenseId, "");
            bytes = ctx.getStub().getPrivateData(ADMINMSP_IPDC, lk.toKey());
            if (bytes.length == 0) {
                throw new ChaincodeException("allocated license " + lk.getLicenseId() + " does not exist for asset " + order.getAssetId());
            }

            License license = License.fromByteArray(bytes);
            license.setAllocated(new Allocated(order.getAccount(), expiration, order.getId()));

            // write license update to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, lk.toKey(), license.toByteArray());

            // write license update to ledger
            ctx.getStub().putState(lk.toHashKey(), new byte[]{0});
        }

        return allocatedLicenses;
    }

    private AllocatedLicenses allocateLicenses(Context ctx, Order order, List<String> licenses) {
        // check that there are no duplicate licenses
        HashSet<String> licensesSet = new HashSet<>(licenses);
        if (licensesSet.size() != licenses.size()) {
            throw new ChaincodeException("duplicate licenses are not allowed");
        }

        // compute expiration
        int duration = order.getDuration();
        String expiration = DATE_TIME_FORMATTER.format(
                ctx.getStub()
                   .getTxTimestamp()
                   .atZone(ZoneId.from(UTC))
                   .plusYears(duration)
                   .toInstant()
        );

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
            license.setAllocated(new Allocated(order.getAccount(), expiration, order.getId()));

            // write license update to ADMINMSP
            ctx.getStub().putPrivateData(ADMINMSP_IPDC, licenseKey.toKey(), license.toByteArray());

            // write license update to ledger
            ctx.getStub().putState(licenseKey.toHashKey(), new byte[]{0});
        }

        // create allocation entry for this order to verify the licenses sent match the ones allocated
        AllocatedLicenses allocatedLicenses = new AllocatedLicenses(
                order.getId(),
                order.getAssetId(),
                order.getAccount(),
                expiration,
                new HashSet<>(licenses)
        );

        ctx.getStub().putPrivateData(
                ADMINMSP_IPDC,
                allocatedKey(order.getId(), order.getAssetId()),
                SerializationUtils.serialize(allocatedLicenses)
        );

        return allocatedLicenses;
    }
}
