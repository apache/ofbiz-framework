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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import org.apache.ofbiz.entity.GenericValue;
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
    /** Allows only letters, digits, dots, hyphens, underscores — prevents path traversal and property-file injection. */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[\\w.\\-]+$");

    private SecretManagerEvents() { }

    public static String uploadEncryptedSecrets(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");

        if (security == null || !security.hasPermission("ENTITY_MAINT", userLogin)) {
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
                try {
                    SecretManagerServices.storeEncryptedSecret(delegator,
                            getColumn(record, "target"),
                            getColumn(record, "systemResourceId"),
                            getColumn(record, "systemPropertyId"),
                            getColumn(record, "lookupKey"),
                            getColumn(record, "secretValue"));
                    successCount++;
                } catch (Exception e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
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
    private static List<String> validateCsvContent(byte[] csvBytes) throws IOException {
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

                // target must be a known constant
                if (target != null && !VALID_TARGETS.contains(target)) {
                    errors.add("Row " + rowNum + ": unknown target '" + target
                            + "' — expected PASSWORDS_FILE or SYSTEM_PROPERTY");
                }
                // identifier fields: safe chars only + no path traversal
                validateIdentifier(errors, rowNum, "systemResourceId", resourceId);
                validateIdentifier(errors, rowNum, "systemPropertyId", propertyId);
                validateIdentifier(errors, rowNum, "lookupKey", lookupKey);
            }
        }
        return errors;
    }

    /** Validates that {@code value} contains only safe identifier characters and no {@code ..}. */
    private static void validateIdentifier(List<String> errors, long rowNum, String field, String value) {
        if (value == null) {
            return;
        }
        if (value.contains("..")) {
            errors.add("Row " + rowNum + ": '" + field + "' must not contain '..' (path traversal)");
            return;
        }
        if (!SAFE_IDENTIFIER.matcher(value).matches()) {
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
