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
package org.apache.ofbiz.catalina.container;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.logging.log4j.ThreadContext;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.stats.VisitHandler;

/**
 * Populates Log4j2's {@code ThreadContext} (MDC) with per-request logging correlation fields -
 * {@code requestId}, {@code visitId}, {@code userLoginId} - so every log line produced while
 * handling one HTTP request or one user visit can be tied together (see
 * {@code framework/base/config/log4j2.xml}, the ECS JSON logging profile in particular).
 *
 * <p>Runs as an engine-level Valve, ahead of the servlet filter chain, so the fields are already
 * in place before {@code ControlFilter} and every downstream filter/servlet run.
 *
 * <p>Additional fields can be contributed by any component without changing this class - see
 * {@link CorrelationFieldProvider}.
 */
public class CorrelationValve extends ValveBase {

    private static final String MODULE = CorrelationValve.class.getName();
    private static final Set<String> CORE_FIELDS = Set.of("requestId", "visitId", "userLoginId");
    private static final List<CorrelationFieldProvider> PROVIDERS = loadProviders();
    private static final String PBKDF2_HASH_TYPE = "PBKDF2WithHmacSHA256";
    // Same property HashCrypt's password hashing uses, so hash mode's cost factor stays in step
    // with whatever this deployment already tuned for password storage.
    private static final int PBKDF2_ITERATIONS = UtilProperties.getPropertyAsInteger("security.properties",
            "password.encrypt.pbkdf2.iterations", 10000);

    /**
     * Controls what {@code userLoginId} value (if any) is written into the log correlation
     * context - see {@code log.correlation.userLoginId.mode} in {@code debug.properties}.
     * {@code PLAIN} keeps today's behavior; {@code HASH} substitutes a deterministic PBKDF2
     * digest (same user, same digest, every request, but not reversible to the raw value without
     * the pepper); {@code OFF} omits the field entirely.
     */
    private enum UserLoginIdMode { PLAIN, HASH, OFF }

    private final UserLoginIdMode userLoginIdMode;
    private final String userLoginIdHashPepper;
    // Digest length, not the PRF named above - shrinks the userLoginId field written to every log
    // line. Lowering this does not weaken resistance against an attacker who holds the pepper
    // (that's governed by PBKDF2_ITERATIONS and pepper secrecy alone); it only raises the odds of
    // two different users' digests colliding, negligible at 128 bits for any realistic userbase -
    // and tracing a specific user is expected to go through visitId/Visit, not a reversed digest.
    private final int userLoginIdHashKeyLengthBits;

    CorrelationValve() {
        super();
        this.userLoginIdMode = resolveUserLoginIdMode();
        this.userLoginIdHashPepper = resolveHashPepper(this.userLoginIdMode);
        this.userLoginIdHashKeyLengthBits = resolveHashKeyLengthBits(this.userLoginIdMode);
    }

    private static List<CorrelationFieldProvider> loadProviders() {
        List<CorrelationFieldProvider> providers = new ArrayList<>();
        ServiceLoader.load(CorrelationFieldProvider.class).forEach(providers::add);
        return List.copyOf(providers);
    }

    private static UserLoginIdMode resolveUserLoginIdMode() {
        String mode = UtilProperties.getPropertyValue("debug", "log.correlation.userLoginId.mode", "plain");
        try {
            return UserLoginIdMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            Debug.logWarning("Invalid log.correlation.userLoginId.mode [" + mode + "]; defaulting to 'plain'", MODULE);
            return UserLoginIdMode.PLAIN;
        }
    }

    private static String resolveHashPepper(UserLoginIdMode mode) {
        String pepper = UtilProperties.getPropertyValue("debug", "log.correlation.userLoginId.hash.pepper", "");
        if (mode == UserLoginIdMode.HASH && UtilValidate.isEmpty(pepper)) {
            // Fail fast: a blank pepper must never fall back to "unsalted" or "randomly salted
            // per call" behind the operator's back - either would silently defeat what hash mode
            // promises (see HashCrypt.pbkdf2HashCrypt - an empty salt draws a fresh random one on
            // every call, which would make the same userLoginId hash differently every request).
            throw new IllegalStateException("log.correlation.userLoginId.mode=hash requires "
                    + "log.correlation.userLoginId.hash.pepper to be set - refusing to start with an "
                    + "unconfigured pepper");
        }
        return pepper;
    }

