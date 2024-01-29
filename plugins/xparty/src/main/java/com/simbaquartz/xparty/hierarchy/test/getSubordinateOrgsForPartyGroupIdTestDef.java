

package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.LinkedList;
import java.util.Map;

public class getSubordinateOrgsForPartyGroupIdTestDef extends OFBizTestCase {

	public getSubordinateOrgsForPartyGroupIdTestDef(String name) {
		super(name);
	}
	
	public void testGetSubordinateOrgsForPartyGroupId_MangisPartnerManager1() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("MP.manager1", // Passes because FSD.director1 has special permission check.
                "VA",
                null,
                null);
        assertTrue(subordinatePartyIdList.size() == 3);
        assertTrue(subordinatePartyIdList.contains("VA"));
        assertTrue(subordinatePartyIdList.contains("VAVHA101"));
    }

    public void testGetSubordinateOrgsForPartyGroupId_FsdDirector1() throws Exception
    {
        LinkedList<String> subordinatePartyIdList = doTest("FSD.director1", // Passes because FSD.director1 has special permission check.
                "VA",
                null,
                null);
        assertTrue(subordinatePartyIdList.size() == 3);
        assertTrue(subordinatePartyIdList.contains("VA"));
        assertTrue(subordinatePartyIdList.contains("VAVHA101"));
    }

	public void testGetSubordinateOrgsForPartyGroupId_PermissionFailure_PartnerOrg() throws Exception
    {
        doPermissionFailureTest("SP.manager1",
                "MP",  // This causes permission failure
                "N",
                null);
    }

    public void testGetSubordinateOrgsForPartyGroupId_PermissionFailure_SupplierOrg() throws Exception
    {
        doPermissionFailureTest("FSD.director1",
                "TEMPUS",  // This causes permission failure
                "N",
                null);
    }

    public void testGetSubordinateOrgsForPartyGroupId_PermissionFailure_InternalOrg() throws Exception
    {
        doPermissionFailureTest("FSD.director1",
                "FSD",  // This causes permission failure
                "N",
                null);
    }

	/*Service to establish a subordinate */
	public LinkedList<String> doTest(String userLoginId, String partyGroupPartyId, String recurse, String roleTypeIdTo) throws Exception
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"partyGroupPartyId", partyGroupPartyId,
        			"recurse", recurse,
        			"roleTypeIdTo", roleTypeIdTo);
		
        Map<String, Object> resp = dispatcher.runSync("getSubordinateOrgsForPartyGroupId", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

        return (LinkedList<String>)resp.get("subordinatePartyIdList");
	}
	
	public void doPermissionFailureTest(String userLoginId, String partyGroupPartyId, String recurse, String roleTypeIdTo) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "partyGroupPartyId", partyGroupPartyId,
                "recurse", recurse,
                "roleTypeIdTo", roleTypeIdTo);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("getSubordinateOrgsForPartyGroupId", ctx);
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
