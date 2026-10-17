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
package org.apache.ofbiz.webtools.secret;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.crypto.ConfigCryptoUtil;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.secret.SecretProviderFactory;
import org.apache.ofbiz.base.secret.SecretValueResolver;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * Encrypts secret/password values with {@link ConfigCryptoUtil} (AES-256-GCM, keyed by the
 * {@code OFBIZ_MASTER_KEY} environment variable) and stores the resulting {@code ENC(...)} value
 * either in a {@code SystemProperty} record or as a {@code jdbc-password.<lookupKey>} entry in
 * {@code framework/base/config/passwords.properties}.
 */
public final class SecretManagerServices {

    private static final String MODULE = SecretManagerServices.class.getName();

    public static final String TARGET_SYSTEM_PROPERTY = "SYSTEM_PROPERTY";
    public static final String TARGET_PASSWORDS_FILE = "PASSWORDS_FILE";

    private static final String PASSWORDS_FILE_LOCATION = "component://base/config/passwords.properties";
    private static final String JDBC_PASSWORD_PREFIX = "jdbc-password.";
    /** Maximum length for lookupKey and related identifier fields written to properties files or logs. */
    static final int MAX_KEY_LENGTH = 256;
    /** Maximum length for a secret value; guards against oversized inputs that could exhaust memory or storage. */
    static final int MAX_SECRET_VALUE_LENGTH = 8192;
    /** Allows only letters, digits, dots, hyphens, underscores — blocks marker wrappers and path traversal. */
    static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[\\w.\\-]+$");

    /** Read once at class load — deployment mode never changes at runtime. */
    private static final String DEPLOYMENT_MODE =
            UtilProperties.getPropertyValue("security", "secret.audit.deployment.mode", "DIRECT");

    private SecretManagerServices() { }

    private static boolean hasSecretMaintPermission(Security security, GenericValue userLogin) {
        return security != null
                && (security.hasPermission("SECRET_MAINT", userLogin)
                        || security.hasPermission("ENTITY_MAINT", userLogin));
    }

