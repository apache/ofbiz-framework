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

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.webapp.control.RequestHandlerExceptionAllowExternalRequests;

public interface ICsrfDefenseStrategy {

    String generateToken();

    /**
     * Limit the number of subfolders in request uri to reduce the number of CSRF tokens needed.
     * @param requestUri
     * @return
     */
    int maxSubFolderInRequestUrlForTokenMapLookup(String requestUri);

    /**
     * Override security csrf-token value in request map
     * @param requestUri
     * @param requestMapMethod  get, post or all
     * @param securityCsrfToken
     * @return
     */
    boolean modifySecurityCsrfToken(String requestUri, String requestMapMethod, String securityCsrfToken);

    /**
     * Whether to reuse the token after it is consumed
     * @param requestUri
     * @param requestMethod GET, POST, or PUT
     * @return
     */
    boolean keepTokenAfterUse(String requestUri, String requestMethod);

    void invalidTokenResponse(String requestUri, HttpServletRequest request)
            throws RequestHandlerExceptionAllowExternalRequests;

}
