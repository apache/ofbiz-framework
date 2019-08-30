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
package org.apache.ofbiz.service.test;

import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.Map;

public class ServicePermissionTests extends OFBizTestCase {

    public ServicePermissionTests(String name) {
        super(name);
    }


    private GenericValue getUserLogin(String userLoginId) throws Exception {
        return EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
    }

    public void testPermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"));
        Map<String, Object> results = dispatcher.runSync("testSimplePermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    public void testServicePermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "Y");
        Map<String, Object> results = dispatcher.runSync("testSimpleServicePermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    public void testGroupPermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "Y");
        Map<String, Object> results = dispatcher.runSync("testSimpleGroupAndPermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));

        testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "N");
        results = dispatcher.runSync("testSimpleGroupOrPermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    public void testPermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"));
        try {
            Map<String, Object> results = dispatcher.runSync("testSimplePermission", testingPermMap);
            assertFalse("The service testSimplePermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

    public void testServicePermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"), "givePermission", "N");
        try{
            Map<String, Object> results = dispatcher.runSync("testSimpleServicePermission", testingPermMap);
            assertFalse("The service testServicePermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

    public void testGroupPermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"), "givePermission", "Y");
        try{
            Map<String, Object> results = dispatcher.runSync("testSimpleGroupAndPermission", testingPermMap);
            assertFalse("The testGroupPermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }

        testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "N");
        try{
            Map<String, Object> results = dispatcher.runSync("testSimpleGroupOrPermission", testingPermMap);
            assertFalse("The testGroupPermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

}