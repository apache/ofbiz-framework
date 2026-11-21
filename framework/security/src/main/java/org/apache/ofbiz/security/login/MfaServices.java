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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
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
    public static final String PENDING_REPLAY_KIND = "_MFA_PENDING_REPLAY_KIND_";
    public static final String PENDING_REPLAY_PARAMS = "_MFA_PENDING_REPLAY_PARAMS_";
    public static final String PENDING_REPLAY_AT = "_MFA_PENDING_REPLAY_AT_";
    private static final String MODULE = MfaServices.class.getName();
    private static final String RESOURCE = "SecurityextUiLabels";
    private static final String TOTP = "ULAF_TOTP";
    public static final String BACKUP_CODE = "ULAF_BACKUP";
    public static final int DEFAULT_BACKUP_CODES_COUNT = 10;
    // Canonical Crockford Base32 symbol set (10 digits + 22 letters, excluding I, L, O, and U to prevent visual confusion and profanity)
    private static final String BACKUP_CODE_CHARS = "0123456789abcdefghjkmnpqrstvwxyz";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private MfaServices() { }

    /**
     * Decides whether {@code userLoginId} must complete a second factor at all: true if they already have
     * an active primary (non-backup) factor enrolled, or the global switch is on and they belong to a
     * security group with {@code requireAuthFactor='Y'}. System accounts are always exempt.
     * @param delegator the delegator
     * @param userLoginId the account to evaluate
     * @return true if a second factor is required
     * @throws GenericEntityException if the UserLogin or its factors cannot be read
     */
    public static boolean isMfaRequired(Delegator delegator, String userLoginId) throws GenericEntityException {
        GenericValue targetUserLogin = userLoginId == null ? null : EntityQuery.use(delegator)
                .from("UserLogin").where("userLoginId", userLoginId).cache(false).queryOne();
        if (targetUserLogin == null || "Y".equals(targetUserLogin.getString("isSystem"))) {
            return false;
        }
        boolean enabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                "security.login.authFactor.enable", "false", delegator));
        // A factor the user already enrolled is honored regardless of the global switch (matching how
        // GitHub/Google treat a member's own 2FA versus an org-wide enforcement toggle); the switch only
        // gates whether security-group membership can *force* enrollment on users who have no factor yet.
        return !activePrimaryFactors(delegator, userLoginId).isEmpty()
                || (enabled && hasRequiredSecurityGroup(delegator, userLoginId));
    }

    /**
     * True when the user already has at least one active MFA factor enrolled. Used as a cheap pre-check
     * by callers that otherwise skip the MFA-aware code path entirely while the global switch is off, so
     * that an individually-enrolled factor is never silently bypassed.
     */
    public static boolean hasActiveFactor(Delegator delegator, String userLoginId) {
        if (UtilValidate.isEmpty(userLoginId)) {
            return false;
        }
        try {
            return !activePrimaryFactors(delegator, userLoginId).isEmpty();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to check active MFA factors for [" + userLoginId + "]", MODULE);
            return false;
        }
    }

    /**
     * Service wrapper around {@link #isMfaRequired}: also checks whether the supplied
     * {@code authenticationContext} already satisfies the policy, so a trusted external assurance (e.g. a
     * JWT {@code amr} claim) can skip the interactive challenge.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLoginId}, optional {@code authenticationContext}
     * @return {@code required} and {@code mfaSatisfied} flags
     */
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_evaluate_policy",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * True when the current session already completed a required factor within the configured
     * re-authentication window, or when a fresh factor code accompanying this request satisfies it now.
     * Used as the single, reusable step-up gate for every sensitive action (password change, factor
     * management, backup-code generation) regardless of which caller invokes it.
     */
    public static boolean hasRecentMfaProof(HttpServletRequest request, GenericValue userLogin, LocalDispatcher dispatcher) {
        try {
            Map<String, Object> policy = dispatcher.runSync("isUserLoginMfaRequired",
                    UtilMisc.toMap("userLoginId", userLogin.getString("userLoginId")));
            if (ServiceUtil.isError(policy)) return false;
            if (!Boolean.TRUE.equals(policy.get("required"))) return true;
            HttpSession session = request.getSession(false);
            if (session == null) return false;
            Object verifiedAt = session.getAttribute(MFA_VERIFIED_AT);
            int maxAgeSeconds = EntityUtilProperties.getPropertyAsInteger("security",
                    "security.login.authFactor.reauthenticationWindow", 300);
            if (verifiedAt instanceof Number) {
                long ageMillis = System.currentTimeMillis() - ((Number) verifiedAt).longValue();
                if (ageMillis >= 0 && ageMillis <= maxAgeSeconds * 1000L) {
                    return true;
                }
            }
            // Step-up / Re-authentication: if the re-auth window expired, accept a fresh factor code
            // bundled with this same request instead of forcing a separate logout/login round trip.
            String code = request.getParameter("mfaCode");
            if (UtilValidate.isEmpty(code)) {
                code = request.getParameter("code");
            }
            if (UtilValidate.isNotEmpty(code)) {
                Delegator delegator = (Delegator) request.getAttribute("delegator");
                if (delegator == null) {
                    delegator = (Delegator) session.getAttribute("delegator");
                }
                GenericValue usedFactor = consumeFactorCode(delegator, userLogin.getString("userLoginId"), code);
                if (usedFactor != null) {
                    session.setAttribute(MFA_VERIFIED_AT, System.currentTimeMillis());
                    recordAuthenticationEvent(delegator, userLogin.getString("userLoginId"),
                            "MFA_REVERIFY_SUCCESS", usedFactor.getString("factorType"), null);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** How long a deferred action may sit waiting for its step-up re-verification before it is
     * discarded rather than replayed; kept short since it only bridges a single reverify round trip. */
    private static final int PENDING_REPLAY_MAX_AGE_SECONDS = 180;

    /** A sensitive operation's submitted fields, held just long enough to replay it once the user
     * re-verifies, so they are not asked to resubmit the original form. */
    public static final class PendingMfaReplay {
        private final String kind;
        private final Map<String, String> params;

        private PendingMfaReplay(String kind, Map<String, String> params) {
            this.kind = kind;
            this.params = params;
        }

        public String getKind() {
            return kind;
        }

        public Map<String, String> getParams() {
            return params;
        }
    }

    /**
     * Stashes the current request's submitted fields in the session under {@code kind}, so that once
     * the user satisfies {@link #hasRecentMfaProof}, {@link #takePendingMfaReplay} can hand the exact
     * same fields back to complete the operation without a second form submission. One-shot: callers
     * must consume it via takePendingMfaReplay, which removes it immediately.
     * @param request the current request, whose parameters are captured
     * @param kind a short caller-defined tag identifying which deferred operation this is
     */
    public static void stashPendingMfaReplay(HttpServletRequest request, String kind) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            if (values != null && values.length > 0 && values[0] != null) {
                params.put(name, values[0]);
            }
        });
        HttpSession session = request.getSession();
        session.setAttribute(PENDING_REPLAY_KIND, kind);
        session.setAttribute(PENDING_REPLAY_PARAMS, params);
        session.setAttribute(PENDING_REPLAY_AT, System.currentTimeMillis());
    }

    /**
     * Removes and returns the pending replay stashed by {@link #stashPendingMfaReplay}, or {@code null}
     * if there is none or it is too old to trust. Always clears the session attributes, so a replay is
     * consumed at most once regardless of outcome.
     * @param session the current session
     * @return the stashed kind and fields, or null
     */
    @SuppressWarnings("unchecked")
    public static PendingMfaReplay takePendingMfaReplay(HttpSession session) {
        String kind = (String) session.getAttribute(PENDING_REPLAY_KIND);
        Map<String, String> params = (Map<String, String>) session.getAttribute(PENDING_REPLAY_PARAMS);
        Object stashedAt = session.getAttribute(PENDING_REPLAY_AT);
        session.removeAttribute(PENDING_REPLAY_KIND);
        session.removeAttribute(PENDING_REPLAY_PARAMS);
        session.removeAttribute(PENDING_REPLAY_AT);
        if (kind == null || params == null || !(stashedAt instanceof Number)) {
            return null;
        }
        long ageMillis = System.currentTimeMillis() - ((Number) stashedAt).longValue();
        if (ageMillis < 0 || ageMillis > PENDING_REPLAY_MAX_AGE_SECONDS * 1000L) {
            return null;
        }
        return new PendingMfaReplay(kind, params);
    }

    /**
     * Begins self-service TOTP enrollment: expires any other not-yet-confirmed TOTP factor for the
     * account, creates a new pending one with a fresh secret, and returns its provisioning URI and QR
     * code for the user to scan. Requires the caller's current password; the factor stays inactive until
     * {@link #confirmUserLoginTotpFactor} accepts a code against it.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, optional {@code label}, {@code currentPassword},
     *         optional {@code locale}
     * @return the new factor's id, secret, provisioning URI and QR code data URI
     */
    public static Map<String, Object> createUserLoginTotpFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null || !reauthenticate(ctx, userLogin, (String) context.get("currentPassword"), context.get("locale"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.current_credentials_not_verified",
                    (Locale) context.get("locale")));
        }
        Delegator delegator = ctx.getDelegator();
        String secret = TotpUtil.generateSecret();
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_create_pending_factor",
                    (Locale) context.get("locale")));
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

    /**
     * Confirms a pending TOTP factor: checks the submitted code against its secret within the configured
     * step window, and on success activates the factor and issues a fresh set of backup recovery codes.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, {@code factorId}, {@code code}
     * @return the newly generated backup codes
     */
    public static Map<String, Object> confirmUserLoginTotpFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String factorId = (String) context.get("factorId");
        String code = (String) context.get("code");
        if (userLogin == null || factorId == null || code == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                    (Locale) context.get("locale")));
        }
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId, "userLoginId", userLogin.getString("userLoginId"),
                            "factorType", TOTP, "needsValidation", "Y", "thruDate", null)
                    .cache(false).queryOne();
            long acceptedStep = findMatchingTotpStep(delegator, factor == null ? null : factor.getString("secret"), code);
            if (factor == null || acceptedStep < 0) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                        (Locale) context.get("locale")));
            }
            factor.set("needsValidation", "N");
            factor.set("lastUsedStep", acceptedStep);
            factor.set("lastUsedDate", UtilDateTime.nowTimestamp());
            factor.store();
            recordEvent(delegator, userLogin.getString("userLoginId"), "MFA_ENROLL", TOTP, null);
            List<String> backupCodes = generateBackupCodes(delegator, userLogin.getString("userLoginId"));
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("backupCodes", backupCodes);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * Lists an account's active, non-backup authentication factors (id, type, label, validation state,
     * enrollment/last-used dates) without exposing any secret. Viewing another user's factors requires
     * the {@code SECURITY} {@code _VIEW} permission.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, optional {@code targetUserLoginId} (defaults to
     *         the caller)
     * @return the matching factors, each with its secret omitted
     */
    public static Map<String, Object> getUserLoginAuthFactors(DispatchContext ctx, Map<String, ?> context) {
        GenericValue actor = (GenericValue) context.get("userLogin");
        String targetUserLoginId = (String) context.get("targetUserLoginId");
        if (targetUserLoginId == null && actor != null) {
            targetUserLoginId = actor.getString("userLoginId");
        }
        if (actor == null || targetUserLoginId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_retrieve_factors",
                    (Locale) context.get("locale")));
        }
        boolean self = targetUserLoginId.equals(actor.getString("userLoginId"));
        if (!self && !ctx.getSecurity().hasEntityPermission("SECURITY", "_VIEW", actor)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.not_authorized_to_view_factors",
                    (Locale) context.get("locale")));
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
                if (BACKUP_CODE.equals(factor.getString("factorType"))) {
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_retrieve_factors",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * Revokes (sets {@code thruDate} on) an authentication factor. Self-service revocation
     * re-authenticates with the current password; an administrator revoking another user's factor needs
     * the {@code SECURITY} {@code _UPDATE} permission and a non-blank reason. Refuses to drop the
     * account's last factor while a security-group policy still mandates MFA for it.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin} (the actor), {@code factorId}, optional
     *         {@code currentPassword}, optional {@code reason}
     * @return success, or an error if not authorized or the factor is still required
     */
    public static Map<String, Object> revokeUserLoginAuthFactor(DispatchContext ctx, Map<String, ?> context) {
        GenericValue actor = (GenericValue) context.get("userLogin");
        String factorId = (String) context.get("factorId");
        String currentPassword = (String) context.get("currentPassword");
        String reason = (String) context.get("reason");
        if (actor == null || factorId == null || reason != null && reason.length() > 255) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_revoke_factor",
                    (Locale) context.get("locale")));
        }
        try {
            Delegator delegator = ctx.getDelegator();
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId).cache(false).queryOne();
            if (factor == null || factor.getTimestamp("thruDate") != null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.factor_unavailable", (Locale) context.get("locale")));
            }
            String targetUserLoginId = factor.getString("userLoginId");
            String actorUserLoginId = actor.getString("userLoginId");
            boolean self = actorUserLoginId.equals(targetUserLoginId);
            if (self) {
                if (!reauthenticate(ctx, actor, currentPassword, context.get("locale"))) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.current_credentials_not_verified",
                            (Locale) context.get("locale")));
                }
            } else if (!ctx.getSecurity().hasEntityPermission("SECURITY", "_UPDATE", actor)
                    || reason == null || reason.isBlank()) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.not_authorized_to_revoke_factor",
                        (Locale) context.get("locale")));
            }
            boolean mfaEnabled = Boolean.parseBoolean(EntityUtilProperties.getPropertyValue("security",
                    "security.login.authFactor.enable", "false", delegator));
            if (self && mfaEnabled && hasRequiredSecurityGroup(delegator, targetUserLoginId)
                    && activeFactors(delegator, targetUserLoginId).size() <= 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.required_factor_cannot_be_removed",
                        (Locale) context.get("locale")));
            }
            Timestamp now = UtilDateTime.nowTimestamp();
            int updated = delegator.storeByCondition("UserLoginAuthFactor", Map.of("thruDate", now), and(
                    EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factorId),
                    EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, targetUserLoginId),
                    EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
            if (updated != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.factor_unavailable", (Locale) context.get("locale")));
            }
            recordEvent(delegator, targetUserLoginId, actorUserLoginId, "MFA_FACTOR_REVOKED",
                    factor.getString("factorType"), null, reason);
            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_revoke_factor",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * Creates a pending {@code UserLoginAuthChallenge} bound to the current session for a user who has
     * already passed primary authentication. Returns {@code mustEnroll=true} instead of a challenge when
     * the account has no factor yet but belongs to an MFA-mandated security group.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, {@code request}
     * @return {@code required}/{@code mustEnroll} flags and, if a challenge was created, its
     *         {@code challengeId}
     */
    public static Map<String, Object> createUserLoginMfaChallenge(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (userLogin == null || request == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_start_challenge",
                    (Locale) context.get("locale")));
        }
        Delegator delegator = ctx.getDelegator();
        try {
            List<GenericValue> primaryFactors = activePrimaryFactors(delegator, userLogin.getString("userLoginId"));
            boolean mustEnroll = primaryFactors.isEmpty()
                    && hasRequiredSecurityGroup(delegator, userLogin.getString("userLoginId"));
            if (primaryFactors.isEmpty() && !mustEnroll && activeFactors(delegator, userLogin.getString("userLoginId")).isEmpty()) {
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
                    "sessionBindingHash", sessionHash(request.getSession().getId()),
                    "challengeStatus", "PENDING",
                    "attemptCount", 0L,
                    "createdDate", now,
                    "expiresDate", new Timestamp(now.getTime() + ttl * 1000L)));
            challenge.create();
            HttpSession session = request.getSession();
            session.setAttribute(PENDING_CHALLENGE, challengeId);
            session.setAttribute(PENDING_USER, userLogin.getString("userLoginId"));
            session.removeAttribute(PRIMARY_VERIFIED);
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
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_start_challenge",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * Finishes a mandatory TOTP enrollment forced during login (see {@link #createUserLoginMfaChallenge}'s
     * {@code mustEnroll}): validates the submitted code against the pending factor bound to this session,
     * activates it on success, and issues backup codes. Locks the account via
     * {@link #handleFailedMfaAttempt} after too many wrong codes.
     * @param ctx the service dispatch context
     * @param context service input: {@code request}, {@code code}
     * @return {@code activated} flag and the new backup codes
     */
    public static Map<String, Object> completeForcedTotpEnrollment(DispatchContext ctx, Map<String, ?> context) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_complete_enrollment",
                    (Locale) context.get("locale")));
        }
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(PENDING_ENROLLMENT))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_complete_enrollment",
                    (Locale) context.get("locale")));
        }
        String pendingUserId = (String) session.getAttribute(PENDING_USER);
        String factorId = (String) session.getAttribute(PENDING_ENROLLMENT_FACTOR);
        String code = (String) context.get("code");
        if (pendingUserId == null || factorId == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_complete_enrollment",
                    (Locale) context.get("locale")));
        }
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                    .where("challengeId", session.getAttribute(PENDING_CHALLENGE), "userLoginId", pendingUserId,
                            "challengeStatus", "PENDING")
                    .cache(false).queryOne();
            if (!challengeIsValid(challenge, request)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_complete_enrollment",
                        (Locale) context.get("locale")));
            }
            int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
            long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
            if (attempts >= maxAttempts) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.unable_to_complete_enrollment",
                        (Locale) context.get("locale")));
            }
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                    .where("userLoginId", pendingUserId).cache(false).queryOne();
            if (userLogin == null || "N".equals(userLogin.getString("enabled"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.account_disabled", (Locale) context.get("locale")));
            }
            GenericValue factor = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("factorId", factorId, "userLoginId", pendingUserId, "factorType", TOTP,
                            "needsValidation", "Y", "thruDate", null).cache(false).queryOne();
            long acceptedStep = findMatchingTotpStep(delegator, factor == null ? null : factor.getString("secret"), code);
            if (factor == null || acceptedStep < 0) {
                handleFailedMfaAttempt(delegator, userLogin, challenge, attempts, maxAttempts);
                recordEvent(delegator, pendingUserId, "MFA_VERIFY_FAILURE", TOTP,
                        (String) session.getAttribute(PENDING_CHALLENGE));
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                        (Locale) context.get("locale")));
            }
            int activated = delegator.storeByCondition("UserLoginAuthFactor",
                    Map.of("needsValidation", "N", "lastUsedDate", UtilDateTime.nowTimestamp(), "lastUsedStep", acceptedStep),
                    and(EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factorId),
                            EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, pendingUserId),
                            EntityCondition.makeCondition("needsValidation", EntityOperator.EQUALS, "Y"),
                            EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
            if (activated != 1) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                        (Locale) context.get("locale")));
            }
            session.removeAttribute(PENDING_ENROLLMENT);
            session.removeAttribute(PRIMARY_VERIFIED);
            recordEvent(delegator, pendingUserId, "MFA_ENROLL", TOTP, null);
            List<String> backupCodes = generateBackupCodes(delegator, pendingUserId);
            session.setAttribute("mfaBackupCodes", backupCodes);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("activated", true);
            result.put("backupCodes", backupCodes);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_authenticator_confirmation_failed",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * Verifies a submitted TOTP/backup code against the session's pending login challenge and, on
     * success, marks it {@code VERIFIED} (optimistic-concurrency guarded on {@code attemptCount}) for
     * {@code LoginServices#completeUserLoginMfa} to consume. Enforces the per-challenge attempt limit and
     * locks the account via {@link #handleFailedMfaAttempt} after too many failures.
     * @param ctx the service dispatch context
     * @param context service input: {@code request}, {@code code}
     * @return {@code verified} flag, or {@code accountLocked} if this failure crossed the lockout
     *         threshold
     */
    public static Map<String, Object> verifyUserLoginMfaChallenge(DispatchContext ctx, Map<String, ?> context) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        String code = (String) context.get("code");
        if (request == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                    (Locale) context.get("locale")));
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                    (Locale) context.get("locale")));
        }
        String challengeId = (String) session.getAttribute(PENDING_CHALLENGE);
        String pendingUserId = (String) session.getAttribute(PENDING_USER);
        if (challengeId == null || pendingUserId == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                    (Locale) context.get("locale")));
        }
        Delegator delegator = ctx.getDelegator();
        try {
            GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                    .where("userLoginId", pendingUserId).cache(false).queryOne();
            if (userLogin == null || "N".equals(userLogin.getString("enabled"))) {
                Map<String, Object> result = ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.account_disabled",
                        (Locale) context.get("locale")));
                result.put("accountLocked", true);
                return result;
            }
            GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                    .where("challengeId", challengeId, "userLoginId", pendingUserId, "challengeStatus", "PENDING")
                    .cache(false).queryOne();
            if (!challengeIsValid(challenge, request)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                        (Locale) context.get("locale")));
            }
            int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
            long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
            if (attempts >= maxAttempts) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                        (Locale) context.get("locale")));
            }
            GenericValue usedFactor = consumeFactorCode(delegator, pendingUserId, code);
            if (usedFactor == null) {
                boolean locked = handleFailedMfaAttempt(delegator, userLogin, challenge, attempts, maxAttempts);
                recordEvent(delegator, pendingUserId, "MFA_VERIFY_FAILURE", null, challengeId);
                Map<String, Object> result = ServiceUtil.returnSuccess();
                result.put("verified", false);
                if (locked) {
                    result.put("accountLocked", true);
                }
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
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                        (Locale) context.get("locale")));
            }
            session.setAttribute(VERIFIED_CHALLENGE, challengeId);
            recordEvent(delegator, pendingUserId, "MFA_VERIFY_SUCCESS", usedFactor.getString("factorType"), challengeId);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("verified", true);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge",
                    (Locale) context.get("locale")));
        }
    }

    /**
     * True when the account has at least one active factor of any kind, including backup codes (unlike
     * {@link #hasActiveFactor}, which only counts primary factors).
     */
    public static boolean hasActiveFactors(Delegator delegator, String userLoginId) throws GenericEntityException {
        return !activeFactors(delegator, userLoginId).isEmpty();
    }

    /**
     * Atomically marks a session-verified challenge {@code CONSUMED}, so it cannot be reused to complete
     * a second login. Fails if the challenge was not the one this session itself verified, has expired,
     * or was already consumed.
     * @param delegator the delegator
     * @param request the current request, whose session must hold the verified challenge id
     * @param userLoginId the account the challenge belongs to
     * @param challengeId the challenge to consume
     * @return true if the challenge was successfully claimed
     * @throws GenericEntityException if the update fails
     */
    public static boolean consumeVerifiedChallenge(Delegator delegator, HttpServletRequest request,
            String userLoginId, String challengeId) throws GenericEntityException {
        HttpSession session = request.getSession(false);
        if (session == null || !challengeId.equals(session.getAttribute(VERIFIED_CHALLENGE))) {
            return false;
        }
        int consumed = delegator.storeByCondition("UserLoginAuthChallenge", Map.of("challengeStatus", "CONSUMED"),
                and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challengeId),
                        EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId),
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

    /**
     * The account's active factors excluding backup codes -- i.e. the factors that actually satisfy
     * "has MFA enrolled" for policy purposes (backup codes are a recovery mechanism, not a standalone
     * factor).
     */
    public static List<GenericValue> activePrimaryFactors(Delegator delegator, String userLoginId) throws GenericEntityException {
        return activeFactors(delegator, userLoginId).stream()
                .filter(factor -> !BACKUP_CODE.equals(factor.getString("factorType")))
                .toList();
    }

    private static String generateSingleBackupCode() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(BACKUP_CODE_CHARS.charAt(SECURE_RANDOM.nextInt(BACKUP_CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Normalizes a user-entered backup code for comparison: strips separators/whitespace, lower-cases it,
     * and maps visually-confusable Crockford Base32 characters (o/i/l) to their canonical digit.
     */
    public static String normalizeBackupCode(String code) {
        if (code == null) {
            return "";
        }
        String clean = code.replace("-", "").replace(" ", "").trim().toLowerCase();
        // Crockford Base32 error tolerance: map visual lookalikes to canonical characters
        return clean.replace('o', '0')
                    .replace('i', '1')
                    .replace('l', '1');
    }

    /**
     * Generates a fresh batch of single-use backup recovery codes, expiring any unused codes from a
     * previous batch first. Codes are stored only as a salted hash ({@code secretHash}); the plaintext
     * values returned here are the only time they are ever available.
     * @param delegator the delegator
     * @param userLoginId the account to generate codes for
     * @return the plaintext codes, for one-time display to the user
     * @throws GenericEntityException if the factor rows cannot be written
     */
    public static List<String> generateBackupCodes(Delegator delegator, String userLoginId) throws GenericEntityException {
        int count = getIntProperty(delegator, "security.login.authFactor.backupCodes.count", DEFAULT_BACKUP_CODES_COUNT);
        Timestamp now = UtilDateTime.nowTimestamp();
        delegator.storeByCondition("UserLoginAuthFactor", Map.of("thruDate", now), and(
                EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId),
                EntityCondition.makeCondition("factorType", EntityOperator.EQUALS, BACKUP_CODE),
                EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));

        String hashType = EntityUtilProperties.getPropertyValue("security", "password.encrypt.hash.type", "SHA", delegator);
        List<String> plainCodes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String plainCode = generateSingleBackupCode();
            String normalized = normalizeBackupCode(plainCode);
            String hashed = HashCrypt.cryptUTF8(hashType, null, normalized);
            String factorId = delegator.getNextSeqId("UserLoginAuthFactor");
            GenericValue factor = delegator.makeValue("UserLoginAuthFactor", Map.of(
                    "factorId", factorId,
                    "userLoginId", userLoginId,
                    "factorType", BACKUP_CODE,
                    "secretHash", hashed,
                    "factorLabel", "Backup Recovery Code",
                    "needsValidation", "N",
                    "fromDate", now));
            factor.create();
            plainCodes.add(plainCode);
        }
        recordEvent(delegator, userLoginId, "MFA_BACKUP_CODES_GENERATED", BACKUP_CODE, null);
        return plainCodes;
    }

    private static GenericValue consumeBackupCode(Delegator delegator, String userLoginId, String code)
            throws GenericEntityException {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = normalizeBackupCode(code);
        List<GenericValue> backupFactors = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                .where("userLoginId", userLoginId, "factorType", BACKUP_CODE, "needsValidation", "N", "thruDate", null)
                .cache(false).queryList();
        String hashType = EntityUtilProperties.getPropertyValue("security", "password.encrypt.hash.type", "SHA", delegator);
        for (GenericValue factor : backupFactors) {
            String secretHash = factor.getString("secretHash");
            if (secretHash != null && HashCrypt.comparePassword(secretHash, hashType, normalized)) {
                Timestamp now = UtilDateTime.nowTimestamp();
                int consumed = delegator.storeByCondition("UserLoginAuthFactor",
                        Map.of("thruDate", now, "lastUsedDate", now, "lastUsedStep", 1L),
                        and(EntityCondition.makeCondition("factorId", EntityOperator.EQUALS, factor.getString("factorId")),
                                EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, userLoginId),
                                EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null)));
                if (consumed == 1) {
                    recordEvent(delegator, userLoginId, "MFA_BACKUP_CODE_USED", BACKUP_CODE, null);
                    return factor;
                }
            }
        }
        return null;
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
                long acceptedStep = findMatchingTotpStep(delegator, factor.getString("secret"), code);
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

    /**
     * Tries a submitted code as a TOTP code first, then as a backup recovery code, consuming whichever
     * one matches. Returns the factor that was consumed, or null if the code matched neither.
     */
    public static GenericValue consumeFactorCode(Delegator delegator, String userLoginId, String code)
            throws GenericEntityException {
        if (code == null) {
            return null;
        }
        GenericValue factor = consumeTotpCode(delegator, userLoginId, code);
        if (factor != null) {
            return factor;
        }
        return consumeBackupCode(delegator, userLoginId, code);
    }

    /**
     * REST counterpart of {@link #createUserLoginMfaChallenge}: creates a pending challenge bound to the
     * requesting API group (via {@code sessionBindingHash}, prefixed {@code "rest:"}) instead of an HTTP
     * session, so {@link #verifyRestMfaChallenge} can later confirm it was issued for that same group.
     * @param delegator the delegator
     * @param userLoginId the already-password-authenticated account
     * @param apiGroupPath the REST API group the eventual token will be scoped to
     * @return the challenge id and allowed-factor hints, or {@code required=false} if no challenge is
     *         needed
     * @throws GenericEntityException if the challenge cannot be created
     */
    public static Map<String, Object> createRestMfaChallenge(Delegator delegator, String userLoginId, String apiGroupPath)
            throws GenericEntityException {
        List<GenericValue> factors = activePrimaryFactors(delegator, userLoginId);
        boolean mustEnroll = factors.isEmpty() && hasRequiredSecurityGroup(delegator, userLoginId);
        if (factors.isEmpty() && !mustEnroll && activeFactors(delegator, userLoginId).isEmpty()) {
            return Map.of("required", false);
        }
        List<String> allowedFactorHints = new ArrayList<>();
        if (mustEnroll) {
            allowedFactorHints.add("ENROLLMENT_REQUIRED");
            allowedFactorHints.add(TOTP);
        } else {
            for (GenericValue factor : activeFactors(delegator, userLoginId)) {
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

    /**
     * REST counterpart of {@link #verifyUserLoginMfaChallenge}: verifies a submitted code against a
     * group-bound REST challenge, commits the login (resets {@code successiveFailedLogins}, records
     * {@code UserLoginHistory}) on success, and enforces the same per-challenge attempt limit and account
     * lockout.
     * @param delegator the delegator
     * @param challengeId the challenge to verify
     * @param code the submitted TOTP/backup code
     * @return the authenticated {@code userLoginId} and {@code factorType}, for the caller to mint a
     *         token with
     * @throws GenericEntityException if the challenge or UserLogin cannot be read/updated
     */
    public static Map<String, Object> verifyRestMfaChallenge(Delegator delegator, String challengeId, String code)
            throws GenericEntityException {
        if (challengeId == null || code == null || code.length() > 64) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "loginevents.mfa_invalid_or_expired_challenge", (Locale) null));
        }
        GenericValue challenge = EntityQuery.use(delegator).from("UserLoginAuthChallenge")
                .where("challengeId", challengeId)
                .cache(false).queryOne();
        if (challenge == null || !"PENDING".equals(challenge.getString("challengeStatus"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.challenge_invalid_or_expired", (Locale) null));
        }
        Timestamp expires = challenge.getTimestamp("expiresDate");
        if (expires == null || expires.before(UtilDateTime.nowTimestamp())) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.challenge_invalid_or_expired", (Locale) null));
        }
        int maxAttempts = getIntProperty(delegator, "security.login.authFactor.maxVerifyAttempts", 5);
        long attempts = challenge.getLong("attemptCount") == null ? 0 : challenge.getLong("attemptCount");
        if (attempts >= maxAttempts) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.max_verification_attempts_exceeded", (Locale) null));
        }
        String userLoginId = challenge.getString("userLoginId");
        GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin")
                .where("userLoginId", userLoginId).cache(false).queryOne();
        if (userLogin == null || "N".equals(userLogin.getString("enabled"))) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.account_disabled", (Locale) null));
        }
        GenericValue usedFactor = consumeFactorCode(delegator, userLoginId, code);
        if (usedFactor == null) {
            boolean locked = handleFailedMfaAttempt(delegator, userLogin, challenge, attempts, maxAttempts);
            recordEvent(delegator, userLoginId, "MFA_VERIFY_FAILURE", null, challengeId);
            if (locked) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.account_disabled_too_many_failed_attempts",
                        (Locale) null));
            }
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.invalid_authentication_code", (Locale) null));
        }
        int claimed = delegator.storeByCondition("UserLoginAuthChallenge",
                Map.of("challengeStatus", "CONSUMED", "verifiedDate", UtilDateTime.nowTimestamp(),
                        "factorId", usedFactor.getString("factorId")),
                and(EntityCondition.makeCondition("challengeId", EntityOperator.EQUALS, challengeId),
                        EntityCondition.makeCondition("challengeStatus", EntityOperator.EQUALS, "PENDING"),
                        EntityCondition.makeCondition("attemptCount", EntityOperator.EQUALS, attempts)));
        if (claimed != 1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.challenge_invalid_or_expired", (Locale) null));
        }
        Long failedLogins = userLogin.getLong("successiveFailedLogins");
        if (failedLogins != null && failedLogins > 0) {
            userLogin.set("successiveFailedLogins", 0L);
        }
        if ("Y".equals(userLogin.getString("hasLoggedOut"))) {
            userLogin.set("hasLoggedOut", "N");
        }
        userLogin.store();
        if ("true".equals(EntityUtilProperties.getPropertyValue("security", "store.login.history", delegator))) {
            Map<String, Object> history = UtilMisc.toMap("userLoginId", userLoginId,
                    "fromDate", UtilDateTime.nowTimestamp(), "successfulLogin", "Y");
            if (userLogin.getModelEntity().isField("partyId")) {
                history.put("partyId", userLogin.get("partyId"));
            }
            delegator.create("UserLoginHistory", history);
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

    private static boolean handleFailedMfaAttempt(Delegator delegator, GenericValue userLogin,
            GenericValue challenge, long attempts, int maxAttempts) throws GenericEntityException {
        incrementChallengeAttempts(delegator, challenge, attempts, maxAttempts);
        if (userLogin != null && !"Y".equals(userLogin.getString("isSystem"))) {
            Long currentFailedLogins = userLogin.getLong("successiveFailedLogins");
            long nextFailedLogins = (currentFailedLogins == null ? 0L : currentFailedLogins) + 1L;
            userLogin.set("successiveFailedLogins", nextFailedLogins);

            String mflStr = EntityUtilProperties.getPropertyValue("security", "max.failed.logins", delegator);
            long maxFailedLogins = 3;
            try {
                maxFailedLogins = Long.parseLong(mflStr);
            } catch (Exception e) {
                maxFailedLogins = 3;
                Debug.logWarning("Could not parse max.failed.logins from security.properties, using default of 3", MODULE);
            }

            boolean isLocked = false;
            if (maxFailedLogins > 0 && nextFailedLogins >= maxFailedLogins) {
                userLogin.set("enabled", "N");
                userLogin.set("disabledDateTime", UtilDateTime.nowTimestamp());
                isLocked = true;
                recordEvent(delegator, userLogin.getString("userLoginId"), "MFA_ACCOUNT_LOCKED", null,
                        challenge == null ? null : challenge.getString("challengeId"));
            }
            userLogin.store();
            return isLocked;
        }
        return false;
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

    /**
     * Public entry point for callers outside this class to append a {@code UserAuthenticationEvent} audit
     * row (and trigger a security-alert notification) for events that don't involve an administrative
     * actor.
     */
    public static void recordAuthenticationEvent(Delegator delegator, String userLoginId,
            String eventType, String factorType, String challengeId) {
        recordEvent(delegator, userLoginId, eventType, factorType, challengeId);
    }

    /**
     * Like {@link #recordAuthenticationEvent}, but for actions an administrator performed on another
     * user's account (e.g. an administrative password reset), recording the administrator as the acting
     * user along with the supplied reason.
     */
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

    /**
     * Dispatches the {@code sendMfaSecurityNotification} application-layer service (asynchronously) for
     * the security-relevant event types this feature actually wants to email about (enrollment,
     * revocation, and -- only if the operator opted in -- a committed login), gated by the
     * notification-enable property. Deliberately does not touch Party/ContactMech entities itself: that
     * lookup belongs to the application layer, not the security framework.
     */
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
            LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(delegator.getDelegatorName(), delegator);
            if (dispatcher != null) {
                dispatcher.runAsync("sendMfaSecurityNotification", UtilMisc.toMap(
                        "userLoginId", userLoginId,
                        "actorUserLoginId", actorUserLoginId,
                        "eventType", eventType,
                        "factorType", factorType,
                        "reason", reason));
            }
        } catch (Throwable t) {
            Debug.logWarning("Error dispatching security alert notification for " + userLoginId + ": " + t.getMessage(), MODULE);
        }
    }

    private static long findMatchingTotpStep(Delegator delegator, String secret, String code) {
        int windowSteps = getIntProperty(delegator, "security.login.authFactor.totpWindowSteps", 1);
        return TotpUtil.findMatchingTotpStep(secret, code, windowSteps);
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

    /**
     * Self-service wrapper around {@link #generateBackupCodes}: re-authenticates with the current
     * password when one is supplied.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, optional {@code currentPassword}, optional
     *         {@code locale}
     * @return the newly generated backup codes
     */
    public static Map<String, Object> generateUserLoginBackupCodes(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            return ServiceUtil.returnError("Unable to generate backup recovery codes");
        }
        String currentPassword = (String) context.get("currentPassword");
        Delegator delegator = ctx.getDelegator();
        String userLoginId = userLogin.getString("userLoginId");
        try {
            if (UtilValidate.isNotEmpty(currentPassword)) {
                if (!reauthenticate(ctx, userLogin, currentPassword, context.get("locale"))) {
                    return ServiceUtil.returnError("Current credentials could not be verified");
                }
            }
            List<String> codes = generateBackupCodes(delegator, userLoginId);
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("backupCodes", codes);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, "Unable to generate backup recovery codes", MODULE);
            return ServiceUtil.returnError("Unable to generate backup recovery codes");
        }
    }

    /**
     * Counts an account's unused backup recovery codes, for display on the MFA settings page (e.g.
     * "3 codes remaining"). Viewing another user's count requires the {@code SECURITY} {@code _VIEW}
     * permission.
     * @param ctx the service dispatch context
     * @param context service input: {@code userLogin}, optional {@code targetUserLoginId} (defaults to
     *         the caller)
     * @return {@code remainingCount}
     */
    public static Map<String, Object> getRemainingBackupCodesCount(DispatchContext ctx, Map<String, ?> context) {
        GenericValue actor = (GenericValue) context.get("userLogin");
        String targetUserLoginId = (String) context.get("targetUserLoginId");
        if (targetUserLoginId == null && actor != null) {
            targetUserLoginId = actor.getString("userLoginId");
        }
        if (actor == null || targetUserLoginId == null) {
            return ServiceUtil.returnError("Unable to retrieve backup codes count");
        }
        boolean self = targetUserLoginId.equals(actor.getString("userLoginId"));
        if (!self && !ctx.getSecurity().hasEntityPermission("SECURITY", "_VIEW", actor)) {
            return ServiceUtil.returnError("Not authorized to view authentication factors");
        }
        Delegator delegator = ctx.getDelegator();
        try {
            long count = EntityQuery.use(delegator).from("UserLoginAuthFactor")
                    .where("userLoginId", targetUserLoginId, "factorType", BACKUP_CODE, "needsValidation", "N", "thruDate", null)
                    .cache(false).queryCount();
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("remainingCount", (int) count);
            return result;
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError("Unable to retrieve backup codes count");
        }
    }

    /**
     * Renders {@code text} (an {@code otpauth://} provisioning URI) as a QR code and returns it as a
     * base64-encoded PNG data URI, so the browser can display it with no external QR-rendering
     * dependency. Returns null if encoding fails.
     */
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

    /**
     * Purges expired MFA challenges and old authentication audit events.
     * @param ctx the dispatch context
     * @param context input parameters: daysToKeepChallenges (default 1), daysToKeepEvents (default 90)
     * @return map with purged counts
     */
    public static Map<String, Object> purgeOldMfaChallengesAndEvents(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        Integer daysToKeepChallenges = (Integer) context.get("daysToKeepChallenges");
        if (daysToKeepChallenges == null || daysToKeepChallenges < 0) {
            daysToKeepChallenges = 1;
        }
        Integer daysToKeepEvents = (Integer) context.get("daysToKeepEvents");
        if (daysToKeepEvents == null || daysToKeepEvents < 0) {
            daysToKeepEvents = 90;
        }

        Timestamp challengePurgeThreshold = Timestamp.from(Instant.now().minus(Duration.ofDays(daysToKeepChallenges)));
        Timestamp eventPurgeThreshold = Timestamp.from(Instant.now().minus(Duration.ofDays(daysToKeepEvents)));

        int purgedChallenges = 0;
        int purgedEvents = 0;

        try {
            EntityCondition challengeCond = EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition("expiresDate", EntityOperator.LESS_THAN, challengePurgeThreshold),
                    EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition("challengeStatus", EntityOperator.IN, List.of("CONSUMED", "LOCKED")),
                            EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN, challengePurgeThreshold)));
            purgedChallenges = delegator.removeByCondition("UserLoginAuthChallenge", challengeCond);

            EntityCondition eventCond = EntityCondition.makeCondition("eventDate", EntityOperator.LESS_THAN, eventPurgeThreshold);
            purgedEvents = delegator.removeByCondition("UserAuthenticationEvent", eventCond);

            Debug.logInfo("Purged " + purgedChallenges + " old MFA challenges and " + purgedEvents
                    + " old authentication audit events.", MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error purging old MFA challenges and events", MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "mfaservices.purge_error",
                    UtilMisc.toMap("errorDetail", e.getMessage()), (Locale) context.get("locale")));
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("purgedChallengesCount", purgedChallenges);
        result.put("purgedEventsCount", purgedEvents);
        return result;
    }
}
