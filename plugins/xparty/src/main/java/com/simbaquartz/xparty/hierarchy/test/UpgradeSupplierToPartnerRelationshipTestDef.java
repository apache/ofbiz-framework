

package com.simbaquartz.xparty.hierarchy.test;

import java.util.Map;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

public class UpgradeSupplierToPartnerRelationshipTestDef extends OFBizTestCase {

    public UpgradeSupplierToPartnerRelationshipTestDef(String name) {
        super(name);
    }

    public void testUpgradeSupplierToPartnerRelationship_AcmeSupplier() throws Exception
    {
        doTest("forrest.rae",
                "ACME");
    }
    
    public void testUpgradeSupplierToPartnerRelationship_PermissionFailure_AcmeSupplier() throws Exception
    {
        doPermissionFailureTest("FSD.sales.rep1",// This causes the expected failure, sales rep trying to upgrade relationship
                "ACME");
    }

    public void doTest(String userLoginId, String supplierPartyId) throws Exception
    {
    	GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
    	String internalOrgPartyId = UtilProperties.getPropertyValue("general", "ORGANIZATION_PARTY");
		
    	Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin, "supplierPartyId", supplierPartyId);
		
		Map<String, Object> resp = dispatcher.runSync("upgradeSupplierToPartnerRelationship", ctx);

		// check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

		// Check if the relationship was successfully created
		List<GenericValue> prtyRelationships = delegator.findByAnd(
				"PartyRelationship", UtilMisc.toMap("partyIdTo",
						supplierPartyId, "partyIdFrom", internalOrgPartyId, "roleTypeIdTo",
						"PARTNER", "roleTypeIdFrom", "DISTRIBUTOR",
						"partyRelationshipTypeId", "PARTNERSHIP"), null, true);
		
		int rowsReturned = prtyRelationships.size();
		
		assertTrue(rowsReturned > 0);
    }

    public void doPermissionFailureTest(String userLoginId, String supplierPartyId) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin, "supplierPartyId", supplierPartyId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("upgradeSupplierToPartnerRelationship", ctx);
        }
        catch (org.apache.ofbiz.service.ServiceAuthException e)
        {
            Debug.logInfo("Correctly rejected (ServiceAuthException) due to permission-service returning hasPermission = false.", module);
            assertTrue(true);
            return;
        }
        // If we made it here, it's an error.
        fail("Incorrectly allowed to run due to permission-service incorrectly returning hasPermission = true.");
    }
}
