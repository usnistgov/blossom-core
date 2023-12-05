# Authorization Chaincode

Chaincode functions to handle Blossom authorization.

## Build Chaincode
From `blossom-core/chaincode/authorization`:
 ```
 ./gradlew shadowJar
 ```
This will build a jar file containing the chaincode and the dependencies at `blossom-core/chaincode/authorization/build/libs/authorization.jar`. 
When packaging the chaincode point to `blossom-core/chaincode/authorization/build/libs`.

To rebuild the `/libs` folder, run `./gradlew build --build-file build-libs

### Build using docker
To build the chaincode using java 11 and gradle 5.6.2, from `blossom-core/chaincode/authorization/` run:
```
make build-auth
```

This will create the shadowJar in `blossom-core/chaincode/authorization/build/libs/authorization.jar` and rebuild the libs
directory `blossom-core/chaincode/authorization/build/libs`.


## Authorization Statuses

- AUTHORIZED
- PENDING
- NOT_AUTHORIZED

## Chaincode Functions

- bootstrap
  - Bootstrap
- account
  - GetAccounts
  - GetAccount
  - GetAccountStatus
  - GetAccountHistory
- ato
  - CreateATO
  - UpdateATO
  - SubmitFeedback
- mou
  - UpdateMOU
  - GetMOU
  - GetMOUHistory
  - SignMOU
  - Join
- vote
  - InitiateVote 
  - Vote
  - CertifyOngoingVote
  - GetOngoingVote
  - GetVoteHistory

## ATO and Feedback Transient Data

The inputs for `CreateATO`, `UpdateATO` and `SubmitFeedback` in the `ATOContract` must be embedded in the transient data field of the request.
This will ensure the inputs are not attached to the transaction allowing unauthorized members from reading them.
 
### `CreateATO` and `UpdateATO`
```json
{
  "memo": "memo text", 
  "artifacts": "artifacts text"
}
```

### `SubmitFeedback`
```json
{
  "targetAccountId": "target id", 
  "atoVersion": "ato version #", 
  "comments": "comments text"
}
```

## Voting System

- There can only be one ongoing vote at a time. 
- If the Blossom Admin is voted to a status other than AUTHORIZED, subsequent votes with any other member as a target will 
fail until there is a vote to reauthorize the Blossom Admin. 
- Members cannot initiate a vote on themselves regardless of status, including the Blossom Admin. The only exception is 
if there are no other authorized members. In this case, the Blossom Admin will be granted temporary privileges to initiate a vote on themselves. Once authorized, they will lose the privileges.
- Only AUTHORIZED members at the time `InitiateVote` is called can participate in a vote. 
- Only Blossom Admin and the member that initiated a vote can call `CertifyOngoingVote`. If the Blossom Admin is not authorized,
they will not be able to certify.
- Members can vote for themselves.

## Common Workflows
### Bootstrap

- bootstrap:Bootstrap
- mou:UpdateMOU

### Joining

- mou:GetMOU
- mou:SignMOU
- ato:CreateATO
- ato:SubmitFeedback
- ato:UpdateATO
- vote:InitiateVote
- vote:Vote
- vote:CertifyVote
- mou:Join

### ATO Process

- ato:CreateATO
- ato:SubmitFeedback
- ato:UpdateATO
- ato:SubmitFeedback

### Voting

- vote:InitiateVote
- vote:Vote
- vote:CertifyVote
