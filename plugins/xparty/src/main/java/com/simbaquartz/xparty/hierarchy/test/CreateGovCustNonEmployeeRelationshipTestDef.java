

package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateGovCustNonEmployeeRelationshipTestDef extends OFBizTestCase {

    public CreateGovCustNonEmployeeRelationshipTestDef(String name) {
        super(name);
    }

    public void testCreateGovCustNonEmployeeRelationship_Contact1() throws Exception
    {
        doTest("MP.manager1",
                "kato.wilder@va.gov",
                "VA523",
                "CONTACT");
    }

    public void testCreateGovCustNonEmployeeRelationship_Contact2() throws Exception
    {
        doTest("MP.manager1",
                "hamish.fisher@va.gov", // Doesn't have CONTRACTING_OFFICER PartyRole.
                "VA523",
                "CONTACT");
    }

    public void testCreateGovCustNonEmployeeRelationship_PermissionFailure_PartyIdEqualUserLogin() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "james.cimini@va.gov", // This causes the expected failure.
                "VA523",
                "CONTACT");
    }

    public void testCreateGovCustNonEmployeeRelationship_PermissionFailure_InternalPartyGroup() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "zeph.taylor@va.gov",
                "FSD",  // This causes the expected failure.
                "CONTACT");
    }

    public void testCreateGovCustNonEmployeeRelationship_PermissionFailure_PartnerPartyGroup() throws Exception
    {
        doPermissionFailureTest("james.cimini@va.gov",
                "herman.bowers@va.gov",
                "MP",  // This causes the expected failure.
                "CONTACT");
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
            assertTrue(e.getMessage().startsWith("You do not have permission to invoke the service"));
            return;
        }
        // If we made it here, it's an error.
        fail("Incorrectly allowed to run due to permission-service incorrectly returning hasPermission = true.");
    }
}
