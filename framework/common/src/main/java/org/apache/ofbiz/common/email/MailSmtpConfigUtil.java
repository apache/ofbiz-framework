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
package org.apache.ofbiz.common.email;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

/**
 * Resolves {@code MailSmtpConfig} entity fields, falling back per-field to legacy
 * {@code mail.smtp.*} general.properties/SystemProperty keys when unset. Shared by
 * {@link EmailServices} and {@link SmtpOAuth2TokenProvider}.
 */
public final class MailSmtpConfigUtil {

    private static final String MODULE = MailSmtpConfigUtil.class.getName();

    private MailSmtpConfigUtil() { }

    /** Resolved SMTP configuration, after entity + legacy-property fallback merge. */
    public static final class ResolvedConfig {
        //ALLOW PUBLIC FIELDS
        public final String mailSmtpConfigId;
        public final String relayHost;
        public final String port;
        public final boolean starttlsEnable;
        public final String socketFactoryPort;
        public final String socketFactoryClass;
        public final String socketFactoryFallback;
        public final boolean sendPartial;
        public final String authMechanism;
        public final String authUser;
        public final String authPassword;
        public final String oauth2ClientId;
        public final String oauth2ClientSecret;
        public final String oauth2RefreshToken;
        public final String oauth2TokenEndpoint;
        public final String oauth2Scope;
        //FORBID PUBLIC FIELDS

        ResolvedConfig(String mailSmtpConfigId, String relayHost, String port, boolean starttlsEnable,
                String socketFactoryPort, String socketFactoryClass, String socketFactoryFallback,
                boolean sendPartial, String authMechanism, String authUser, String authPassword,
                String oauth2ClientId, String oauth2ClientSecret, String oauth2RefreshToken,
                String oauth2TokenEndpoint, String oauth2Scope) {
            this.mailSmtpConfigId = mailSmtpConfigId;
            this.relayHost = relayHost;
            this.port = port;
            this.starttlsEnable = starttlsEnable;
            this.socketFactoryPort = socketFactoryPort;
            this.socketFactoryClass = socketFactoryClass;
            this.socketFactoryFallback = socketFactoryFallback;
            this.sendPartial = sendPartial;
            this.authMechanism = authMechanism;
            this.authUser = authUser;
            this.authPassword = authPassword;
            this.oauth2ClientId = oauth2ClientId;
            this.oauth2ClientSecret = oauth2ClientSecret;
            this.oauth2RefreshToken = oauth2RefreshToken;
            this.oauth2TokenEndpoint = oauth2TokenEndpoint;
            this.oauth2Scope = oauth2Scope;
        }
    }

    /** Looks up the (at most one, for now) MailSmtpConfig row and resolves it per {@link ResolvedConfig}. */
    public static ResolvedConfig resolve(Delegator delegator) {
        GenericValue configRow;
        try {
            configRow = EntityQuery.use(delegator).from("MailSmtpConfig").cache(true).orderBy("mailSmtpConfigId").queryFirst();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Error loading MailSmtpConfig entity; falling back to legacy properties", MODULE);
            configRow = null;
        }
        String mailSmtpConfigId = configRow != null ? configRow.getString("mailSmtpConfigId") : null;

        String relayHost = valueOrFallback(configRow, "relayHost", delegator, "mail.smtp.relay.host", "localhost");
        String port = valueOrFallback(configRow, "port", delegator, "mail.smtp.port", "");
        boolean starttlsEnable = booleanOrFallback(configRow, "starttlsEnable", delegator, "mail.smtp.starttls.enable");
        String socketFactoryPort = valueOrFallback(configRow, "socketFactoryPort", delegator, "mail.smtp.socketFactory.port", "");
        String socketFactoryClass = valueOrFallback(configRow, "socketFactoryClass", delegator, "mail.smtp.socketFactory.class", "");
        String socketFactoryFallback = valueOrFallback(configRow, "socketFactoryFallback", delegator,
                "mail.smtp.socketFactory.fallback", "false");
        boolean sendPartial = booleanOrFallback(configRow, "sendPartial", delegator, "mail.smtp.sendpartial");
        String authUser = valueOrFallback(configRow, "authUser", delegator, "mail.smtp.auth.user", "");
        String authPassword = valueOrFallback(configRow, "authPassword", delegator, "mail.smtp.auth.password", "");

        String authMechanism = configRow != null ? configRow.getString("authMechanism") : null;
        if (UtilValidate.isEmpty(authMechanism)) {
            authMechanism = UtilValidate.isNotEmpty(authUser) ? "BASIC" : "NONE";
        }

        return new ResolvedConfig(mailSmtpConfigId, relayHost, port, starttlsEnable, socketFactoryPort,
                socketFactoryClass, socketFactoryFallback, sendPartial, authMechanism, authUser, authPassword,
                entityValue(configRow, "oauth2ClientId"), entityValue(configRow, "oauth2ClientSecret"),
                entityValue(configRow, "oauth2RefreshToken"), entityValue(configRow, "oauth2TokenEndpoint"),
                entityValue(configRow, "oauth2Scope"));
    }

    /** OAuth2 fields have no legacy fallback; an unset entity field just resolves to "". */
    private static String entityValue(GenericValue configRow, String fieldName) {
        String value = configRow != null ? configRow.getString(fieldName) : null;
        return value != null ? value : "";
    }

    private static String valueOrFallback(GenericValue configRow, String fieldName, Delegator delegator,
            String legacyKey, String legacyDefault) {
        String value = configRow != null ? configRow.getString(fieldName) : null;
        if (UtilValidate.isNotEmpty(value)) {
            return value;
        }
        return EntityUtilProperties.getPropertyValue("general", legacyKey, legacyDefault, delegator);
    }

    private static boolean booleanOrFallback(GenericValue configRow, String fieldName, Delegator delegator, String legacyKey) {
        String value = configRow != null ? configRow.getString(fieldName) : null;
        if (UtilValidate.isNotEmpty(value)) {
            return "Y".equalsIgnoreCase(value);
        }
        return EntityUtilProperties.propertyValueEqualsIgnoreCase("general", legacyKey, "true", delegator);
    }
}
