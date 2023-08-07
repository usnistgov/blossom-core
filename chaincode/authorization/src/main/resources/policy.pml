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

function initiateVoteDenyLabel(string accountUA) string {
    return concat(["deny-", accountUA, "-initiate_vote-except-on-self"])
}

function denyInitiateVoteExceptOnSelf(string accountUA, string accountOA) {
    create prohibition initiateVoteDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["initiate_vote"]
    on intersection of ["Status.accounts", !accountOA]
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

const ALL_ACCOUNTS_USERS    = "all account users"
const ALL_ACCOUNTS          = "all accounts"

create pc "Accounts"

    create ua ALL_ACCOUNTS_USERS in ["Accounts"]

    create oa ALL_ACCOUNTS in ["Accounts"]

    associate ALL_ACCOUNTS_USERS and ALL_ACCOUNTS with ["initiate_vote"]

    function approveAccount(string accountId) {
        let accountUA = accountUsersNodeName(accountId)
        let accountOA = accountContainerNodeName(accountId)
        let accountO = accountObjectNodeName(accountId)

        create ua accountUA in [ALL_ACCOUNTS_USERS, "pending"]
        create oa accountOA in [ALL_ACCOUNTS]
        create o accountO in [accountOA, "RBAC.accounts", "Status.accounts"]

        associate accountUA and accountOA   with ["*"]

        denyInitiateVoteExceptOnSelf(accountUA, accountOA)
    }

create pc "RBAC"

    create ua "Blossom Admin"        in ["RBAC"]
    create ua "System Owner"         in ["RBAC"]
    create ua "System Administrator" in ["RBAC"]

    create oa "RBAC.blossom_system" in ["RBAC"]
    create oa "RBAC.accounts"       in ["RBAC"]
    create oa "RBAC.votes"          in ["RBAC"]
    create oa "blossom_system"      in ["RBAC.blossom_system"]

    associate "Blossom Admin"           and "RBAC.blossom_system"   with ["bootstrap", "update_account_status", "approve_account"]
    # this association is with the blossom system because these permissions are never lost
    # association directly with the OA applies the association to all policy classes the OA is contained in
    associate "System Owner"            and "blossom_system"        with ["request_account"]
    associate "System Owner"            and "RBAC.accounts"         with ["initiate_vote"]
    associate "System Owner"            and "RBAC.votes"            with ["delete_vote", "complete_vote", "vote"]
    associate "System Administrator"    and "RBAC.accounts"         with ["upload_ato"]

create pc "Voting"

    # vote oas will be assigned to this attribute
    create oa "votes" in ["Voting"]

    # vote initiators will be assigned to this attribute and associated with their vote oa
    create ua "Vote Initiator" in ["Voting"]

    associate "System Owner"    and "votes" with ["vote"]
    associate "Blossom Admin"   and "votes" with ["complete_vote"]

    function initiateVote(string initiator, string voteID, string targetMember) {
        let initiatorUsers  = accountUsersNodeName(initiator)
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])
        let voteobj         = concat([targetMember, "-", voteID, " vote"])

        create ua voteua in ["Vote Initiator"]
        create oa voteoa in ["votes"]
        create o voteobj in [voteoa, "RBAC.votes", "Status.votes"]
        associate voteua and voteoa with ["delete_vote", "complete_vote"]

        assign initiatorUsers to [voteua]
    }

    function deleteVote(string initiator, string voteID, string targetMember) {
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])
        let voteobj         = concat([targetMember, "-", voteID, " vote"])

        deassign initiator from [voteua]
        foreach child in getChildren(voteua) {
            deassign child from [voteua]
        }
        delete ua voteua
        delete o voteobj
        delete oa voteoa
    }

    function completeVote(string voteID, string targetMember) {
        let voteua          = concat([targetMember, "-", voteID, " initiator"])
        let voteoa          = concat([targetMember, "-", voteID, " vote attr"])

        dissociate voteua and voteoa
    }

# status policy
const AUTHORIZED_STATUSES   = ["AUTHORIZED"]
const PENDING_STATUSES      = ["PENDING_APPROVAL", "PENDING_ATO"]
const UNAUTHORIZED_STATUSES = ["UNAUTHORIZED_DENIED", "UNAUTHORIZED_ATO", "UNAUTHORIZED_OPTOUT", "UNAUTHORIZED_SECURITY_RISK", "UNAUTHORIZED_ROB"]
create pc "Status"

    create ua "statuses"        in ["Status"]
    create ua "authorized"      in ["statuses"]
    create ua "pending"         in ["statuses"]
    create ua "unauthorized"    in ["pending"]

    create oa "Status.blossom_system"   in ["Status"]
    create oa "Status.accounts"         in ["Status"]
    create oa "Status.votes"            in ["Status"]
    assign "blossom_system" to ["Status.blossom_system"]

    associate "authorized"      and "Status.blossom_system" with ["*r"]
    associate "authorized"      and "Status.accounts"       with ["initiate_vote"]
    associate "pending"         and "Status.accounts"       with ["initiate_vote", "upload_ato"]
    associate "pending"         and "Status.votes"          with ["vote"]
    associate "authorized"      and "Status.votes"          with ["vote", "complete_vote", "delete_vote"]

    function updateAccountStatus(string accountID, string status) {
        let accountUA = accountUsersNodeName(accountID)
        let accountOA = accountObjectNodeName(accountID)

        if contains(AUTHORIZED_STATUSES, status) {
            assign      accountUA to    ["authorized"]
            deassign    accountUA from  ["pending", "unauthorized"]

            # delete initiate vote prohibition that prevents the account from initiating votes on
            # other accounts when status is pending
            delete prohibition initiateVoteDenyLabel(accountUA)
        } else if contains(PENDING_STATUSES, status) {
            assign      accountUA to    ["pending"]
            deassign    accountUA from  ["authorized", "unauthorized"]

            delete prohibition initiateVoteDenyLabel(accountUA)
            denyInitiateVoteExceptOnSelf(accountUA, accountOA)
        } else {
            assign      accountUA to    ["unauthorized"]
            deassign    accountUA from  ["authorized", "pending"]

            delete prohibition initiateVoteDenyLabel(accountUA)
            denyInitiateVoteExceptOnSelf(accountUA, accountOA)
        }
    }

# bootstrap adminmsp account
let adminmsp_users      = accountUsersNodeName(ADMINMSP)
let adminmsp_container  = accountContainerNodeName(ADMINMSP)
let adminmsp_object     = accountObjectNodeName(ADMINMSP)

create ua adminmsp_users in [ALL_ACCOUNTS_USERS, "authorized"]

create oa adminmsp_container in [ALL_ACCOUNTS]
create o adminmsp_object in [ALL_ACCOUNTS, "RBAC.accounts", "Status.accounts"]



