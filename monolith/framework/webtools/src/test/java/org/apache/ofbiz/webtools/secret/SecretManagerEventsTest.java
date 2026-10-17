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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for the CSV validation logic in {@link SecretManagerEvents}.
 *
 * <p>{@link SecretManagerEvents#validateCsvContent} is package-private so this test class,
 * being in the same package, can call it directly without HTTP infrastructure.</p>
 */
public class SecretManagerEventsTest {

    private static final String HEADER = "target,systemResourceId,systemPropertyId,lookupKey,secretValue\n";

    private static List<String> validate(String csv) throws IOException {
        return SecretManagerEvents.validateCsvContent(csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void happyPathReturnsNoErrors() throws IOException {
        String csv = HEADER + "SYSTEM_PROPERTY,my-resource,my.property,my-key,mypassword\n";
        assertTrue(validate(csv).isEmpty());
    }

    @Test
    public void passwordsFileTargetIsAccepted() throws IOException {
        String csv = HEADER + "PASSWORDS_FILE,,,my-jdbc-key,supersecret\n";
        assertTrue(validate(csv).isEmpty());
    }

    @Test
    public void unknownTargetIsRejected() throws IOException {
        String csv = HEADER + "BAD_TARGET,res,prop,key,password\n";
        List<String> errors = validate(csv);
        assertFalse("Should reject unknown target", errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("unknown target")));
    }

    @Test
    public void missingRequiredHeaderIsRejected() throws IOException {
        String csvNoLookupKey = "target,systemResourceId,systemPropertyId,secretValue\n"
                + "SYSTEM_PROPERTY,res,prop,password\n";
        List<String> errors = validate(csvNoLookupKey);
        assertFalse("Should reject CSV missing lookupKey header", errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lookupKey")));
    }

    @Test
    public void encPrefixInSecretValueIsRejected() throws IOException {
        String csv = HEADER + "SYSTEM_PROPERTY,resource,property,mykey,ENC(abc123==)\n";
        List<String> errors = validate(csv);
        assertFalse("Should reject ENC() value in secretValue", errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("ENC(")));
    }

    @Test
    public void pathTraversalInSystemResourceIdIsRejected() throws IOException {
        String csv = HEADER + "SYSTEM_PROPERTY,../etc/passwd,property,key,password\n";
        List<String> errors = validate(csv);
        assertFalse("Should reject path traversal in systemResourceId", errors.isEmpty());
    }

    @Test
    public void invalidCharsInLookupKeyAreRejected() throws IOException {
        String csv = HEADER + "SYSTEM_PROPERTY,resource,property,key with spaces,password\n";
        List<String> errors = validate(csv);
        assertFalse("Should reject lookupKey with spaces", errors.isEmpty());
    }

    @Test
    public void rowCountExceededIsRejected() throws IOException {
        StringBuilder csv = new StringBuilder(HEADER);
        for (int i = 0; i <= 500; i++) {
            csv.append("SYSTEM_PROPERTY,res,prop,key").append(i).append(",password\n");
        }
        List<String> errors = validate(csv.toString());
        assertFalse("Should reject CSV with more than 500 rows", errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("maximum")));
    }

    @Test
    public void lookupKeyWithDotsAndHyphensIsAccepted() throws IOException {
        String csv = HEADER + "PASSWORDS_FILE,,,jdbc-password.mysql-ofbiz,dbpassword\n";
        assertTrue("Dots and hyphens in lookupKey should be valid", validate(csv).isEmpty());
    }

    @Test
    public void identifierExceedingMaxLengthIsRejected() throws IOException {
        String longKey = "a".repeat(257);
        String csv = HEADER + "SYSTEM_PROPERTY,resource,property," + longKey + ",password\n";
        List<String> errors = validate(csv);
        assertFalse("Should reject identifier exceeding 256 chars", errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("lookupKey")));
    }
}
