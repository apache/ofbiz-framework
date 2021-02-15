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
import org.apache.ofbiz.service.ServiceAuthException;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.service.testtools.OFBizTestCase;

import java.util.Map;

public class ServicePermissionTests extends OFBizTestCase {

    public ServicePermissionTests(String name) {
        super(name);
    }

    /**
     * Test permission success.
     * @throws Exception the exception
     */
    public void testPermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"));
        Map<String, Object> results = getDispatcher().runSync("testSimplePermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    /**
     * Test service permission success.
     * @throws Exception the exception
     */
    public void testServicePermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "Y");
        Map<String, Object> results = getDispatcher().runSync("testSimpleServicePermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    /**
     * Test service permission error.
     * @throws Exception the exception
     */
    public void testServicePermissionError() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "N");
        try {
            Map<String, Object> results = getDispatcher().runSync("testSimpleServicePermission", testingPermMap);
            assertFalse("The testGroupPermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test group permission success.
     * @throws Exception the exception
     */
    public void testGroupPermissionSuccess() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "Y");
        Map<String, Object> results = getDispatcher().runSync("testSimpleGroupAndPermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));

        testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "N");
        results = getDispatcher().runSync("testSimpleGroupOrPermission", testingPermMap);
        assertTrue(ServiceUtil.isSuccess(results));
    }

    /**
     * Test permission failed.
     * @throws Exception the exception
     */
    public void testPermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"));
        try {
            Map<String, Object> results = getDispatcher().runSync("testSimplePermission", testingPermMap);
            assertFalse("The service testSimplePermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test service permission failed.
     * @throws Exception the exception
     */
    public void testServicePermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"), "givePermission", "N");
        try {
            Map<String, Object> results = getDispatcher().runSync("testSimpleServicePermission", testingPermMap);
            assertFalse("The service testServicePermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

    /**
     * Test group permission failed.
     * @throws Exception the exception
     */
    public void testGroupPermissionFailed() throws Exception {
        Map<String, Object> testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser2"), "givePermission", "Y");
        try {
            Map<String, Object> results = getDispatcher().runSync("testSimpleGroupAndPermission", testingPermMap);
            assertFalse("The testGroupPermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }

        testingPermMap = UtilMisc.toMap("userLogin", getUserLogin("permUser1"), "givePermission", "N");
        try {
            Map<String, Object> results = getDispatcher().runSync("testSimpleGroupOrPermission", testingPermMap);
            assertFalse("The testGroupPermission don't raise service exception", ServiceUtil.isError(results));
        } catch (ServiceAuthException e) {
            assertNotNull(e);
        }
    }

}
