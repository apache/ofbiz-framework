

package com.simbaquartz.xparty.hierarchy.test;

import java.util.Locale;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

public class UserLoginPartnerRelationshipCheckTestDef extends OFBizTestCase {

    public static final String resourceError = "HierarchyErrorUiLabels";
    public static final Locale locale = Locale.getDefault();
    public UserLoginPartnerRelationshipCheckTestDef(String name) {
        super(name);
    }

    public void testSuccessUserLoginPartnerRelationshipCheck_fsdDirector1() throws Exception
    {
        doTest("FSD.director1", "123456");
    }

    /* Disabling for now as I don't want the UserLogin enabled in the seed data.
    public void testSuccessUserLoginPartnerRelationshipCheck_CustomerJamesCimini() throws Exception
    {
        doErrorTest("james.cimini@va.gov", "123456", UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale));
    }*/

    public void testFailureUserLoginPartnerRelationshipCheck_fsdDirector1WrongPassword() throws Exception
    {
        doFailureTest("FSD.director1", "WrongPassword", "Password incorrect.");
    }

    public void testErrorUserLoginPartnerRelationshipCheck_BothPartnerAndInternalOrg() throws Exception
    {
        doErrorTest("robb.caswell", "123456", UtilProperties.getMessage(resourceError, "HierarchyPartnerLoginError", locale));
    }

    public void doTest(String username, String password) throws Exception
    {
        Map<String, Object> ctx = UtilMisc.toMap("login.username", username, "login.password", password);
		Map<String, Object> result = dispatcher.runSync("userLogin", ctx);

		// check if the service was successfully executed
		assertTrue(ServiceUtil.isSuccess(result));
    }

    public void doFailureTest(String username, String password, String expectedErrorMessage) throws Exception
    {
        Map<String, Object> ctx = UtilMisc.toMap("login.username", username, "login.password", password);
        Map<String, Object> result = dispatcher.runSync("userLogin", ctx);

        // check if the service was successfully executed
        assertTrue(ServiceUtil.isFailure(result));
        assertTrue(ServiceUtil.getErrorMessage(result).equals(expectedErrorMessage));
    }

    public void doErrorTest(String username, String password, String expectedErrorMessage) throws Exception
    {
        Map<String, Object> ctx = UtilMisc.toMap("login.username", username, "login.password", password);
        Map<String, Object> result = dispatcher.runSync("userLogin", ctx);

        // check if the service was successfully executed
        assertTrue(ServiceUtil.isFailure(result));
        assertTrue(ServiceUtil.getErrorMessage(result).equals(expectedErrorMessage));
    }
}