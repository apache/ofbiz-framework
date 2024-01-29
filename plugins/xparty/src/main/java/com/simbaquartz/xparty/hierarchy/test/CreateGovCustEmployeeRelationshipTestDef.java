

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

public class CreateGovCustEmployeeRelationshipTestDef extends OFBizTestCase {

    public CreateGovCustEmployeeRelationshipTestDef(String name) {
        super(name);
    }

    public void testcreateGovCustPersonRelationship_ContractingOfficer1() throws Exception
    {
        doTest("MP.manager1",
                "co1@va.gov",
                "VA664",
                "CONTRACTING_OFFICER");
    }

    public void testcreateGovCustPersonRelationship_ContractingOfficer2() throws Exception
    {
        doTest("MP.manager1",
                "co2@va.gov", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "VA664",
                "CONTRACTING_OFFICER");
    }

    public void testcreateGovCustPersonRelationship_ContractingOfficerManager() throws Exception
    {
        doTest("MP.manager1",
                "co.mgr@va.gov", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "VA664",
                "MANAGER");
    }

    public void testcreateGovCustPersonRelationship_ShipmentClerk() throws Exception
    {
        doTest("MP.manager1",
                "maurice.dock@va.gov", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "VA523",
                "SHIPMENT_CLERK");
    }

    public void testcreateGovCustPersonRelationship_EndUserCustomer() throws Exception
    {
        doTest("MP.manager1",
                "dr.dug.harper@va.gov", // Doesn't have END_USER_CUSTOMER PartyRole.
                "VA523",
                "END_USER_CUSTOMER");
    }

    public void testcreateGovCustPersonRelationship_PermissionFailure_PartyIdEqualUserLogin() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "james.cimini@va.gov", // This causes the expected failure.
                "VA523",
                "END_USER_CUSTOMER");
    }

    public void testcreateGovCustPersonRelationship_PermissionFailure_InternalPartyGroup() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "dr.jon.hodges@va.gov",
                "FSD",  // This causes the expected failure.
                "END_USER_CUSTOMER");
    }

    public void testcreateGovCustPersonRelationship_PermissionFailure_PartnerPartyGroup() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "dr.jon.hodges@va.gov",
                "MP",  // This causes the expected failure.
                "END_USER_CUSTOMER");
    }

    public void doTest(String userLoginId, String partyId, String partyGroupPartyId, String roleTypeId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("partyGroupPartyId", partyGroupPartyId);
        ctx.put("roleTypeId", roleTypeId);

        Map<String, Object> resp = dispatcher.runSync(
                "createGovCustPersonRelationship", ctx);
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
        ctx.put("partyGroupPartyId", partyGroupPartyId);
        ctx.put("roleTypeId", roleTypeId);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("createGovCustPersonRelationship", ctx);
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
