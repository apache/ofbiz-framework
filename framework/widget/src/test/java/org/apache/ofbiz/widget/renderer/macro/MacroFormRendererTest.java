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
package org.apache.ofbiz.widget.renderer.macro;

import com.google.common.collect.ImmutableMap;
import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilCodec.SimpleEncoder;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.widget.model.ModelFormField;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

public class MacroFormRendererTest {

    @Mocked
    private HttpServletRequest request;

    @Mocked
    private HttpServletResponse response;

    @Mocked
    private HttpSession httpSession;

    @Mocked
    private Template template;

    @Mocked
    private Environment environment;

    @Mocked
    private VisualTheme visualTheme;

    @Mocked
    private RequestHandler requestHandler;

    @Mocked
    private SimpleEncoder simpleEncoder;

    @Mocked
    private ModelFormField.ContainerField containerField;

    @Mocked
    private ModelFormField modelFormField;

    @Mocked
    private Appendable appendable;

    @Mocked
    private StringReader stringReader;

    @Before
    public void setupMockups() {
        new FreeMarkerWorkerMockUp();
        new ThemeFactoryMockUp();
        new RequestHandlerMockUp();
        new UtilHttpMockUp();
        new UtilCodecMockUp();
    }

    @Test
    public void textRendererUsesContainerId(@Mocked ModelFormField.TextField textField) throws IOException, TemplateException {
        new Expectations() {{
            httpSession.getAttribute("delegatorName");
            result = "delegator";

            textField.getModelFormField();
            result = modelFormField;

            modelFormField.getTooltip(withNotNull());
            result = "";

            modelFormField.getCurrentContainerId(withNotNull());
            result = "CurrentTextId";

            new StringReader(withSubstring("id=\"CurrentTextId\""));
        }};

        final MacroFormRenderer macroFormRenderer = new MacroFormRenderer(null, request, response);
        macroFormRenderer.renderTextField(appendable, ImmutableMap.of("session", httpSession), textField);
    }

    @Test
    public void containerRendererUsesContainerId() throws IOException, TemplateException {
        new Expectations() {{
            modelFormField.getCurrentContainerId(withNotNull());
            result = "CurrentContainerId";

            new StringReader(withSubstring("id=\"CurrentContainerId\""));
        }};

        final MacroFormRenderer macroFormRenderer = new MacroFormRenderer(null, request, response);
        macroFormRenderer.renderContainerFindField(appendable, ImmutableMap.of(), containerField);
    }

    class FreeMarkerWorkerMockUp extends MockUp<FreeMarkerWorker> {
        @Mock
        public Template getTemplate(String templateLocation) {
            return template;
        }

        @Mock
        public Environment renderTemplate(Template template, Map<String, Object> context, Appendable outWriter) {
            return environment;
        }
    }

    class ThemeFactoryMockUp extends MockUp<ThemeFactory> {
        @Mock
        public VisualTheme resolveVisualTheme(HttpServletRequest request) {
            return visualTheme;
        }
    }

    class RequestHandlerMockUp extends MockUp<RequestHandler> {
        @Mock
        public RequestHandler from(HttpServletRequest request) {
            return requestHandler;
        }
    }

    class UtilHttpMockUp extends MockUp<UtilHttp> {
        @Mock
        public boolean isJavaScriptEnabled(HttpServletRequest request) {
            return true;
        }
    }

    class UtilCodecMockUp extends MockUp<UtilCodec> {
        @Mock
        public SimpleEncoder getEncoder(String type) {
            return simpleEncoder;
        }
    }
}

