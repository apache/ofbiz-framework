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
package org.apache.ofbiz.securityext.login;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.party.contact.ContactHelper;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Security notification services bridging authentication lifecycle events with the Party application.
 */
public final class MfaNotificationServices {

    private static final String MODULE = MfaNotificationServices.class.getName();
    private static final String RESOURCE = "SecurityextUiLabels";

    private MfaNotificationServices() { }

    /**
     * Dispatches an email notification to the party associated with an MFA authentication event.
     * @param ctx the dispatch context
     * @param context service input parameters
     * @return service result map
     */
    public static Map<String, Object> sendMfaSecurityNotification(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        String userLoginId = (String) context.get("userLoginId");
        String actorUserLoginId = (String) context.get("actorUserLoginId");
        String eventType = (String) context.get("eventType");
        String factorType = (String) context.get("factorType");
        String reason = (String) context.get("reason");

        if (delegator == null || UtilValidate.isEmpty(userLoginId)) {
            return ServiceUtil.returnSuccess();
        }
        boolean emailEnabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                "security.login.authFactor.notify.email.enable", "false", delegator));
        if (!emailEnabled) {
            return ServiceUtil.returnSuccess();
        }

        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                    .where("userLoginId", userLoginId).cache(false).queryOne();
            if (userLogin == null) {
                return ServiceUtil.returnSuccess();
            }
            String partyId = userLogin.getString("partyId");
            if (UtilValidate.isEmpty(partyId)) {
                return ServiceUtil.returnSuccess();
            }

            GenericValue userParty = userLogin.getRelatedOne("Party", false);
            String emailAddress = null;
            if (userParty != null) {
                Collection<GenericValue> contactMechs = ContactHelper.getContactMechByPurpose(userParty, "PRIMARY_EMAIL", false);
                if (UtilValidate.isNotEmpty(contactMechs)) {
                    emailAddress = contactMechs.iterator().next().getString("infoString");
                }
            }
            if (UtilValidate.isEmpty(emailAddress)) {
                GenericValue pcm = EntityQuery.use(delegator).from("PartyAndContactMech")
                        .where("partyId", partyId, "contactMechTypeId", "EMAIL_ADDRESS")
                        .filterByDate().queryFirst();
                if (pcm != null) {
                    emailAddress = pcm.getString("infoString");
                }
            }
            if (UtilValidate.isEmpty(emailAddress)) {
                return ServiceUtil.returnSuccess();
            }

            String fromAddress = EntityUtilProperties.getPropertyValue("security",
                    "security.login.authFactor.notify.email.from", "notifications-noreply@example.com", delegator);

            Locale locale = UtilMisc.ensureLocale(userLogin.get("lastLocale"));
            Timestamp now = UtilDateTime.nowTimestamp();
            Map<String, Object> messageMap = UtilMisc.toMap("userLoginId", userLoginId, "eventTime", now,
                    "factorType", factorType != null ? factorType : "Authenticator",
                    "reason", reason, "actorUserLoginId", actorUserLoginId);

            String subject;
            StringBuilder body = new StringBuilder();
            body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.security_alert_greeting", locale)).append("\n\n");

            switch (eventType) {
            case "MFA_ENROLL":
                subject = UtilProperties.getMessage(RESOURCE, "mfanotificationservices.mfa_enroll_subject", locale);
                body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.mfa_enroll_body", messageMap, locale))
                        .append("\n\n");
                break;
            case "MFA_FACTOR_REVOKED":
                subject = UtilProperties.getMessage(RESOURCE, "mfanotificationservices.mfa_factor_revoked_subject", locale);
                body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.mfa_factor_revoked_body", messageMap, locale))
                        .append("\n");
                if (reason != null && !reason.isBlank()) {
                    body.append(UtilProperties.getMessage(RESOURCE,
                            "mfanotificationservices.mfa_factor_revoked_reason_line", messageMap, locale)).append("\n");
                }
                if (actorUserLoginId != null && !actorUserLoginId.equals(userLoginId)) {
                    body.append(UtilProperties.getMessage(RESOURCE,
                            "mfanotificationservices.mfa_factor_revoked_by_admin_line", messageMap, locale)).append("\n");
                }
                body.append("\n");
                break;
            case "LOGIN_COMMITTED":
                subject = UtilProperties.getMessage(RESOURCE, "mfanotificationservices.login_committed_subject", locale);
                body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.login_committed_body", messageMap, locale))
                        .append("\n\n");
                break;
            default:
                return ServiceUtil.returnSuccess();
            }

            body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.security_alert_time_line", messageMap, locale))
                    .append("\n\n");
            body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.security_alert_footer", locale)).append("\n\n");
            body.append(UtilProperties.getMessage(RESOURCE, "mfanotificationservices.security_alert_signature", locale));

            if (dispatcher != null) {
                Debug.logInfo("Dispatching MFA security alert email [" + subject + "] to " + emailAddress
                        + " for user [" + userLoginId + "]", MODULE);
                Map<String, Object> emailCtx = new HashMap<>();
                emailCtx.put("sendTo", emailAddress);
                emailCtx.put("sendFrom", fromAddress);
                emailCtx.put("subject", subject);
                emailCtx.put("body", body.toString());
                emailCtx.put("partyId", partyId);
                try {
                    dispatcher.runAsync("sendMail", emailCtx);
                } catch (GenericServiceException e) {
                    Debug.logWarning("Unable to dispatch security alert email to " + emailAddress + ": " + e.getMessage(), MODULE);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, "Error preparing security alert notification for " + userLoginId, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
