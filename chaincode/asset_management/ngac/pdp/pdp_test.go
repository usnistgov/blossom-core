package pdp

import (
	"github.com/stretchr/testify/require"
	"github.com/usnistgov/blossom/chaincode/asset_management/adminmsp"
	"github.com/usnistgov/blossom/chaincode/asset_management/collections"
	"github.com/usnistgov/blossom/chaincode/asset_management/mocks"
	"testing"
)

func TestInitCatalogNGAC(t *testing.T) {
	ctx := mocks.NewCtx()

	ctx.CreateCollection(collections.Catalog(), []string{adminmsp.AdminMSP}, []string{adminmsp.AdminMSP})
	err := ctx.SetClientIdentity(mocks.Super)
	require.NoError(t, err)

	err = InitCatalogNGAC(ctx)
	require.NoError(t, err)

	err = ctx.SetClientIdentity(mocks.Org2SystemOwner)
	require.NoError(t, err)

	err = InitCatalogNGAC(ctx)
	require.Error(t, err)
}
