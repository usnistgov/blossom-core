package ngac;

import gov.nist.csd.pm.pap.PAP;
import gov.nist.csd.pm.pap.memory.MemoryPolicyStore;
import gov.nist.csd.pm.policy.exceptions.PMException;
import gov.nist.csd.pm.policy.model.access.AccessRightSet;
import gov.nist.csd.pm.policy.model.access.UserContext;
import gov.nist.csd.pm.policy.model.prohibition.ContainerCondition;
import gov.nist.csd.pm.policy.model.prohibition.ProhibitionSubject;
import model.Status;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ChaincodeException;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import static gov.nist.csd.pm.policy.model.access.AdminAccessRights.ALL_ACCESS_RIGHTS;
import static model.Status.*;

public class PolicyBuilder {

    public static final String ADMINMSP = "Org1MSP";
    public static final String ACQ_OFFICER = "Acquisition Officer";
    public static final String TPOC = "Technical Point of Contact";
    public static final String SYS_OWNER = "System Owner";
    public static final String AUTH_CHAINCODE_NAME = "authorization";
    public static final String AUTH_CHANNEL_NAME = "authorization";
    public static final String BLOSSOM_ROLE_ATTR = "blossom.role";
    public static final String ASSET_TARGET = "asset_target";
    public static final String ADMINMSP_ACCOUNT_OA = accountOA(ADMINMSP);

    public static final String WRITE_ASSET = "write_asset";
    public static final String READ_ASSETS = "read_assets";
    public static final String READ_ASSET_DETAIL = "read_asset_detail";
    public static final String READ_ORDER = "read_order";
    public static final String ALLOCATE_LICENSE = "allocate_license";
    public static final String RETURN_LICENSE = "return_license";
    public static final String READ_LICENSE = "read_license";
    public static final String WRITE_SWID = "write_swid";
    public static final String READ_SWID = "read_swid";
    public static final String INITIATE_ORDER = "initiate_order";
    public static final String APPROVE_ORDER = "approve_order";
    public static final String DENY_ORDER = "deny_order";
    public static final AccessRightSet RESOURCE_ARSET = new AccessRightSet(
            WRITE_ASSET,
            READ_ASSETS,
            READ_ASSET_DETAIL,
            INITIATE_ORDER,
            APPROVE_ORDER,
            DENY_ORDER,
            READ_ORDER,
            ALLOCATE_LICENSE,
            RETURN_LICENSE,
            READ_LICENSE,
            WRITE_SWID,
            READ_SWID
    );

    public static PAP buildPolicyForAssetDecision(Context ctx) throws PMException {
        PAP pap = buildPolicyBase(ctx);

        // create asset target for decisions
        pap.graph().createObject(ASSET_TARGET, "RBAC/asset");

        // deny non adminmsp ACQ eleveated privs on assets
        String cidAccount = ctx.getClientIdentity().getMSPID();
        if (!cidAccount.equals(ADMINMSP)) {
            pap.prohibitions().create(
                    "deny non-adminmsp ACQ",
                    ProhibitionSubject.userAttribute(ACQ_OFFICER),
                    new AccessRightSet(READ_ASSET_DETAIL, ALLOCATE_LICENSE),
                    false,
                    new ContainerCondition("RBAC/asset", false)
            );
        }

        return pap;
    }

    public static PAP buildPolicyForAccountDecision(Context ctx, String targetAccount) throws PMException {
        PAP pap = buildPolicyBase(ctx);

        String accountTarget = accountTarget(targetAccount);
        String targetAccountOA = accountOA(targetAccount);

        // create target account config
        pap.graph().createObjectAttribute(targetAccountOA, "Account");
        pap.graph().createObject(accountTarget, targetAccountOA, "RBAC/account", "Status/account");

        // if the targetAccountUA exists, then the request is from the same account, associate the ua and oa
        // otherwise do nothing as the accounts dont match, adminmsp will be taken care of in following block
        String targetAccountUA = accountUA(targetAccount);
        if (pap.graph().nodeExists(targetAccountUA)) {
            pap.graph().associate(targetAccountUA, targetAccountOA, new AccessRightSet(ALL_ACCESS_RIGHTS));
        }

        // if cid is adminmsp, grant access to target account oa and
        // deny adminmsp elevated privs on account target
        String cidAccount = ctx.getClientIdentity().getMSPID();
        if (cidAccount.equals(ADMINMSP)) {
            String cidAcctUA = accountUA(cidAccount);
            pap.graph().associate(cidAcctUA, targetAccountOA, new AccessRightSet(ALL_ACCESS_RIGHTS));

            pap.prohibitions().create(
                    "deny non-adminmsp ACQ",
                    ProhibitionSubject.userAttribute(ACQ_OFFICER),
                    new AccessRightSet(APPROVE_ORDER, DENY_ORDER),
                    false,
                    new ContainerCondition("RBAC/account", false)
            );
        }

        return pap;
    }

