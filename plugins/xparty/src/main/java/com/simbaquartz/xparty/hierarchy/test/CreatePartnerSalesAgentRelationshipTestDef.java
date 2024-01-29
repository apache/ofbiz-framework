

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

public class CreatePartnerSalesAgentRelationshipTestDef extends OFBizTestCase {

    public CreatePartnerSalesAgentRelationshipTestDef(String name) {
        super(name);
    }

    public void testCreatePartnerSalesAgentRelationship_Agent1() throws Exception
    {
        doTest("MP.manager1",
                "grawford@ex.com",
                "MP");
    }

    public void testCreatePartnerSalesAgentRelationship_PermissionFailure_NotManager() throws Exception
    {
        doPermissionFailureTest("MP.sales.rep1", // This causes the expected failure, not a manager.
                "ktodd@ex.com",
                "MP");
    }

    public void testCreatePartnerSalesAgentRelationship_PermissionFailure_PartyIdEqualUserLogin() throws Exception
    {
        doPermissionFailureTest("MP.sales.rep1",
                "MP.sales.rep1", // This causes the expected failure, userLogin and partyId are equal.
                "MP");
    }

    public void testCreatePartnerSalesAgentRelationship_PermissionFailure_NotPartner() throws Exception
    {
        doPermissionFailureTest("forrest.rae",
                "ktodd@ex.com",
                "ACME");// This causes the expected failure, not a Partner.
    }

    public void testCreatePartnerSalesAgentRelationship_PermissionFailure_IsCustomer() throws Exception
    {
        doPermissionFailureTest("forrest.rae",
                "ktodd@ex.com",
                "VA523");// This causes the expected failure, not a Partner.
    }

    public void doTest(String userLoginId, String partyId, String partyGroupPartyId) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyId", partyId);
        ctx.put("partyGroupPartyId", partyGroupPartyId);

        Map<String, Object> resp = dispatcher.runSync(
                "createPartnerSalesAgentRelationship", ctx);
        // check if the service was successfully executed
        assertTrue(ServiceUtil.isSuccess(resp));
        // Check if the relationship was successfully created
        List<GenericValue> prtyRelationships = delegator.findByAnd(
                "PartyRelationship", UtilMisc.toMap(
                        "partyIdTo", partyId,
                        "partyIdFrom", partyGroupPartyId,
                        "partyRelationshipTypeId", "AGENT",
                        "roleTypeIdTo", "AGENT",
                        "roleTypeIdFrom", "_NA_"), null, true);
        int rowsReturned = prtyRelationships.size();
        assertTrue(rowsReturned > 0);

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
            resp = dispatcher.runSync("createPartnerSalesAgentRelationship", ctx);
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
