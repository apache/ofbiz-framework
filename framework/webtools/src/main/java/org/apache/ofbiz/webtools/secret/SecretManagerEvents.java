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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItem;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.security.SecuredUpload;
import org.apache.ofbiz.security.Security;

/**
 * Handles bulk creation of encrypted secrets from a CSV upload on the webtools "Encrypt Value"
 * screen. Each row is processed by {@link SecretManagerServices#storeEncryptedSecret}, the same
 * logic used by the single-entry form.
 *
 * <p>Expected CSV header: {@code target,systemResourceId,systemPropertyId,lookupKey,secretValue}.
 * {@code target} is either {@code SYSTEM_PROPERTY} or {@code PASSWORDS_FILE}.</p>
 */
public final class SecretManagerEvents {

    private static final String MODULE = SecretManagerEvents.class.getName();
    private static final String UPLOAD_FIELD_NAME = "uploadedFile";

    private static final int MAX_FILE_SIZE_BYTES = 512 * 1024; // 512 KB
    private static final int MAX_ROWS = 500;
    private static final List<String> REQUIRED_HEADERS =
            List.of("target", "systemResourceId", "systemPropertyId", "lookupKey", "secretValue");
    private static final Set<String> VALID_TARGETS = Set.of(
            SecretManagerServices.TARGET_SYSTEM_PROPERTY, SecretManagerServices.TARGET_PASSWORDS_FILE);

    private SecretManagerEvents() { }

    public static String uploadEncryptedSecrets(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        if (security == null
                || (!security.hasPermission("SECRET_MAINT", userLogin)
                        && !security.hasPermission("ENTITY_MAINT", userLogin))) {
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                    .clientIpAddress(request.getRemoteAddr())
                    .action("STORE")
                    .outcome("DENIED")
                    .build());
            request.setAttribute("_ERROR_MESSAGE_", "You do not have permission to perform this operation");
            return "error";
        }

        byte[] csvBytes = getUploadedFileBytes(request);
        if (csvBytes == null || csvBytes.length == 0) {
            request.setAttribute("_ERROR_MESSAGE_", "No CSV file was uploaded");
            return "error";
        }

        if (csvBytes.length > MAX_FILE_SIZE_BYTES) {
            request.setAttribute("_ERROR_MESSAGE_",
                    "CSV file exceeds the maximum allowed size of " + (MAX_FILE_SIZE_BYTES / 1024) + " KB");
            return "error";
        }

        // Pre-scan: validate every row before writing anything — reject the entire file on any issue
        List<String> validationErrors;
        try {
            validationErrors = validateCsvContent(csvBytes);
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", "CSV file is malformed: " + e.getMessage());
            return "error";
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Error reading CSV file: " + e.getMessage());
            return "error";
        }
        if (!validationErrors.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", validationErrors);
            return "error";
        }

