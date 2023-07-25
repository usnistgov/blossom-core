const ADMINMSP = "Org1MSP"

function accountUsersNodeName(string account) string {
    return concat([account, " users"])
}

function accountContainerNodeName(string account) string {
    return concat([account, " account attr"])
}

function accountObjectNodeName(string account) string {
    return concat([account, " account"])
}

set resource access rights [
    "request_account",
    "approve_account",
    "upload_ato",
    "update_account_status",
    "initiate_vote",
    "delete_vote",
    "complete_vote",
    "bootstrap"
]

create pc "RBAC"

    create ua "Blossom Admin"        in ["RBAC"]
    create ua "System Owner"         in ["RBAC"]
    create ua "System Administrator" in ["RBAC"]

    create oa "RBAC.blossom_system" in ["RBAC"]
    create oa "RBAC.accounts"       in ["RBAC"]
    create oa "RBAC.votes"          in ["RBAC"]
    create oa "blossom_system"      in ["RBAC.blossom_system"]

    associate "Blossom Admin"           and "RBAC.blossom_system"   with ["bootstrap", "update_account_status", "approve_account"]
    associate "Blossom Admin"           and "RBAC.accounts"         with ["create_object"]
    # this association is with the blossom system because these permissions are never lost
    # association directly with the OA applies the association to all policy classes the OA is contained in
    associate "System Owner"            and "blossom_system"        with ["request_account"]
    associate "System Owner"            and "RBAC.blossom_system"   with ["initiate_vote"]
    associate "System Owner"            and "RBAC.votes"            with ["delete_vote", "complete_vote"]
    associate "System Administrator"    and "RBAC.accounts"         with ["upload_ato"]


# status policy
const AUTHORIZED_STATUSES   = ["AUTHORIZED"]
const PENDING_STATUSES      = ["PENDING_APPROVAL", "PENDING_ATO"]
const UNAUTHORIZED_STATUSES = ["UNAUTHORIZED_DENIED", "UNAUTHORIZED_ATO", "UNAUTHORIZED_OPTOUT", "UNAUTHORIZED_SECURITY_RISK", "UNAUTHORIZED_ROB"]
create pc "Status"

    create ua "statuses"        in ["Status"]
    create ua "authorized"      in ["statuses"]
    create ua "pending"         in ["statuses"]
    create ua "unauthorized"    in ["pending"]
    create ua "Status Writer"   in ["Status"]
    assign "Blossom Admin"      to ["Status Writer"]

    create oa "Status.blossom_system" in ["Status"]
    assign "blossom_system" to ["Status.blossom_system"]

    associate "authorized"      and "Status.blossom_system" with ["*r"]
    # associate "pending"         and "Status.blossom_system" with ["initiate_vote", "delete_vote", "complete_vote"]

    # status writers can write statuses
    associate "Status Writer"   and "statuses"              with ["create_user_attribute", "assign_user_attribute", "associate", "associate_user_attribute"]

    create prohibition 'deny-pending-status-write'
    deny user attribute 'pending'
    access rights ["create_user_attribute", "assign_user_attribute", "associate", "associate_user_attribute"]
    on union of ["statuses"]

    function updateAccountStatus(string accountName, string status) {
        let accountUA = accountUsersNodeName(accountName)

        if contains(AUTHORIZED_STATUSES, status) {
            assign      accountUA to    ["authorized"]
            deassign    accountUA from  ["pending", "unauthorized"]
        } else if contains(PENDING_STATUSES, status) {
            assign      accountUA to    ["pending"]
            deassign    accountUA from  ["authorized", "unauthorized"]
        } else {
            assign      accountUA to    ["unauthorized"]
            deassign    accountUA from  ["authorized", "pending"]
        }
    }

const ALL_ACCOUNTS_USERS    = "all account users"
const ALL_ACCOUNTS          = "all accounts"
create pc "Accounts"

    create ua ALL_ACCOUNTS_USERS in ["Accounts"]

    create oa ALL_ACCOUNTS in ["Accounts"]

    associate "Blossom Admin" and ALL_ACCOUNTS_USERS with ["create_user_attribute", "assign_user", "associate_user_attribute"]
    associate "Blossom Admin" and ALL_ACCOUNTS with ["create_object_attribute", "create_object", "associate_object_attribute"]

    function approveAccount(string accountName) {
        let accountUA = accountUsersNodeName(accountName)
        let accountOA = accountContainerNodeName(accountName)
        let accountO = accountObjectNodeName(accountName)

        create ua accountUA in [ALL_ACCOUNTS_USERS, "pending"]
        create oa accountOA in [ALL_ACCOUNTS]
        create o accountO in [accountOA, "RBAC.accounts"]

        associate accountUA and accountOA with ["*"]

    }

create pc "Voting"

    # vote oas will be assigned to this attribute
    create oa "votes" in ["Voting"]

    # vote initiators will be assigned to this attribute and associated with their vote oa
    create ua "Vote Initiator" in ["Voting"]

    associate "Blossom Admin" and "votes" with ["create_object_attribute", "create_object", "delete_object", "delete_object_attribute", "associate"]
    associate "Blossom Admin" and "Vote Initiator" with ["assign_to_user_attribute", "associate", "delete_user_attribute"]

    function initiateVote(string initiator, string voteID, string targetMember) {
        let initiatorUsers  = accountUsersNodeName(initiator)
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])
        let voteobj         = concat([targetMember, "-", voteID, " vote"])

        create ua voteua in ["Vote Initiator"]
        create oa voteoa in ["votes"]
        create o voteobj in [voteoa, "RBAC.votes"]
        associate voteua and voteoa with ["delete_vote", "complete_vote"]

        assign initiatorUsers to [voteua]
    }

    function deleteVote(string initiator, string voteID, string targetMember) {
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])
        let voteobj         = concat([targetMember, "-", voteID, " vote"])

        deassign initiator from [voteua]
        delete ua voteua
        delete o voteobj
        delete oa voteoa
    }

    function completeVote(string voteID, string targetMember) {
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])

        dissociate voteua and voteoa
    }

# bootstrap adminmsp account
let adminmsp_users      = accountUsersNodeName(ADMINMSP)
let adminmsp_container  = accountContainerNodeName(ADMINMSP)
let adminmsp_object     = accountObjectNodeName(ADMINMSP)

create ua adminmsp_users in [ALL_ACCOUNTS_USERS, "authorized"]

create oa adminmsp_container in [ALL_ACCOUNTS]
create o adminmsp_object in [ALL_ACCOUNTS, "RBAC.accounts"]



