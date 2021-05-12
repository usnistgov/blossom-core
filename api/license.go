package api

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
	"github.com/hyperledger/fabric-protos-go/ledger/queryresult"
	"github.com/pkg/errors"
	"github.com/usnistgov/blossom/chaincode/model"
	"github.com/usnistgov/blossom/chaincode/ngac/pdp"
	"strings"
	"time"
)

type (
	// LicenseInterface provides the functions to interact with Licenses in fabric.
	LicenseInterface interface {
		// OnboardLicense adds a new license to Blossom.  This will create a new license object on the ledger and in the
		// NGAC graph. Licenses are identified by the LicenseNumber field. The user performing the request will need to
		// have permission to add a license to the ledger/NGAC. The license will be an object attribute in NGAC and the
		// license keys will be objects that are assigned to the license.
		OnboardLicense(ctx contractapi.TransactionContextInterface, license *model.License) error

		// OffboardLicense removes an existing license in Blossom.  This will remove the license from the ledger
		// and from NGAC.
		// TODO this should revoke any outstanding license keys
		OffboardLicense(ctx contractapi.TransactionContextInterface, licenseID string) error

		// Licenses returns all licenses in Blossom. This information does not include which agencies have keys for each
		// license
		Licenses(ctx contractapi.TransactionContextInterface) ([]*model.License, error)

		// LicenseInfo returns the info for the license with the given license ID.
		LicenseInfo(ctx contractapi.TransactionContextInterface, licenseID string) (*model.License, error)

		// CheckoutLicense requests a software license for an agency.  The requesting user must have permission to request
		// (i.e. System Administrator). The amount parameter is the amount of software license keys the agency is requesting.
		// This number is subtracted from the total available for the license. Return the set of keys that are now assigned to
		// the agency.
		CheckoutLicense(ctx contractapi.TransactionContextInterface, licenseID string, agency string, amount int) (map[string]time.Time, error)

		// CheckinLicense returns the license keys to Blossom.  The return of these keys is reflected in the amount available for
		// the license, and the keys assigned to the agency on the ledger.
		CheckinLicense(ctx contractapi.TransactionContextInterface, licenseID string, returnedKeys []string, agencyName string) error
	}
)

const LicensePrefix = "license:"

func NewLicenseContract() LicenseInterface {
	return &BlossomSmartContract{}
}

// LicenseKey returns the key for a license on the ledger.  Licenses are stored with the format: "license:<license_name>".
func LicenseKey(id string) string {
	return fmt.Sprintf("%s%s", LicensePrefix, id)
}

func (b *BlossomSmartContract) licenseExists(ctx contractapi.TransactionContextInterface, licenseID string) (bool, error) {
	data, err := ctx.GetStub().GetState(model.AgencyKey(licenseID))
	if err != nil {
		return false, errors.Wrapf(err, "error checking if license %q already exists on the ledger", licenseID)
	}

	return data != nil, nil
}

func (b *BlossomSmartContract) OnboardLicense(ctx contractapi.TransactionContextInterface, license *model.License) error {
	if ok, err := b.licenseExists(ctx, license.ID); err != nil {
		return errors.Wrapf(err, "error checking if license already exists")
	} else if ok {
		return errors.Errorf("a license with the ID %q already exists", license.ID)
	}

	if err := pdp.NewLicenseDecider().OnboardLicense(ctx, license); err != nil {
		return errors.Wrapf(err, "error onboarding license %q", license.Name)
	}

	// at the time of onboarding all keys are available
	license.AvailableKeys = license.AllKeys
	license.OnboardingDate = time.Now()
	license.CheckedOut = make(map[string]map[string]time.Time)

	// convert license to bytes
	bytes, err := json.Marshal(license)
	if err != nil {
		return errors.Wrapf(err, "error marshaling license %q", license.Name)
	}

	// add license to world state
	if err = ctx.GetStub().PutState(LicenseKey(license.ID), bytes); err != nil {
		return errors.Wrapf(err, "error adding license to ledger")
	}

	return nil
}