        int successCount = 0;
        List<String> errors = new ArrayList<>();
        CSVFormat format = buildCsvFormat();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8);
                CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                long rowNum = record.getRecordNumber() + 1;
                String rowTarget = getColumn(record, "target");
                String rowResourceId = getColumn(record, "systemResourceId");
                String rowPropertyId = getColumn(record, "systemPropertyId");
                String rowLookupKey = getColumn(record, "lookupKey");
                try {
                    SecretManagerServices.storeEncryptedSecret(delegator, userLogin,
                            rowTarget, rowResourceId, rowPropertyId, rowLookupKey,
                            getColumn(record, "secretValue"));
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    // PCI-DSS 10.2: failed stores are individually attributable events too.
                    SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                            .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                            .clientIpAddress(request.getRemoteAddr())
                            .action("STORE")
                            .secretKeyRef(rowLookupKey)
                            .secretTarget(rowTarget)
                            .systemResourceId(rowResourceId)
                            .systemPropertyId(rowPropertyId)
                            .outcome("FAILURE")
                            .errorCategory("PROVIDER_ERROR")
                            .build());
                }
            }
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Error parsing CSV file: " + e.getMessage());
            return "error";
        }

        request.setAttribute("_EVENT_MESSAGE_", successCount + " secret(s) encrypted and stored successfully");
        if (!errors.isEmpty()) {
            request.setAttribute("_ERROR_MESSAGE_LIST_", errors);
            return "error";
        }
        return "success";
    }

    private static CSVFormat buildCsvFormat() {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .get();
    }

    /**
     * Validates every row of the CSV before any writes occur. Returns a list of human-readable
     * error messages (one per problem found). An empty list means the file is safe to process.
     *
     * <p>Checks performed:</p>
     * <ul>
     *   <li>Whole-file content scan via {@link SecuredUpload#isValidTextContent} — rejects null
     *       bytes and C0/C1 control characters at the Unicode code-point level before any parsing.</li>
     *   <li>All required column headers are present.</li>
     *   <li>Row count does not exceed {@link #MAX_ROWS}.</li>
     *   <li>{@code target} is one of the two known constants.</li>
     *   <li>{@code systemResourceId}, {@code systemPropertyId}, {@code lookupKey} contain only
     *       safe identifier characters (letters, digits, dots, hyphens, underscores) and no
     *       {@code ..} path-traversal sequences.</li>
     * </ul>
     */
    static List<String> validateCsvContent(byte[] csvBytes) throws IOException {
        List<String> errors = new ArrayList<>();

        // Whole-file content scan using OFBiz's existing SecuredUpload allow-list validator.
        // Rejects null bytes and C0/C1 control characters at the Unicode code-point level —
        // more robust than a simple char scan because it cannot be bypassed by encoding tricks.
        String csvText = new String(csvBytes, StandardCharsets.UTF_8);
        if (!SecuredUpload.isValidTextContent(csvText)) {
            errors.add("CSV file contains illegal characters (null bytes or control characters) and cannot be processed");
            return errors;
        }

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8);
                CSVParser parser = buildCsvFormat().parse(reader)) {
            // Required headers
            Map<String, Integer> headers = parser.getHeaderMap();
            for (String col : REQUIRED_HEADERS) {
                if (!headers.containsKey(col)) {
                    errors.add("Missing required column header: '" + col + "'");
                }
            }
            if (!errors.isEmpty()) {
                return errors; // can't validate rows without the correct headers
            }
            int rowCount = 0;
            for (CSVRecord record : parser) {
                if (++rowCount > MAX_ROWS) {
                    errors.add("File exceeds the maximum of " + MAX_ROWS + " data rows");
                    break;
                }
                long rowNum = record.getRecordNumber() + 1;
                String target = getColumn(record, "target");
                String resourceId = getColumn(record, "systemResourceId");
                String propertyId = getColumn(record, "systemPropertyId");
                String lookupKey = getColumn(record, "lookupKey");
                String secretValue = getColumn(record, "secretValue");

                // target must be a known constant
                if (target != null && !VALID_TARGETS.contains(target)) {
                    errors.add("Row " + rowNum + ": unknown target '" + target
                            + "' — expected PASSWORDS_FILE or SYSTEM_PROPERTY");
                }
                // identifier fields: safe chars only + no path traversal
                validateIdentifier(errors, rowNum, "systemResourceId", resourceId);
                validateIdentifier(errors, rowNum, "systemPropertyId", propertyId);
                validateIdentifier(errors, rowNum, "lookupKey", lookupKey);
                // secretValue must be the plain secret, not an already-encrypted value
                if (secretValue != null && secretValue.trim().startsWith("ENC(")) {
                    errors.add("Row " + rowNum + ": 'secretValue' must be the plain secret"
                            + " — do not enter an ENC(...) encrypted value");
                }
            }
        }
        return errors;
    }

    /** Validates that {@code value} contains only safe identifier characters, no {@code ..}, and is within length. */
    private static void validateIdentifier(List<String> errors, long rowNum, String field, String value) {
        if (value == null) {
            return;
        }
        if (value.length() > SecretManagerServices.MAX_KEY_LENGTH) {
            errors.add("Row " + rowNum + ": '" + field + "' must not exceed "
                    + SecretManagerServices.MAX_KEY_LENGTH + " characters");
            return;
        }
        if (value.contains("..")) {
            errors.add("Row " + rowNum + ": '" + field + "' must not contain '..' (path traversal)");
            return;
        }
        if (!SecretManagerServices.SAFE_IDENTIFIER.matcher(value).matches()) {
            errors.add("Row " + rowNum + ": '" + field
                    + "' contains invalid characters — only letters, digits, dots, hyphens, and underscores are allowed");
        }
    }

    private static String getColumn(CSVRecord record, String name) {
        if (!record.isMapped(name)) {
            return null;
        }
        String value = record.get(name);
        return UtilValidate.isEmpty(value) ? null : value;
    }

    /**
     * Streams a CSV export of SecretAuditLog rows for the requested date range.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>Date range (dateFrom, dateTo) is required.</li>
     *   <li>Window may not exceed 90 days (to prevent runaway memory / bandwidth usage).</li>
     *   <li>Output is capped at {@value #MAX_EXPORT_ROWS} rows; a comment header is prepended if the
     *       cap is hit, instructing the operator to narrow the range.</li>
     * </ul>
     * Optional filters (filterUserLoginId, filterAction, filterOutcome, filterSecretKeyRef) are
     * carried from the viewer page and applied to the export query.
     */
    private static final int MAX_EXPORT_ROWS = 50000;
    private static final int MAX_EXPORT_WINDOW_DAYS = 90;

    public static String exportSecretAuditLogs(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        if (!security.hasPermission("SECRET_AUDIT_VIEW", userLogin)
                && !security.hasPermission("ENTITY_MAINT", userLogin)) {
            SecretAuditLogger.log(delegator, SecretAuditEvent.builder()
                    .userLoginId(userLogin != null ? userLogin.getString("userLoginId") : null)
                    .clientIpAddress(request.getRemoteAddr())
                    .action("EXPORT_AUDIT")
                    .outcome("DENIED")
                    .build());
            request.setAttribute("_ERROR_MESSAGE_", "You do not have permission to export audit logs.");
            return "error";
        }

        String dateFrom = UtilValidate.isEmpty(request.getParameter("filterDateFrom")) ? null
                : request.getParameter("filterDateFrom").trim();
        String dateTo = UtilValidate.isEmpty(request.getParameter("filterDateTo")) ? null
                : request.getParameter("filterDateTo").trim();

        if (dateFrom == null || dateTo == null) {
            request.setAttribute("_ERROR_MESSAGE_", "Date range (From and To) is required for export.");
            return "error";
        }

        Timestamp tsFrom;
        Timestamp tsTo;
        try {
            tsFrom = Timestamp.valueOf(dateFrom + " 00:00:00");
            tsTo = Timestamp.valueOf(dateTo + " 23:59:59");
        } catch (IllegalArgumentException e) {
            request.setAttribute("_ERROR_MESSAGE_", "Invalid date format — use YYYY-MM-DD.");
            return "error";
        }
        if (tsTo.before(tsFrom)) {
            request.setAttribute("_ERROR_MESSAGE_", "'Date To' must be on or after 'Date From'.");
            return "error";
        }
        long windowDays = Duration.between(tsFrom.toInstant(), tsTo.toInstant()).toDays();
        if (windowDays > MAX_EXPORT_WINDOW_DAYS) {
            request.setAttribute("_ERROR_MESSAGE_", "Export window may not exceed " + MAX_EXPORT_WINDOW_DAYS
                    + " days. Requested: " + windowDays + " days.");
            return "error";
        }

        List<EntityCondition> conds = new ArrayList<>();
        conds.add(EntityCondition.makeCondition("auditTimestamp", EntityOperator.GREATER_THAN_EQUAL_TO, tsFrom));
        conds.add(EntityCondition.makeCondition("auditTimestamp", EntityOperator.LESS_THAN_EQUAL_TO, tsTo));
        String filterUser = request.getParameter("filterUserLoginId");
        String filterAction = request.getParameter("filterAction");
        String filterOutcome = request.getParameter("filterOutcome");
        String filterKey = request.getParameter("filterSecretKeyRef");
        String filterProviderType = request.getParameter("filterProviderType");
        String filterDeployMode = request.getParameter("filterDeploymentMode");
        if (UtilValidate.isNotEmpty(filterUser)) {
            conds.add(EntityCondition.makeCondition("userLoginId", EntityOperator.EQUALS, filterUser));
        }
        if (UtilValidate.isNotEmpty(filterAction)) {
            conds.add(EntityCondition.makeCondition("action", EntityOperator.EQUALS, filterAction));
        }
        if (UtilValidate.isNotEmpty(filterOutcome)) {
            conds.add(EntityCondition.makeCondition("outcome", EntityOperator.EQUALS, filterOutcome));
        }
        if (UtilValidate.isNotEmpty(filterKey)) {
            conds.add(EntityCondition.makeCondition("secretKeyRef", EntityOperator.EQUALS, filterKey));
        }
        if (UtilValidate.isNotEmpty(filterProviderType)) {
            conds.add(EntityCondition.makeCondition("providerType", EntityOperator.EQUALS, filterProviderType));
        }
        if (UtilValidate.isNotEmpty(filterDeployMode)) {
            conds.add(EntityCondition.makeCondition("deploymentMode", EntityOperator.EQUALS, filterDeployMode));
        }
        EntityCondition where = EntityCondition.makeCondition(conds, EntityOperator.AND);

        List<GenericValue> rows;
        try {
            // Fetch one extra row to detect whether the cap was hit without a COUNT query.
            rows = EntityQuery.use(delegator).from("SecretAuditLog")
                    .where(where).orderBy("auditTimestamp")
                    .maxRows(MAX_EXPORT_ROWS + 1).queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, "exportSecretAuditLogs: query failed", MODULE);
            request.setAttribute("_ERROR_MESSAGE_", "Export query failed.");
            return "error";
        }
        boolean capped = rows.size() > MAX_EXPORT_ROWS;
        if (capped) rows = rows.subList(0, MAX_EXPORT_ROWS);

        String filename = "secret-audit-" + dateFrom + "-to-" + dateTo + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        try {
            PrintWriter writer = response.getWriter();
            if (capped) {
                writer.println("# NOTICE: export capped at " + MAX_EXPORT_ROWS
                        + " rows — narrow the date range to retrieve remaining records.");
            }
            writer.println("secretAuditLogId,auditTimestamp,userLoginId,clientIpAddress,"
                    + "action,outcome,errorCategory,secretKeyRef,secretTarget,accessMode,"
                    + "providerType,deploymentMode,systemResourceId,systemPropertyId");
            for (GenericValue row : rows) {
                writer.println(
                        csvEscape(row.getString("secretAuditLogId")) + ","
                        + csvEscape(String.valueOf(row.get("auditTimestamp"))) + ","
                        + csvEscape(row.getString("userLoginId")) + ","
                        + csvEscape(row.getString("clientIpAddress")) + ","
                        + csvEscape(row.getString("action")) + ","
                        + csvEscape(row.getString("outcome")) + ","
                        + csvEscape(row.getString("errorCategory")) + ","
                        + csvEscape(row.getString("secretKeyRef")) + ","
                        + csvEscape(row.getString("secretTarget")) + ","
                        + csvEscape(row.getString("accessMode")) + ","
                        + csvEscape(row.getString("providerType")) + ","
                        + csvEscape(row.getString("deploymentMode")) + ","
                        + csvEscape(row.getString("systemResourceId")) + ","
                        + csvEscape(row.getString("systemPropertyId")));
            }
            writer.flush();
        } catch (IOException e) {
            Debug.logError(e, "exportSecretAuditLogs: write failed", MODULE);
            return "error";
        }
        return "success";
    }

    private static String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    /**
     * Returns the bytes of the uploaded {@code uploadedFile} part. The multipart body has already
     * been parsed by {@code ControlFilter} (via {@code UtilHttp.getParameterMap}), which stashes the
     * parsed {@link FileItem}s in the {@code fileItems} request attribute - the underlying request
     * input stream can not be re-parsed here.
     */
    private static byte[] getUploadedFileBytes(HttpServletRequest request) {
        List<FileItem<DiskFileItem>> items = UtilGenerics.cast(request.getAttribute("fileItems"));
        if (items == null) {
            return null;
        }
        for (FileItem<DiskFileItem> item : items) {
            if (!item.isFormField() && UPLOAD_FIELD_NAME.equals(item.getFieldName())) {
                return item.get();
            }
        }
        return null;
    }
}
