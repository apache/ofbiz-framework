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
package org.apache.ofbiz.base.crypto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Standalone entry point for the {@code reEncryptAllSecrets} Gradle task.
 *
 * <p>Re-encrypts every {@code ENC(...)} blob in {@code passwords.properties} and in every
 * {@code SystemProperty} DB row using a new master key. All decryptions are validated before
 * any writes occur — if a single decryption fails the task aborts with no side effects.</p>
 *
 * <p>Write order: DB transaction first (fully rollback-able), then atomic file rename. If the
 * file rename fails after DB commit the new values are logged at ERROR level for manual recovery.</p>
 *
 * <p>Invoked by the Gradle task; do not call directly.</p>
 */
public final class ReEncryptSecretsMain {

    private static final String MODULE = ReEncryptSecretsMain.class.getName();
    private static final String PASSWORDS_FILE = "framework/base/config/passwords.properties";
    private static final String ENTITY_ENGINE_FILE = "framework/entity/config/entityengine.xml";
    private static final String ENC_PREFIX = "ENC(";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            Debug.logError("Usage: ReEncryptSecretsMain <oldMasterKey> <newMasterKey> [true|false (dryRun)]", MODULE);
            System.exit(1);
        }
        String oldKey = args[0];
        String newKey = args[1];
        boolean dryRun = args.length >= 3 && "true".equalsIgnoreCase(args[2]);

        Debug.logWarning("[reEncryptAllSecrets] Master keys passed as command-line arguments are visible in"
                + " shell history and 'ps' output. Prefer the OFBIZ_OLD_MASTER_KEY / OFBIZ_NEW_MASTER_KEY env vars.", MODULE);
        Debug.logInfo("[reEncryptAllSecrets] dry-run=" + dryRun, MODULE);

        // ── Phase 1: passwords.properties ────────────────────────────────────────────────────
        Path pwdPath = Paths.get(PASSWORDS_FILE);
        if (!Files.exists(pwdPath)) {
            Debug.logError("[reEncryptAllSecrets] passwords.properties not found at " + pwdPath.toAbsolutePath(), MODULE);
            System.exit(1);
        }
        List<String> originalLines = Files.readAllLines(pwdPath, StandardCharsets.UTF_8);
        // key → new ENC(...) value for lines that need updating
        Map<String, String> fileUpdates = new LinkedHashMap<>();

        Debug.logInfo("[reEncryptAllSecrets] Scanning passwords.properties ...", MODULE);
        for (String line : originalLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || !trimmed.contains("=")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            String propKey = trimmed.substring(0, eq).trim();
            String propVal = trimmed.substring(eq + 1).trim();
            if (!propVal.startsWith(ENC_PREFIX)) {
                continue;
            }
            try {
                String newEnc = ConfigCryptoUtil.reEncrypt(propVal, oldKey, newKey);
                fileUpdates.put(propKey, newEnc);
                Debug.logInfo("[reEncryptAllSecrets]   file: " + propKey + " -> re-encrypted OK", MODULE);
            } catch (GeneralException e) {
                Debug.logError("[reEncryptAllSecrets] Failed to re-encrypt file key '" + propKey
                        + "': " + e.getMessage() + " — aborting, no changes written.", MODULE);
                System.exit(1);
            }
        }
        Debug.logInfo("[reEncryptAllSecrets] passwords.properties: " + fileUpdates.size() + " value(s) to re-encrypt.", MODULE);

        // ── Phase 2: SystemProperty DB rows ──────────────────────────────────────────────────
        DatasourceInfo ds = parseDatasource();
        List<SystemPropertyRow> dbRows = new ArrayList<>();

        if (ds != null) {
            String jdbcPassword = resolveJdbcPassword(ds, originalLines, oldKey);
            if (jdbcPassword == null) {
                Debug.logWarning("[reEncryptAllSecrets] Could not resolve JDBC password; skipping DB re-encryption.", MODULE);
            } else {
                Debug.logInfo("[reEncryptAllSecrets] Connecting to datasource '" + ds.getName() + "' at " + ds.getJdbcUri() + " ...", MODULE);
                try (Connection conn = DriverManager.getConnection(ds.getJdbcUri(), ds.getJdbcUsername(), jdbcPassword)) {
                    dbRows = loadAndReEncryptDbRows(conn, oldKey, newKey);
                } catch (SQLException e) {
                    Debug.logError("[reEncryptAllSecrets] DB connection or query failed: " + e.getMessage()
                            + " — aborting, no changes written.", MODULE);
                    System.exit(1);
                }
            }
        } else {
            Debug.logWarning("[reEncryptAllSecrets] Could not parse entityengine.xml; skipping DB re-encryption.", MODULE);
        }
        Debug.logInfo("[reEncryptAllSecrets] SystemProperty: " + dbRows.size() + " row(s) to re-encrypt.", MODULE);

        // ── Dry-run: print plan and exit ──────────────────────────────────────────────────────
        if (dryRun) {
            Debug.logInfo("[reEncryptAllSecrets] DRY-RUN — no changes will be written.", MODULE);
            if (!fileUpdates.isEmpty()) {
                Debug.logInfo("[reEncryptAllSecrets] passwords.properties changes:", MODULE);
                fileUpdates.forEach((k, v) -> Debug.logInfo("    " + k + " = " + v, MODULE));
            }
            if (!dbRows.isEmpty()) {
                Debug.logInfo("[reEncryptAllSecrets] SystemProperty DB changes:", MODULE);
                for (SystemPropertyRow row : dbRows) {
                    Debug.logInfo("    [" + row.getResourceId() + "/" + row.getPropertyId() + "] = " + row.getNewValue(), MODULE);
                }
            }
            Debug.logInfo("[reEncryptAllSecrets] Dry-run complete. Re-run without -PdryRun=true to apply.", MODULE);
            return;
        }

        // ── Phase 3: Write DB (transactional) ────────────────────────────────────────────────
        if (!dbRows.isEmpty() && ds != null) {
            String jdbcPassword = resolveJdbcPassword(ds, originalLines, oldKey);
            Debug.logInfo("[reEncryptAllSecrets] Writing " + dbRows.size() + " SystemProperty row(s) in a single transaction ...", MODULE);
            try (Connection conn = DriverManager.getConnection(ds.getJdbcUri(), ds.getJdbcUsername(), jdbcPassword)) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE SYSTEM_PROPERTY SET SYSTEM_PROPERTY_VALUE = ? "
                        + "WHERE SYSTEM_RESOURCE_ID = ? AND SYSTEM_PROPERTY_ID = ?")) {
                    for (SystemPropertyRow row : dbRows) {
                        ps.setString(1, row.getNewValue());
                        ps.setString(2, row.getResourceId());
                        ps.setString(3, row.getPropertyId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
                Debug.logInfo("[reEncryptAllSecrets] DB transaction committed.", MODULE);
            } catch (SQLException e) {
                Debug.logError("[reEncryptAllSecrets] DB write failed (transaction rolled back): " + e.getMessage(), MODULE);
                System.exit(1);
            }
        }

        // ── Phase 4: Write passwords.properties (atomic rename) ───────────────────────────────
        if (!fileUpdates.isEmpty()) {
            Debug.logInfo("[reEncryptAllSecrets] Writing passwords.properties ...", MODULE);
            List<String> newLines = new ArrayList<>(originalLines.size());
            for (String line : originalLines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("#") && trimmed.contains("=")) {
                    int eq = trimmed.indexOf('=');
                    String propKey = trimmed.substring(0, eq).trim();
                    if (fileUpdates.containsKey(propKey)) {
                        newLines.add(propKey + "=" + fileUpdates.get(propKey));
                        continue;
                    }
                }
                newLines.add(line);
            }
            String newContent = String.join(System.lineSeparator(), newLines) + System.lineSeparator();
            Path temp = null;
            try {
                temp = Files.createTempFile(pwdPath.getParent(), "passwords-", ".tmp");
                Files.write(temp, newContent.getBytes(StandardCharsets.UTF_8));
                try {
                    Files.move(temp, pwdPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, pwdPath, StandardCopyOption.REPLACE_EXISTING);
                }
                Debug.logInfo("[reEncryptAllSecrets] passwords.properties updated.", MODULE);
            } catch (IOException e) {
                Debug.logError("[reEncryptAllSecrets] Failed to write passwords.properties: " + e.getMessage(), MODULE);
                if (!dbRows.isEmpty()) {
                    Debug.logError("[reEncryptAllSecrets] DB is already committed. Manual recovery — new file values:", MODULE);
                    fileUpdates.forEach((k, v) -> Debug.logError("  " + k + "=" + v, MODULE));
                }
                if (temp != null) {
                    try {
                        Files.deleteIfExists(temp);
                    } catch (IOException ignored) {
                        // best-effort cleanup — ignore
                    }
                }
                System.exit(1);
            }
        }

        Debug.logInfo("[reEncryptAllSecrets] Done. Update OFBIZ_MASTER_KEY to the new key before restarting OFBiz.", MODULE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────────────────

    private static List<SystemPropertyRow> loadAndReEncryptDbRows(Connection conn, String oldKey, String newKey)
            throws SQLException {
        List<SystemPropertyRow> rows = new ArrayList<>();
        String sql = "SELECT SYSTEM_RESOURCE_ID, SYSTEM_PROPERTY_ID, SYSTEM_PROPERTY_VALUE"
                + " FROM SYSTEM_PROPERTY WHERE SYSTEM_PROPERTY_VALUE LIKE 'ENC(%'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String resourceId = rs.getString("SYSTEM_RESOURCE_ID");
                String propertyId = rs.getString("SYSTEM_PROPERTY_ID");
                String currentValue = rs.getString("SYSTEM_PROPERTY_VALUE");
                try {
                    String newValue = ConfigCryptoUtil.reEncrypt(currentValue, oldKey, newKey);
                    rows.add(new SystemPropertyRow(resourceId, propertyId, newValue));
                    Debug.logInfo("[reEncryptAllSecrets]   db: [" + resourceId + "/" + propertyId + "] re-encrypted OK", MODULE);
                } catch (GeneralException e) {
                    throw new SQLException("Failed to re-encrypt SystemProperty [" + resourceId + "/" + propertyId
                            + "]: " + e.getMessage() + " — aborting, no changes written.", e);
                }
            }
        }
        return rows;
    }

    private static String resolveJdbcPassword(DatasourceInfo ds, List<String> pwdLines, String oldKey) {
        if (ds.getJdbcPassword() != null) {
            return ds.getJdbcPassword();
        }
        if (ds.getJdbcPasswordLookup() == null) {
            return null;
        }
        String lookupKey = "jdbc-password." + ds.getJdbcPasswordLookup();
        for (String line : pwdLines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(lookupKey + "=")) {
                String val = trimmed.substring(lookupKey.length() + 1).trim();
                if (val.startsWith(ENC_PREFIX)) {
                    try {
                        String base64 = val.substring(ENC_PREFIX.length(), val.length() - 1);
                        return ConfigCryptoUtil.decrypt(base64, oldKey);
                    } catch (GeneralException e) {
                        Debug.logWarning("[reEncryptAllSecrets] Could not decrypt JDBC password for '"
                                + lookupKey + "': " + e.getMessage(), MODULE);
                        return null;
                    }
                }
                return val;
            }
        }
        return null;
    }

    private static DatasourceInfo parseDatasource() {
        File entityEngineFile = new File(ENTITY_ENGINE_FILE);
        if (!entityEngineFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc;
            try (InputStream is = Files.newInputStream(entityEngineFile.toPath())) {
                doc = builder.parse(is);
            }
            doc.getDocumentElement().normalize();

            // Find the default delegator's datasource name for org.apache.ofbiz group
            String datasourceName = null;
            NodeList delegators = doc.getElementsByTagName("delegator");
            for (int i = 0; i < delegators.getLength(); i++) {
                Element del = (Element) delegators.item(i);
                if ("default".equals(del.getAttribute("name"))) {
                    NodeList groupMaps = del.getElementsByTagName("group-map");
                    for (int j = 0; j < groupMaps.getLength(); j++) {
                        Element gm = (Element) groupMaps.item(j);
                        if ("org.apache.ofbiz".equals(gm.getAttribute("group-name"))) {
                            datasourceName = gm.getAttribute("datasource-name");
                            break;
                        }
                    }
                    break;
                }
            }
            if (datasourceName == null) {
                return null;
            }

            // Find the datasource element
            NodeList datasources = doc.getElementsByTagName("datasource");
            for (int i = 0; i < datasources.getLength(); i++) {
                Element ds = (Element) datasources.item(i);
                if (datasourceName.equals(ds.getAttribute("name"))) {
                    NodeList inlineJdbcs = ds.getElementsByTagName("inline-jdbc");
                    if (inlineJdbcs.getLength() > 0) {
                        Element jdbc = (Element) inlineJdbcs.item(0);
                        String uri = jdbc.getAttribute("jdbc-uri");
                        String user = jdbc.getAttribute("jdbc-username");
                        String pwd = jdbc.getAttribute("jdbc-password");
                        String pwdLookup = jdbc.getAttribute("jdbc-password-lookup");
                        return new DatasourceInfo(datasourceName, uri, user,
                                pwd.isEmpty() ? null : pwd,
                                pwdLookup.isEmpty() ? null : pwdLookup);
                    }
                }
            }
        } catch (Exception e) {
            Debug.logWarning("[reEncryptAllSecrets] Could not parse entityengine.xml: " + e.getMessage(), MODULE);
        }
        return null;
    }

    private static final class DatasourceInfo {
        private final String name;
        private final String jdbcUri;
        private final String jdbcUsername;
        private final String jdbcPassword;
        private final String jdbcPasswordLookup;

        DatasourceInfo(String name, String jdbcUri, String jdbcUsername,
                String jdbcPassword, String jdbcPasswordLookup) {
            this.name = name;
            this.jdbcUri = jdbcUri;
            this.jdbcUsername = jdbcUsername;
            this.jdbcPassword = jdbcPassword;
            this.jdbcPasswordLookup = jdbcPasswordLookup;
        }

        String getName() {
            return name;
        }
        String getJdbcUri() {
            return jdbcUri;
        }
        String getJdbcUsername() {
            return jdbcUsername;
        }
        String getJdbcPassword() {
            return jdbcPassword;
        }
        String getJdbcPasswordLookup() {
            return jdbcPasswordLookup;
        }
    }

    private static final class SystemPropertyRow {
        private final String resourceId;
        private final String propertyId;
        private final String newValue;

        SystemPropertyRow(String resourceId, String propertyId, String newValue) {
            this.resourceId = resourceId;
            this.propertyId = propertyId;
            this.newValue = newValue;
        }

        String getResourceId() {
            return resourceId;
        }
        String getPropertyId() {
            return propertyId;
        }
        String getNewValue() {
            return newValue;
        }
    }

    private ReEncryptSecretsMain() {
    }
}
