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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Transaction;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.HttpClient;
import org.apache.ofbiz.base.util.HttpClientException;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Obtains and caches SMTP XOAUTH2 access tokens via the standard OAuth2
 * refresh_token grant against a configurable, provider-agnostic token endpoint.
 */
public final class SmtpOAuth2TokenProvider {

    private static final String MODULE = SmtpOAuth2TokenProvider.class.getName();
    private static final long EXPIRY_SAFETY_MARGIN_MILLIS = 60_000L;

    private static final UtilCache<String, String> TOKEN_CACHE = UtilCache.createUtilCache("smtp.oauth2.accessToken");
    private static final Map<String, Object> REFRESH_LOCKS = new ConcurrentHashMap<>();

    private SmtpOAuth2TokenProvider() { }

    static final class TokenResponse {
        //ALLOW PUBLIC FIELDS
        final String accessToken;
        final long expiresInSeconds;
        final String rotatedRefreshToken;
        //FORBID PUBLIC FIELDS

        TokenResponse(String accessToken, long expiresInSeconds, String rotatedRefreshToken) {
            this.accessToken = accessToken;
            this.expiresInSeconds = expiresInSeconds;
            this.rotatedRefreshToken = rotatedRefreshToken;
        }
    }

    /** Parses the token endpoint's JSON response. */
    static TokenResponse parseTokenResponse(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        JsonNode accessTokenNode = node.get("access_token");
        if (accessTokenNode == null || accessTokenNode.asText().isEmpty()) {
            throw new IOException("OAuth2 token endpoint response missing access_token: " + json);
        }
        long expiresIn = node.path("expires_in").asLong(3600L);
        String rotatedRefreshToken = node.hasNonNull("refresh_token") ? node.get("refresh_token").asText() : null;
        return new TokenResponse(accessTokenNode.asText(), expiresIn, rotatedRefreshToken);
    }

    /** Explicit HTTP timeout (milliseconds) applied to the token-endpoint refresh call. */
    private static final int REFRESH_HTTP_TIMEOUT_MILLIS = 30000;

