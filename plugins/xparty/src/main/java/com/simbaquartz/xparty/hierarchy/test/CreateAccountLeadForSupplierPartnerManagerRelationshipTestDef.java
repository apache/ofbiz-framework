

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

public class CreateAccountLeadForSupplierPartnerManagerRelationshipTestDef extends OFBizTestCase {

	public CreateAccountLeadForSupplierPartnerManagerRelationshipTestDef(String name) {
		super(name);
	}

	public void testSuccessfulScenarioForManager1() throws Exception {
		doTest("FSD.director1", "FSD.manager1", "MP.manager1", "SALES_REP");
	}

    public void testSuccessfulScenarioForManager2() throws Exception {
        doTest("FSD.manager1", "FSD.manager2", "MP.manager1", "SHIPMENT_CLERK");
    }

    public void testPermissionFailureScenarioForSalesRep() throws Exception {
        doPermissionFailureTest("FSD.sales.rep1", "FSD.director1", "MP.manager1", "MANAGER");
    }

	public void testNonManagerRoleFailureCase() throws Exception {
		doRoleFailureTest("FSD.director1", "FSD.manager1", "MP.sales.rep1", "SALES_REP");
	}

	public void doTest(String userLoginId, String acountLeadPartyId, String managerPartyId, String roleTypeIdTo) throws Exception {
		String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

		Map<String, Object> resp = dispatcher.runSync("createAccountLeadForSupplierPartnerManagerRelationship",
                UtilMisc.toMap("userLogin", userLogin,
                        "accountLeadPartyId", acountLeadPartyId,
                        "managerPartyId", managerPartyId,
                        "roleTypeIdTo", roleTypeIdTo));
		assertTrue(ServiceUtil.isSuccess(resp));

		// Check if the relationship was successfully created
		List<GenericValue> prtyRelationships = delegator.findByAnd("PartyRelationship",
				UtilMisc.toMap("partyIdTo", managerPartyId,
						"partyIdFrom", acountLeadPartyId,
						"roleTypeIdTo", roleTypeIdTo,
						"roleTypeIdFrom", "ACCOUNT_LEAD",
						"partyRelationshipTypeId", "ACCOUNT"),
				null, true);
		int rowsReturned = prtyRelationships.size();
		assertTrue("Party Relationship result size was expected to be greater than 0", rowsReturned > 0);
	}

	public void doPermissionFailureTest(String userLoginId, String accountLeadPartyId, String managerPartyId, String roleTypeIdTo) throws Exception
	{
		Map<String, Object> ctx = new HashMap<String, Object>();
		String module = Thread.currentThread().getStackTrace()[2].getMethodName();
		GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

		ctx.put("userLogin", userLogin);
		ctx.put("accountLeadPartyId", accountLeadPartyId);
		ctx.put("managerPartyId", managerPartyId);
		ctx.put("roleTypeIdTo", roleTypeIdTo);

		Map<String, Object> resp = null;
		try
		{
			resp = dispatcher.runSync("createAccountLeadForSupplierPartnerManagerRelationship", ctx);
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

	public void doRoleFailureTest(String userLoginId, String acountLeadPartyId, String managerPartyId, String roleTypeIdTo ) throws Exception {
		String module = Thread.currentThread().getStackTrace()[2].getMethodName();
		GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

		Map<String, Object> resp = dispatcher.runSync("createAccountLeadForSupplierPartnerManagerRelationship",
				UtilMisc.toMap("userLogin", userLogin,
						"accountLeadPartyId", acountLeadPartyId,
						"managerPartyId", managerPartyId,
						"roleTypeIdTo", roleTypeIdTo));
		assertTrue("Service didn't return failure as expected",ServiceUtil.isFailure(resp)); // Expect Failure from service
	}
}