func (b *BlossomSmartContract) OffboardLicense(ctx contractapi.TransactionContextInterface, licenseID string) error {
	if ok, err := b.licenseExists(ctx, licenseID); err != nil {
		return errors.Wrapf(err, "error checking if license exists")
	} else if !ok {
		return nil
	}

	if err := pdp.NewLicenseDecider().OffboardLicense(ctx, licenseID); err != nil {
		return errors.Wrapf(err, "error onboarding license %q", licenseID)
	}

	// remove license from world state
	if err := ctx.GetStub().DelState(LicenseKey(licenseID)); err != nil {
		return errors.Wrapf(err, "error offboarding license from blossom")
	}

	return nil
}

func (b *BlossomSmartContract) Licenses(ctx contractapi.TransactionContextInterface) ([]*model.License, error) {
	licenses, err := licenses(ctx)
	if err != nil {
		return nil, errors.Wrapf(err, "error getting licenses")
	}

	return licenses, nil
}

func licenses(ctx contractapi.TransactionContextInterface) ([]*model.License, error) {
	resultsIterator, err := ctx.GetStub().GetStateByRange("", "")
	if err != nil {
		return nil, err
	}
	defer resultsIterator.Close()

	var licenses []*model.License
	for resultsIterator.HasNext() {
		var queryResponse *queryresult.KV
		if queryResponse, err = resultsIterator.Next(); err != nil {
			return nil, err
		}

		// licenses on the ledger begin with the license prefix -- ignore other assets
		if !strings.HasPrefix(queryResponse.Key, model.LicensePrefix) {
			continue
		}

		var asset model.License
		if err = json.Unmarshal(queryResponse.Value, &asset); err != nil {
			return nil, err
		}

		licenses = append(licenses, &asset)
	}

	return licenses, nil
}

func (b *BlossomSmartContract) LicenseInfo(ctx contractapi.TransactionContextInterface, licenseID string) (*model.License, error) {
	if ok, err := b.licenseExists(ctx, licenseID); err != nil {
		return nil, errors.Wrapf(err, "error checking if license exists")
	} else if !ok {
		return nil, errors.Errorf("a license with the ID %q does not exist", licenseID)
	}

	var (
		license *model.License
		bytes   []byte
		err     error
	)

	if bytes, err = ctx.GetStub().GetState(model.LicenseKey(licenseID)); err != nil {
		return nil, errors.Wrapf(err, "error getting license from ledger")
	}

	if err = json.Unmarshal(bytes, license); err != nil {
		return nil, errors.Wrapf(err, "error deserializing license")
	}

	return license, nil
}

func (b *BlossomSmartContract) CheckoutLicense(
	ctx contractapi.TransactionContextInterface,
	licenseID string,
	agencyName string,
	amount int) (map[string]time.Time, error) {

	var (
		license *model.License
		agency  *model.Agency
		err     error
	)

	// get agency
	if agency, err = b.Agency(ctx, agencyName); err != nil {
		return nil, errors.Wrapf(err, "error getting agency %q", agencyName)
	}

	// get license
	if license, err = b.LicenseInfo(ctx, licenseID); err != nil {
		return nil, errors.Wrapf(err, "error getting info for license %q", licenseID)
	}

	// check that the amount requested is less than the amount available
	if amount > license.Available {
		return nil, errors.Errorf("requested amount (%v) cannot be greater than the available amount (%v)",
			amount, license.Available)
	}

	// update available amount
	license.Available -= amount

	// get the available keys
	fromAvailable := license.AvailableKeys[0:amount]
	// update available keys
	license.AvailableKeys = license.AvailableKeys[amount:]

	// create the array of keys that are checked out including expiration dates
	checkedOutKeys := make(map[string]time.Time, 0)
	expiration := time.Now().AddDate(1, 0, 0)
	for _, key := range fromAvailable {
		// set the expiration of the license key to one year from now
		checkedOutKeys[key] = expiration
	}

	// update the agency licenses
	// add to existing keys if they are checking out more of a license
	checkedOutLicenseKeys := agency.Licenses[licenseID]
	if checkedOutLicenseKeys == nil {
		checkedOutLicenseKeys = checkedOutKeys
	} else {
		for key, coKey := range checkedOutKeys {
			checkedOutLicenseKeys[key] = coKey
		}
	}

	// update license in the agency
	agency.Licenses[licenseID] = checkedOutLicenseKeys

	// update the licenses agency tracker
	license.CheckedOut[agencyName] = checkedOutLicenseKeys

	// update agency
	var bytes []byte
	if bytes, err = json.Marshal(agency); err != nil {
		return nil, errors.Wrapf(err, "error marshaling agency %q", agency.Name)
	}

	if err = ctx.GetStub().PutState(model.AgencyKey(agency.Name), bytes); err != nil {
		return nil, errors.Wrapf(err, "error updating agency state")
	}

	// update license
	if bytes, err = json.Marshal(license); err != nil {
		return nil, errors.Wrapf(err, "error marshaling license %q", license.ID)
	}

	if err = ctx.GetStub().PutState(model.LicenseKey(license.Name), bytes); err != nil {
		return nil, errors.Wrapf(err, "error updating license state")
	}

	return checkedOutKeys, nil
}

