
package com.simbaquartz.xparty.hierarchy.test;

import com.fidelissd.zcp.xcommon.util.hierarchy.HierarchyUtils;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetOrgAllPersonsTestDef extends OFBizTestCase {

	public GetOrgAllPersonsTestDef(String name) {
		super(name);
	}
	
	public void testGetAllPersonsTestDef_FSD() throws Exception
    {
        List<GenericValue> allPersonsList = doTest("FSD.sales.rep1", "FSD");
        // Get all the partyIds into a List.
        List<String> persons = new ArrayList<String>();
        for (GenericValue person: allPersonsList)
        {
            persons.add(person.getString("partyId"));
        }
        assertEquals(22, persons.size());
        assertTrue(persons.contains("adam.coerper"));
        assertTrue(persons.contains("matthew.hendricksen"));
        assertTrue(persons.contains("dustin.lee"));
        assertTrue(persons.contains("joel.kohn"));
        assertTrue(persons.contains("forrest.rae"));
        assertTrue(persons.contains("FSD.sales.exec"));
        assertTrue(persons.contains("FSD.director1"));
        assertTrue(persons.contains("FSD.director2"));
        assertTrue(persons.contains("FSD.director3"));
        assertTrue(persons.contains("FSD.manager1"));
        assertTrue(persons.contains("FSD.manager2"));
        assertTrue(persons.contains("FSD.manager3"));
        assertTrue(persons.contains("FSD.manager4"));
        assertTrue(persons.contains("FSD.sales.rep1"));
        assertTrue(persons.contains("FSD.sales.rep2"));
        assertTrue(persons.contains("FSD.sales.rep3"));
        assertTrue(persons.contains("FSD.sales.rep4"));
        assertTrue(persons.contains("FSD.sales.rep5"));
        assertTrue(persons.contains("FSD.sales.rep6"));
        assertTrue(persons.contains("dev.test"));
        assertTrue(persons.contains("mandeep.sidhu"));
    }

    public void testGetAllPersonsTestDef_FsdOwner_MP() throws Exception
    {
        List<GenericValue> allPersonsList = doTest("forrest.rae", "MP");
        assertTrue(allPersonsList.size() >= 15);
        // Get all the partyIds into a List.
        List<String> persons = new ArrayList<String>();
        for (GenericValue person: allPersonsList)
        {
            persons.add(person.getString("partyId"));
        }
        assertTrue(persons.contains("MP.sales.exec"));
        assertTrue(persons.contains("MP.manager2"));
        assertTrue(persons.contains("MP.manager1"));
        assertTrue(persons.contains("MP.director3"));
        assertTrue(persons.contains("MP.director2"));
        assertTrue(persons.contains("MP.director1"));
        assertTrue(persons.contains("MP.sales.rep6"));
        assertTrue(persons.contains("MP.sales.rep5"));
        assertTrue(persons.contains("MP.sales.rep4"));
        assertTrue(persons.contains("MP.sales.rep3"));
        assertTrue(persons.contains("MP.sales.rep2"));
        assertTrue(persons.contains("MP.sales.rep1"));
        assertTrue(persons.contains("MP.manager4"));
        assertTrue(persons.contains("MP.manager3"));
        assertTrue(persons.contains("MP.agent1"));
    }

    public void testGetAllPersonsTestDef_PermissionFailure_DifferentOrg1() throws Exception
    {
        doPermissionFailureTest(
                "SP.manager1", //different partner SP.manager1 tries to create get employees of MP, gets service auth exception
                "MP");
    }
    public void testGetAllPersonsTestDef_PermissionFailure_DifferentOrg2() throws Exception
    {
        doPermissionFailureTest(
                "MP.sales.rep1", //different person FSD.sales.rep1 tries to create get employees of MP, gets service auth exception
                "SP");
    }

    public List<GenericValue> doTest(String userLoginId, String partyGroupPartyId) throws Exception
	{
        //String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
        			"partyGroupPartyId", partyGroupPartyId);
		
        Map<String, Object> resp = dispatcher.runSync("getOrgAllPersons", ctx);
        // check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(resp));

        return UtilGenerics.checkList(resp.get("orgPersonsList"));
	}

    public void doPermissionFailureTest(String userLoginId, String partyGroupPartyId) throws Exception
    {
        String module = Thread.currentThread().getStackTrace()[2].getMethodName();
        GenericValue userLogin = HierarchyUtils.getUserLogin(delegator, userLoginId);
        Map<String, Object> ctx = UtilMisc.toMap("userLogin", userLogin,
                "partyGroupPartyId", partyGroupPartyId);

        try {
            dispatcher.runSync("getOrgAllPersons", ctx);
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