    private static PAP buildPolicyBase(Context ctx) throws PMException {
        PAP pap = new PAP(new MemoryPolicyStore());

        pap.graph().setResourceAccessRights(RESOURCE_ARSET);

        // build attribute hierarchy
        buildAttributes(pap);

        // build account ua config
        String cidAccount = ctx.getClientIdentity().getMSPID();
        String accountUA = accountUA(cidAccount);
        pap.graph().createUserAttribute(accountUA, "Account");

        // create user and assign to attributes
        UserContext userContext = getUserContextFromCID(ctx.getClientIdentity());
        String role = getRole(ctx, cidAccount);
        Status status = getAccountStatus(ctx);
        pap.graph().createUser(userContext.getUser(), role, accountUA, status.toString());

        return pap;
    }

    private static void buildAttributes(PAP pap) throws PMException {
        // RBAC PC
        pap.graph().createPolicyClass("RBAC");

        pap.graph().createObjectAttribute("RBAC/asset", "RBAC");
        pap.graph().createObjectAttribute("RBAC/account", "RBAC");

        pap.graph().createUserAttribute(SYS_OWNER, "RBAC");
        pap.graph().createUserAttribute(ACQ_OFFICER, "RBAC");
        pap.graph().createUserAttribute(TPOC, "RBAC");

        // SO
        pap.graph().associate(SYS_OWNER, "RBAC/asset", new AccessRightSet(
                READ_ASSETS,
                WRITE_ASSET,
                READ_ASSET_DETAIL
        ));
        pap.graph().associate(SYS_OWNER, "RBAC/account", new AccessRightSet(
                READ_ORDER,
                READ_SWID
        ));

        // ACQ
        pap.graph().associate(ACQ_OFFICER, "RBAC/asset", new AccessRightSet(
                READ_ASSETS,
                READ_ASSET_DETAIL,
                ALLOCATE_LICENSE
        ));
        pap.graph().associate(ACQ_OFFICER, "RBAC/account", new AccessRightSet(
                READ_ORDER,
                APPROVE_ORDER,
                DENY_ORDER,
                READ_LICENSE,
                READ_SWID
        ));

        // TPOC
        pap.graph().associate(TPOC, "RBAC/asset", new AccessRightSet(READ_ASSETS));
        pap.graph().associate(TPOC, "RBAC/account", new AccessRightSet(
                INITIATE_ORDER,
                READ_ORDER,
                READ_SWID,
                WRITE_SWID,
                READ_LICENSE,
                RETURN_LICENSE
        ));

        // Account PC
        pap.graph().createPolicyClass("Account");

        // Status PC
        pap.graph().createPolicyClass("Status");
        pap.graph().createUserAttribute(AUTHORIZED.toString(), "Status");
        pap.graph().createUserAttribute(PENDING.toString(), "Status");
        pap.graph().createUserAttribute(UNAUTHORIZED.toString(), PENDING.toString());

        pap.graph().createObjectAttribute("Status/account", "Status");
        pap.graph().createObjectAttribute("Status/asset", "Status");

        pap.graph().associate(AUTHORIZED.name(), "Status/account", new AccessRightSet(ALL_ACCESS_RIGHTS));
        pap.graph().associate(AUTHORIZED.name(), "Status/asset", new AccessRightSet(ALL_ACCESS_RIGHTS));
    }

    public static UserContext getUserContextFromCID(ClientIdentity cid) {
        X509Certificate cert = cid.getX509Certificate();
        String mspid = cid.getMSPID();

        String user;
        try {
            JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(cert);
            X500Name subject = jcaX509CertificateHolder.getSubject();
            RDN cnRDN = subject.getRDNs(BCStyle.CN)[0];
            AttributeTypeAndValue first = cnRDN.getFirst();
            user = first.getValue().toString();
        } catch (CertificateEncodingException e) {
            throw new ChaincodeException(e);
        }

        return new UserContext(user + ":" + mspid);
    }

    private static Status getAccountStatus(Context ctx) {
        // invoke the ATO channel chaincode to get the status of the requesting account using GetAccountStatus
        // the MSPID to check is embedded in the request context
        Chaincode.Response response = ctx.getStub()
                                         .invokeChaincodeWithStringArgs(
                                                 AUTH_CHAINCODE_NAME,
                                                 "GetAccountStatus",
                                                 AUTH_CHANNEL_NAME
                                         );
        return Status.fromString(response.getStringPayload());
    }

    private static String getRole(Context ctx, String account) {
        String role = ctx.getClientIdentity().getAttributeValue(BLOSSOM_ROLE_ATTR);

        // if adminmsp only SO and ACQ are allowed roles
        // if not adminmsp only TPOC and ACQ are allowed roles
        if ((account.equals(ADMINMSP) && !(role.equals(SYS_OWNER) || role.equals(ACQ_OFFICER))) ||
                (!account.equals(ADMINMSP) && !(role.equals(TPOC) || role.equals(ACQ_OFFICER)))) {
            throw new ChaincodeException("invalid role " + role);
        }

        return role;
    }

    public static String accountTarget(String account) {
        return account + "_target";
    }

    private static String accountUA(String account) {
        return account + "_UA";
    }

    private static String accountOA(String account) {
        return account + "_OA";
    }
}
