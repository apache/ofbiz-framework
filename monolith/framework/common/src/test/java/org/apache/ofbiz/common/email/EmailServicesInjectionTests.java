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

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.testtools.JunitJupiterTest;
import org.apache.ofbiz.testtools.JupiterTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * sendMailFromScreen's bodyText and subject are ordinary caller-supplied strings -- for example, the
 * "Send Per Email" invoice form (accounting's InvoiceForms.xml) posts bodyText straight from a request
 * textarea. Before the fix, EmailServices ran both through FlexibleStringExpander.expandString(), which
 * compiles and executes any {@code ${groovy:...}} substring it finds using a GroovyClassLoader carrying
 * no SecureASTCustomizer -- arbitrary code execution reachable from a form field held by any
 * authenticated user the webapp lets in. This guards the fix that stops expanding that text at all.
 *
 * <p>The marker below is set via a fully-qualified call to a static method on this class rather than
 * {@code System.setProperty(...)} deliberately: security.properties' deniedScriptletsTokens denylist
 * (a separate, pre-existing control -- see security.properties' "System\s*\." token) would otherwise
 * block the marker script before it ever reached the code path this test exists to guard, making the
 * test pass for the wrong reason regardless of the fix under test. Confirmed by running this test
 * against the pre-fix code: with {@code System.setProperty(...)} it passed vacuously (blocked by the
 * denylist); with this marker it fails as expected, proving it actually exercises the vulnerability.
 */
@JunitJupiterTest
public final class EmailServicesInjectionTests implements JupiterTestHelper {

    private static volatile boolean marker;

    private static final String INJECTION =
            "before-${groovy: org.apache.ofbiz.common.email.EmailServicesInjectionTests.setMarker() }-after";

    /** Called from the injected scriptlet, if it is ever compiled and executed. */
    public static void setMarker() {
        marker = true;
    }

    @AfterEach
    public void clearMarker() {
        marker = false;
    }

    @Test
    public void sendMailFromScreenDoesNotEvaluateGroovyInBodyText() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("userLogin", getUserLogin());
        context.put("bodyText", INJECTION);
        context.put("sendTo", "nobody@example.org");
        context.put("sendFrom", "nobody@example.org");
        context.put("contentType", "text/plain");

        // sendMailFromScreen goes on to fail the actual send (no SMTP server in the test environment);
        // that happens after the injection point and is not what this test is about, so any result --
        // success or error -- is fine here. Only the marker matters.
        getDispatcher().runSync("sendMailFromScreen", context);

        assertFalse(marker, "bodyText must not be compiled and executed as Groovy by sendMailFromScreen");
    }

    @Test
    public void sendMailFromScreenDoesNotEvaluateGroovyInSubject() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("userLogin", getUserLogin());
        context.put("subject", INJECTION);
        context.put("sendTo", "nobody@example.org");
        context.put("sendFrom", "nobody@example.org");
        context.put("contentType", "text/plain");

        getDispatcher().runSync("sendMailFromScreen", context);

        assertFalse(marker, "subject must not be compiled and executed as Groovy by sendMailFromScreen");
    }
}
