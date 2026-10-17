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
package org.apache.ofbiz.ws.rs.test;

import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;
import org.apache.ofbiz.ws.rs.util.RestServiceUtil;

public class RestTestServices {

    /**
     * Testservice returning a custom ErrorCode
     *
     * @param dctx
     * @param context
     * @return result
     */
    public static Map<String, Object> returnCustomErrorTest(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = RestServiceUtil.returnError("Planned test-error, can be ignored", 999);
        return result;
    }

    // ============== Status Code Test Services =================== //
    /**
     * TestService returning a success
     *
     * @param dctx
     * @param context
     * @return result
     */
    public static Map<String, Object> returnSuccess(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        return result;
    }

    /**
     * TestService returning a success but explicitly returns status code 201 instead of default 200
     *
     * @param dctx
     * @param context
     * @return result
     */
    public static Map<String, Object> returnSuccessButOverwriteStatusCode(DispatchContext dctx, Map<String, ? extends Object> context) {
        return RestServiceUtil.returnSuccess(null, 201);
    }

    /**
     * TestService requiring a customHeader 'x-custom-header' to be present
     *
     * @param dctx
     * @param context
     * @return result
     */
    public static Map<String, Object> useCustomHeaderAsServiceParameter(DispatchContext dctx, Map<String, ? extends Object> context) {
        String customHeader = (String) context.get("x-custom-header");
        if (UtilValidate.isEmpty(customHeader)) {
            return ServiceUtil.returnError("Missing custom header 'x-custom-header'");
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("x-custom-header", customHeader);
        return result;
    }

    /**
     * TestService returning the received locale as a String
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map<String, Object> useLocaleSetInRequestHeader(DispatchContext dctx, Map<String, ? extends Object> context) {
        Locale locale = (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        String localeAsString = locale.toString();
        result.put("localeAsString", localeAsString);
        return result;
    }

    public static Map<String, Object> testServiceInputParameters(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Debug.logInfo("My value" + (String) context.get("myInput"), null);
        result.put("myInput", (String) context.get("myInput"));
        return result;
    }
}
