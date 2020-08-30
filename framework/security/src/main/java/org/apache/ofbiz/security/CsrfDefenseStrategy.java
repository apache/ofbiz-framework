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
package org.apache.ofbiz.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.webapp.control.RequestHandlerExceptionAllowExternalRequests;

public class CsrfDefenseStrategy implements ICsrfDefenseStrategy {

    private static final String MODULE = CsrfDefenseStrategy.class.getName();
    private static SecureRandom secureRandom = null;
    private static final String PRNG = "SHA1PRNG";
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static int requestlimit = (int) Long.parseLong(
            UtilProperties.getPropertyValue("security", "csrf.entity.request.limit", "3"));

    static {
        try {
            secureRandom = SecureRandom.getInstance(PRNG);
        } catch (NoSuchAlgorithmException e) {
            Debug.logError(e, MODULE);
        }
    }

    @Override
    public String generateToken() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < 12 + 1; i++) {
            int index = secureRandom.nextInt(CHARSET.length());
            char c = CHARSET.charAt(index);
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public int maxSubFolderInRequestUrlForTokenMapLookup(String requestUri) {
        if (requestUri.startsWith("entity/")) {
            return requestlimit;
        }
        return 0;
    }

    @Override
    public boolean modifySecurityCsrfToken(String requestUri, String requestMapMethod, String securityCsrfToken) {
        // main request URI is exempted from CSRF token check
        if ("main".equals(requestUri)) {
            return false;
        } else {
            return !"false".equals(securityCsrfToken);
        }
    }


    @Override
    public boolean keepTokenAfterUse(String requestUri, String requestMethod) {
        // to allow back and forth browser buttons to work,
        // token value is unchanged when request.getMethod is GET
        if ("GET".equals(requestMethod)) {
            return true;
        }
        return false;
    }

    @Override
    public void invalidTokenResponse(String requestUri, HttpServletRequest request)
            throws RequestHandlerExceptionAllowExternalRequests {
        request.setAttribute("_ERROR_MESSAGE_",
                "Invalid or missing CSRF token to path '" + request.getPathInfo() + "'. Click <a href='"
                        + request.getContextPath() + "'>here</a> to continue.");
        throw new RequestHandlerExceptionAllowExternalRequests();
    }
}
