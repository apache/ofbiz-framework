/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ofbiz.ws.rs.util;

import java.util.Map;

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.LocalDispatcher;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves REST runtime objects from the request when a tenant was selected.
 * A tenant-marked request must never fall back to the shared base context.
 */
public final class RestApiRequestContext {

    private RestApiRequestContext() { }

    public static Delegator getDelegator(HttpServletRequest request, ServletContext context) {
        return resolve(request, context, "delegator", Delegator.class);
    }

    public static LocalDispatcher getDispatcher(HttpServletRequest request, ServletContext context) {
        return resolve(request, context, "dispatcher", LocalDispatcher.class);
    }

    public static Security getSecurity(HttpServletRequest request, ServletContext context) {
        return resolve(request, context, "security", Security.class);
    }

    /**
     * Prevents tokens issued for one tenant from being replayed against a
     * different tenant when installations share an issuer or signing key.
     * Base requests accept only tokens without a tenant claim.
     */
    public static boolean matchesTenant(HttpServletRequest request, Map<String, ?> claims) {
        String currentTenant = (String) request.getAttribute("userTenantId");
        Object issuedTenant = claims.get("tenantId");
        return currentTenant == null ? issuedTenant == null : currentTenant.equals(issuedTenant);
    }

    private static <T> T resolve(HttpServletRequest request, ServletContext context, String name, Class<T> type) {
        Object value = request.getAttribute(name);
        // A partially populated tenant context is an error, not a reason to
        // execute against the base database or base authorization context.
        if (value == null && request.getAttribute("userTenantId") == null) {
            value = context.getAttribute(name);
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Missing REST API request context: " + name);
        }
        return type.cast(value);
    }
}
