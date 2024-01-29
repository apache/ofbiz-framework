package com.simbaquartz.xparty.hierarchy.test;

import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

public class CreateSubordinateForPartyRelationshipTestDef extends OFBizTestCase {

	public CreateSubordinateForPartyRelationshipTestDef(String name) {
		super(name);
	}
	
	public void testCreateSubordinateForPartyRelationship_SpecialPermission() throws Exception
    {
        doTest("forrest.rae", // Passes because forrest.rae has special permission check.
                "MP.manager1",
                "MP.sales.rep2",
                "MANAGER");
    }

    public void testCreateSubordinateForPartyRelationship_SalesExecCreateManager() throws Exception
    {
        doTest("MP.sales.exec",
                "MP.manager1",
                "hmalone@ex.com",
                "MANAGER");
    }

    public void testCreateSubordinateForPartyRelationship_SalesExecCreateSalesRep() throws Exception
    {
        doTest("MP.sales.exec",
                "hmalone@ex.com",
                "lcohen@ex.com",
                "MANAGER");
    }

	public void testCreateSubordinateForPartyRelationship_PermissionFailure_DifferentOrg1() throws Exception
    {
        doPermissionFailureTest("SP.manager1", //different partner SP.manager1 tries to create a subordinate party relationship, gets service auth exception
                                "MP.manager1",
                                "MP.sales.rep2",
                                "MANAGER");
    }

    public void testCreateSubordinateForPartyRelationship_PermissionFailure_DifferentOrg2() throws Exception
    {
        doPermissionFailureTest("MP.manager1", //different partner SP.manager1 tries to create a subordinate party relationship, gets service auth exception
                "MP.manager1",
                "SP.sales.rep1",
                "SALES_REP");
    }

	/*Service to establish a subordinate */
	public void doTest(String userLoginId, String managerPartyId, String subordinatePartyId, String subordinateRoleTypeId) throws Exception 
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"managerPartyId", managerPartyId, 
        			"subordinatePartyId", subordinatePartyId,
        			"subordinateRoleTypeId", subordinateRoleTypeId);
		
        Map<String, Object> resp = dispatcher.runSync("createSubordinateForPartyRelationship", ctx);

        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));
		
		// Check if the relationship was successfully created
		List<GenericValue> prtyRelationships = delegator.findByAnd("PartyRelationship", 
												UtilMisc.toMap(
														"partyIdFrom", managerPartyId, 
														"partyIdTo",subordinatePartyId, 
														"roleTypeIdTo", subordinateRoleTypeId,
														"roleTypeIdFrom", "MANAGER", 
														"partyRelationshipTypeId", "REPORTS_TO"), 
												null, true);
		int rowsReturned = prtyRelationships.size();
		assertTrue(rowsReturned > 0);
		
	}
	
	public void doPermissionFailureTest(String userLoginId, String managerPartyId, String subordinatePartyId, String subordinateRoleTypeId) throws Exception 
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
    			"managerPartyId", managerPartyId, 
    			"subordinatePartyId", subordinatePartyId,
    			"subordinateRoleTypeId", subordinateRoleTypeId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("createSubordinateForPartyRelationship", ctx);
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