func (b *BlossomSmartContract) CheckinLicense(ctx contractapi.TransactionContextInterface, licenseID string, returnedKeys []string, agencyName string) error {
	var (
		license *model.License
		agency  *model.Agency
		err     error
	)

	// get agency
	if agency, err = b.Agency(ctx, agencyName); err != nil {
		return errors.Wrapf(err, "error getting agency %q", agencyName)
	}

	// get license
	if license, err = b.LicenseInfo(ctx, licenseID); err != nil {
		return errors.Wrapf(err, "error getting info for license %q", licenseID)
	}

	// update the agency
	if err = updateAgencyOnCheckIn(agency, licenseID, returnedKeys); err != nil {
		return errors.Wrapf(err, "error updating agency")
	}

	// update the license
	if err = updateLicenseOnCheckIn(license, agencyName, returnedKeys); err != nil {
		return errors.Wrapf(err, "error updating license")
	}

	return nil
}

func updateAgencyOnCheckIn(agency *model.Agency, licenseID string, returnedKeys []string) error {
	checkedOutLicense := agency.Licenses[licenseID]
	for _, returnedKey := range returnedKeys {
		// check that the returned key is leased to the agency
		if _, ok := agency.Licenses[licenseID][returnedKey]; !ok {
			return errors.Errorf("returned key %s was not checked out by %s", returnedKey, agency.Name)
		}

		delete(checkedOutLicense, returnedKey)
	}

	// if all keys were returned remove license from agency
	if len(checkedOutLicense) == 0 {
		delete(agency.Licenses, licenseID)
	}

	// update agency keys
	agency.Licenses[licenseID] = checkedOutLicense

	return nil
}

func updateLicenseOnCheckIn(license *model.License, agencyName string, returnedKeys []string) error {
	agencyCheckedOut, ok := license.CheckedOut[agencyName]
	if !ok {
		return errors.Errorf("agency %s has not checked out any keys for license %s", agencyName, license.ID)
	}

	for _, returnedKey := range returnedKeys {
		// check that the agency has the key checked out
		if _, ok = agencyCheckedOut[returnedKey]; !ok {
			return errors.Errorf("returned key %s was not checked out by %s", returnedKey, agencyName)
		}

		// remove the returned key from the checked out keys
		delete(agencyCheckedOut, returnedKey)

		// add the returned key to the available keys
		license.AvailableKeys = append(license.AvailableKeys, returnedKey)
	}

	// if all keys are returned, remove the agency from the license
	if len(agencyCheckedOut) == 0 {
		delete(license.CheckedOut, agencyName)
	}

	// update number of available keys
	license.Available += len(returnedKeys)

	return nil
}
