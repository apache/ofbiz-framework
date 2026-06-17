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
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.service.DispatchContext;
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

    private SecretManagerServices() { }

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
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        try {
            String value = SecretProviderFactory.getInstance().getSecret(testKey);
            String outcome = UtilValidate.isNotEmpty(value) ? "found" : "empty-value";
            Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=testSecretProviderConnection"
                    + " testKey=" + testKey + " outcome=" + outcome, MODULE);
            if (UtilValidate.isNotEmpty(value)) {
                return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' found successfully");
            }
            return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' returned an empty value");
        } catch (GeneralException e) {
            String msg = e.getMessage();
            // "not found" responses confirm connectivity; only network/auth errors are real failures
            if (msg != null && (msg.contains("not found") || msg.contains("NotFound") || msg.contains("does not exist"))) {
                Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=testSecretProviderConnection"
                        + " testKey=" + testKey + " outcome=key-not-found", MODULE);
                return ServiceUtil.returnSuccess("Connected — key '" + testKey + "' was not found in the provider (vault is reachable)");
            }
            // Log the full message server-side but never expose SDK internals to the browser:
            // vault error messages can contain endpoint URLs, IAM ARNs, or credential fragments.
            Debug.logWarning("[SECRET_AUDIT] user=" + userLoginId + " action=testSecretProviderConnection"
                    + " testKey=" + testKey + " outcome=connection-failed detail=" + msg, MODULE);
            return ServiceUtil.returnError("Connection failed (" + e.getClass().getSimpleName()
                    + ") — check server logs for details");
        }
    }

    /**
     * Resets in-memory secret usage counters (hit/miss counts) to zero. Useful after a secret
     * rotation so operators can observe clean post-rotation metrics without a JVM restart.
     */
    public static Map<String, Object> resetSecretUsageStats(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        SecretValueResolver.resetUsageStats();
        Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=resetSecretUsageStats", MODULE);
        return ServiceUtil.returnSuccess("Secret usage statistics reset successfully");
    }

    /**
     * Returns in-memory usage statistics from {@link SecretValueResolver}: aggregate hit/miss
     * totals and a per-key breakdown. The data covers the current JVM lifetime only and resets
     * on restart; it is meant for operator visibility, not durable monitoring.
     */
    public static Map<String, Object> getSecretUsageStats(DispatchContext dctx, Map<String, ? extends Object> context) {
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
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        SecretValueResolver.invalidateAll();
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        SecretProviderFactory.invalidateCache();
        Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=flushSecretCache", MODULE);
        return ServiceUtil.returnSuccess("Secret value cache flushed successfully");
    }

    /**
     * Re-runs {@link SecretProviderFactory} discovery, replacing the active provider instance.
     * Useful after deploying a new vault plugin jar or editing that plugin's own connection
     * settings (e.g. a rotated AWS access key, a new HashiCorp AppRole secret_id) in its
     * {@code config/*.properties} file, without requiring a full OFBiz restart.
     */
    public static Map<String, Object> reloadSecretProvider(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        SecretProviderFactory.reload();
        String providerName = SecretProviderFactory.getProviderName();
        Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=reloadSecretProvider"
                + " provider=" + providerName, MODULE);
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
        String userLoginId = (userLogin != null) ? userLogin.getString("userLoginId") : "unknown";
        String secretTarget = (String) context.get("secretTarget");
        String systemResourceId = (String) context.get("systemResourceId");
        String systemPropertyId = (String) context.get("systemPropertyId");
        String lookupKey = (String) context.get("lookupKey");

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
            Debug.logWarning("[SECRET_AUDIT] user=" + userLoginId + " action=syncSecretFromProvider"
                    + " providerKey=" + providerKey + " outcome=fetch-failed detail=" + e.getClass().getSimpleName(),
                    MODULE);
            return ServiceUtil.returnError("Failed to fetch the current value from the active secret provider for key '"
                    + providerKey + "' (" + e.getClass().getSimpleName() + ") — check server logs for details");
        }
        if (UtilValidate.isEmpty(freshValue)) {
            return ServiceUtil.returnError("Secret provider returned an empty value for key '" + providerKey + "'");
        }

        try {
            storeEncryptedSecret(delegator, userLogin, secretTarget, systemResourceId, systemPropertyId, lookupKey, freshValue);
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId + " action=syncSecretFromProvider"
                + " providerKey=" + providerKey + " outcome=synced", MODULE);
        return ServiceUtil.returnSuccess("Local encrypted snapshot for '" + lookupKey
                + "' synced from the active secret provider");
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
     * <p>A structured audit log entry is written on every successful call so that the
     * who/what/when of each secret operation is preserved for compliance review.</p>
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
        Debug.logInfo("[SECRET_AUDIT] user=" + userLoginId
                + " action=storeEncryptedSecret"
                + " target=" + secretTarget
                + " systemResourceId=" + systemResourceId
                + " systemPropertyId=" + systemPropertyId
                + " lookupKey=" + lookupKey, MODULE);
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
