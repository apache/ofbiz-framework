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
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlNoop;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlString;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlVisitor;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Processes FTL templates and writes result to Appendables.
 */
public final class FtlWriter {
    private static final String MODULE = FtlWriter.class.getName();

    private final DefaultObjectWrapper defaultObjectWrapper = new DefaultObjectWrapperBuilder(FreeMarkerWorker.VERSION).build();
    private final WeakHashMap<Appendable, Environment> environments = new WeakHashMap<>();
    private final Template macroLibrary;
    private final VisualTheme visualTheme;

    public FtlWriter(final String macroLibraryPath, final VisualTheme visualTheme) throws IOException {
        this.macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        this.visualTheme = visualTheme;
    }

    /**
     * Process the given RenderableFtl as a template and write the result to the Appendable.
     *
     * @param writer        The Appendable to write the result of the template processing to.
     * @param renderableFtl The Renderable FTL to process as a template.
     */
    public void processFtl(final Appendable writer, final RenderableFtl renderableFtl) {
        final RenderableFtlFtlWriterVisitor writerVisitor = new RenderableFtlFtlWriterVisitor(writer);
        renderableFtl.accept(writerVisitor);
    }

    /**
     * Process the given FTL string as a template and write the result to the Appendable.
     *
     * @param writer    The Appendable to write the result of the template processing to.
     * @param ftlString The FTL string to process as a template.
     */
    public void processFtlString(Appendable writer, Locale locale, String ftlString) {
        try {
            final Environment environment = getEnvironment(writer, locale);
            environment.setVariable("visualTheme", FreeMarkerWorker.autoWrap(visualTheme, environment));
            environment.setVariable("modelTheme",
                    FreeMarkerWorker.autoWrap(visualTheme.getModelTheme(), environment));
            Reader templateReader = new StringReader(ftlString);
            Template template = new Template(UUID.randomUUID().toString(), templateReader,
                    FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            environment.include(template);
        } catch (TemplateException | IOException e) {
            Debug.logError(e, "Error rendering ftl, ftlString: " + ftlString, MODULE);
        }
    }

    private Environment getEnvironment(Appendable writer, Locale locale) throws TemplateException, IOException {
        Environment environment = environments.get(writer);
        if (environment == null) {
            Map<String, Object> input = UtilMisc.toMap("key", null);
            environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
            environments.put(writer, environment);
        }
        if (locale != null) {
            environment.setLocale(locale);
        } else {
            environment.setLocale(Locale.getDefault());
        }
        return environment;
    }

    /**
     * Visitor for RenderableFtl objects that will process the contents of the objects as FTL templates and write the
     * results to the configured Appendable.
     */
    private final class RenderableFtlFtlWriterVisitor implements RenderableFtlVisitor {

        private final Appendable writer;

        /**
         * Construct the visitor, specifying the writer that FTL should be rendered to.
         * @param writer The Appendable to write the result of the template processing to.
         */
        private RenderableFtlFtlWriterVisitor(final Appendable writer) {
            this.writer = writer;
        }

        @Override
        public void visit(RenderableFtlMacroCall renderableFtlMacroCall) {
            final String name = renderableFtlMacroCall.getName();

            try {
                final Environment environment = getEnvironment(writer, null);

                environment.setVariable("$args$" + name,
                        defaultObjectWrapper.wrap(renderableFtlMacroCall.getParameters()));

                processFtlString(writer, null, String.format("<@%s?with_args($args$%s)/>", name, name));
            } catch (TemplateException | IOException e) {
                Debug.logError(e, "Error rendering ftl macro: " + name, MODULE);
            }
        }

        @Override
        public void visit(RenderableFtlNoop renderableFtlNoop) {
            // Do nothing.
        }

        @Override
        public void visit(RenderableFtlString renderableFtlString) {
            processFtlString(writer, null, renderableFtlString.getFtlString());
        }
    }
}
