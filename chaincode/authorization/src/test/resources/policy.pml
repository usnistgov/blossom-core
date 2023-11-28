const ADMINMSP = "Org1MSP"

set resource access rights [
    "bootstrap",
    "update_vote_config",
    "update_mou",
    "get_mou",
    "sign_mou",
    "join",
    "write_ato",
    "initiate_vote",
    "vote",
    "abort_vote",
    "certify_vote",
    "submit_feedback",
    "read_ato",
    "join"
]

create pc "RBAC" {
    user attributes {
        "Authorizing Official"
            "Blossom Admin"
    }

    object attributes {
        "RBAC/blossom_target"
            "blossom_target"
        "RBAC/accounts"
        "RBAC/votes"
    }

    associations {
        "Blossom Admin"         and "RBAC/blossom_target"   with ["bootstrap", "update_mou", "update_vote_config"]
        // this association is with the blossom target because these permissions are never lost
        // association directly with the OA applies the association to all policy classes the OA is contained in
        "Authorizing Official"  and "blossom_target"        with ["get_mou", "sign_mou", "join"]
        "Authorizing Official"  and "RBAC/votes"            with ["vote", "certify_vote", "abort_vote", "read_ato"]
        "Authorizing Official"  and "RBAC/accounts"         with ["initiate_vote", "submit_feedback", "read_ato", "join"]
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
        "Status/blossom_target"
            "blossom_target"
        "Status/accounts"
        "Status/votes"
    }

    associations {
        "authorized"    and "Status/blossom_target" with ["*r"]
        "authorized"    and "Status/accounts"       with ["*r"]
        "authorized"    and "Status/votes"          with ["*r"]
    }
}

create pc "Votes" {
    object attributes {
        "all votes"
    }

    associations {
        "Blossom Admin"         and "all votes" with ["certify_vote"]
        "Authorizing Official"  and "all votes" with ["vote"]
    }
}

// bootstrap adminmsp account
signMOU(ADMINMSP)
updateAccountStatus(ADMINMSP, "AUTHORIZED")

function initiateVote(string initiator, string voteID, string targetMember) {
    initiatorUsers  := accountUsersNodeName(initiator)
    voteua          := targetMember + "-" + voteID + " initiator"
    voteoa          := targetMember + "-" + voteID + " vote attr"
    voteobj         := targetMember + "-" + voteID + " vote"

    create ua voteua  assign to ["Votes"]
    create oa voteoa  assign to ["all votes"]
    create o  voteobj assign to ["RBAC/votes", "Status/votes", voteoa]

    associate voteua and voteoa with ["abort_vote", "certify_vote"]

    assign initiatorUsers to [voteua]
}

function endVote(string voteID, string targetMember) {
    voteua  := targetMember + "-" + voteID + " initiator"
    voteoa  := targetMember + "-" + voteID + " vote attr"
    voteobj := targetMember + "-" + voteID + " vote"

    foreach child in getChildren(voteua) {
        deassign child from [voteua]
    }

    delete ua voteua
    delete o  voteobj
    delete oa voteoa
}

function updateAccountStatus(string accountID, string status) {
    accountUA := accountUsersNodeName(accountID)
    accountOA := accountAttributeNodeName(accountID)

    if status == "AUTHORIZED" {
        assign      accountUA to    ["authorized"]
        deassign    accountUA from  ["pending", "unauthorized"]

        // delete initiate vote prohibition that prevents the account from initiating votes on
        // other accounts when status is pending
        delete prohibition accountDenyLabel(accountUA)
    } else if status == "PENDING" {
        assign      accountUA to    ["pending"]
        deassign    accountUA from  ["authorized", "unauthorized"]

        delete prohibition accountDenyLabel(accountUA)
        denyPendingAccountsOnAccounts(accountUA, accountOA)
    } else {
        assign      accountUA to    ["unauthorized"]
        deassign    accountUA from  ["authorized", "pending"]

        delete prohibition accountDenyLabel(accountUA)
        denyPendingAccountsOnAccounts(accountUA, accountOA)
    }
}

function signMOU(string accountId) {
    accountUA := accountUsersNodeName(accountId)
    accountOA := accountAttributeNodeName(accountId)
    accountO  := accountObjectNodeName(accountId)

    if nodeExists(accountUA) {
        return
    }

    // create account ua, container, and object
    create ua accountUA assign to ["pending"]
    create oa accountOA assign to ["RBAC/accounts", "Status/accounts"]
    create o accountO assign to [accountOA]

    associate accountUA and accountOA with ["write_ato", "submit_feedback", "read_ato"]

    denyPendingAccountsOnAccounts(accountUA, accountOA)
}

function denyPendingAccountsOnAccounts(string accountUA, string accountOA) {
    create prohibition accountDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["initiate_vote", "submit_feedback", "read_ato"]
    on intersection of ["Status/accounts", !accountOA]
}

function accountUsersNodeName(string account) string {
    return account + " users"
}

function accountAttributeNodeName(string account) string {
    return account + " account"
}

function accountObjectNodeName(string account) string {
    return account + " target"
}

function accountDenyLabel(string accountUA) string {
    return "deny " + accountUA + " initiate_vote, submit_feedback, read_ato except on self"
}