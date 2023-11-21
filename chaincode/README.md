# Blossom Smart Contracts

This package contains the code for the Blossom Smart Contracts. There are two Smart Contracts: Authorization and Assets (WIP).

## Common elements

### Status
In general, an account can be authorized, pending, or unauthorized. The status an account has determines what actions they can perform in the Blossom system. The available statuses are:

- **PENDING**
- **AUTHORIZED**
- **UNAUTHORIZED**

#### User Registration

Below are examples of registering a user with Blossom roles as attributes.

_Note: The users MSPID is determined by the Fabric CA the identity is registered with._

- Using the node sdk

  ```javascript  
  // create an organization system owner  
  const secret = await caClient.register({     
	affiliation: '',     
	enrollmentID: 'org1_sys_owner', 
	role: 'client',
	[         
		{name: 'blossom.role', value: 'SystemOwner', ecert: true}
	]  
  }, adminUser);  
  ```  
- Using the CLI

  ```shell
  # Create a system owner  
  ./fabric-ca-client register ... --id.attrs 'blossom.role=SystemOwner' ...  
  ```

## Authorization

### Overview
There are three contracts defined in the authorization chaincode:

- **[bootstrap](./authorization/src/main/java/contract/BootstrapContract.java)** - Initialize the NGAC policy and Blossom Admin account.
- **[mou](./authorization/src/main/java/contract/MOUContract.java)** - Update, retrieve and sign the Blossom MOU. Officially join the network. 
- **[ato](./authorization/src/main/java/contract/ATOContract.java)** - Create, update, and provide feedback on ATOs.
- **[account](./authorization/src/main/java/contract/AccountContract.java)** - Retrieve account information including status.
- **[vote](./authorization/src/main/java/contract/VoteContract.java)** - Vote on Blossom member statuses.

### Roles
There is only one end user role supported by the Blossom authorization chaincode: `Authorizing Official`.

### Next Generation Access Control  (NGAC)
NGAC provides a layer of access control to the Blossom chaincode, ensuring operations are performed only by authorized users. NGAC uses **resource access rights** to refer to the set of operations possible on NGAC resources. The resource access rights supported by the PDP are:

- bootstrap
- update_vote_config
- update_mou
- get_mou
- sign_mou
- join
- write_ato
- initiate_vote
- vote
- abort_vote
- certify_vote
- submit_feedback

These access rights are defined in the policy, and checked by the PDP. They are transparent to the chaincode business logic. The NGAC policy is defined in the [policy.pml](./authorization/src/main/resources/policy.pml) file embedded in the chaincode resource folder.

##### Blossom Admin
Blossom Admin refers to the entity in the Blossom network that is responsible for bootstrapping the network. Specifically, the users with the `Authorizing Official` role within this member. 
These users are granted extra permissions in the NGAC policy in order to maintain smooth operation of the Blossom system. 
The Blossom Admin account ID (MSPID) is hardcoded in the [NGAC policy](./authorization/src/main/resources/policy.pml) defined as the ADMINMSP constant. 
This is the policy loaded during bootstrapping. Hardcoding the Blossom Admin account ID in the policy ensures that any changes to the AdminMSP 
value requires review and approval from the rest of the network.

### Build and Deploy

#### Prerequisites
##### Setting Blossom Admin Membership Service Provider ID (MSPID)

The first step before doing anything else is to set the Administrative MSPID in the code. This will ensure  
all peers that install the chaincode will have the same Admin MSPID set. If two peers have different values for the Admin MSPID,  
their packages will have different hashes and will fail the commit stage for approving two different packages.

1. In [./authorization/src/main/resources/policy.pml](./authorization/src/main/resources/policy.pml) set the value of `ADMINMSP` to the MSPID of the Blossom Admin member.

   **Example:** `const ADMINMSP = "SAMS-MSPID"`

##### Lifecycle Endorsement Policy

In the `configtx.yaml` used to create the channel. Modify the `Application > Policies > LifecycleEndorsement` policy to:

