/*
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
 */
package org.apache.ofbiz.security.login;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Encapsulates the method, origin, and claims associated with an authentication attempt.
 * Evaluates whether external assurance satisfies MFA requirements (RFC 8176 amr, acr, trusted sources).
 */
@SuppressWarnings("serial")
public final class AuthenticationContext implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String METHOD_PASSWORD = "PASSWORD";
    public static final String METHOD_JWT = "JWT";
    public static final String METHOD_HTTP_HEADER = "HTTP_HEADER";
    public static final String METHOD_REMOTE_USER = "REMOTE_USER";
    public static final String METHOD_TOMCAT_SSO = "TOMCAT_SSO";
    public static final String METHOD_X509 = "X509";
    public static final String METHOD_EXTERNAL_LOGIN_KEY = "EXTERNAL_LOGIN_KEY";

    private final String authMethod;
    private final boolean interactive;
    private final boolean upstreamTrusted;
    private final String issuer;
    private final List<String> amr;
    private final String acr;
    private final Map<String, Object> claims;

    public AuthenticationContext(String authMethod) {
        this(authMethod, null, true, false, null, null, null);
    }

    public AuthenticationContext(String authMethod, Map<String, Object> claims) {
        this(authMethod,
                claims != null && claims.get("iss") != null ? String.valueOf(claims.get("iss")) : null,
                true,
                true,
                extractAmr(claims),
                claims != null && claims.get("acr") != null ? String.valueOf(claims.get("acr")) : null,
                claims);
    }

    public AuthenticationContext(String authMethod, String issuer, boolean interactive, boolean upstreamTrusted,
                                 List<String> amr, String acr, Map<String, Object> claims) {
        this.authMethod = authMethod != null ? authMethod : METHOD_PASSWORD;
        this.issuer = issuer;
        this.interactive = interactive;
        this.upstreamTrusted = upstreamTrusted;
        this.amr = amr != null ? Collections.unmodifiableList(new ArrayList<>(amr)) : Collections.emptyList();
        this.acr = acr;
        this.claims = claims != null ? Collections.unmodifiableMap(claims) : Collections.emptyMap();
    }

    private static List<String> extractAmr(Map<String, Object> claims) {
        if (claims == null) {
            return Collections.emptyList();
        }
        Object amrObj = claims.get("amr");
        if (amrObj instanceof List<?>) {
            List<String> list = new ArrayList<>();
            for (Object item : (List<?>) amrObj) {
                if (item != null) {
                    list.add(String.valueOf(item).trim());
                }
            }
            return list;
        } else if (amrObj instanceof String[]) {
            List<String> list = new ArrayList<>();
            for (String item : (String[]) amrObj) {
                if (item != null) {
                    list.add(item.trim());
                }
            }
            return list;
        } else if (amrObj instanceof String) {
            return StringUtil.split((String) amrObj, ",");
        } else if (claims.get("amrString") instanceof String) {
            return StringUtil.split((String) claims.get("amrString"), ",");
        }
        return Collections.emptyList();
    }

    public String getAuthMethod() {
        return authMethod;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean isUpstreamTrusted() {
        return upstreamTrusted;
    }

    public String getIssuer() {
        return issuer;
    }

    public List<String> getAmr() {
        return amr;
    }

    public String getAcr() {
        return acr;
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    /**
     * Evaluates whether this authentication context provides verified proof of multi-factor authentication
     * satisfying policy without requiring a local interactive step-up factor.
     */
    public boolean isMfaSatisfied(Delegator delegator) {
        if (METHOD_JWT.equalsIgnoreCase(authMethod)) {
            // 1. Verify issuer trust if configured
            String trustedIssuers = getProperty("security", "security.jwt.mfa.issuers.trusted", "", delegator);
            if (UtilValidate.isNotEmpty(trustedIssuers) && !"*".equals(trustedIssuers.trim())) {
                List<String> allowed = StringUtil.split(trustedIssuers, ",");
                if (issuer == null || !allowed.contains(issuer.trim())) {
                    return false;
                }
            }

            // 2. Verify AMR claim (RFC 8176)
            String trustedAmrStr = getProperty("security", "security.jwt.mfa.amr.trusted",
                    "mfa,otp,totp,sms,hwk,fido", delegator);
            List<String> trustedAmrList = StringUtil.split(trustedAmrStr.toLowerCase(Locale.ROOT), ",");
            for (String val : amr) {
                if (trustedAmrList.contains(val.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }

            // 3. Verify ACR claim if configured
            String trustedAcrStr = getProperty("security", "security.jwt.mfa.acr.trusted", "", delegator);
            if (UtilValidate.isNotEmpty(trustedAcrStr)) {
                List<String> trustedAcrList = StringUtil.split(trustedAcrStr, ",");
                if (acr != null && trustedAcrList.contains(acr.trim())) {
                    return true;
                }
            }
            return false;
        }

        if (METHOD_HTTP_HEADER.equalsIgnoreCase(authMethod)) {
            return "true".equalsIgnoreCase(getProperty("security", "security.login.http.header.mfaTrusted", "false", delegator));
        }

        if (METHOD_REMOTE_USER.equalsIgnoreCase(authMethod) || METHOD_TOMCAT_SSO.equalsIgnoreCase(authMethod)) {
            return "true".equalsIgnoreCase(getProperty("security", "security.login.tomcat.sso.mfaTrusted", "false", delegator));
        }

        if (METHOD_X509.equalsIgnoreCase(authMethod)) {
            return "true".equalsIgnoreCase(getProperty("security", "security.login.cert.mfaTrusted", "false", delegator));
        }

        if (METHOD_EXTERNAL_LOGIN_KEY.equalsIgnoreCase(authMethod)) {
            return upstreamTrusted;
        }

        // Default: primary password authentication does NOT satisfy MFA
        return false;
    }

    private static String getProperty(String resource, String name, String defaultValue, Delegator delegator) {
        if (delegator != null) {
            return EntityUtilProperties.getPropertyValue(resource, name, defaultValue, delegator);
        }
        return org.apache.ofbiz.base.util.UtilProperties.getPropertyValue(resource, name, defaultValue);
    }
}
