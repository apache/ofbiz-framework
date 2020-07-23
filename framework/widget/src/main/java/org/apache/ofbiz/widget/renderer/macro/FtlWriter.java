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

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.widget.renderer.VisualTheme;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.rmi.server.UID;
import java.util.Map;
import java.util.WeakHashMap;

public final class FtlWriter {
    private static final String MODULE = FtlWriter.class.getName();

    private final WeakHashMap<Appendable, Environment> environments = new WeakHashMap<>();
    private final Template macroLibrary;
    private final VisualTheme visualTheme;

    public FtlWriter(final String macroLibraryPath, final VisualTheme visualTheme) throws IOException {
        this.macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        this.visualTheme = visualTheme;
    }

    public void executeMacro(Appendable writer, String macro) {
        try {
            Environment environment = getEnvironment(writer);
            environment.setVariable("visualTheme", FreeMarkerWorker.autoWrap(visualTheme, environment));
            environment.setVariable("modelTheme", FreeMarkerWorker.autoWrap(visualTheme.getModelTheme(), environment));
            Reader templateReader = new StringReader(macro);
            Template template = new Template(new UID().toString(), templateReader, FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            environment.include(template);
        } catch (TemplateException | IOException e) {
            Debug.logError(e, "Error rendering screen thru ftl, macro: " + macro, MODULE);
        }
    }

    private Environment getEnvironment(Appendable writer) throws TemplateException, IOException {
        Environment environment = environments.get(writer);
        if (environment == null) {
            Map<String, Object> input = UtilMisc.toMap("key", null);
            environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
            environments.put(writer, environment);
        }
        return environment;
    }
}