```yaml  
LifecycleEndorsement:  
  Type: Signature  Rule: “AND('SAMS-MSPID.member', OutOf(2, 'NIST-MSPID.member', 'DHS-MSPID.member'))"  
```  

The `OutOf` function will need to be updated everytime an organization is added to the network. The new organization should  
be added to the list (i.e. `OutOf(2, 'NIST-MSPID.member', 'DHS-MSPID.member', NewOrg-MSPID.member)`) and the `2` should be updated  
to ensure it is a majority of the members in the list.

#### Steps
1. Build the Java chaincode.

   ```
   make clean-auth-cc build-auth-cc
   ```

   This will clean any existing chaincode build and create a new build folder in `/chaincode/authorization/build`.  The `build-auth-cc` target uses a docker container with java 11 and gradle to build the chaincode since Fabric only supports up to Java 11.


2. Package chaincode on each peer.

   ```shell  
   peer lifecycle chaincode package authorization.tar.gz --path ../../chaincode/authorization/build/install/authorization --lang java --label authorization_1
   ```  

   This will package the chaincode into a file called `authorization.tar.gz`.  Notice the `--path` arg points to the `/install/authorization` folder inside the `/build` folder created in **step 1**.


3. Install chaincode on each peer.

   ```shell  
   peer lifecycle chaincode install authorization.tar.gz   
   ``` 


4. Get chaincode package ID.

   ```shell  
   peer lifecycle chaincode queryinstalled   
   ```  

   Look for the label that matches the `--label` arg in **step 2**. The output should look something like:


5. Approve chaincode definition.

   ```shell  
   peer lifecycle chaincode approveformyorg \
	   -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com \
	   --tls --cafile $ORDERER_CA \
	   --channelID authorization --name authorization \
	   --version 1 --sequence 1 \
	   --package-id <package id from step 4>
   ```  

   This command will need to be executed by enough organizations to satisfy the [policy](#lifecycle-endorsement-policy) defined in the channel's `configtx.yaml`.


6. Check commit readiness.

   ```shell  
   peer lifecycle chaincode checkcommitreadiness --channelID $CHANNEL --name authorization --version 1.0 --sequence 1 --tls --cafile $ORDERER_CA --output json
   ```  

   This command will show which organizations on the channel have approved the chaincode and which ones haven't.


7. Commit chaincode.

   ```shell  
   peer lifecycle chaincode commit \    
   -o $ORDERER \    
   --tls --cafile $ORDERER_CA \    
   --channelID $CHANNEL --name authorization \    
   --peerAddresses <PEER_ADDRESS> --tlsRootCertFiles <path to peer tls ca cert> \    
   --version 1.0 --sequence 1
   ```  

The `--peerAddresses`  arg specifies 1 or more peers that **have approved the chaincode** to target for the commit transaction.  This is when the [lifecycle endorsement policy](#lifecycle-endorsement-policy) will be checked. An endorsement policy error will be returned if not enough organizations have approved the chaincode to satisfy the policy.

### Invoke
There are three contracts defined in the authorization chaincode: **bootstrap**, **account**, and **vote**. In order to invoke a contract function, you will need to specify the chaincode name, contract name, and function name.

- Command line - Specify the chaincode name using the `-n` arg. Prepend the contract name and a semi colon to the function name in the `-c` arg.
  ```shell
  -n authorization -c '{"function":"account:GetAccounts","Args":[]}'
  ```
- Node sdk - Specify the chaincode name and contract name in the `fabric-network.Network#getContract` method. Then pass the function name to `fabric-network.Contract#submitTransaction`.
  ```node
  // authorization = chaincode name
  // account = "contract name"
  let contract = network.getContract("authorization", "account");
  contract.submitTransaction("GetAccounts")
  ```

#### `--peerAddresses`
- 1 or more peers that have approved the chaincode to target for invoke.
- This is only needed if more than one peer is needed for endorsement.
- If an org did not approve the chaincode, they will need to target a org that did or else an error will occur.
- If an org did approve the chaincode, they do not need to target another peer.

## Assets
WIP
