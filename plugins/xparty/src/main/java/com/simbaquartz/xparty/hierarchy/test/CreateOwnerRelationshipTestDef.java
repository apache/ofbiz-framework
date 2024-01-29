

package com.simbaquartz.xparty.hierarchy.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

/* WAD-228: Service to establish an internal organization account lead for the partner
* PartyGroup: createOwnerRelationship
*/
public class CreateOwnerRelationshipTestDef extends OFBizTestCase {

	public CreateOwnerRelationshipTestDef(String name) {
		super(name);
	}

    // Make FSD.sales.rep1 an owner of FSD
	public void testSuccessfulRelationshipUsingFSD() throws Exception {
		doTest("forrest.rae", "FSD.sales.rep1", "FSD");
	}

    // Make MP.sales.exec an owner of MP
	public void testSuccessfulRelationshipUsingSalesExec() throws Exception {
		doTest("forrest.rae", "MP.sales.exec", "MP");
	}

    // Fails because FSD.sales.exec is not an owner.
    public void testFailureRelationshipUsingSalesExec() throws Exception {
        doPermissionFailureTest("FSD.sales.exec", "MP.sales.exec", "MP");
    }

    // Fails because cberger@ex.com is not an owner.
    public void testFailureRelationshipUsingInternalManager() throws Exception {
        doPermissionFailureTest("cberger@ex.com", "FSD.director1", "FSD");
    }


	public void doTest(String userLoginId, String partyId, String partyGroupPartyId) throws Exception {
		String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

		Map<String, Object> resp = dispatcher.runSync("createOwnerRelationship",
                UtilMisc.toMap("userLogin", userLogin,
                        "partyId", partyId,
                        "partyGroupPartyId", partyGroupPartyId));
		assertTrue(ServiceUtil.isSuccess(resp));

		// Check if the relationship was successfully created
		List<GenericValue> prtyRelationships = delegator.findByAnd("PartyRelationship",
                UtilMisc.toMap("partyIdTo", partyId,
                        "partyIdFrom", partyGroupPartyId,
				        "roleTypeIdTo", "OWNER",
				        "roleTypeIdFrom", "_NA_",
				        "partyRelationshipTypeId", "OWNER"),
                null, true);
		int rowsReturned = prtyRelationships.size();
		assertTrue("Party Relationship result size was expected to be greater than 0", rowsReturned > 0);
	}

    public void doPermissionFailureTest(String userLoginId, String partyId, String partyGroupPartyId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("partyGroupPartyId", partyGroupPartyId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("createOwnerRelationship", ctx);
        }
        catch (ServiceAuthException e)
        {
            Debug.logInfo("Correctly rejected (ServiceAuthException) due to permission-service returning hasPermission = false.", module);
            assertTrue(true);
            return;
        }
        // If we made it here, it's an error.
        fail("Incorrectly allowed to run due to permission-service incorrectly returning hasPermission = true.");
    }


}
