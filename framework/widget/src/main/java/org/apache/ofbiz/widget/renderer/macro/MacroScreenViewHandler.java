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

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.webapp.view.AbstractViewHandler;
import org.apache.ofbiz.webapp.view.ViewHandlerException;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.FormStringRenderer;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.TreeStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;
import freemarker.template.utility.StandardCompress;

public class MacroScreenViewHandler extends AbstractViewHandler {

    public static final String module = MacroScreenViewHandler.class.getName();

    protected ServletContext servletContext = null;

    @Override
    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    private ScreenStringRenderer loadRenderers(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> context, Writer writer) throws TemplateException, IOException {
        VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
        ModelTheme modelTheme = visualTheme.getModelTheme();

        String screenMacroLibraryPath = modelTheme.getScreenRendererLocation(getName());
        String formMacroLibraryPath = modelTheme.getFormRendererLocation(getName());
        String treeMacroLibraryPath = modelTheme.getTreeRendererLocation(getName());
        String menuMacroLibraryPath = modelTheme.getMenuRendererLocation(getName());
        ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(modelTheme.getType(getName()), screenMacroLibraryPath);
        if (UtilValidate.isNotEmpty(formMacroLibraryPath)) {
            FormStringRenderer formStringRenderer = new MacroFormRenderer(formMacroLibraryPath, request, response);
            context.put("formStringRenderer", formStringRenderer);
        }
        if (UtilValidate.isNotEmpty(treeMacroLibraryPath)) {
            TreeStringRenderer treeStringRenderer = new MacroTreeRenderer(treeMacroLibraryPath, writer);
            context.put("treeStringRenderer", treeStringRenderer);
        }
        if (UtilValidate.isNotEmpty(menuMacroLibraryPath)) {
            MenuStringRenderer menuStringRenderer = new MacroMenuRenderer(menuMacroLibraryPath, request, response);
            context.put("menuStringRenderer", menuStringRenderer);
        }
        return screenStringRenderer;
    }

    @Override
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        try {
            Writer writer = response.getWriter();
            VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
            ModelTheme modelTheme = visualTheme.getModelTheme();
            // compress output if configured to do so
            if (UtilValidate.isEmpty(encoding)) {
                encoding = modelTheme.getEncoding(getName());
            }
            boolean compressOutput = "compressed".equals(encoding);
            if (!compressOutput) {
                compressOutput = "true".equals(modelTheme.getCompress(getName()));
            }
            if (!compressOutput && this.servletContext != null) {
                compressOutput = "true".equals(this.servletContext.getAttribute("compressHTML"));
            }
            if (compressOutput) {
                // StandardCompress defaults to a 2k buffer. That could be increased
                // to speed up output.
                writer = new StandardCompress().getWriter(writer, null);
            }
            MapStack<String> context = MapStack.create();
            ScreenRenderer.populateContextForRequest(context, null, request, response, servletContext);
            ScreenStringRenderer screenStringRenderer = loadRenderers(request, response, context, writer);
            ScreenRenderer screens = new ScreenRenderer(writer, context, screenStringRenderer);
            context.put("screens", screens);
            context.put("simpleEncoder", UtilCodec.getEncoder(visualTheme.getModelTheme().getEncoder(getName())));
             screenStringRenderer.renderScreenBegin(writer, context);
            screens.render(page);
            screenStringRenderer.renderScreenEnd(writer, context);
            writer.flush();
        } catch (TemplateException e) {
            Debug.logError(e, "Error initializing screen renderer", module);
            throw new ViewHandlerException(e.getMessage());
        } catch (IOException e) {
            throw new ViewHandlerException("Error in the response writer/output stream: " + e.toString(), e);
        } catch (SAXException | ParserConfigurationException e) {
            throw new ViewHandlerException("XML Error rendering page: " + e.toString(), e);
        } catch (GeneralException e) {
            throw new ViewHandlerException("Lower level error rendering page: " + e.toString(), e);
        }
    }
}
