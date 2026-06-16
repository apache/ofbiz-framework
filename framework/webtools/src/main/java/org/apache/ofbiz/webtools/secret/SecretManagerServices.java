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
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.crypto.ConfigCryptoUtil;
import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
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

    private SecretManagerServices() { }

    /** Service implementation for the webtools "Encrypt Value" screen. */
    public static Map<String, Object> createEncryptedSecret(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        String secretTarget = (String) context.get("secretTarget");
        String systemResourceId = (String) context.get("systemResourceId");
        String systemPropertyId = (String) context.get("systemPropertyId");
        String lookupKey = (String) context.get("lookupKey");
        String secretValue = (String) context.get("secretValue");

        try {
            storeEncryptedSecret(delegator, secretTarget, systemResourceId, systemPropertyId, lookupKey, secretValue);
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
     */
    public static void storeEncryptedSecret(Delegator delegator, String secretTarget, String systemResourceId,
            String systemPropertyId, String lookupKey, String secretValue) throws GeneralException {
        if (UtilValidate.isEmpty(secretValue)) {
            throw new GeneralException("secretValue is required");
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
                // Case B: property-file combined write — use plain lookupKey, no jdbc-password. prefix
                writePasswordsProperty(lookupKey, encryptedValue);
                updatePropertiesFileAndRefreshCache(systemResourceId, systemPropertyId, lookupKey);
            } else {
                // Case A: entityengine.xml jdbc-password-lookup — keep the jdbc-password. prefix
                writePasswordsProperty(JDBC_PASSWORD_PREFIX + lookupKey, encryptedValue);
            }
        } else if (TARGET_SYSTEM_PROPERTY.equals(secretTarget)) {
            if (UtilValidate.isEmpty(systemResourceId) || UtilValidate.isEmpty(systemPropertyId)) {
                throw new GeneralException("systemResourceId and systemPropertyId are required for SystemProperty");
            }
            storeSystemPropertySecret(delegator, systemResourceId, systemPropertyId, lookupKey, encryptedValue);
        } else {
            throw new GeneralException("Unknown secretTarget '" + secretTarget + "'");
        }
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
            systemProperty.set("systemPropertyLookup", "SECRET(" + lookupKey + ")");
        }
        delegator.createOrStore(systemProperty);
    }

    /**
     * Searches all loaded OFBiz components for {@code config/<systemResourceId>.properties},
     * sets {@code <systemPropertyId>=SECRET(<lookupKey>)} in the source file, then clears the
     * OFBiz property caches so the change is picked up immediately without a server restart.
     *
     * <p>If the property previously referenced a different lookup key via {@code SECRET(old-key)},
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
        writePropertiesEntry(propsFile, systemPropertyId, "SECRET(" + lookupKey + ")");
        UtilCache.clearCachesThatStartWith("properties.UtilProperties");
        Debug.logInfo("Set " + systemPropertyId + "=SECRET(" + lookupKey + ") in "
                + propsFile.getPath() + " and refreshed property caches", MODULE);
    }

    /**
     * Reads {@code file} and returns the key inside {@code SECRET(<key>)} for the given
     * {@code propertyId}, or {@code null} if the property is absent or not a SECRET reference.
     */
    private static String readSecretLookupKey(File file, String propertyId) throws GeneralException {
        try {
            String linePrefix = propertyId + "=";
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith(linePrefix)) {
                    String value = line.substring(linePrefix.length()).trim();
                    if (value.startsWith("SECRET(") && value.endsWith(")")) {
                        return value.substring("SECRET(".length(), value.length() - 1);
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
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
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
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GeneralException(
                    "Unable to update " + file.getName() + ": " + e.getMessage(), e);
        }
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
        String masterKey = System.getenv("OFBIZ_MASTER_KEY");
        if (UtilValidate.isEmpty(masterKey)) {
            throw new GeneralException("The OFBIZ_MASTER_KEY environment variable is not set on the server");
        }
        return masterKey;
    }
}
