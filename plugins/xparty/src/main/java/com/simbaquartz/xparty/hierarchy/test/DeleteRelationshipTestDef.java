

package com.simbaquartz.xparty.hierarchy.test;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.testtools.OFBizTestCase;
import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

public class DeleteRelationshipTestDef extends OFBizTestCase {

    private java.sql.Timestamp timestamp;
    public DeleteRelationshipTestDef(String name) {
        super(name);

        try
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            Date parsedDate = dateFormat.parse("2014-01-01 00:00:00");
            timestamp = new java.sql.Timestamp(parsedDate.getTime());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void testDeleteRelationship_AcountLead_MagnisPartnerSalesRep1() throws Exception
    {
        doTest("forrest.rae",
                "MP.manager1",
                "MP.sales.rep1",
                this.timestamp,
                "MANAGER",
                "SALES_REP");
    }

    public void testDeleteRelationship_Manager_MagnisPartnerSalesRep1() throws Exception
    {
        doTest("MP.manager1",
                "MP.manager1",
                "MP.sales.rep2",
                this.timestamp,
                "MANAGER",
                "SALES_REP");
    }

    public void testCreateGovCustEmployeeRelationship_PermissionFailure_PartyIdEqualUserLogin() throws Exception
    {
        doPermissionFailureTest("MP.sales.rep3",
                "MP.manager3",
                "MP.sales.rep5",
                this.timestamp,
                "MANAGER",
                "SALES_REP");
    }

    public void doTest(String userLoginId, String partyIdFrom, String subordinatePartyId, Timestamp fromDate, String roleTypeIdFrom, String roleTypeIdTo) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyIdFrom", partyIdFrom);
        ctx.put("subordinatePartyId", subordinatePartyId);
        ctx.put("fromDate", fromDate);
        ctx.put("roleTypeIdFrom", roleTypeIdFrom);
        ctx.put("roleTypeIdTo", roleTypeIdTo);

        Map<String, Object> resp = dispatcher.runSync(
                "deleteRelationship", ctx);
        // check if the service was successfully executed
        assertTrue(ServiceUtil.isSuccess(resp));
        // Check if the relationship was successfully created
        List<GenericValue> prtyRelationships = delegator.findByAnd(
                "PartyRelationship", UtilMisc.toMap(
                        "partyIdTo", subordinatePartyId,
                        "partyIdFrom", partyIdFrom,
                        "roleTypeIdTo", roleTypeIdTo,
                        "roleTypeIdFrom", roleTypeIdFrom,
                        "fromDate", fromDate), null, true);
        int rowsReturned = prtyRelationships.size();
        assertTrue(rowsReturned > 0);

    }

    public void doPermissionFailureTest(String userLoginId, String partyIdFrom, String subordinatePartyId, Timestamp fromDate, String roleTypeIdFrom, String roleTypeIdTo) throws Exception
    {
        Map<String, Object> ctx = new HashMap<String, Object>();
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        ctx.put("userLogin", userLogin);
        ctx.put("partyIdFrom", partyIdFrom);
        ctx.put("subordinatePartyId", subordinatePartyId);
        ctx.put("fromDate", fromDate);
        ctx.put("roleTypeIdFrom", roleTypeIdFrom);
        ctx.put("roleTypeIdTo", roleTypeIdTo);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("deleteRelationship", ctx);
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
