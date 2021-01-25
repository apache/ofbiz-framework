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
package org.apache.ofbiz.widget;

import mockit.Mock;
import mockit.MockUp;
import org.apache.ofbiz.security.CsrfUtil;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;

public class WidgetWorkerTest {

    @Before
    public void setupMockups() {
        new RequestHandlerMockUp();
    }

    @Test
    public void buildsHyperlinkUriWithAlreadyUrlEncodedTarget() {
        final URI alreadyEncodedTargetUri = WidgetWorker.buildHyperlinkUri(
                "&#47;projectmgr&#47;control&#47;EditTaskContents&#63;workEffortId&#61;10003",
                "plain", new HashMap<>(), null, false, true, true, null, null);

        assertThat(alreadyEncodedTargetUri, hasProperty("path", equalTo("/projectmgr/control/EditTaskContents")));
        assertThat(alreadyEncodedTargetUri, hasProperty("query", equalTo("workEffortId=10003")));
    }

    @Test
    public void buildsHyperlinkUriWithSpaces() {
        final URI withEncodedSpaces = WidgetWorker.buildHyperlinkUri(
                "javascript:set_value('system', 'system', '')",
                "plain", new HashMap<>(), null, false, true, true, null, null);

        assertThat(withEncodedSpaces, hasProperty("scheme", equalTo("javascript")));
        assertThat(withEncodedSpaces, hasProperty("schemeSpecificPart", equalTo("set_value('system', 'system', '')")));
    }

    class RequestHandlerMockUp extends MockUp<CsrfUtil> {
        @Mock
        public String generateTokenForNonAjax(HttpServletRequest request, String pathOrRequestUri) {
            return null;
        }
    }
}
