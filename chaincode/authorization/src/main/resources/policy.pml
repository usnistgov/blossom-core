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

    create ua "BlossomAdmin"        in ["RBAC"]
    create ua "SystemOwner"         in ["RBAC"]
    create ua "SystemAdministrator" in ["RBAC"]

    create oa "RBAC.blossom_system" in ["RBAC"]
    create oa "RBAC.accounts"       in ["RBAC"]
    create oa "RBAC.votes"          in ["RBAC"]
    create oa "blossom_system"      in ["RBAC.blossom_system"]

    associate "BlossomAdmin"           and "RBAC.blossom_system"   with ["bootstrap", "update_account_status", "approve_account"]
    # this association is with the blossom system because these permissions are never lost
    # association directly with the OA applies the association to all policy classes the OA is contained in
    associate "SystemOwner"            and "blossom_system"        with ["request_account"]
    associate "SystemOwner"            and "RBAC.blossom_system"   with ["initiate_vote"]
    associate "SystemOwner"            and "RBAC.votes"            with ["delete_vote", "complete_vote"]
    associate "SystemAdministrator"    and "RBAC.accounts"         with ["upload_ato"]

function updateAccountStatus(string accountUA, string status) {
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

# status policy
const AUTHORIZED_STATUSES   = ["AUTHORIZED"]
const PENDING_STATUSES      = ["PENDING_APPROVAL", "PENDING_ATO"]
const UNAUTHORIZED_STATUSES = ["UNAUTHORIZED_DENIED", "UNAUTHORIZED_ATO", "UNAUTHORIZED_OPTOUT", "UNAUTHORIZED_SECURITY_RISK", "UNAUTHORIZED_ROB"]
create pc "Status"

    create ua "authorized"                  in ["Status"]
    create ua "pending"                     in ["Status"]
    create ua "unauthorized"                in ["pending"]

    create oa "Status.blossom_system" in ["Status"]
    assign "blossom_system" to ["Status.blossom_system"]

    associate "authorized"      and "Status.blossom_system" with ["*r"]
    associate "pending"         and "Status.blossom_system" with ["initiate_vote", "delete_vote", "complete_vote"]

    create obligation "account status" {
        create rule "update account status"
        when any user
        performs ["update_account_status"]
        do(ctx) {
            let accountName = ctx["event"]["accountName"]
            let status      = ctx["event"]["status"]
            let accountUA = accountUsersNodeName(accountName)

            updateAccountStatus(accountUA, status)
        }
    }

const ALL_ACCOUNTS_USERS    = "all account users"
const ALL_ACCOUNTS          = "all accounts"
create pc "Accounts"

    create ua ALL_ACCOUNTS_USERS in ["Accounts"]

    create oa ALL_ACCOUNTS in ["Accounts"]

    create obligation "account" {
        create rule "approve account"
        when any user
        performs ["approve_account"]
        do(ctx) {
            let accountName = ctx["event"]["accountName"]
            let accountUA = accountUsersNodeName(accountName)
            let accountOA = accountContainerNodeName(accountName)
            let accountO = accountObjectNodeName(accountName)

            create ua accountUA in [ALL_ACCOUNTS_USERS, "pending"]
            create oa accountOA in [ALL_ACCOUNTS]
            create o accountO in [accountOA, "RBAC.accounts"]

            associate accountUA and accountOA with ["*"]
        }
    }

create pc "Voting"

    # vote oas will be assigned to this attribute
    create oa "votes" in ["Voting"]

    # vote initiators will be assigned to this attribute and associated with their vote oa
    create ua "Vote Initiator" in ["Voting"]

    create obligation "vote" {
        create rule "initiate vote"
        when any user
        performs ["initiate_vote", "delete_vote", "complete_vote"]
        do(ctx) {
            let event           = ctx["eventName"]
            let initiator       = ctx["event"]["initiatingMSP"]
            let voteID          = ctx["event"]["id"]
            let targetMember    = ctx["event"]["targetMember"]

            let initiatorUsers  = accountUsersNodeName(initiator)
            let voteua          = concat([targetMember, "-", voteID, " initiator"])
            let voteoa          = concat([targetMember, "-", voteID, " vote attr"])
            let voteobj         = concat([targetMember, "-", voteID, " vote"])

            if equals(event, "initiate_vote") {
                create ua voteua in ["Vote Initiator"]
                create oa voteoa in ["votes"]
                create o voteobj in [voteoa, "RBAC.votes"]
                associate voteua and voteoa with ["delete_vote", "complete_vote"]

                assign initiatorUsers to [voteua]

            } else if equals(event, "delete_vote") {
                deassign initiator from [voteua]
                delete ua voteua
                delete o voteobj
                delete oa voteoa

            } else if equals(event, "complete_vote") {
                let passed = ctx["event"]["passed"]
                let status = ctx["event"]["status"]

                dissociate voteua and voteoa

                # update account status if vote passed
                if passed {
                    updateAccountStatus(accountUsersNodeName(targetMember), status)
                }
            }
        }
    }

# bootstrap adminmsp account
let adminmsp_users      = accountUsersNodeName(ADMINMSP)
let adminmsp_container  = accountContainerNodeName(ADMINMSP)
let adminmsp_object     = accountObjectNodeName(ADMINMSP)

create ua adminmsp_users in [ALL_ACCOUNTS_USERS, "authorized"]

create oa adminmsp_container in [ALL_ACCOUNTS]
create o adminmsp_object in [ALL_ACCOUNTS, "RBAC.accounts"]