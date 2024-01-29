/******************************************************************************************
 * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
 * Unauthorized copying of this file, via any medium is strictly prohibited               *
 * Proprietary and confidential                                                           *
 * Written by Forrest Rae <forrest.rae@fidelissd.com>, December 2016                      *
 ******************************************************************************************/
package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.List;
import java.util.Map;

public class GetCustomerRelatedPersonsByRoleTestDef extends OFBizTestCase {

	public GetCustomerRelatedPersonsByRoleTestDef(String name) {
		super(name);
	}
	
	public void testGetCustomerRelatedPersonsByRole_SpecialPermission() throws Exception
    {
        List relatedPersonsList = doTest("FSD.director1", // Passes because FSD.director1 has special permission check.
                "VAVISN1",
                "CONTRACTING_OFFICER");
        assertEquals(2, relatedPersonsList.size());
        List<String> persons = UtilMisc.toList(((GenericValue) relatedPersonsList.get(0)).getString("partyId"), ((GenericValue) relatedPersonsList.get(1)).getString("partyId"));
        assertTrue(persons.contains("jared.levin@va.gov"));
        assertTrue(persons.contains("james.cimini@va.gov"));
    }

    public void testGetCustomerRelatedPersonsByRole_JamesCimini() throws Exception
    {
        List relatedPersonsList = doTest("james.cimini@va.gov",
                "VAVISN1",
                "CONTRACTING_OFFICER");
        assertEquals(2, relatedPersonsList.size());
        List<String> persons = UtilMisc.toList(((GenericValue) relatedPersonsList.get(0)).getString("partyId"), ((GenericValue) relatedPersonsList.get(1)).getString("partyId"));
        assertTrue(persons.contains("jared.levin@va.gov"));
        assertTrue(persons.contains("james.cimini@va.gov"));
    }

    public void testGetCustomerRelatedPersonsByRole_SemperSalesExec() throws Exception
    {
        List relatedPersonsList = doTest("SP.sales.exec",
                "VAVISN1",
                "CONTRACTING_OFFICER");
        assertEquals(2, relatedPersonsList.size());
        List<String> persons = UtilMisc.toList(((GenericValue) relatedPersonsList.get(0)).getString("partyId"), ((GenericValue) relatedPersonsList.get(1)).getString("partyId"));
        assertTrue(persons.contains("jared.levin@va.gov"));
        assertTrue(persons.contains("james.cimini@va.gov"));
    }

	public void testGetCustomerRelatedPersonsByRole_PermissionFailure_DifferentOrg1() throws Exception
    {
        doPermissionFailureTest("SP.manager1", //different partner SP.manager1 tries to create get subordinates, gets service auth exception
                "MP",
                "MANAGER");
    }

    public void testGetCustomerRelatedPersonsByRole_Failure_IncorrectRole() throws Exception
    {
        doInvalidRoleTypeIdFailureTest("james.cimini@va.gov",
                "VAVISN1",
                "OWNER");
    }

	/*Service to establish a subordinate */
	public List doTest(String userLoginId, String partyGroupPartyId, String roleTypeIdTo) throws Exception
	{
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"partyGroupPartyId", partyGroupPartyId,
        			"roleTypeId", roleTypeIdTo);
		
        Map<String, Object> resp = dispatcher.runSync("getCustomerRelatedPersonsByRole", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

        return (List)resp.get("relatedPersonsList");
	}

    public void doInvalidRoleTypeIdFailureTest(String userLoginId, String partyGroupPartyId, String roleTypeIdTo) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "partyGroupPartyId", partyGroupPartyId,
                "roleTypeId", roleTypeIdTo);

        Map<String, Object> resp = dispatcher.runSync("getCustomerRelatedPersonsByRole", ctx);
        // check if the service was successfully executed
        assertTrue(ServiceUtil.isFailure(resp));
    }
	
	public void doPermissionFailureTest(String userLoginId, String partyGroupPartyId, String roleTypeIdTo) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);

        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "partyGroupPartyId", partyGroupPartyId,
                "roleTypeId", roleTypeIdTo);

        Map<String, Object> resp = null;
        try
        {
            resp = dispatcher.runSync("getCustomerRelatedPersonsByRole", ctx);
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
