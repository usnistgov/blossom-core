const ADMINMSP = "Org1MSP"

set resource access rights [
    "bootstrap",
    "update_mou",
    "get_mou",
    "sign_mou",
    "join",
    "write_ato",
    "read_ato",
    "submit_feedback",
    "initiate_vote",
    "vote",
    "certify_vote",
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
        "Blossom Admin"         and "RBAC/blossom_target"   with ["bootstrap", "update_mou"]
        // this association is with the blossom target because these permissions are never lost
        // association directly with the OA applies the association to all policy classes the OA is contained in
        "Authorizing Official"  and "blossom_target"        with ["get_mou", "sign_mou", "join"]
        "Authorizing Official"  and "RBAC/votes"            with ["vote", "certify_vote", "initiate_vote"]
        "Authorizing Official"  and "RBAC/accounts"         with ["initiate_vote", "submit_feedback", "read_ato", "join"]
    }
}

// status policy
create pc "Status" {
    user attributes {
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
        "pending"       and "Status/accounts"       with ["submit_feedback", "read_ato", "write_ato"]
    }
}

create pc "Votes" {
    object attributes {
        "all votes"
    }

    associations {
        "Blossom Admin"         and "all votes" with ["certify_vote"]
        "Authorizing Official"  and "all votes" with ["vote", "initiate_vote"]
    }
}

// bootstrap adminmsp account
signMOU(ADMINMSP)
updateAccountStatus(ADMINMSP, "AUTHORIZED")

function initiateVote(string initiator, string targetMember) {
    initiatorUsers  := accountUsersNodeName(initiator)
    voteua          := voteInitiatorAttr(targetMember)

    assign initiatorUsers to [voteua]
}

function certifyVote(string targetMember) {
    voteua  := voteInitiatorAttr(targetMember)

    foreach child in getChildren(voteua) {
        deassign child from [voteua]
    }
}

function updateAccountStatus(string accountId, string status) {
    accountUA := accountUsersNodeName(accountId)
    accountOA := accountAttributeNodeName(accountId)

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
        createAccountDeny(accountUA, accountOA)
    } else {
        assign      accountUA to    ["unauthorized"]
        deassign    accountUA from  ["authorized", "pending"]

        delete prohibition accountDenyLabel(accountUA)
        createAccountDeny(accountUA, accountOA)
    }

    // if there are no authorized accounts, grant the ADMINMSP "initiate_vote" on themselves
    accountUA = accountUsersNodeName(ADMINMSP)
    if noAuthorizedAccounts() {
        associate accountUA and "Status/accounts" with ["initiate_vote"]
        delete prohibition initiateVoteDenyLabel(accountUA)
    } else {
        accountOA = accountAttributeNodeName(ADMINMSP)

        dissociate accountUA and ["Status/accounts"]
        delete prohibition initiateVoteDenyLabel(accountUA)
        createInitiateVoteDeny(accountUA, accountOA)
    }
}

function noAuthorizedAccounts() bool {
    foreach x in getChildren("authorized") {
        return false
    }

    return true
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

    // create vote attr and object for this account
    voteua  := voteInitiatorAttr(accountId)
    voteoa  := voteObjAttr(accountId)
    voteobj := accountId + " vote"

    create ua voteua  assign to ["Votes"]
    create oa voteoa  assign to ["RBAC/votes", "Status/votes", "all votes"]
    create o  voteobj assign to [voteoa]

    associate accountUA and voteoa with ["vote"]
    associate voteua    and voteoa with ["certify_vote"]

    createAccountDeny(accountUA, accountOA)
    createInitiateVoteDeny(accountUA, accountOA)
}

// account deny happens only when not authorized
function createAccountDeny(string accountUA, string accountOA) {
    create prohibition accountDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["submit_feedback", "read_ato"]
    on intersection of ["Status/accounts", !accountOA]
}

// initiate vote deny happens all the time except for the ADMINMSP when there are no authorized users
function createInitiateVoteDeny(string accountUA, string accountOA) {
    create prohibition initiateVoteDenyLabel(accountUA)
    deny user attribute accountUA
    access rights ["initiate_vote"]
    on intersection of ["Status/accounts", accountOA]
}

function voteObjAttr(string account) string {
    return account + " vote attr"
}

function voteInitiatorAttr(string account) string {
    return account + " initiator"
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
    return "deny " + accountUA + " submit_feedback, read_ato except on self"
}

function initiateVoteDenyLabel(string accountUA) string {
    return "deny " + accountUA + " initiate_vote on self"
}