    private static Map<String, Object> deniedResult(Delegator delegator, GenericValue userLogin,
            String action, String secretKeyRef, String secretTarget,
            String systemResourceId, String systemPropertyId) {
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        String userLoginIdOrNull = (userLogin != null) ? userLogin.getString("userLoginId") : null;
        Debug.logWarning("[SECRET_AUDIT] action=" + action + " user=" + userLoginId
                + (secretKeyRef != null ? " key=" + secretKeyRef : "")
                + (secretTarget != null ? " target=" + secretTarget : "")
                + " mode=" + DEPLOYMENT_MODE + " accessMode=NOT_APPLICABLE outcome=DENIED", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLoginIdOrNull)
                .action(action)
                .secretKeyRef(secretKeyRef)
                .secretTarget(secretTarget)
                .systemResourceId(systemResourceId)
                .systemPropertyId(systemPropertyId)
                .outcome("DENIED")
                .build());
        return ServiceUtil.returnError("You do not have permission to perform this operation");
    }

    /**
     * Tests connectivity to the active {@link org.apache.ofbiz.base.secret.SecretProvider} by
     * attempting to resolve {@code testKey}. Reports three outcomes:
     * <ul>
     *   <li><em>Connected — key found</em>: the provider returned a non-empty value.</li>
     *   <li><em>Connected — key not found</em>: the provider responded normally but the key does
     *       not exist (confirms vault connectivity even when the test key is wrong).</li>
     *   <li><em>Connection failed</em>: the provider threw an SDK or network error.</li>
     * </ul>
     */
    public static Map<String, Object> testSecretProviderConnection(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            String testKeyRaw = (String) context.get("testKey");
            return deniedResult(delegator, userLogin, "TEST_CONNECTION",
                    UtilValidate.isNotEmpty(testKeyRaw) ? testKeyRaw.trim() : null, null, null, null);
        }
        String testKey = (String) context.get("testKey");
        if (UtilValidate.isEmpty(testKey)) {
            return ServiceUtil.returnError("testKey is required");
        }
        testKey = testKey.trim();
        if (testKey.length() > MAX_KEY_LENGTH) {
            return ServiceUtil.returnError("testKey must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        if (!SAFE_IDENTIFIER.matcher(testKey).matches()) {
            return ServiceUtil.returnError("testKey must contain only letters, digits, dots, hyphens, and underscores");
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        String userLoginIdOrNull = (userLogin != null) ? userLogin.getString("userLoginId") : null;
        try {
            String value = SecretProviderFactory.getInstance().getSecret(testKey);
            String providerName = SecretProviderFactory.getProviderName();
            Debug.logInfo("[SECRET_AUDIT] action=TEST_CONNECTION user=" + userLoginId
                    + " key=" + testKey
                    + " provider=" + providerName
                    + " mode=" + DEPLOYMENT_MODE
                    + " accessMode=PROVIDER_CALL outcome=SUCCESS", MODULE);
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLoginIdOrNull)
                    .action("TEST_CONNECTION")
                    .secretKeyRef(testKey)
                    .providerType(providerName)
                    .accessMode("PROVIDER_CALL")
                    .outcome("SUCCESS")
                    .build());
            if (UtilValidate.isNotEmpty(value)) {
                return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' found successfully");
            }
            return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' returned an empty value");
        } catch (GeneralException e) {
            String msg = e.getMessage();
            String providerName = SecretProviderFactory.getProviderName();
            // "not found" responses confirm connectivity; only network/auth errors are real failures
            if (msg != null && (msg.contains("not found") || msg.contains("NotFound") || msg.contains("does not exist"))) {
                Debug.logInfo("[SECRET_AUDIT] action=TEST_CONNECTION user=" + userLoginId
                        + " key=" + testKey
                        + " provider=" + providerName
                        + " mode=" + DEPLOYMENT_MODE
                        + " accessMode=PROVIDER_CALL outcome=NOT_FOUND", MODULE);
                SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                        .userLoginId(userLoginIdOrNull)
                        .action("TEST_CONNECTION")
                        .secretKeyRef(testKey)
                        .providerType(providerName)
                        .accessMode("PROVIDER_CALL")
                        .outcome("NOT_FOUND")
                        .build());
                return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' was not found in the provider (vault is reachable)");
            }
            // Full SDK message logged separately for server-side debugging only — never in the [SECRET_AUDIT] line.
            Debug.logWarning("[SECRET_AUDIT] action=TEST_CONNECTION user=" + userLoginId
                    + " key=" + testKey
                    + " provider=" + providerName
                    + " mode=" + DEPLOYMENT_MODE
                    + " accessMode=PROVIDER_CALL outcome=FAILURE errorCategory=PROVIDER_ERROR", MODULE);
            Debug.logWarning("Secret provider connection test failed for key '" + testKey + "': " + msg, MODULE);
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLoginIdOrNull)
                    .action("TEST_CONNECTION")
                    .secretKeyRef(testKey)
                    .providerType(providerName)
                    .accessMode("PROVIDER_CALL")
                    .outcome("FAILURE")
                    .errorCategory("PROVIDER_ERROR")
                    .build());
            return ServiceUtil.returnError("Connection failed (" + e.getClass().getSimpleName()
                    + ") — check server logs for details");
        }
    }

    /**
     * Resets in-memory secret usage counters (hit/miss counts) to zero. Useful after a secret
     * rotation so operators can observe clean post-rotation metrics without a JVM restart.
     */
    public static Map<String, Object> resetSecretUsageStats(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "RESET_STATS", null, null, null, null);
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        SecretValueResolver.resetUsageStats();
        Debug.logInfo("[SECRET_AUDIT] action=RESET_STATS user=" + userLoginId
                + " mode=" + DEPLOYMENT_MODE + " accessMode=NOT_APPLICABLE outcome=SUCCESS", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("RESET_STATS")
                .outcome("SUCCESS")
                .build());
        return ServiceUtil.returnSuccess("Secret usage statistics reset successfully");
    }

    /**
     * Returns in-memory usage statistics from {@link SecretValueResolver}: aggregate hit/miss
     * totals and a per-key breakdown. The data covers the current JVM lifetime only and resets
     * on restart; it is meant for operator visibility, not durable monitoring.
     */
    public static Map<String, Object> getSecretUsageStats(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "VIEW_STATS", null, null, null, null);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("usageSummary", SecretValueResolver.getUsageSummary());
        result.put("usageReport", SecretValueResolver.getUsageReport());
        return result;
    }

    /**
     * Flushes all in-memory cached secret values so the next lookup re-fetches from the provider.
     * Useful after a secret rotation so the new value is picked up immediately without a restart.
     *
     * <p>This clears <strong>both</strong> cache layers: the {@link SecretValueResolver} TTL cache
     * (and the {@code UtilProperties} file cache it sits behind) and the active
     * {@link org.apache.ofbiz.base.secret.SecretProvider}'s own internal cache via
     * {@link SecretProviderFactory#invalidateCache()}. Clearing only the former is not sufficient:
     * each bundled vault provider (AWS, Azure, GCP, HashiCorp Vault, Bitwarden, 1Password) keeps its
     * own TTL-based cache (default 1 hour), so without this second call the next lookup would still
     * return the stale value straight from the provider's cache instead of re-fetching from the vault.</p>
     */
    public static Map<String, Object> flushSecretCache(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "FLUSH_CACHE", null, null, null, null);
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        SecretValueResolver.invalidateAll();
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        SecretProviderFactory.invalidateCache();
        Debug.logInfo("[SECRET_AUDIT] action=FLUSH_CACHE user=" + userLoginId
                + " mode=" + DEPLOYMENT_MODE + " accessMode=NOT_APPLICABLE outcome=SUCCESS", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("FLUSH_CACHE")
                .outcome("SUCCESS")
                .build());
        return ServiceUtil.returnSuccess("Secret value cache flushed successfully");
    }

    /**
     * Re-runs {@link SecretProviderFactory} discovery, replacing the active provider instance.
     * Useful after deploying a new vault plugin jar or editing that plugin's own connection
     * settings (e.g. a rotated AWS access key, a new HashiCorp AppRole secret_id) in its
     * {@code config/*.properties} file, without requiring a full OFBiz restart.
     */
    public static Map<String, Object> reloadSecretProvider(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "RELOAD_PROVIDER", null, null, null, null);
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        SecretProviderFactory.reload();
        String providerName = SecretProviderFactory.getProviderName();
        Debug.logInfo("[SECRET_AUDIT] action=RELOAD_PROVIDER user=" + userLoginId
                + " provider=" + providerName
                + " mode=" + DEPLOYMENT_MODE + " accessMode=NOT_APPLICABLE outcome=SUCCESS", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("RELOAD_PROVIDER")
                .providerType(providerName)
                .outcome("SUCCESS")
                .build());
        return ServiceUtil.returnSuccess("Secret provider reloaded — active provider is now: " + providerName);
    }

    /**
     * Pulls the current value of {@code lookupKey} from the active {@link
     * org.apache.ofbiz.base.secret.SecretProvider} and re-encrypts it into the local fallback
     * snapshot ({@code passwords.properties} or {@code SystemProperty.systemPropertyValue}),
     * keeping the ENC(...) value used when the remote vault is unreachable in sync with whatever
     * was last rotated in the vault. Delegates to {@link #storeEncryptedSecret} for the actual
     * write, so the same validation, audit logging, and cache-invalidation logic applies.
     *
     * <p>For {@code PASSWORDS_FILE} without a {@code systemResourceId}/{@code systemPropertyId}
     * pair (the {@code jdbc-password-lookup} case), the remote key fetched is
     * {@code jdbc-password.<lookupKey>}, matching what {@link
     * org.apache.ofbiz.entity.config.model.EntityConfig#getJdbcPassword} resolves at runtime. In
     * every other case the raw {@code lookupKey} is fetched directly.</p>
     */
    public static Map<String, Object> syncSecretFromProvider(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String secretTarget = (String) context.get("secretTarget");
        String systemResourceId = (String) context.get("systemResourceId");
        String systemPropertyId = (String) context.get("systemPropertyId");
        String lookupKey = (String) context.get("lookupKey");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "SYNC",
                    UtilValidate.isNotEmpty(lookupKey) ? lookupKey.trim() : null,
                    secretTarget, systemResourceId, systemPropertyId);
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";

        if (UtilValidate.isEmpty(lookupKey)) {
            return ServiceUtil.returnError("lookupKey is required to sync from the active secret provider");
        }
        lookupKey = lookupKey.trim();
        if (lookupKey.length() > MAX_KEY_LENGTH) {
            return ServiceUtil.returnError("lookupKey must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        if (!SAFE_IDENTIFIER.matcher(lookupKey).matches()) {
            return ServiceUtil.returnError("lookupKey must contain only letters, digits, dots, hyphens, and underscores");
        }

        boolean hasResourceId = UtilValidate.isNotEmpty(systemResourceId);
        String providerKey = (TARGET_PASSWORDS_FILE.equals(secretTarget) && !hasResourceId)
                ? JDBC_PASSWORD_PREFIX + lookupKey
                : lookupKey;

        String freshValue;
        try {
            freshValue = SecretProviderFactory.getInstance().getSecret(providerKey);
        } catch (GeneralException e) {
            Debug.logWarning("[SECRET_AUDIT] action=SYNC user=" + userLoginId
                    + " key=" + providerKey
                    + " target=" + secretTarget
                    + " provider=" + SecretProviderFactory.getProviderName()
                    + " mode=" + DEPLOYMENT_MODE
                    + " accessMode=PROVIDER_CALL outcome=FAILURE errorCategory=PROVIDER_ERROR", MODULE);
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                    .action("SYNC")
                    .secretKeyRef(providerKey)
                    .secretTarget(secretTarget)
                    .providerType(SecretProviderFactory.getProviderName())
                    .accessMode("PROVIDER_CALL")
                    .systemResourceId(systemResourceId)
                    .systemPropertyId(systemPropertyId)
                    .outcome("FAILURE")
                    .errorCategory("PROVIDER_ERROR")
                    .build());
            return ServiceUtil.returnError("Failed to fetch the current value from the active secret provider for key '"
                    + providerKey + "' (" + e.getClass().getSimpleName() + ") — check server logs for details");
        }
        if (UtilValidate.isEmpty(freshValue)) {
            return ServiceUtil.returnError("Secret provider returned an empty value for key '" + providerKey + "'");
        }

        try {
            String currentValue = readCurrentStoredValue(delegator, secretTarget, systemResourceId, systemPropertyId, providerKey);
            if (currentValue != null && currentValue.equals(freshValue)) {
                Debug.logInfo("[SECRET_AUDIT] action=SYNC user=" + userLoginId
                        + " key=" + providerKey
                        + " target=" + secretTarget
                        + " provider=" + SecretProviderFactory.getProviderName()
                        + " mode=" + DEPLOYMENT_MODE
                        + " accessMode=PROVIDER_CALL outcome=NO_CHANGE", MODULE);
                SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                        .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                        .action("SYNC")
                        .secretKeyRef(providerKey)
                        .secretTarget(secretTarget)
                        .providerType(SecretProviderFactory.getProviderName())
                        .accessMode("PROVIDER_CALL")
                        .systemResourceId(systemResourceId)
                        .systemPropertyId(systemPropertyId)
                        .outcome("NO_CHANGE")
                        .build());
                Map<String, Object> result = ServiceUtil.returnSuccess("Local encrypted snapshot for '" + lookupKey
                        + "' is already up to date — no change detected from the active secret provider");
                result.put("changed", Boolean.FALSE);
                return result;
            }
            storeEncryptedSecret(delegator, userLogin, secretTarget, systemResourceId, systemPropertyId, lookupKey, freshValue);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Debug.logInfo("[SECRET_AUDIT] action=SYNC user=" + userLoginId
                + " key=" + providerKey
                + " target=" + secretTarget
                + " provider=" + SecretProviderFactory.getProviderName()
                + " mode=" + DEPLOYMENT_MODE
                + " accessMode=PROVIDER_CALL outcome=SUCCESS", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("SYNC")
                .secretKeyRef(providerKey)
                .secretTarget(secretTarget)
                .providerType(SecretProviderFactory.getProviderName())
                .accessMode("PROVIDER_CALL")
                .systemResourceId(systemResourceId)
                .systemPropertyId(systemPropertyId)
                .outcome("SUCCESS")
                .build());
        Map<String, Object> result = ServiceUtil.returnSuccess("Local encrypted snapshot for '" + lookupKey
                + "' synced from the active secret provider");
        result.put("changed", Boolean.TRUE);
        return result;
    }

    /**
     * Returns the current plaintext value already stored locally for {@code providerKey}, or
     * {@code null} if there is nothing stored yet (e.g. first-ever sync). Used by {@link
     * #syncSecretFromProvider} to skip re-encrypting and rewriting when the vault value hasn't
     * actually changed, so {@code lastRotatedDate} only advances on a real rotation.
     */
    static String readCurrentStoredValue(Delegator delegator, String secretTarget,
            String systemResourceId, String systemPropertyId, String providerKey) throws GeneralException {
        if (TARGET_SYSTEM_PROPERTY.equals(secretTarget)) {
            GenericValue systemProperty = EntityQuery.use(delegator).from("SystemProperty")
                    .where("systemResourceId", systemResourceId, "systemPropertyId", systemPropertyId)
                    .queryOne();
            if (systemProperty == null) {
                return null;
            }
            String storedValue = systemProperty.getString("systemPropertyValue");
            return UtilValidate.isEmpty(storedValue) ? null : ConfigCryptoUtil.decryptIfEncrypted(storedValue, providerKey);
        } else if (TARGET_PASSWORDS_FILE.equals(secretTarget)) {
            String storedValue = UtilProperties.getPropertyValue("passwords", providerKey);
            return UtilValidate.isEmpty(storedValue) ? null : ConfigCryptoUtil.decryptIfEncrypted(storedValue, providerKey);
        }
        return null;
    }

    /**
     * Scheduled-job entry point (see {@code JobSandbox} seed data "SECRET_AUTOSYNC") that
     * automatically calls {@link #syncSecretFromProvider} for every {@code SystemProperty} row that
     * has a non-empty {@code systemPropertyLookup}, so a secret rotated in the remote vault is
     * written back to {@code SystemProperty.systemPropertyValue} without an admin manually clicking
     * "Sync Now" in the {@code EncryptValue} screen.
     *
     * <p>No-ops when {@code secret.rotation.autosync.enabled} (security.properties) is not
     * {@code true} — disabled by default. A failure resolving one key is logged and counted, but
     * does not prevent the remaining keys from being processed.</p>
     */
    public static Map<String, Object> autoSyncRotatedSecrets(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "ROTATION_POLL", null, null, null, null);
        }
        if (!UtilProperties.getPropertyAsBoolean("security", "secret.rotation.autosync.enabled", false)) {
            return ServiceUtil.returnSuccess(
                    "Automatic secret rotation sync is disabled (secret.rotation.autosync.enabled=false)");
        }
        return syncAllRotatedSystemProperties(dctx, userLogin);
    }

    /**
     * Queries every {@code SystemProperty} row with a non-empty {@code systemPropertyLookup} and
     * calls {@link #syncSecretFromProvider} for each, isolating per-row failures. Split out from
     * {@link #autoSyncRotatedSecrets} so the looping/counting logic can be unit-tested without
     * depending on the {@code secret.rotation.autosync.enabled} flag.
     */
    static Map<String, Object> syncAllRotatedSystemProperties(DispatchContext dctx, GenericValue userLogin) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        long syncedCount = 0;
        long unchangedCount = 0;
        long failedCount = 0;
        try {
            List<GenericValue> lookupBackedProperties = EntityQuery.use(delegator).from("SystemProperty")
                    .where(EntityCondition.makeCondition("systemPropertyLookup", EntityOperator.NOT_EQUAL, null))
                    .queryList();
            for (GenericValue systemProperty : lookupBackedProperties) {
                String systemResourceId = systemProperty.getString("systemResourceId");
                String systemPropertyId = systemProperty.getString("systemPropertyId");
                String lookupKey = systemProperty.getString("systemPropertyLookup");
                if (UtilValidate.isEmpty(lookupKey)) {
                    continue;
                }
                try {
                    Map<String, Object> syncResult = dispatcher.runSync("syncSecretFromProvider", UtilMisc.toMap(
                            "secretTarget", TARGET_SYSTEM_PROPERTY,
                            "systemResourceId", systemResourceId,
                            "systemPropertyId", systemPropertyId,
                            "lookupKey", lookupKey,
                            "userLogin", userLogin));
                    if (ServiceUtil.isError(syncResult)) {
                        failedCount++;
                        Debug.logWarning("Secret auto-sync failed for " + systemResourceId + "." + systemPropertyId
                                + ": " + ServiceUtil.getErrorMessage(syncResult), MODULE);
                    } else if (Boolean.TRUE.equals(syncResult.get("changed"))) {
                        syncedCount++;
                    } else {
                        unchangedCount++;
                    }
                } catch (GeneralException e) {
                    failedCount++;
                    Debug.logWarning("Secret auto-sync exception for " + systemResourceId + "." + systemPropertyId
                            + " (" + e.getClass().getSimpleName() + ")", MODULE);
                }
            }
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            Debug.logWarning("[SECRET_AUDIT] action=ROTATION_POLL"
                    + " mode=" + DEPLOYMENT_MODE + " outcome=FAILURE errorCategory=PROVIDER_ERROR", MODULE);
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                    .action("ROTATION_POLL")
                    .providerType(SecretProviderFactory.getProviderName())
                    .outcome("FAILURE")
                    .errorCategory("PROVIDER_ERROR")
                    .build());
            return ServiceUtil.returnError("Unable to query SystemProperty rows for rotation sync: " + e.getMessage());
        }

        long totalChecked = syncedCount + unchangedCount + failedCount;
        String pollOutcome = failedCount > 0 ? "PARTIAL_FAILURE" : "SUCCESS";
        String providerName = SecretProviderFactory.getProviderName();
        Debug.logInfo("[SECRET_AUDIT] action=ROTATION_POLL checked=" + totalChecked
                + " synced=" + syncedCount + " unchanged=" + unchangedCount + " failed=" + failedCount
                + " provider=" + providerName + " mode=" + DEPLOYMENT_MODE + " outcome=" + pollOutcome, MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("ROTATION_POLL")
                .providerType(providerName)
                .outcome(pollOutcome)
                .build());
        Map<String, Object> result = ServiceUtil.returnSuccess("Automatic secret rotation sync complete: "
                + syncedCount + " synced, " + unchangedCount + " unchanged, " + failedCount + " failed");
        result.put("syncedCount", syncedCount);
        result.put("unchangedCount", unchangedCount);
        result.put("failedCount", failedCount);
        return result;
    }

    /** Service implementation for the webtools "Encrypt Value" screen. */
    public static Map<String, Object> createEncryptedSecret(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String secretTarget = (String) context.get("secretTarget");
        String systemResourceId = (String) context.get("systemResourceId");
        String systemPropertyId = (String) context.get("systemPropertyId");
        String lookupKey = (String) context.get("lookupKey");
        String secretValue = (String) context.get("secretValue");
        String secretValueConfirm = (String) context.get("secretValueConfirm");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "STORE",
                    UtilValidate.isNotEmpty(lookupKey) ? lookupKey.trim() : null,
                    secretTarget, systemResourceId, systemPropertyId);
        }

        if (UtilValidate.isNotEmpty(secretValueConfirm) && !secretValueConfirm.equals(secretValue)) {
            return ServiceUtil.returnError("Secret Value and Confirm Secret Value do not match");
        }

        try {
            storeEncryptedSecret(delegator, userLogin, secretTarget, systemResourceId, systemPropertyId, lookupKey, secretValue);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess("Secret value encrypted and stored successfully");
    }

    /**
     * Encrypts {@code secretValue} and stores it as configured by {@code secretTarget}. Used by
     * both the single-entry service ({@link #createEncryptedSecret}) and the CSV bulk-upload
     * event so that both paths share the exact same validation and storage logic.
     *
     * <p>A {@code SecretAuditLog} row is written on every successful call via
     * {@link SecretAuditLogger} so the who/what/when of each store is preserved for compliance.</p>
     */
    public static void storeEncryptedSecret(Delegator delegator, GenericValue userLogin, String secretTarget,
            String systemResourceId, String systemPropertyId, String lookupKey, String secretValue)
            throws GeneralException {
        if (UtilValidate.isEmpty(secretValue) || secretValue.trim().isEmpty()) {
            throw new GeneralException("secretValue is required and must not be blank");
        }
        if (secretValue.length() > MAX_SECRET_VALUE_LENGTH) {
            throw new GeneralException("secretValue must not exceed " + MAX_SECRET_VALUE_LENGTH + " characters");
        }
        if (secretValue.trim().startsWith("ENC(")) {
            throw new GeneralException("secretValue must be the plain secret — do not enter an ENC(...) encrypted value");
        }
        if (UtilValidate.isNotEmpty(systemResourceId) && systemResourceId.trim().length() > MAX_KEY_LENGTH) {
            throw new GeneralException("systemResourceId must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        if (UtilValidate.isNotEmpty(systemResourceId) && !SAFE_IDENTIFIER.matcher(systemResourceId.trim()).matches()) {
            throw new GeneralException(
                    "systemResourceId contains invalid characters — only letters, digits, dots, hyphens, and underscores are allowed");
        }
        if (UtilValidate.isNotEmpty(systemPropertyId) && systemPropertyId.trim().length() > MAX_KEY_LENGTH) {
            throw new GeneralException("systemPropertyId must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        if (UtilValidate.isNotEmpty(systemPropertyId) && !SAFE_IDENTIFIER.matcher(systemPropertyId.trim()).matches()) {
            throw new GeneralException(
                    "systemPropertyId contains invalid characters — only letters, digits, dots, hyphens, and underscores are allowed");
        }
        if (UtilValidate.isNotEmpty(lookupKey) && lookupKey.trim().length() > MAX_KEY_LENGTH) {
            throw new GeneralException("lookupKey must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        if (UtilValidate.isNotEmpty(lookupKey) && !SAFE_IDENTIFIER.matcher(lookupKey.trim()).matches()) {
            throw new GeneralException("lookupKey must contain only letters, digits, dots, hyphens, and underscores"
                    + " — do not enter a " + SecretValueResolver.MARKER_NAME + "(...) marker or path separator");
        }
        String encryptedValue = "ENC(" + ConfigCryptoUtil.encrypt(secretValue, getMasterKey()) + ")";

        if (TARGET_PASSWORDS_FILE.equals(secretTarget)) {
            if (UtilValidate.isEmpty(lookupKey)) {
                throw new GeneralException("lookupKey is required for passwords.properties");
            }
            boolean hasResourceId = UtilValidate.isNotEmpty(systemResourceId);
            boolean hasPropertyId = UtilValidate.isNotEmpty(systemPropertyId);
            if (hasResourceId != hasPropertyId) {
                throw new GeneralException(
                        "systemResourceId and systemPropertyId must both be provided together");
            }
            if (hasResourceId) {
                // Case B: property-file combined write — use plain lookupKey, no jdbc-password. prefix.
                // Write to passwords.properties first; if the source-file update then fails, remove
                // the orphaned ENC entry so the two files don't end up in an inconsistent state.
                writePasswordsProperty(lookupKey, encryptedValue);
                try {
                    updatePropertiesFileAndRefreshCache(systemResourceId, systemPropertyId, lookupKey);
                } catch (GeneralException e) {
                    removePasswordsEntry(lookupKey);
                    throw e;
                }
                // Invalidate the resolved-value cache so the new secret is visible immediately.
                SecretValueResolver.invalidate(lookupKey);
            } else {
                // Case A: entityengine.xml jdbc-password-lookup — keep the jdbc-password. prefix.
                // Clear both the UtilProperties file cache and the SecretValueResolver TTL cache so
                // FileBasedSecretProvider picks up the new ENC(...) value without a restart.
                writePasswordsProperty(JDBC_PASSWORD_PREFIX + lookupKey, encryptedValue);
                UtilCache.clearCachesThatStartWith("properties.UtilProperties");
                SecretValueResolver.invalidate(JDBC_PASSWORD_PREFIX + lookupKey);
            }
        } else if (TARGET_SYSTEM_PROPERTY.equals(secretTarget)) {
            if (UtilValidate.isEmpty(systemResourceId) || UtilValidate.isEmpty(systemPropertyId)) {
                throw new GeneralException("systemResourceId and systemPropertyId are required for SystemProperty");
            }
            storeSystemPropertySecret(delegator, systemResourceId, systemPropertyId, lookupKey, encryptedValue);
            // Invalidate the resolved-value cache for the lookup key so EntityUtilProperties
            // picks up the new value immediately via SecretValueResolver.resolveKey().
            if (UtilValidate.isNotEmpty(lookupKey)) {
                SecretValueResolver.invalidate(lookupKey);
            }
        } else {
            throw new GeneralException("Unknown secretTarget '" + secretTarget + "'");
        }

        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        Debug.logInfo("[SECRET_AUDIT] action=STORE user=" + userLoginId
                + " key=" + lookupKey
                + " target=" + secretTarget
                + " mode=" + DEPLOYMENT_MODE + " accessMode=NOT_APPLICABLE outcome=SUCCESS", MODULE);
        SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                .action("STORE")
                .secretKeyRef(lookupKey)
                .secretTarget(secretTarget)
                .systemResourceId(systemResourceId)
                .systemPropertyId(systemPropertyId)
                .outcome("SUCCESS")
                .build());
    }

    /**
     * Updates the {@code secret.audit.retention.days} and {@code secret.audit.purge.batch.size}
     * {@code SystemProperty} rows from the Audit Log Viewer retention settings panel.
     * Requires {@code SECRET_MAINT} (enforced inline; see {@link #hasSecretMaintPermission}).
     */
    public static Map<String, Object> updateSecretAuditRetention(DispatchContext dctx,
            Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!hasSecretMaintPermission(dctx.getSecurity(), userLogin)) {
            return deniedResult(delegator, userLogin, "UPDATE_RETENTION", null, null, null, null);
        }
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";

        String retentionDaysStr = UtilValidate.isNotEmpty((String) context.get("retentionDays"))
                ? ((String) context.get("retentionDays")).trim() : "";
        String batchSizeStr = UtilValidate.isNotEmpty((String) context.get("batchSize"))
                ? ((String) context.get("batchSize")).trim() : "";

        int retentionDays;
        try {
            retentionDays = Integer.parseInt(retentionDaysStr);
        } catch (NumberFormatException e) {
            return ServiceUtil.returnError("Retention period must be a positive integer");
        }
        if (retentionDays < 1) {
            return ServiceUtil.returnError("Retention period must be at least 1 day");
        }

        int batchSize;
        try {
            batchSize = Integer.parseInt(batchSizeStr);
        } catch (NumberFormatException e) {
            return ServiceUtil.returnError("Purge batch size must be a positive integer");
        }
        if (batchSize < 1 || batchSize > 10000) {
            return ServiceUtil.returnError("Purge batch size must be between 1 and 10000");
        }

        try {
            upsertSystemProperty(delegator, "security", "secret.audit.retention.days",
                    String.valueOf(retentionDays));
            upsertSystemProperty(delegator, "security", "secret.audit.purge.batch.size",
                    String.valueOf(batchSize));
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Failed to update retention settings: " + e.getMessage());
        }

        Debug.logInfo("[SECRET_AUDIT] action=UPDATE_RETENTION user=" + userLoginId
                + " retentionDays=" + retentionDays + " batchSize=" + batchSize
                + " mode=" + DEPLOYMENT_MODE + " outcome=SUCCESS", MODULE);
        return ServiceUtil.returnSuccess(
                "Audit log retention updated: " + retentionDays + " days, batch size " + batchSize);
    }

    private static void upsertSystemProperty(Delegator delegator, String resourceId,
            String propertyId, String value) throws GenericEntityException {
        GenericValue sp = EntityQuery.use(delegator).from("SystemProperty")
                .where("systemResourceId", resourceId, "systemPropertyId", propertyId)
                .queryOne();
        if (sp == null) {
            sp = delegator.makeValue("SystemProperty",
                    "systemResourceId", resourceId, "systemPropertyId", propertyId);
        }
        sp.set("systemPropertyValue", value);
        delegator.createOrStore(sp);
    }

    /**
     * Scheduled purge job: deletes {@link SecretAuditLog} rows whose {@code auditTimestamp} is older
     * than the {@code secret.audit.retention.days} SystemProperty (default 365 days). Rows are removed
     * in bounded batches of at most {@code secret.audit.purge.batch.size} (default 500) per database
     * transaction so the purge never holds a long-running table lock.
     *
     * <p>The {@code SECRET_AUDIT_PURGE} JobSandbox entry runs this weekly. PCI-DSS 4.0 §10.7 requires
     * at least 12 months — do not set {@code secret.audit.retention.days} below 365 in production.</p>
     */
    public static Map<String, Object> purgeExpiredSecretAuditLogs(DispatchContext dctx,
            Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        int retentionDays = 365;
        int batchSize = 500;
        try {
            String v = EntityUtilProperties.getPropertyValue("security", "secret.audit.retention.days", delegator);
            if (UtilValidate.isNotEmpty(v)) {
                retentionDays = Integer.parseInt(v.trim());
            }
        } catch (NumberFormatException e) {
            Debug.logWarning("secret.audit.retention.days is not a valid integer — using default 365", MODULE);
        }
        try {
            String v = EntityUtilProperties.getPropertyValue("security", "secret.audit.purge.batch.size", delegator);
            if (UtilValidate.isNotEmpty(v)) {
                batchSize = Integer.parseInt(v.trim());
            }
        } catch (NumberFormatException e) {
            Debug.logWarning("secret.audit.purge.batch.size is not a valid integer — using default 500", MODULE);
        }

        Timestamp cutoff = Timestamp.from(Instant.now().minus(Duration.ofDays(retentionDays)));
        EntityCondition expired = EntityCondition.makeCondition(
                "auditTimestamp", EntityOperator.LESS_THAN, cutoff);

        long totalPurged = 0;
        try {
            while (true) {
                List<GenericValue> batch = EntityQuery.use(delegator)
                        .select("secretAuditLogId")
                        .from("SecretAuditLog")
                        .where(expired)
                        .maxRows(batchSize)
                        .queryList();
                if (batch.isEmpty()) {
                    break;
                }
                delegator.removeAll(batch);
                totalPurged += batch.size();
                if (batch.size() < batchSize) {
                    break;
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "purgeExpiredSecretAuditLogs: batch delete failed after "
                    + totalPurged + " rows", MODULE);
            return ServiceUtil.returnError("Purge failed after " + totalPurged + " rows: "
                    + e.getMessage());
        }

        Debug.logInfo("purgeExpiredSecretAuditLogs: purged " + totalPurged
                + " SecretAuditLog row(s) older than " + retentionDays + " days", MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess(
                "Purged " + totalPurged + " SecretAuditLog row(s) older than " + retentionDays + " days");
        result.put("purgedCount", totalPurged);
        return result;
    }

    private static void storeSystemPropertySecret(Delegator delegator, String systemResourceId, String systemPropertyId,
            String lookupKey, String encryptedValue) throws GeneralException {
        GenericValue systemProperty = EntityQuery.use(delegator).from("SystemProperty")
                .where("systemResourceId", systemResourceId, "systemPropertyId", systemPropertyId)
                .queryOne();
        if (systemProperty == null) {
            systemProperty = delegator.makeValue("SystemProperty",
                    "systemResourceId", systemResourceId, "systemPropertyId", systemPropertyId);
        }
        systemProperty.set("systemPropertyValue", encryptedValue);
        if (UtilValidate.isNotEmpty(lookupKey)) {
            systemProperty.set("systemPropertyLookup", lookupKey);
        }
        systemProperty.set("lastRotatedDate", UtilDateTime.nowTimestamp());
        delegator.createOrStore(systemProperty);
    }

    /**
     * Searches all loaded OFBiz components for {@code config/<systemResourceId>.properties},
     * sets {@code <systemPropertyId>=LOOKUP(<lookupKey>)} in the source {@code .properties} file
     * (so that {@link org.apache.ofbiz.base.secret.SecretValueResolver} resolves it at runtime),
     * then clears the OFBiz property caches so the change is picked up immediately without a restart.
     *
     * <p>If the property previously referenced a different lookup key via {@code LOOKUP(old-key)},
     * the stale {@code old-key} entry is removed from passwords.properties before the new one is written.</p>
     *
     * <p>Scanning component directories (not the classpath) ensures we write to the actual
     * source {@code .properties} file rather than the build-output copy.</p>
     */
    private static void updatePropertiesFileAndRefreshCache(String systemResourceId,
            String systemPropertyId, String lookupKey) throws GeneralException {
        File propsFile = findSourcePropertiesFile(systemResourceId);
        if (propsFile == null) {
            throw new GeneralException(
                    "Properties file not found for resource: " + systemResourceId);
        }
        // If the property already points to a different lookup key, remove the stale passwords.properties entry
        String existingLookupKey = readSecretLookupKey(propsFile, systemPropertyId);
        if (existingLookupKey != null && !existingLookupKey.equals(lookupKey)) {
            removePasswordsEntry(existingLookupKey);
            Debug.logInfo("Removed stale passwords.properties entry for old lookup key: " + existingLookupKey, MODULE);
        }
        writePropertiesEntry(propsFile, systemPropertyId, SecretValueResolver.MARKER_NAME + "(" + lookupKey + ")");
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        Debug.logInfo("Set " + systemPropertyId + "=" + SecretValueResolver.MARKER_NAME + "(" + lookupKey + ") in "
                + propsFile.getPath() + " and refreshed property caches", MODULE);
    }

    /**
     * Reads {@code file} and returns the key inside a {@code LOOKUP(<key>)} marker for the given
     * {@code propertyId}, or {@code null} if the property is absent or not a LOOKUP reference.
     * Used only for the source {@code .properties} file path (Case B); {@code SystemProperty.systemPropertyLookup}
     * stores the raw key without a wrapper.
     */
    private static String readSecretLookupKey(File file, String propertyId) throws GeneralException {
        try {
            String linePrefix = propertyId + "=";
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith(linePrefix)) {
                    String value = line.substring(linePrefix.length()).trim();
                    String markerPrefix = SecretValueResolver.MARKER_NAME + "(";
                    if (value.startsWith(markerPrefix) && value.endsWith(")")) {
                        return value.substring(markerPrefix.length(), value.length() - 1);
                    }
                    return null;
                }
            }
            return null;
        } catch (IOException e) {
            throw new GeneralException("Unable to read " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    /** Removes the line for {@code key} from passwords.properties (no-op if not present). */
    private static void removePasswordsEntry(String key) throws GeneralException {
        removePropertiesEntry(getPasswordsFile(), key);
    }

    /** Removes the {@code key=...} line from {@code file} (no-op if not present). */
    private static synchronized void removePropertiesEntry(File file, String key) throws GeneralException {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String linePrefix = key + "=";
            if (lines.removeIf(line -> line.startsWith(linePrefix))) {
                atomicWrite(file.toPath(), lines);
            }
        } catch (IOException e) {
            throw new GeneralException("Unable to update " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    /** Scans all loaded component {@code config/} directories for {@code <resourceId>.properties}. */
    private static File findSourcePropertiesFile(String systemResourceId) {
        String fileName = systemResourceId + ".properties";
        for (ComponentConfig cc : ComponentConfig.getAllComponents()) {
            Path candidate = cc.rootLocation().resolve("config").resolve(fileName);
            if (Files.isRegularFile(candidate)) {
                return candidate.toFile();
            }
        }
        return null;
    }

    /** Updates (or appends) {@code key=value} in an arbitrary properties file, preserving all other lines. */
    private static synchronized void writePropertiesEntry(File file, String key, String value)
            throws GeneralException {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String newLine = key + "=" + value;
            String linePrefix = key + "=";
            int index = -1;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(linePrefix)) {
                    index = i;
                    break;
                }
            }
            if (index >= 0) {
                lines.set(index, newLine);
            } else {
                lines.add(newLine);
            }
            atomicWrite(file.toPath(), lines);
        } catch (IOException e) {
            throw new GeneralException(
                    "Unable to update " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Writes {@code lines} to {@code target} atomically: first to a sibling {@code .tmp} file,
     * then renamed over the target with {@link StandardCopyOption#ATOMIC_MOVE}. A JVM crash
     * during the write will leave the original file intact rather than producing a partial file.
     */
    private static void atomicWrite(Path target, List<String> lines) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, lines, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Updates (or appends) {@code propertyName=value} in passwords.properties, preserving all other lines. */
    private static void writePasswordsProperty(String propertyName, String value) throws GeneralException {
        writePropertiesEntry(getPasswordsFile(), propertyName, value);
    }

    private static File getPasswordsFile() throws GeneralException {
        try {
            URL url = FlexibleLocation.resolveLocation(PASSWORDS_FILE_LOCATION);
            if (url == null) {
                throw new GeneralException("Unable to locate passwords.properties");
            }
            return new File(url.toURI());
        } catch (MalformedURLException | URISyntaxException e) {
            throw new GeneralException("Unable to locate passwords.properties: " + e.getMessage(), e);
        }
    }

    private static String getMasterKey() throws GeneralException {
        String envVar = ConfigCryptoUtil.MASTER_KEY_ENV_VAR;
        String masterKey = System.getenv(envVar);
        if (UtilValidate.isEmpty(masterKey)) {
            throw new GeneralException("The " + envVar + " environment variable is not set on the server");
        }
        return masterKey;
    }
}
