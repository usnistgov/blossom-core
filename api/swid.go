package main

import (
	"encoding/json"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/hyperledger/fabric/protos/ledger/queryresult"
	"github.com/pkg/errors"
	"github.com/usnistgov/blossom/chaincode/model"
	"github.com/usnistgov/blossom/chaincode/ngac/pdp"
	"strings"
)

type (
	// SwIDInterface provides the functions to interact with SwID tags in fabric.
	SwIDInterface interface {
		// ReportSwID is used by Agencies to report to Blossom when a software user has installed a piece of software associated
		// with an asset that agency has checked out. This function will invoke NGAC chaincode to add the SwID to the NGAC graph.
		ReportSwID(stub shim.ChaincodeStubInterface, swid *model.SwID, agency string) error

		// GetSwID returns the SwID object including the XML that matches the provided primaryTag parameter.
		GetSwID(stub shim.ChaincodeStubInterface, primaryTag string) (*model.SwID, error)

		// GetSwIDsAssociatedWithAsset returns the SwIDs that are associated with the given asset.
		GetSwIDsAssociatedWithAsset(stub shim.ChaincodeStubInterface, asset string) ([]*model.SwID, error)
	}
)

func NewSwIDContract() SwIDInterface {
	return &BlossomSmartContract{}
}

func (b *BlossomSmartContract) swidExists(stub shim.ChaincodeStubInterface, primaryTag string) (bool, error) {
	data, err := stub.GetState(model.SwIDKey(primaryTag))
	if err != nil {
		return false, errors.Wrapf(err, "error checking if SwID with primary tag %q already exists on the ledger", primaryTag)
	}

	return data != nil, nil
}

func (b *BlossomSmartContract) ReportSwID(stub shim.ChaincodeStubInterface, swid *model.SwID, agency string) error {
	if ok, err := b.swidExists(stub, swid.PrimaryTag); err != nil {
		return errors.Wrapf(err, "error checking if SwID with primary tag %s already exists", swid.PrimaryTag)
	} else if ok {
		return errors.Errorf("a SwID tag with the primary tag %s has already been reported", swid.PrimaryTag)
	}

	// begin NGAC
	if err := pdp.NewSwIDDecider().ReportSwID(stub, swid, agency); err != nil {
		return errors.Wrap(err, "error reporting SwID")
	}
	// end NGAC

	swidBytes, err := json.Marshal(swid)
	if err != nil {
		return errors.Wrapf(err, "error serializing swid tag")
	}

	if err = stub.PutState(model.SwIDKey(swid.PrimaryTag), swidBytes); err != nil {
		return errors.Wrapf(err, "error updating SwID %s", swid.PrimaryTag)
	}

	return nil
}

func (b *BlossomSmartContract) GetSwID(stub shim.ChaincodeStubInterface, primaryTag string) (*model.SwID, error) {
	if ok, err := b.swidExists(stub, primaryTag); err != nil {
		return nil, errors.Wrapf(err, "error checking if SwID with primary tag %s already exists", primaryTag)
	} else if ok {
		return nil, errors.Errorf("a SwID tag with the primary tag %s has already been reported", primaryTag)
	}

	var (
		swidBytes []byte
		err       error
	)

	if swidBytes, err = stub.GetState(model.SwIDKey(primaryTag)); err != nil {
		return nil, errors.Wrapf(err, "error getting SwID %s", primaryTag)
	}

	swid := &model.SwID{}
	if err = json.Unmarshal(swidBytes, swid); err != nil {
		return nil, errors.Wrapf(err, "error deserializing SwID tag %s", primaryTag)
	}

	// begin NGAC
	if err = pdp.NewSwIDDecider().FilterSwID(stub, swid); err != nil {
		return nil, errors.Wrap(err, "error filtering SwID")
	}
	// end NGAC

	return &model.SwID{}, nil
}

func (b *BlossomSmartContract) GetSwIDsAssociatedWithAsset(stub shim.ChaincodeStubInterface, asset string) ([]*model.SwID, error) {
	swids, err := b.getSwIDsAssociatedWithAsset(stub, asset)
	if err != nil {
		return nil, errors.Wrapf(err, "error retrieving swids from ledger")
	}

	// begin NGAC
	// filter out any swid tags the user cannot view
	if swids, err = pdp.NewSwIDDecider().FilterSwIDs(stub, swids); err != nil {
		return nil, errors.Wrap(err, "error filtering swids")
	}
	// end NGAC

	return swids, nil
}

func (b *BlossomSmartContract) getSwIDsAssociatedWithAsset(stub shim.ChaincodeStubInterface, asset string) ([]*model.SwID, error) {
	resultsIterator, err := stub.GetStateByRange("", "")
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	swids := make([]*model.SwID, 0)
	for resultsIterator.HasNext() {
		var queryResponse *queryresult.KV
		if queryResponse, err = resultsIterator.Next(); err != nil {
			return nil, err
		}

		// agencies on the ledger begin with the agency prefix -- ignore other assets
		if !strings.HasPrefix(queryResponse.Key, model.SwIDPrefix) {
			continue
		}

		swid := &model.SwID{}
		if err = json.Unmarshal(queryResponse.Value, swid); err != nil {
			return nil, err
		}

		// continue if the asset associated with this swid tag does not match the given asset ID
		if swid.Asset != asset {
			continue
		}

		swids = append(swids, swid)
	}

	return swids, nil
}