    private static int resolveHashKeyLengthBits(UserLoginIdMode mode) {
        int keyLengthBits = UtilProperties.getPropertyAsInteger("debug",
                "log.correlation.userLoginId.hash.keyLengthBits", 128);
        if (mode == UserLoginIdMode.HASH && keyLengthBits <= 0) {
            // Fail fast at startup rather than at first login: PBEKeySpec rejects a non-positive
            // key length with an unchecked IllegalArgumentException that hashUserLoginId's
            // catch block doesn't handle, so a bad value would otherwise only surface as an
            // uncaught error on the first authenticated request.
            throw new IllegalStateException("log.correlation.userLoginId.mode=hash requires "
                    + "log.correlation.userLoginId.hash.keyLengthBits to be a positive number, got "
                    + keyLengthBits);
        }
        return keyLengthBits;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        Set<String> contextKeys = new LinkedHashSet<>();
        try {
            putField(contextKeys, "requestId", generateRequestId());

            HttpSession session = request.getSession(false);
            String visitId = session != null ? VisitHandler.getVisitId(session) : null;
            if (visitId != null) {
                putField(contextKeys, "visitId", visitId);
            }
            GenericValue userLogin = session != null ? (GenericValue) session.getAttribute("userLogin") : null;
            if (userLogin != null) {
                switch (userLoginIdMode) {
                case HASH:
                    putField(contextKeys, "userLoginId", hashUserLoginId(userLogin.getString("userLoginId")));
                    break;
                case OFF:
                    break;
                case PLAIN:
                default:
                    putField(contextKeys, "userLoginId", userLogin.getString("userLoginId"));
                }
            }

            for (CorrelationFieldProvider provider : PROVIDERS) {
                applyProvider(provider, request, userLogin, contextKeys);
            }

            getNext().invoke(request, response);
        } finally {
            contextKeys.forEach(ThreadContext::remove);
        }
    }

    private void applyProvider(CorrelationFieldProvider provider, Request request, GenericValue userLogin, Set<String> contextKeys) {
        try {
            Map<String, String> fields = provider.getFields(request, userLogin);
            if (fields == null) {
                return;
            }
            for (Map.Entry<String, String> field : fields.entrySet()) {
                if (CORE_FIELDS.contains(field.getKey())) {
                    Debug.logWarning("CorrelationFieldProvider [" + provider.getClass().getName() + "] overwrote core "
                            + "correlation field '" + field.getKey() + "'", MODULE);
                }
                putField(contextKeys, field.getKey(), field.getValue());
            }
        } catch (Exception e) {
            Debug.logError(e, "CorrelationFieldProvider [" + provider.getClass().getName() + "] failed to provide "
                    + "correlation fields; skipping it for this request", MODULE);
        }
    }

    private void putField(Set<String> contextKeys, String key, String value) {
        ThreadContext.put(key, value);
        contextKeys.add(key);
    }

    /**
     * Hashes {@code userLoginId} with the same password-grade PBKDF2 algorithm/iteration count
     * OFBiz already uses for password storage, so the same user always produces the same value
     * (preserving cross-request correlation) while making offline guessing of low-entropy values
     * like email addresses meaningfully more expensive than a fast digest would.
     *
     * <p>Deliberately does <b>not</b> call {@code HashCrypt.pbkdf2HashCrypt} - that method's
     * output format is {@code {type}iterations$base64(salt)$base64(hash)}, i.e. it embeds the
     * salt itself, base64-encoded (trivially reversible), directly in the returned string. That's
     * correct for password storage, where each password's own random salt has to travel with its
     * hash to be verifiable later. Here the "salt" is {@link #userLoginIdHashPepper}, one fixed
     * deployment secret already available out-of-band from {@code debug.properties} - embedding
     * it in every hashed log line would put the pepper in cleartext-recoverable form in the exact
     * artifact this feature exists to protect, regardless of whether the pepper is ever otherwise
     * "leaked". Deriving the key directly and returning only the digest avoids that.
     */
    private String hashUserLoginId(String userLoginId) {
        try {
            PBEKeySpec spec = new PBEKeySpec(userLoginId.toCharArray(),
                    userLoginIdHashPepper.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, userLoginIdHashKeyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_HASH_TYPE);
            byte[] key = factory.generateSecret(spec).getEncoded();
            return "{PBKDF2-SHA256}" + Base64.getEncoder().encodeToString(key);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new GeneralRuntimeException("Error hashing userLoginId for log correlation", e);
        }
    }

    /**
     * Generates a UUID-shaped log correlation id from {@link ThreadLocalRandom} instead of
     * {@link UUID#randomUUID()}. Request ids only need to be unique for log correlation, not
     * cryptographically unpredictable, and UUID.randomUUID() draws from a shared SecureRandom
     * that can become a contention point across threads under high concurrency.
     */
    private static String generateRequestId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }
}
