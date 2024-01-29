

package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.Map;

public class GetCustomerParentOrgForPartyGroupIdTestDef extends OFBizTestCase {

	public GetCustomerParentOrgForPartyGroupIdTestDef(String name) {
		super(name);
	}

	public void testGetCustomerParentOrgForPartyGroupId_MangisPartnerManager1() throws Exception
    {
        doTest("MP.manager1", // Passes because FSD.director1 has special permission check.
                "VA664",
                "VAVISN22");
    }

    public void testGetCustomerParentOrgForPartyGroupId_FsdDirector1() throws Exception
    {
        doTest("FSD.director1", // Passes because FSD.director1 has special permission check.
                "VA405",
                "VAVISN1");
    }

	public void testGetCustomerParentOrgForPartyGroupId_PermissionFailure_PartnerOrg() throws Exception
    {
        doPermissionFailureTest("SP.manager1",
                "MP",  // This causes permission failure
                "VAVISN1");
    }

    public void testGetCustomerParentOrgForPartyGroupId_PermissionFailure_SupplierOrg() throws Exception
    {
        doPermissionFailureTest("FSD.director1",
                "TEMPUS",  // This causes permission failure
                "VAVISN1");
    }

    public void testGetCustomerParentOrgForPartyGroupId_PermissionFailure_InternalOrg() throws Exception
    {
        doPermissionFailureTest("FSD.director1",
                "FSD",  // This causes permission failure
                "VAVISN1");
    }

	/*Service to establish a subordinate */
	public void doTest(String userLoginId, String partyGroupPartyId, String expectedParentPartyId) throws Exception
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap(
                "userLogin", userLogin,
        		"partyGroupPartyId", partyGroupPartyId);

        Map<String, Object> resp = dispatcher.runSync("getCustomerParentOrgForPartyGroupId", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));
        GenericValue parentOrg = (GenericValue) resp.get("parentOrg");

        assertTrue(parentOrg.getString("partyId").equals(expectedParentPartyId));
	}

	public void doPermissionFailureTest(String userLoginId, String partyGroupPartyId, String expectedParentPartyId) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap(
                "userLogin", userLogin,
                "partyGroupPartyId", partyGroupPartyId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("getCustomerParentOrgForPartyGroupId", ctx);
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
