

package com.simbaquartz.xparty.hierarchy.test;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

public class CreateSameOrgEmployeeRelationshipTestDef extends OFBizTestCase {

    public CreateSameOrgEmployeeRelationshipTestDef(String name) {
        super(name);
    }

    public void testCreateSameOrgEmployeeRelationship_SalesRep1() throws Exception
    {
        doTest("MP.manager1",
                "bconway@mp.com",
                "MP",
                "SALES_REP");
    }

    public void testCreateSameOrgEmployeeRelationship_SalesRep2() throws Exception
    {
        doTest("MP.manager1",
                "ezra.snider@mp.com", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "MP",
                "SALES_REP");
    }

    public void testCreateSameOrgEmployeeRelationship_SalesManager() throws Exception
    {
        doTest("MP.manager1",
                "sblair.mgr@mp.com", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "MP",
                "MANAGER");
    }

    public void testCreateSameOrgEmployeeRelationship_ShipToCustomer() throws Exception
    {
        doRoleFailureTest("james.cimini@va.gov", // Doesn't have MANAGER PartyRole.
                "roth.hull@va.gov",
                "VA523",
                "SHIPMENT_CLERK");
    }

    public void testCreateSameOrgEmployeeRelationship_PermissionFailure_PartyIdEqualUserLogin() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "james.cimini@va.gov", // This causes the expected failure.
                "VA523",
                "END_USER_CUSTOMER");
    }

    public void testCreateSameOrgEmployeeRelationship_PermissionFailure_InternalPartyGroup() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",// This causes the expected failure.
                "james.cimini@va.gov",// This causes the expected failure.
                "FSD",
                "END_USER_CUSTOMER");
    }

    public void doTest(String userLoginId, String partyId, String partyGroupPartyId, String roleTypeId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("roleTypeId", roleTypeId);

        Map<String, Object> resp = dispatcher.runSync(
                "createSameOrgEmployeeRelationship", ctx);
        // check if the service was successfully executed
        assertTrue(ServiceUtil.isSuccess(resp));
        // Check if the relationship was successfully created
        List<GenericValue> prtyRelationships = delegator.findByAnd(
                "PartyRelationship", UtilMisc.toMap(
                        "partyIdTo", partyId,
                        "partyIdFrom", partyGroupPartyId,
                        "roleTypeIdTo", roleTypeId,
                        "roleTypeIdFrom", "_NA_"), null, true);
        int rowsReturned = prtyRelationships.size();
        assertTrue(rowsReturned > 0);

    }

    public void doPermissionFailureTest(String userLoginId, String partyId, String partyGroupPartyId, String roleTypeId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("roleTypeId", roleTypeId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("createSameOrgEmployeeRelationship", ctx);
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

    public void doRoleFailureTest(String userLoginId, String partyId, String partyGroupPartyId, String roleTypeId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("roleTypeId", roleTypeId);

        Map<String, Object> resp = dispatcher.runSync("createSameOrgEmployeeRelationship", ctx);
        assertTrue("Service didn't return failure as expected",ServiceUtil.isFailure(resp)); // Expect Failure from service
    }
}