    /**
     * Core refresh call; no Delegator dependency, so unit-testable against a local stub server.
     * Does not enforce https:// (the public entry point below does).
     *
     * <p>Known limitation: {@link HttpClient} has no reliable connect or read timeout, so a token
     * endpoint that accepts a connection but never responds can stall this call (and the per-config
     * refresh lock in {@link #getAccessToken}) indefinitely.
     *
     * <p>Security note: enabling OFBiz's HTTP verbose debug logging logs {@code client_secret} and
     * {@code refresh_token} in plain text via {@link HttpClient#post()}.
     */
    static TokenResponse refreshAccessToken(String tokenEndpoint, String clientId, String clientSecret,
            String refreshToken, String scope) throws HttpClientException, IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("refresh_token", refreshToken);
        if (UtilValidate.isNotEmpty(scope)) {
            params.put("scope", scope);
        }
        HttpClient httpClient = new HttpClient(tokenEndpoint, params);
        httpClient.setTimeout(REFRESH_HTTP_TIMEOUT_MILLIS);
        String responseBody = httpClient.post();
        if (UtilValidate.isEmpty(responseBody)) {
            throw new IOException("Empty response from OAuth2 token endpoint: " + tokenEndpoint);
        }
        return parseTokenResponse(responseBody);
    }

    /** Returns a cached access token when valid, otherwise refreshes it and persists a rotated refresh token, if issued. */
    public static String getAccessToken(Delegator delegator, MailSmtpConfigUtil.ResolvedConfig config)
            throws GeneralException {
        if (!config.oauth2TokenEndpoint.startsWith("https://")) {
            throw new GeneralException("SMTP OAuth2 token endpoint must use https://: " + config.oauth2TokenEndpoint);
        }
        if (UtilValidate.isEmpty(config.authUser) || UtilValidate.isEmpty(config.oauth2ClientId)
                || UtilValidate.isEmpty(config.oauth2ClientSecret) || UtilValidate.isEmpty(config.oauth2RefreshToken)) {
            throw new GeneralException("SMTP OAuth2 configuration for mailSmtpConfigId ["
                    + config.mailSmtpConfigId + "] is incomplete: authUser, oauth2ClientId, oauth2ClientSecret and "
                    + "oauth2RefreshToken are all required when authMechanism is XOAUTH2");
        }
        String cacheKey = config.mailSmtpConfigId;
        Object lock = REFRESH_LOCKS.computeIfAbsent(cacheKey, k -> new Object());
        String accessToken;
        String rotatedRefreshToken = null;
        synchronized (lock) {
            String cachedToken = TOKEN_CACHE.get(cacheKey);
            if (cachedToken != null) {
                return cachedToken;
            }
            TokenResponse response;
            try {
                response = refreshAccessToken(config.oauth2TokenEndpoint, config.oauth2ClientId,
                        config.oauth2ClientSecret, config.oauth2RefreshToken, config.oauth2Scope);
            } catch (HttpClientException | IOException e) {
                throw new GeneralException("Failed to refresh SMTP OAuth2 access token for mailSmtpConfigId ["
                        + cacheKey + "]: " + e.getMessage(), e);
            }
            accessToken = response.accessToken;
            long ttlMillis = (response.expiresInSeconds * 1000L) - EXPIRY_SAFETY_MARGIN_MILLIS;
            if (ttlMillis > 0) {
                // UtilCache treats a non-positive expireTimeMillis as "never expire" rather than
                // "already expired" (no eviction pulse is scheduled), so an already-expired-or-about-
                // to-expire token must simply not be cached at all, forcing a refresh on the next call.
                TOKEN_CACHE.put(cacheKey, accessToken, ttlMillis);
            }
            if (UtilValidate.isNotEmpty(response.rotatedRefreshToken)
                    && !response.rotatedRefreshToken.equals(config.oauth2RefreshToken)) {
                rotatedRefreshToken = response.rotatedRefreshToken;
            }
        }
        // Outside the lock: uses its own transaction, and the cache is already updated so no
        // concurrent caller will trigger a redundant refresh while this DB write is in flight.
        if (rotatedRefreshToken != null) {
            persistRotatedRefreshToken(delegator, cacheKey, rotatedRefreshToken);
        }
        return accessToken;
    }

    /**
     * Persists in its own suspended transaction, independent of the caller's (typically
     * {@code sendMail}): the provider has already issued (and may have invalidated the prior)
     * refresh token by now, so an unrelated later failure in the caller must not roll this back.
     */
    private static void persistRotatedRefreshToken(Delegator delegator, String mailSmtpConfigId, String newRefreshToken) {
        Transaction parentTx = null;
        boolean beganTransaction = false;
        try {
            if (TransactionUtil.isTransactionInPlace()) {
                parentTx = TransactionUtil.suspend();
            }
            try {
                beganTransaction = TransactionUtil.begin();
                GenericValue configRow = EntityQuery.use(delegator).from("MailSmtpConfig")
                        .where("mailSmtpConfigId", mailSmtpConfigId).queryOne();
                if (configRow != null) {
                    configRow.set("oauth2RefreshToken", newRefreshToken);
                    configRow.store();
                }
                TransactionUtil.commit(beganTransaction);
            } catch (Exception e) {
                // Catches beyond GenericEntityException: must roll back this child transaction, or
                // resuming the parent below throws IllegalStateException and leaves it suspended.
                TransactionUtil.rollback(beganTransaction, "Failed to persist rotated SMTP OAuth2 refresh token", e);
                Debug.logError(e, "Failed to persist rotated SMTP OAuth2 refresh token for mailSmtpConfigId ["
                        + mailSmtpConfigId + "]", MODULE);
            }
        } catch (GenericTransactionException e) {
            Debug.logError(e, "Transaction error while persisting rotated SMTP OAuth2 refresh token for mailSmtpConfigId ["
                    + mailSmtpConfigId + "]", MODULE);
        } finally {
            if (parentTx != null) {
                try {
                    TransactionUtil.resume(parentTx);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Failed to resume parent transaction after persisting rotated SMTP OAuth2 refresh token", MODULE);
                }
            }
        }
    }
}
