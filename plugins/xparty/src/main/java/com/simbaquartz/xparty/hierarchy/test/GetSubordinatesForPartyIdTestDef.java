

package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.LinkedList;
import java.util.Map;

public class GetSubordinatesForPartyIdTestDef extends OFBizTestCase {

	public GetSubordinatesForPartyIdTestDef(String name) {
		super(name);
	}
	
	public void testGetSubordinatesForPartyId_SpecialPermission() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("FSD.director1", // Passes because FSD.director1 has special permission check.
                "MP.manager1",
                "N",
                null);
        assertTrue(subordinatePartyIdList.size() == 3);
        assertTrue(subordinatePartyIdList.contains("MP.manager1"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep2"));
    }

    public void testGetSubordinatesForPartyId_SemperManager1() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("SP.manager1",
                "SP.manager1",
                "N",
                null);
        assertTrue(subordinatePartyIdList.size() == 3);
        assertTrue(subordinatePartyIdList.contains("SP.manager1"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep2"));
    }

    public void testGetSubordinatesForPartyId_SemperSalesExec() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("SP.sales.exec",
                "SP.director1",
                "Y",
                null);
        assertTrue(subordinatePartyIdList.size() == 7);
        assertTrue(subordinatePartyIdList.contains("SP.director1"));
        assertTrue(subordinatePartyIdList.contains("SP.manager1"));
        assertTrue(subordinatePartyIdList.contains("SP.manager2"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep3"));
        assertTrue(subordinatePartyIdList.contains("SP.sales.rep4"));
    }

    public void testGetSubordinatesForPartyId_isPersonCustomerPermissionCheck() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("SP.sales.exec",
                "jared.levin@va.gov",
                "Y",
                null);
        assertTrue(subordinatePartyIdList.size() == 1);
        assertTrue(subordinatePartyIdList.contains("jared.levin@va.gov"));
    }

    public void testGetSubordinatesForPartyId_fsdDirector1_recurse() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("FSD.director1",
                "FSD.director1",
                "Y",
                null);
        assertTrue(subordinatePartyIdList.size() == 22);
        assertTrue(subordinatePartyIdList.contains("FSD.director1"));
        assertTrue(subordinatePartyIdList.contains("FSD.manager1"));
        assertTrue(subordinatePartyIdList.contains("FSD.manager2"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep3"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep4"));
        assertTrue(subordinatePartyIdList.contains("FSD.director1"));
        assertTrue(subordinatePartyIdList.contains("FSD.manager1"));
        assertTrue(subordinatePartyIdList.contains("FSD.manager2"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep3"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep4"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.exec"));
        assertTrue(subordinatePartyIdList.contains("MP.director1"));
        assertTrue(subordinatePartyIdList.contains("MP.director2"));
        assertTrue(subordinatePartyIdList.contains("MP.director3"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep6"));
        assertTrue(subordinatePartyIdList.contains("MP.agent1"));
        assertTrue(subordinatePartyIdList.contains("MP.manager1"));
        assertTrue(subordinatePartyIdList.contains("MP.manager2"));
        assertTrue(subordinatePartyIdList.contains("MP.manager3"));
        assertTrue(subordinatePartyIdList.contains("MP.manager4"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep3"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep4"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep5"));
    }

    public void testGetSubordinatesForPartyId_fsdManger1_recurse() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("FSD.sales.exec",
                "FSD.manager1",
                "Y",
                null);
        assertTrue(subordinatePartyIdList.size() == 18);
        assertTrue(subordinatePartyIdList.contains("FSD.manager1"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("FSD.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.exec"));
        assertTrue(subordinatePartyIdList.contains("MP.director1"));
        assertTrue(subordinatePartyIdList.contains("MP.director2"));
        assertTrue(subordinatePartyIdList.contains("MP.director3"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep6"));
        assertTrue(subordinatePartyIdList.contains("MP.agent1"));
        assertTrue(subordinatePartyIdList.contains("MP.manager1"));
        assertTrue(subordinatePartyIdList.contains("MP.manager2"));
        assertTrue(subordinatePartyIdList.contains("MP.manager3"));
        assertTrue(subordinatePartyIdList.contains("MP.manager4"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep1"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep2"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep3"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep4"));
        assertTrue(subordinatePartyIdList.contains("MP.sales.rep5"));
    }

	public void testGetSubordinatesForPartyId_PermissionFailure_DifferentOrg1() throws Exception
    {
        doPermissionFailureTest("SP.manager1", //different partner SP.manager1 tries to create get subordinates, gets service auth exception
                "MP.manager1",
                "N",
                null);
    }

	/*Service to establish a subordinate */
	public LinkedList<String> doTest(String userLoginId, String partyId, String recurse, String roleTypeIdTo) throws Exception
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();

        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"partyId", partyId,
        			"recurse", recurse,
        			"roleTypeIdTo", roleTypeIdTo);
		
        Map<String, Object> resp = dispatcher.runSync("getSubordinatesForPartyId", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

        return (LinkedList<String>) resp.get("subordinatePartyIdList");
	}
	
	public void doPermissionFailureTest(String userLoginId, String partyId, String recurse, String roleTypeIdTo) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "partyId", partyId,
                "recurse", recurse);
        if(roleTypeIdTo != null)
            ctx.put("roleTypeIdTo", roleTypeIdTo);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("getSubordinatesForPartyId", ctx);
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
