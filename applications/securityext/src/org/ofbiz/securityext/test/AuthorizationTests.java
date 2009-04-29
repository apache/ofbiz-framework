package org.ofbiz.securityext.test;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.security.SecurityConfigurationException;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.security.authz.AuthorizationFactory;
import org.ofbiz.service.testtools.OFBizTestCase;

public class AuthorizationTests extends OFBizTestCase {

    private static final String module = AuthorizationTests.class.getName();
    protected GenericDelegator delegator;
    protected Authorization security;
    
    public AuthorizationTests(String name) throws SecurityConfigurationException {
        super(name);
        delegator = GenericDelegator.getGenericDelegator("default"); 
        security = AuthorizationFactory.getInstance(delegator);
    }
                   
    public void testBasicAdminPermission() throws Exception {
        Debug.logInfo("Running testBasicAdminPermission()", module);
        assertTrue("User was not granted permission as expected", security.hasPermission("system", "access:foo:bar", null, true));
    }
    
    public void testBasePermissionFailure() throws Exception {
        Debug.logInfo("Running testBasePermissionFailure()", module);
        assertFalse("Permission did not fail as expected", security.hasPermission("system", "no:permission", null, true));
    }
            
    public void testDynamicAccessFromClasspath() throws Exception {
        Debug.logInfo("Running testDynamicAccessFromClasspath()", module);
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:groovy2:2000", null, true));
    }
    
    public void testDynamicAccessService() throws Exception {
        Debug.logInfo("Running testDynamicAccessService()", module);
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:service:2000", null, true));
    }
    
    public void testDynamicAccessFailure() throws Exception {
        Debug.logInfo("Running testDynamicAccessFailure()", module);
        assertFalse("Dynamic access did not fail as expected", security.hasPermission("system", "test:groovy1:2000", null, true));
    }
    
    public void testAutoGrantPermissions() throws Exception {
        Debug.logInfo("Running testDynamicAccessFailure()", module);
        
        // first verify the user does not have the initial permission
        assertFalse("User already has the auto-granted permission", security.hasPermission("system", "test:autogranted", null, true));
        
        // next run security check to setup the auto-grant
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:groovy1:1000", null, true));
        
        // as long as this runs in the same thread (and it should) access should now be granted
        assertTrue("User was not auto-granted expected permission", security.hasPermission("system", "test:autogranted", null, true));
    }
    
    public void testAutoGrantCleanup() throws Exception {
        Debug.logInfo("Running testAutoGrantCleanup()", module);
        assertFalse("User was auto-granted an unexpected permission", security.hasPermission("user", "test:autogranted", null, true));
    }
    
    public void testDynamicAccessRecursion() throws Exception {
        Debug.logInfo("Running testDynamicAccessRecursion()", module);
        assertFalse("User was granted an unexpected permission", security.hasPermission("user", "test:recursion", null, true));
    }
}
