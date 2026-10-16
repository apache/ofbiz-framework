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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.StringWriter;

import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import freemarker.core.Environment;
import freemarker.template.Template;

public final class FtlWriterTest {

    private final Template template = mock(Template.class);
    private final Environment environment = mock(Environment.class);

    private MockedStatic<FreeMarkerWorker> freeMarkerWorkerMock;

    @BeforeEach
    public void setupMockups() {
        freeMarkerWorkerMock = mockStatic(FreeMarkerWorker.class, CALLS_REAL_METHODS);
        freeMarkerWorkerMock.when(() -> FreeMarkerWorker.getTemplate(anyString())).thenReturn(template);
        freeMarkerWorkerMock.when(() -> FreeMarkerWorker.renderTemplate(any(Template.class), anyMap(), any())).thenReturn(environment);
    }

    @AfterEach
    public void tearDownMockups() {
        freeMarkerWorkerMock.close();
    }

    @Test
    public void renderableFtlStringIsWrittenAsItIsAndNeverCompiled() throws Exception {
        final FtlWriter ftlWriter = new FtlWriter("LIBRARY");
        final StringWriter writer = new StringWriter();
        final String html = "<a href=\"#\">${1+1} <#stop \"stopped\"> #{2}</a>";

        ftlWriter.processFtl(writer, null, new RenderableFtlString(html));

        assertThat(writer.toString(), equalTo(html));
        // a compiled string would have been included in the environment of the writer
        verify(environment, never()).include(any(Template.class));
    }
}
