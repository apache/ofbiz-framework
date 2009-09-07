/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.securityext.test;

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.security.SecurityConfigurationException;
import org.ofbiz.security.authz.AbstractAuthorization;
import org.ofbiz.security.authz.Authorization;
import org.ofbiz.security.authz.AuthorizationFactory;
import org.ofbiz.service.testtools.OFBizTestCase;

public class AuthorizationTests extends OFBizTestCase {

    private static final String module = AuthorizationTests.class.getName();
    protected Authorization security = null;

    public AuthorizationTests(String name) {
        super(name);
    }

    @Override
    public void setUp() throws SecurityConfigurationException {
        if (security == null) {
            security = AuthorizationFactory.getInstance(delegator);
        }
        AbstractAuthorization.clearThreadLocal();
    }

    public void testBasicAdminPermission() throws Exception {
        Debug.logInfo("Running testBasicAdminPermission()", module);
        assertTrue("User was not granted permission as expected", security.hasPermission("system", "access:foo:bar", null));
    }

    public void testBasePermissionFailure() throws Exception {
        Debug.logInfo("Running testBasePermissionFailure()", module);
        assertFalse("Permission did not fail as expected", security.hasPermission("system", "no:permission", null));
    }

    public void testDynamicAccessFromClasspath() throws Exception {
        Debug.logInfo("Running testDynamicAccessFromClasspath()", module);
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:groovy2:2000", null));
    }

    public void testDynamicAccessService() throws Exception {
        Debug.logInfo("Running testDynamicAccessService()", module);
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:service:2000", null));
    }

    public void testDynamicAccessFailure() throws Exception {
        Debug.logInfo("Running testDynamicAccessFailure()", module);
        assertFalse("Dynamic access did not fail as expected", security.hasPermission("system", "test:groovy1:2000", null));
    }

    public void testAutoGrantPermissions() throws Exception {
        Debug.logInfo("Running testDynamicAccessFailure()", module);

        // first verify the user does not have the initial permission
        assertFalse("User already has the auto-granted permission", security.hasPermission("system", "test:autogranted", null));

        // next run security check to setup the auto-grant
        assertTrue("User was not granted dynamic access as expected", security.hasPermission("system", "test:groovy1:1000", null));

        // as long as this runs in the same thread (and it should) access should now be granted
        assertTrue("User was not auto-granted expected permission", security.hasPermission("system", "test:autogranted", null));
    }

    public void testAutoGrantCleanup() throws Exception {
        Debug.logInfo("Running testAutoGrantCleanup()", module);
        assertFalse("User was auto-granted an unexpected permission", security.hasPermission("user", "test:autogranted", null));
    }

    public void testDynamicAccessRecursion() throws Exception {
        Debug.logInfo("Running testDynamicAccessRecursion()", module);
        assertFalse("User was granted an unexpected permission", security.hasPermission("user", "test:recursion", null));
    }

    public void testFindAllPermissionRegexp() throws Exception {
        Debug.logInfo("Running testFindAllPermissionRegexp()", module);
        Map<String,Boolean> permResultMap = security.findMatchingPermission("system", ".*:example", null);
        assertEquals("Invalid result map size; should be 5", 5, permResultMap.size());
        assertTrue("User was not granted expected permission {access:example}", permResultMap.get("access:example"));
        assertTrue("User was not granted expected permission {create:example}", permResultMap.get("create:example"));
        assertTrue("User was not granted expected permission {read:example}", permResultMap.get("read:example"));
        assertTrue("User was not granted expected permission {update:example}", permResultMap.get("update:example"));
        assertTrue("User was not granted expected permission {delete:example}", permResultMap.get("delete:example"));
    }

    public void testFindLimitedPermissionRegexp() throws Exception {
        Debug.logInfo("Running testFindLimitedPermissionRegexp()", module);
        Map<String,Boolean> permResultMap = security.findMatchingPermission("user", "(access|read):example", null);
        assertEquals("Invalid result map size; should be 2", 2, permResultMap.size());
        assertFalse("User was granted an unexpected permission {access:example}", permResultMap.get("access:example"));
        assertFalse("User was granted an unexpected permission {read:example}", permResultMap.get("read:example"));
    }
}
