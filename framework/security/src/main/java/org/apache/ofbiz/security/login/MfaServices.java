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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceContainer;
import org.apache.ofbiz.service.ServiceUtil;

/** Services for interactive TOTP and recovery-code authentication. */
public final class MfaServices {
    public static final String PENDING_CHALLENGE = "_MFA_PENDING_CHALLENGE_";
    public static final String PENDING_USER = "_MFA_PENDING_USERLOGIN_";
    public static final String VERIFIED_CHALLENGE = "_MFA_VERIFIED_CHALLENGE_";
    public static final String PRIMARY_VERIFIED = "_MFA_PRIMARY_VERIFIED_";
    public static final String PENDING_ENROLLMENT = "_MFA_PENDING_ENROLLMENT_";
    public static final String PENDING_ENROLLMENT_FACTOR = "_MFA_PENDING_ENROLLMENT_FACTOR_";
    public static final String MFA_VERIFIED_AT = "_MFA_VERIFIED_AT_";
    private static final String MODULE = MfaServices.class.getName();
    private static final String TOTP = "ULAF_TOTP";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private MfaServices() { }

    public static boolean isMfaRequired(Delegator delegator, String userLoginId) throws GenericEntityException {
        GenericValue targetUserLogin = userLoginId == null ? null : EntityQuery.use(delegator)
                .from("UserLogin").where("userLoginId", userLoginId).cache(false).queryOne();
        if (targetUserLogin == null || "Y".equals(targetUserLogin.getString("isSystem"))) {
            return false;
        }
        boolean enabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                "security.login.authFactor.enable", "false", delegator));
        return enabled && (!activeFactors(delegator, userLoginId).isEmpty()
                || hasRequiredSecurityGroup(delegator, userLoginId));
    }

    public static Map<String, Object> isUserLoginMfaRequired(DispatchContext ctx, Map<String, ?> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = ctx.getDelegator();
        String userLoginId = (String) context.get("userLoginId");
        AuthenticationContext authCtx = (AuthenticationContext) context.get("authenticationContext");
        try {
            boolean policyApplies = isMfaRequired(delegator, userLoginId);
            if (!policyApplies) {
                result.put("required", false);
                result.put("mfaSatisfied", false);
                return result;
            }
            boolean mfaSatisfied = authCtx != null && authCtx.isMfaSatisfied(delegator);
            result.put("mfaSatisfied", mfaSatisfied);
            result.put("required", !mfaSatisfied);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to evaluate authentication-factor policy");
        }
    }

    public static Map<String, Object> createUserLoginTotpFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null || !reauthenticate(ctx, userLogin, (String) context.get("currentPassword"), context.get("locale"))) {
            return ServiceUtil.returnError("Current credentials could not be verified");
        }
        Delegator delegator = ctx.getDelegator();
        String secret = new DefaultSecretGenerator().generate();
        String factorId = delegator.getNextSeqId("UserLoginAuthFactor");
        String label = (String) context.get("label");
        String issuer = EntityUtilProperties.getPropertyValue("security", "security.login.authFactor.totp.issuer",
                "Apache OFBiz", delegator);
        try {
            delegator.storeByCondition("UserLoginAuthFactor", Map.of("thruDate", UtilDateTime.nowTimestamp()), and(
                    EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLogin.getString("userLoginId")),
                    EntityCondition.makeCondition("factorType", EntityOperator.EQUALS, TOTP),
                    EntityCondition.makeCondition("needsValidation", EntityOperator.EQUALS, "Y"),
                    EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
            GenericValue factor = delegator.makeValue("UserLoginAuthFactor", Map.of(
                    "factorId", factorId,
                    "userLoginId", userLogin.getString("userLoginId"),
                    "factorType", TOTP,
                    "secret", secret,
                    "factorLabel", label == null ? "Authenticator" : label,
                    "needsValidation", "Y",
                    "fromDate", UtilDateTime.nowTimestamp()));
            factor.create();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to create pending authenticator factor");
        }
        String account = userLogin.getString("userLoginId");
        String uri = "otpauth://totp/" + urlEncode(issuer + ":" + account) + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer) + "&algorithm=SHA1&digits=6&period=30";
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("factorId", factorId);
        result.put("secret", secret);
        result.put("provisioningUri", uri);
        result.put("qrCodeDataUri", generateQrCodeDataUri(uri, 220, 220));
        return result;
    }

    public static Map<String, Object> confirmUserLoginTotpFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String factorId = (String) context.get("factorId");
        String code = (String) context.get("code");
        if (userLogin == null || factorId == null || code == null) {
            return ServiceUtil.returnError("Authenticator confirmation failed");
        }
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId, "userLoginId", userLogin.getString("userLoginId"),
                            "factorType", TOTP, "needsValidation", "Y", "thruDate", null)
                    .cache(false).queryOne();
            long acceptedStep = findMatchingTotpStep(factor == null ? null : factor.getString("secret"), code);
            if (factor == null || acceptedStep < 0) {
                return ServiceUtil.returnError("Authenticator confirmation failed");
            }
            factor.set("needsValidation", "N");
            factor.set("lastUsedStep", acceptedStep);
            factor.set("lastUsedDate", UtilDateTime.nowTimestamp());
            factor.store();
            recordEvent(delegator, userLogin.getString("userLoginId"), "MFA_ENROLL", TOTP, null);
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Authenticator confirmation failed");
        }
    }

    public static Map<String, Object> getUserLoginAuthFactors(DispatchContext ctx, Map<String, ?> context) {
        GenericValue actor = (GenericValue) context.get("userLogin");
        String targetUserLoginId = (String) context.get("targetUserLoginId");
        if (targetUserLoginId == null && actor != null) {
            targetUserLoginId = actor.getString("userLoginId");
        }
        if (actor == null || targetUserLoginId == null) {
            return ServiceUtil.returnError("Unable to retrieve authentication factors");
        }
        boolean self = targetUserLoginId.equals(actor.getString("userLoginId"));
        if (!self && !ctx.getSecurity().hasEntityPermission("SECURITY", "_VIEW", actor)) {
            return ServiceUtil.returnError("Not authorized to view authentication factors");
        }
        try {
            List<GenericValue> storedFactors = EntityQuery.use(ctx.getDelegator()).from("UserLoginAuthFactor")
                    .where("userLoginId", targetUserLoginId).cache(false).queryList();
            Timestamp now = UtilDateTime.nowTimestamp();
            List<Map<String, Object>> safeFactors = new ArrayList<>();
            for (GenericValue factor : storedFactors) {
                Timestamp fromDate = factor.getTimestamp("fromDate");
                Timestamp thruDate = factor.getTimestamp("thruDate");
                if (fromDate != null && fromDate.after(now)) {
                    continue;
                }
                if (thruDate != null && !thruDate.after(now)) {
                    continue;
                }
                Map<String, Object> safeFactor = new LinkedHashMap<>();
                safeFactor.put("factorId", factor.getString("factorId"));
                safeFactor.put("factorType", factor.getString("factorType"));
                safeFactor.put("factorLabel", factor.getString("factorLabel"));
                safeFactor.put("needsValidation", factor.getString("needsValidation"));
                safeFactor.put("fromDate", factor.getTimestamp("fromDate"));
                safeFactor.put("lastUsedDate", factor.getTimestamp("lastUsedDate"));
                safeFactors.add(safeFactor);
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("factors", safeFactors);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to retrieve authentication factors");
        }
    }

    public static Map<String, Object> revokeUserLoginAuthFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue actor = (GenericValue) context.get("userLogin");
        String factorId = (String) context.get("factorId");
        String currentPassword = (String) context.get("currentPassword");
        String reason = (String) context.get("reason");
        if (actor == null || factorId == null || reason != null && reason.length() > 255) {
            return ServiceUtil.returnError("Unable to revoke authentication factor");
        }
        try {
            Delegator delegator = ctx.getDelegator();
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId).cache(false).queryOne();
            if (factor == null || factor.getTimestamp("thruDate") != null) {
                return ServiceUtil.returnError("Authentication factor is unavailable");
            }
            String targetUserLoginId = factor.getString("userLoginId");
            String actorUserLoginId = actor.getString("userLoginId");
            boolean self = actorUserLoginId.equals(targetUserLoginId);
            if (self) {
                if (!reauthenticate(ctx, actor, currentPassword, context.get("locale"))) {
                    return ServiceUtil.returnError("Current credentials could not be verified");
                }
            } else if (!ctx.getSecurity().hasEntityPermission("SECURITY", "_UPDATE", actor)
                    || reason == null || reason.isBlank()) {
                return ServiceUtil.returnError("Not authorized to revoke this authentication factor");
            }
            boolean mfaEnabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                    "security.login.authFactor.enable", "false", delegator));
            if (self && mfaEnabled && hasRequiredSecurityGroup(delegator, targetUserLoginId)
                    && activeFactors(delegator, targetUserLoginId).size() <= 1) {
                return ServiceUtil.returnError("A required authentication factor cannot be removed while the group policy is active");
            }
            Timestamp now = UtilDateTime.nowTimestamp();
            int updated = delegator.storeByCondition("UserLoginAuthFactor", Map.of("thruDate", now), and(
                    EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factorId),
                    EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, targetUserLoginId),
                    EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
            if (updated != 1) {
                return ServiceUtil.returnError("Authentication factor is unavailable");
            }
            recordEvent(delegator, targetUserLoginId, actorUserLoginId, "MFA_FACTOR_REVOKED",
                    factor.getString("factorType"), null, reason);
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to revoke authentication factor");
        }
    }

    public static Map<String, Object> createUserLoginMfaChallenge(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (userLogin == null || request == null) {
            return ServiceUtil.returnError("Unable to start authentication challenge");
        }
        Delegator delegator = ctx.getDelegator();
        try {
            List<GenericValue> factors = activeFactors(delegator, userLogin.getString("userLoginId"));
            boolean mustEnroll = factors.isEmpty()
                    && hasRequiredSecurityGroup(delegator, userLogin.getString("userLoginId"));
            if (factors.isEmpty() && !mustEnroll) {
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("required", false);
                return result;
            }
            Timestamp now = UtilDateTime.nowTimestamp();
            String challengeId = randomCode();
            int ttl = getIntProperty(delegator, "security.login.authFactor.pendingTimeout", 300);
            GenericValue challenge = delegator.makeValue("UserLoginAuthChallenge", Map.of(
                    "challengeId", challengeId,
                    "userLoginId", userLogin.getString("userLoginId"),
                    "delegatorName", delegator.getDelegatorName(),
                    "sessionBindingHash", sessionHash(request.getSession().getId()),
                    "challengeStatus", "PENDING",
                    "attemptCount", 0L,
                    "createdDate", now,
                    "expiresDate", new Timestamp(now.getTime() + ttl * 1000L)));
            challenge.create();
            HttpSession session = request.getSession();
            session.setAttribute(PENDING_CHALLENGE, challengeId);
            session.setAttribute(PENDING_USER, userLogin.getString("userLoginId"));
            if (mustEnroll) {
                session.setAttribute(PENDING_ENROLLMENT, Boolean.TRUE);
            } else {
                session.removeAttribute(PENDING_ENROLLMENT);
            }
            recordEvent(delegator, userLogin.getString("userLoginId"), "MFA_REQUIRED", null, challengeId);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("required", true);
            result.put("mustEnroll", mustEnroll);
            result.put("challengeId", challengeId);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to start authentication challenge");
        }
    }

    public static Map<String, Object> completeForcedTotpEnrollment(DispatchContext ctx, Map<String, ?> context) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request == null) {
            return ServiceUtil.returnError("Unable to complete authenticator enrollment");
        }
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(PENDING_ENROLLMENT))) {
            return ServiceUtil.returnError("Unable to complete authenticator enrollment");
        }
        String pendingUserId = (String) session.getAttribute(PENDING_USER);
        String factorId = (String) session.getAttribute(PENDING_ENROLLMENT_FACTOR);
        String code = (String) context.get("code");
        if (pendingUserId == null || factorId == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError("Unable to complete authenticator enrollment");
        }
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                    .where("challengeId", session.getAttribute(PENDING_CHALLENGE), "userLoginId", pendingUserId,
                            "delegatorName", delegator.getDelegatorName(), "challengeStatus", "PENDING")
                    .cache(false).queryOne();
            if (!challengeIsValid(challenge, request)) {
                return ServiceUtil.returnError("Unable to complete authenticator enrollment");
            }
            int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
            long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
            if (attempts >= maxAttempts) {
                return ServiceUtil.returnError("Unable to complete authenticator enrollment");
            }
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId, "userLoginId", pendingUserId, "factorType", TOTP,
                            "needsValidation", "Y", "thruDate", null).cache(false).queryOne();
            long acceptedStep = findMatchingTotpStep(factor == null ? null : factor.getString("secret"), code);
            if (factor == null || acceptedStep < 0) {
                incrementChallengeAttempts(delegator, challenge, attempts, maxAttempts);
                recordEvent(delegator, pendingUserId, "MFA_VERIFY_FAILURE", TOTP,
                        (String) session.getAttribute(PENDING_CHALLENGE));
                return ServiceUtil.returnError("Authenticator confirmation failed");
            }
            int activated = delegator.storeByCondition("UserLoginAuthFactor",
                    Map.of("needsValidation", "N", "lastUsedDate", UtilDateTime.nowTimestamp()),
                    and(EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factorId),
                            EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, pendingUserId),
                            EntityCondition.makeCondition("needsValidation", EntityOperator.EQUALS, "Y"),
                            EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
            if (activated != 1) {
                return ServiceUtil.returnError("Authenticator confirmation failed");
            }
            session.removeAttribute(PENDING_ENROLLMENT);
            session.removeAttribute(PRIMARY_VERIFIED);
            recordEvent(delegator, pendingUserId, "MFA_ENROLL", TOTP, null);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("activated", true);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Authenticator confirmation failed");
        }
    }

    public static Map<String, Object> verifyUserLoginMfaChallenge(DispatchContext ctx, Map<String, ?> context) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        String code = (String) context.get("code");
        if (request == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError("Invalid or expired authentication challenge");
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ServiceUtil.returnError("Invalid or expired authentication challenge");
        }
        String challengeId = (String) session.getAttribute(PENDING_CHALLENGE);
        String pendingUserId = (String) session.getAttribute(PENDING_USER);
        if (challengeId == null || pendingUserId == null) {
            return ServiceUtil.returnError("Invalid or expired authentication challenge");
        }
        Delegator delegator = ctx.getDelegator();
        String currentDelegator = delegator.getDelegatorName();
        try {
            GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                    .where("challengeId", challengeId, "userLoginId", pendingUserId,
                            "delegatorName", currentDelegator, "challengeStatus", "PENDING")
                    .cache(false).queryOne();
            if (!challengeIsValid(challenge, request)) {
                return ServiceUtil.returnError("Invalid or expired authentication challenge");
            }
            int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
            long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
            if (attempts >= maxAttempts) {
                return ServiceUtil.returnError("Invalid or expired authentication challenge");
            }
            GenericValue usedFactor = consumeTotpCode(delegator, pendingUserId, code);
            if (usedFactor == null) {
                incrementChallengeAttempts(delegator, challenge, attempts, maxAttempts);
                recordEvent(delegator, pendingUserId, "MFA_VERIFY_FAILURE", null, challengeId);
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("verified", false);
                return result;
            }
            int claimed = delegator.storeByCondition("UserLoginAuthChallenge",
                    Map.of("challengeStatus", "VERIFIED", "verifiedDate", UtilDateTime.nowTimestamp(),
                            "factorId", usedFactor.getString("factorId")),
                    and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challengeId),
                            EntityCondition.makeCondition("challengeStatus", EntityOperator.EQUALS, "PENDING"),
                            EntityCondition.makeCondition("attemptCount", EntityOperator.EQUALS, attempts),
                            EntityCondition.makeCondition("expiresDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp()),
                            EntityCondition.makeCondition("sessionBindingHash", EntityOperator.EQUALS,
                                    sessionHash(session.getId()))));
            if (claimed != 1) {
                return ServiceUtil.returnError("Invalid or expired authentication challenge");
            }
            session.setAttribute(VERIFIED_CHALLENGE, challengeId);
            recordEvent(delegator, pendingUserId, "MFA_VERIFY_SUCCESS", usedFactor.getString("factorType"), challengeId);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("verified", true);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Invalid or expired authentication challenge");
        }
    }

    public static boolean hasActiveFactors(Delegator delegator, String userLoginId) throws GenericEntityException {
        return !activeFactors(delegator, userLoginId).isEmpty();
    }

    public static boolean consumeVerifiedChallenge(Delegator delegator, HttpServletRequest request,
            String userLoginId, String challengeId) throws GenericEntityException {
        HttpSession session = request.getSession(false);
        if (session == null || !challengeId.equals(session.getAttribute(VERIFIED_CHALLENGE))) {
            return false;
        }
        int consumed = delegator.storeByCondition("UserLoginAuthChallenge", Map.of("challengeStatus", "CONSUMED"),
                and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challengeId),
                        EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId),
                        EntityCondition.makeCondition("delegatorName", EntityOperator.EQUALS, delegator.getDelegatorName()),
                        EntityCondition.makeCondition("challengeStatus", EntityOperator.EQUALS, "VERIFIED"),
                        EntityCondition.makeCondition("expiresDate", EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp()),
                        EntityCondition.makeCondition("sessionBindingHash", EntityOperator.EQUALS, sessionHash(session.getId()))));
        return consumed == 1;
    }

    private static boolean reauthenticate(DispatchContext ctx, GenericValue userLogin, String password, Object locale) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        try {
            Map<String, Object> loginContext = new LinkedHashMap<>();
            loginContext.put("username", userLogin.getString("userLoginId"));
            loginContext.put("password", password);
            if (locale != null) loginContext.put("locale", locale);
            Map<String, Object> result = ctx.getDispatcher().runSync("userLoginMfaPending", loginContext);
            return ServiceUtil.isSuccess(result)
                    && userLogin.getString("userLoginId").equals(((GenericValue) result.get("userLogin")).getString("userLoginId"));
        } catch (Exception e) {
            return false;
        }
    }

    private static List<GenericValue> activeFactors(Delegator delegator, String userLoginId) throws GenericEntityException {
        if (userLoginId == null) {
            return List.of();
        }
        List<GenericValue> factors = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                .where("userLoginId", userLoginId, "needsValidation", "N", "thruDate", null)
                .cache(false).queryList();
        Timestamp now = UtilDateTime.nowTimestamp();
        return factors.stream().filter(factor -> {
            Timestamp from = factor.getTimestamp("fromDate");
            return from == null || !from.after(now);
        }).toList();
    }

    private static boolean hasRequiredSecurityGroup(Delegator delegator, String userLoginId) throws GenericEntityException {
        Timestamp now = UtilDateTime.nowTimestamp();
        List<GenericValue> memberships = EntityQuery.use(delegator).from("UserLoginSecurityGroup")
                .where("userLoginId", userLoginId).cache(false).queryList();
        for (GenericValue membership : memberships) {
            Timestamp fromDate = membership.getTimestamp("fromDate");
            Timestamp thruDate = membership.getTimestamp("thruDate");
            if ((fromDate != null && fromDate.after(now)) || (thruDate != null && !thruDate.after(now))) {
                continue;
            }
            GenericValue securityGroup = EntityQuery.use(delegator).from("SecurityGroup")
                    .where("groupId", membership.getString("groupId")).cache(false).queryOne();
            if (securityGroup != null && "Y".equals(securityGroup.getString("requireAuthFactor"))) {
                return true;
            }
        }
        return false;
    }

    private static GenericValue consumeTotpCode(Delegator delegator, String userLoginId, String code)
            throws GenericEntityException {
        for (GenericValue factor : activeFactors(delegator, userLoginId)) {
            if (TOTP.equals(factor.getString("factorType"))) {
                long acceptedStep = findMatchingTotpStep(factor.getString("secret"), code);
                if (acceptedStep < 0) {
                    continue;
                }
                EntityCondition unusedStep = EntityCondition.makeCondition(List.of(
                        EntityCondition.makeCondition("lastUsedStep", EntityOperator.EQUALS, null),
                        EntityCondition.makeCondition("lastUsedStep", EntityOperator.LESS_THAN, acceptedStep)),
                        EntityOperator.OR);
                int consumed = delegator.storeByCondition("UserLoginAuthFactor",
                        Map.of("lastUsedStep", acceptedStep, "lastUsedDate", UtilDateTime.nowTimestamp()),
                        and(EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factor.getString("factorId")),
                                EntityCondition.makeCondition("needsValidation", EntityOperator.EQUALS, "N"),
                                EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), unusedStep));
                return consumed == 1 ? factor : null;
            }
        }
        return null;
    }

    public static GenericValue consumeFactorCode(Delegator delegator, String userLoginId, String code)
            throws GenericEntityException {
        if (code == null) {
            return null;
        }
        return consumeTotpCode(delegator, userLoginId, code);
    }

    public static Map<String, Object> createRestMfaChallenge(Delegator delegator, String userLoginId, String apiGroupPath)
            throws GenericEntityException {
        List<GenericValue> factors = activeFactors(delegator, userLoginId);
        boolean mustEnroll = factors.isEmpty() && hasRequiredSecurityGroup(delegator, userLoginId);
        List<String> allowedFactorHints = new ArrayList<>();
        if (mustEnroll) {
            allowedFactorHints.add("ENROLLMENT_REQUIRED");
            allowedFactorHints.add(TOTP);
        } else {
            for (GenericValue factor : factors) {
                String type = factor.getString("factorType");
                if (type != null && !allowedFactorHints.contains(type)) {
                    allowedFactorHints.add(type);
                }
            }
        }
        Timestamp now = UtilDateTime.nowTimestamp();
        String challengeId = randomCode();
        int ttl = getIntProperty(delegator, "security.login.authFactor.pendingTimeout", 300);
        String bindingHash = "rest:" + (apiGroupPath != null ? apiGroupPath : "api");
        GenericValue challenge = delegator.makeValue("UserLoginAuthChallenge", Map.of(
                "challengeId", challengeId,
                "userLoginId", userLoginId,
                "delegatorName", delegator.getDelegatorName(),
                "sessionBindingHash", bindingHash,
                "challengeStatus", "PENDING",
                "attemptCount", 0L,
                "createdDate", now,
                "expiresDate", new Timestamp(now.getTime() + ttl * 1000L)));
        challenge.create();
        recordEvent(delegator, userLoginId, "MFA_REQUIRED", null, challengeId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("challengeId", challengeId);
        result.put("allowedFactorHints", allowedFactorHints);
        result.put("mustEnroll", mustEnroll);
        return result;
    }

    public static Map<String, Object> verifyRestMfaChallenge(Delegator delegator, String challengeId, String code)
            throws GenericEntityException {
        if (challengeId == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError("Invalid or expired authentication challenge");
        }
        GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                .where("challengeId", challengeId, "delegatorName", delegator.getDelegatorName())
                .cache(false).queryOne();
        if (challenge == null || !"PENDING".equals(challenge.getString("challengeStatus"))) {
            return ServiceUtil.returnError("Authentication challenge is invalid or has expired");
        }
        Timestamp expires = challenge.getTimestamp("expiresDate");
        if (expires == null || expires.before(UtilDateTime.nowTimestamp())) {
            return ServiceUtil.returnError("Authentication challenge is invalid or has expired");
        }
        int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
        long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
        if (attempts >= maxAttempts) {
            return ServiceUtil.returnError("Maximum verification attempts exceeded. Please restart authentication");
        }
        String userLoginId = challenge.getString("userLoginId");
        GenericValue usedFactor = consumeFactorCode(delegator, userLoginId, code);
        if (usedFactor == null) {
            incrementChallengeAttempts(delegator, challenge, attempts, maxAttempts);
            recordEvent(delegator, userLoginId, "MFA_VERIFY_FAILURE", null, challengeId);
            return ServiceUtil.returnError("Invalid authentication code");
        }
        int claimed = delegator.storeByCondition("UserLoginAuthChallenge",
                Map.of("challengeStatus", "CONSUMED", "verifiedDate", UtilDateTime.nowTimestamp(),
                        "factorId", usedFactor.getString("factorId")),
                and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challengeId),
                        EntityCondition.makeCondition("challengeStatus", EntityOperator.EQUALS, "PENDING"),
                        EntityCondition.makeCondition("attemptCount", EntityOperator.EQUALS, attempts)));
        if (claimed != 1) {
            return ServiceUtil.returnError("Authentication challenge is invalid or has expired");
        }
        String factorType = usedFactor.getString("factorType");
        recordEvent(delegator, userLoginId, "MFA_VERIFY_SUCCESS", factorType, challengeId);
        recordEvent(delegator, userLoginId, "LOGIN_COMMITTED", factorType, challengeId);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("userLoginId", userLoginId);
        result.put("factorType", factorType);
        result.put("sessionBindingHash", challenge.getString("sessionBindingHash"));
        return result;
    }

    private static boolean challengeIsValid(GenericValue challenge, HttpServletRequest request) {
        if (challenge == null || !"PENDING".equals(challenge.getString("challengeStatus"))) {
            return false;
        }
        Timestamp expires = challenge.getTimestamp("expiresDate");
        return expires != null && expires.after(UtilDateTime.nowTimestamp())
                && constantTimeEquals(challenge.getString("sessionBindingHash"), sessionHash(request.getSession().getId()));
    }

    private static void incrementChallengeAttempts(Delegator delegator, GenericValue challenge, long attempts, int maxAttempts)
            throws GenericEntityException {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("attemptCount", attempts + 1);
        if (attempts + 1 >= maxAttempts) {
            fields.put("challengeStatus", "LOCKED");
        }
        delegator.storeByCondition("UserLoginAuthChallenge", fields,
                and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challenge.getString("challengeId")),
                        EntityCondition.makeCondition("challengeStatus", EntityOperator.EQUALS, "PENDING"),
                        EntityCondition.makeCondition("attemptCount", EntityOperator.EQUALS, attempts)));
    }

    public static void recordAuthenticationEvent(Delegator delegator, String userLoginId,
            String eventType, String factorType, String challengeId) {
        recordEvent(delegator, userLoginId, eventType, factorType, challengeId);
    }

    public static void recordAdministrativeAuthenticationEvent(Delegator delegator, String userLoginId,
            String actorUserLoginId, String eventType, String reason) {
        recordEvent(delegator, userLoginId, actorUserLoginId, eventType, null, null, reason);
    }

    private static void recordEvent(Delegator delegator, String userLoginId, String eventType, String factorType, String challengeId) {
        recordEvent(delegator, userLoginId, userLoginId, eventType, factorType, challengeId, null);
    }

    private static void recordEvent(Delegator delegator, String userLoginId, String actorUserLoginId,
            String eventType, String factorType, String challengeId, String reason) {
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("authenticationEventId", delegator.getNextSeqId("UserAuthenticationEvent"));
            fields.put("userLoginId", userLoginId);
            fields.put("actorUserLoginId", actorUserLoginId);
            fields.put("eventType", eventType);
            fields.put("eventDate", UtilDateTime.nowTimestamp());
            if (factorType != null) fields.put("factorType", factorType);
            if (challengeId != null) fields.put("challengeId", challengeId);
            if (reason != null && !reason.isBlank()) fields.put("eventReason", reason);
            delegator.makeValue("UserAuthenticationEvent", fields).create();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to persist authentication audit event [" + eventType + "]", MODULE);
        }
        sendSecurityAlertNotification(delegator, userLoginId, actorUserLoginId, eventType, factorType, reason);
    }

    public static Map<String, Object> sendMfaSecurityNotification(DispatchContext ctx, Map<String, ?> context) {
        String userLoginId = (String) context.get("userLoginId");
        String actorUserLoginId = (String) context.get("actorUserLoginId");
        String eventType = (String) context.get("eventType");
        String factorType = (String) context.get("factorType");
        String reason = (String) context.get("reason");
        sendSecurityAlertNotification(ctx.getDelegator(), userLoginId, actorUserLoginId, eventType, factorType, reason);
        return ServiceUtil.returnSuccess();
    }

    public static void sendSecurityAlertNotification(Delegator delegator, String userLoginId, String actorUserLoginId,
            String eventType, String factorType, String reason) {
        if (delegator == null || userLoginId == null) {
            return;
        }
        boolean emailEnabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                "security.login.authFactor.notify.email.enable", "true", delegator));
        if (!emailEnabled) {
            return;
        }
        if ("LOGIN_COMMITTED".equals(eventType)) {
            boolean notifyOnLogin = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                    "security.login.authFactor.notify.email.onLogin", "false", delegator));
            if (!notifyOnLogin || factorType == null) {
                return;
            }
        } else if (!"MFA_ENROLL".equals(eventType)
                && !"MFA_FACTOR_REVOKED".equals(eventType)) {
            return;
        }
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                    .where("userLoginId", userLoginId).cache(false).queryOne();
            if (userLogin == null) {
                return;
            }
            String partyId = userLogin.getString("partyId");
            if (UtilValidate.isEmpty(partyId)) {
                return;
            }
            String emailAddress = findPartyEmail(delegator, partyId);
            if (UtilValidate.isEmpty(emailAddress)) {
                return;
            }
            String fromAddress = EntityUtilProperties.getPropertyValue("security",
                    "security.login.authFactor.notify.email.from", "notifications-noreply@example.com", delegator);

            String subject;
            StringBuilder body = new StringBuilder();
            body.append("Hello,\n\n");
            Timestamp now = UtilDateTime.nowTimestamp();

            switch (eventType) {
            case "MFA_ENROLL":
                subject = "[Security Alert] Multi-factor authentication method added";
                body.append("A new multi-factor authentication factor (")
                        .append(factorType != null ? factorType : "Authenticator")
                        .append(") was enrolled for your account [").append(userLoginId).append("].\n\n");
                break;
            case "MFA_FACTOR_REVOKED":
                subject = "[Security Alert] Multi-factor authentication method revoked";
                body.append("A multi-factor authentication factor was removed from your account [")
                        .append(userLoginId).append("].\n");
                if (reason != null && !reason.isBlank()) {
                    body.append("Reason: ").append(reason).append("\n");
                }
                if (actorUserLoginId != null && !actorUserLoginId.equals(userLoginId)) {
                    body.append("Revoked by Administrator: ").append(actorUserLoginId).append("\n");
                }
                body.append("\n");
                break;
            case "LOGIN_COMMITTED":
                subject = "[Security Alert] Successful sign-in with multi-factor authentication";
                body.append("A successful sign-in with multi-factor authentication (")
                        .append(factorType != null ? factorType : "MFA")
                        .append(") was completed for your account [").append(userLoginId).append("].\n\n");
                break;
            default:
                return;
            }

            body.append("Time: ").append(now).append("\n\n");
            body.append("If you did not perform or authorize this action, please contact your system administrator immediately.\n\n");
            body.append("Apache OFBiz Security");

            LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(delegator.getDelegatorName(), delegator);
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
                } catch (Throwable t) {
                    Debug.logWarning("Unable to dispatch security alert email to " + emailAddress + ": " + t.getMessage(), MODULE);
                }
            }
        } catch (Throwable t) {
            Debug.logWarning("Error preparing security alert notification for " + userLoginId + ": " + t.getMessage(), MODULE);
        }
    }

    private static String findPartyEmail(Delegator delegator, String partyId) {
        try {
            GenericValue pcm = EntityQuery.use(delegator).from("PartyAndContactMech")
                    .where("partyId", partyId, "contactMechTypeId", "EMAIL_ADDRESS")
                    .filterByDate().queryFirst();
            if (pcm != null && UtilValidate.isNotEmpty(pcm.getString("infoString"))) {
                return pcm.getString("infoString");
            }
        } catch (Throwable t) {
            // view entity might not be loaded in test context
        }
        try {
            List<GenericValue> pcms = EntityQuery.use(delegator).from("PartyContactMech")
                    .where("partyId", partyId).filterByDate().queryList();
            for (GenericValue p : pcms) {
                GenericValue cm = EntityQuery.use(delegator).from("ContactMech")
                        .where("contactMechId", p.getString("contactMechId"), "contactMechTypeId", "EMAIL_ADDRESS")
                        .queryOne();
                if (cm != null && UtilValidate.isNotEmpty(cm.getString("infoString"))) {
                    return cm.getString("infoString");
                }
            }
        } catch (Throwable t) {
            // tables might not be loaded in unit tests
        }
        return null;
    }

    private static long findMatchingTotpStep(String secret, String code) {
        if (secret == null || code == null || !code.matches("[0-9]{6}")) {
            return -1;
        }
        try {
            long currentStep = new SystemTimeProvider().getTime() / 30;
            DefaultCodeGenerator generator = new DefaultCodeGenerator();
            for (long step = currentStep - 1; step <= currentStep + 1; step++) {
                String expected = generator.generate(secret, step);
                if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), code.getBytes(StandardCharsets.US_ASCII))) {
                    return step;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    private static String randomCode() {
        byte[] bytes = new byte[15];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sessionHash(String sessionId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sessionId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static EntityCondition and(EntityCondition... conditions) {
        return EntityCondition.makeCondition(List.of(conditions), EntityOperator.AND);
    }

    private static int getIntProperty(Delegator delegator, String name, int defaultValue) {
        try {
            return Integer.parseInt(EntityUtilProperties.getPropertyValue("security", name,
                    Integer.toString(defaultValue), delegator));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String generateQrCodeDataUri(String text, int width, int height) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, width, height);
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", baos);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            Debug.logError(e, "Error generating QR code data URI", MODULE);
            return null;
        }
    }
}
