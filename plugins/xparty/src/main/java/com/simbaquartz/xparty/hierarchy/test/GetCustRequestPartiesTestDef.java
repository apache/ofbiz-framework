

package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import com.simbaquartz.xparty.hierarchy.orderentity.OrderEntityType;
import com.simbaquartz.xparty.hierarchy.role.EmployerPersonRoles;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.List;
import java.util.Map;

import static com.simbaquartz.xparty.hierarchy.orderentity.OrderEntityUtils.getOrderEntityRoleParties;
import static com.simbaquartz.xparty.hierarchy.orderentity.OrderEntityUtils.getOrderEntityRolePartyIds;

public class GetCustRequestPartiesTestDef extends OFBizTestCase {

    public GetCustRequestPartiesTestDef(String name) {
		super(name);
	}
	
	public void testGetCustRequestParties_SpecialPermission() throws Exception
    {
        List custRequestParties = doTest("FSD.director1", // Passes because FSD.director1 has special permission check.
                "CR-MP.sales.rep1-1");
        assertTrue(custRequestParties.size() == 2);
    }

    public void testGetCustRequestParties_SalesRep1() throws Exception
    {
        List custRequestParties = doTest("MP.sales.rep1", // Passes because FSD.director1 has special permission check.
                "CR-MP.sales.rep1-2");
        assertTrue(custRequestParties.size() == 1);
    }

    public void testGetCustRequestParties_Manager1() throws Exception
    {
        List custRequestParties = doTest("MP.manager1", // Passes because FSD.director1 has special permission check.
                "CR-MP.sales.rep1-3");
        assertTrue(custRequestParties.size() == 2);
    }

	public void testGetCustRequestParties_PermissionFailure_DifferentOrg1() throws Exception
    {
        doPermissionFailureTest("SP.manager1", //different partner SP.manager1 tries to create get subordinates, gets service auth exception
                "CR-MP.sales.rep1-1");
    }

    public void testGetCustRequestParties_PermissionFailure_SalesRep3() throws Exception
    {
        doPermissionFailureTest("MP.sales.rep3", //different partner SP.manager1 tries to create get subordinates, gets service auth exception
                "CR-MP.sales.rep1-1");
    }


    public void testCallGetOrderEntityRolePartiesDirectly() throws Exception
    {
        List custRequestParties = getOrderEntityRoleParties(delegator, OrderEntityType.CUST_REQUEST, EmployerPersonRoles.class, "CR-MP.sales.rep1-1");
        assertTrue(custRequestParties.size() == 2);
    }

    public void testCallGetOrderEntityRolePartyIdsDirectly() throws Exception
    {
        List custRequestPartyIds = getOrderEntityRolePartyIds(delegator, OrderEntityType.CUST_REQUEST, EmployerPersonRoles.class, "CR-MP.sales.rep1-1");
        assertTrue(custRequestPartyIds.size() == 2);
        assertTrue(custRequestPartyIds.contains("MP.sales.rep1"));
        assertTrue(custRequestPartyIds.contains("MP.sales.rep2"));
    }

	/*Service to establish a subordinate */
	public List doTest(String userLoginId, String custRequestId) throws Exception
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"custRequestId", custRequestId,
        			"personRoleEnum", EmployerPersonRoles.class);
		
        Map<String, Object> resp = dispatcher.runSync("getCustRequestParties", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

        return (List)resp.get("custRequestParties");
	}
	
	public void doPermissionFailureTest(String userLoginId, String custRequestId) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "custRequestId", custRequestId,
                "personRoleEnum", EmployerPersonRoles.class);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("getCustRequestParties", ctx);
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
