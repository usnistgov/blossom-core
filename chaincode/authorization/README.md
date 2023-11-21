
## Chaincode invocation

For Java chaincode, to invoke a function in a contract class, prepend the contract name to the function name. For example `account:GetAccounts` calls the `GetAccounts` function
of the `account` contract class. Even though the contract classes have different names they are considered part of the same chaincode and therefore operate on the same fabric namespace on the channel.

The contract names are:
    - account
    - ato
    - bootstrap
    - mou
    - vote

An example invocation:
```bash
peer chaincode query -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls \
--cafile ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem \
-C authorization -n authorization \
--peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt \
-c '{"function":"account:GetAccounts","Args":[]}'
```


## Common Workflows
### Bootstrap

- bootstrap:Bootstrap
- mou:UpdateMOU

### Joining

- mou:GetMOU
- mou:SignMOU
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