const (
    // blossom admin mspid
    ADMINMSP            = "Org1MSP"

    ALL_ACCOUNTS_USERS  = "all account users"
    ALL_ACCOUNTS        = "all accounts"

    AUTHORIZED_STATUSES   = ["AUTHORIZED"]
    PENDING_STATUSES      = ["PENDING_APPROVAL", "PENDING_ATO"]
    UNAUTHORIZED_STATUSES = ["UNAUTHORIZED_DENIED", "UNAUTHORIZED_ATO", "UNAUTHORIZED_OPTOUT", "UNAUTHORIZED_SECURITY_RISK", "UNAUTHORIZED_ROB"]
)

// utility functions
function accountUsersNodeName(string account) string {
    return account + " users"
}

function accountContainerNodeName(string account) string {
    return account + " account attr"
}

function accountObjectNodeName(string account) string {
    return account + " account"
}

function initiateVoteDenyLabel(string accountUA) string {
    return "deny-" + accountUA + "-initiate_vote-except-on-self"
}

function denyInitiateVoteExceptOnSelf(string accountUA, string accountOA) {
    create prohibition initiateVoteDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["initiate_vote"]
    on intersection of ["Status.accounts", !accountOA]
}

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

function initiateVote(string initiator, string voteID, string targetMember) {
    let initiatorUsers  = accountUsersNodeName(initiator)
    let voteua          = targetMember + "-", voteID, " initiator"
    let voteoa          = targetMember + "-", voteID, " vote attr"
    let voteobj         = targetMember + "-", voteID, " vote"

    create ua voteua in ["Vote Initiator"]
    create oa voteoa in ["votes"]
    create o voteobj in [voteoa, "RBAC.votes", "Status.votes"]
    associate voteua and voteoa with ["delete_vote", "complete_vote"]

    assign initiatorUsers to [voteua]
}

function deleteVote(string initiator, string voteID, string targetMember) {
    let voteua          = targetMember + "-" + voteID + " initiator"
    let voteoa          = targetMember + "-" + voteID + " vote attr"
    let voteobj         = targetMember + "-" + voteID + " vote"

    deassign initiator from [voteua]
    foreach child in getChildren(voteua) {
        deassign child from [voteua]
    }
    delete ua voteua
    delete o voteobj
    delete oa voteoa
}

function completeVote(string voteID, string targetMember) {
    let voteua          = targetMember + "-" + voteID + " initiator"
    let voteoa          = targetMember + "-" + voteID + " vote attr"

    dissociate voteua and voteoa
}


function updateAccountStatus(string accountID, string status) {
    let accountUA = accountUsersNodeName(accountID)
    let accountOA = accountObjectNodeName(accountID)

    if contains(AUTHORIZED_STATUSES, status) {
        assign      accountUA to    ["authorized"]
        deassign    accountUA from  ["pending", "unauthorized"]

        // delete initiate vote prohibition that prevents the account from initiating votes on
        // other accounts when status is pending
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

create pc "Accounts" {
    user attributes {
        ALL_ACCOUNTS_USERS
    }

    object attributes {
        ALL_ACCOUNTS
    }

    associations {
        ALL_ACCOUNTS_USERS and ALL_ACCOUNTS with ["initiate_vote"]
    }
}

create pc "RBAC" {
    user attributes {
        "Blossom Admin"
        "Authorizing Official"
    }

    object attributes {
        "RBAC.blossom_system"
            "blossom_system"
        "RBAC.accounts"
        "RBAC.votes"
    }

    associations {
        "Blossom Admin"         and "RBAC.blossom_system"   with ["bootstrap", "update_account_status", "approve_account"]
        // this association is with the blossom system because these permissions are never lost
        // association directly with the OA applies the association to all policy classes the OA is contained in
        "Authorizing Official"  and "blossom_system"        with ["request_account"]
        "Authorizing Official"  and "RBAC.accounts"         with ["initiate_vote"]
        "Authorizing Official"  and "RBAC.votes"            with ["delete_vote", "complete_vote", "vote"]
        "Authorizing Official"  and "RBAC.accounts"         with ["upload_ato"]
    }
}

create pc "Voting" {
    user attributes {
        "Vote Initiator"
    }

    object attributes {
        "votes"
    }

    associations {
        "Authorizing Official"  and "votes" with ["vote"]
        "Blossom Admin"         and "votes" with ["complete_vote"]
    }
}

// status policy
create pc "Status" {
    user attributes {
        "statuses"
            "authorized"
            "pending"
                "unauthorized"
    }

    object attributes {
        "Status.blossom_system"
            "blossom_system"
        "Status.accounts"
        "Status.votes"
    }

    associations {
        "authorized"    and "Status.blossom_system" with ["*r"]
        "authorized"    and "Status.accounts"       with ["initiate_vote"]
        "pending"       and "Status.accounts"       with ["initiate_vote", "upload_ato"]
        "pending"       and "Status.votes"          with ["vote"]
        "authorized"    and "Status.votes"          with ["vote", "complete_vote", "delete_vote"]
    }
}

// bootstrap adminmsp account
create ua accountUsersNodeName(ADMINMSP) in [ALL_ACCOUNTS_USERS, "authorized"]
create oa accountContainerNodeName(ADMINMSP) in [ALL_ACCOUNTS]
create o accountObjectNodeName(ADMINMSP) in [ALL_ACCOUNTS, "RBAC.accounts", "Status.accounts"]



