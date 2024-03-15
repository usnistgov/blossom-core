package ngac;

import contract.response.AssetDetailResponse;
import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pdp.reviewer.PolicyReviewer;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.UserContext;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;

import static ngac.PolicyBuilder.*;
import static ngac.PolicyBuilder.accountTarget;

public class PDP {
    public static void canWriteAsset(Context ctx) {
        checkAssetPrivileges(ctx, WRITE_ASSET);
    }

    public static void canReadAssetDetail(Context ctx) {
         checkAssetPrivileges(ctx, READ_ASSET_DETAIL);
    }

    public static AssetDetailResponse filterAssetDetail(Context ctx, AssetDetailResponse asset) {
        try {
            checkAssetPrivileges(ctx, READ_ASSET_DETAIL);

            return asset;
        } catch (UnauthorizedException e) {}

        // if cannot read detail check if can read asset, this will throw an exception to caller if client cannot
        checkAssetPrivileges(ctx, READ_ASSETS);

        // strip detail out of object
        asset.setAllocatedLicenses(null);
        asset.setAvailableLicenses(null);
        asset.setTotalAmount(0);

        return asset;
    }

    public static void canReadAssets(Context ctx) {
        checkAssetPrivileges(ctx, READ_ASSETS);
    }

    public static void canAllocateLicense(Context ctx) {
        checkAssetPrivileges(ctx, ALLOCATE_LICENSE);
    }

    public static void canReadOrder(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, READ_ORDER);
    }

    public static void canInitiateOrder(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, INITIATE_ORDER);
    }

    public static void canApproveOrder(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, APPROVE_ORDER);
    }

    public static void canDenyOrder(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, APPROVE_ORDER);
    }

    public static void canReturnLicense(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, RETURN_LICENSE);
    }

    public static void canReadLicense(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, READ_LICENSE);
    }

    public static void canReadSWID(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, READ_SWID);
    }

    public static void canWriteSWID(Context ctx, String targetAccount) {
        checkAccountPrivileges(ctx, targetAccount, WRITE_SWID);
    }

    private static void checkAssetPrivileges(Context ctx, String ar) {
        try {
            UserContext userContextFromCID = getUserContextFromCID(ctx.getClientIdentity());
            PAP pap = PolicyBuilder.buildPolicyForAssetDecision(ctx);
            PolicyReviewer policyReviewer = new PolicyReviewer(pap);

            boolean contains = policyReviewer
                    .access()
                    .computePrivileges(userContextFromCID, ASSET_TARGET)
                    .contains(ar);
            if (!contains) {
                throw new UnauthorizedException();
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }

    private static void checkAccountPrivileges(Context ctx, String account, String ar) {
        try {
            UserContext userContextFromCID = getUserContextFromCID(ctx.getClientIdentity());
            PAP pap = PolicyBuilder.buildPolicyForAssetDecision(ctx);
            PolicyReviewer policyReviewer = new PolicyReviewer(pap);

            boolean contains = policyReviewer
                    .access()
                    .computePrivileges(userContextFromCID, accountTarget(account))
                    .contains(ar);

            if (!contains) {
                throw new UnauthorizedException();
            }
        } catch (PMException e) {
            throw new ChaincodeException(e);
        }
    }
}
