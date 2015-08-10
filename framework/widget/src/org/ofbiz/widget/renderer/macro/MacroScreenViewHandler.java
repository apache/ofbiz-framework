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
package org.ofbiz.widget.renderer.macro;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.MapStack;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.ofbiz.widget.renderer.MenuStringRenderer;
import org.ofbiz.widget.renderer.ScreenRenderer;
import org.ofbiz.widget.renderer.ScreenStringRenderer;
import org.ofbiz.widget.renderer.TreeStringRenderer;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;
import freemarker.template.utility.StandardCompress;

public class MacroScreenViewHandler extends AbstractViewHandler {

    public static final String module = MacroScreenViewHandler.class.getName();

    protected ServletContext servletContext = null;

    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    private ScreenStringRenderer loadRenderers(HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> context, Writer writer) throws GeneralException, TemplateException, IOException {
        String screenMacroLibraryPath = UtilProperties.getPropertyValue("widget", getName() + ".screenrenderer");
        String formMacroLibraryPath = UtilProperties.getPropertyValue("widget", getName() + ".formrenderer");
        String treeMacroLibraryPath = UtilProperties.getPropertyValue("widget", getName() + ".treerenderer");
        String menuMacroLibraryPath = UtilProperties.getPropertyValue("widget", getName() + ".menurenderer");
        Map<String, Object> userPreferences = UtilGenerics.cast(context.get("userPreferences"));
        if (userPreferences != null) {
            String visualThemeId = (String) userPreferences.get("VISUAL_THEME");
            if (visualThemeId != null) {
                LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
                Map<String, Object> serviceCtx = dispatcher.getDispatchContext().makeValidContext("getVisualThemeResources",
                        ModelService.IN_PARAM, context);
                serviceCtx.put("visualThemeId", visualThemeId);
                Map<String, Object> serviceResult = dispatcher.runSync("getVisualThemeResources", serviceCtx);
                if (ServiceUtil.isSuccess(serviceResult)) {
                    Map<String, List<String>> themeResources = UtilGenerics.cast(serviceResult.get("themeResources"));
                    List<String> resourceList = UtilGenerics.cast(themeResources.get("VT_SCRN_MACRO_LIB"));
                    if (resourceList != null && !resourceList.isEmpty()) {
                        String macroLibraryPath = resourceList.get(0);
                        if (macroLibraryPath != null) {
                            screenMacroLibraryPath = macroLibraryPath;
                        }
                    }
                    resourceList = UtilGenerics.cast(themeResources.get("VT_FORM_MACRO_LIB"));
                    if (resourceList != null && !resourceList.isEmpty()) {
                        String macroLibraryPath = resourceList.get(0);
                        if (macroLibraryPath != null) {
                            formMacroLibraryPath = macroLibraryPath;
                        }
                    }
                    resourceList = UtilGenerics.cast(themeResources.get("VT_TREE_MACRO_LIB"));
                    if (resourceList != null && !resourceList.isEmpty()) {
                        String macroLibraryPath = resourceList.get(0);
                        if (macroLibraryPath != null) {
                            treeMacroLibraryPath = macroLibraryPath;
                        }
                    }
                    resourceList = UtilGenerics.cast(themeResources.get("VT_MENU_MACRO_LIB"));
                    if (resourceList != null && !resourceList.isEmpty()) {
                        String macroLibraryPath = resourceList.get(0);
                        if (macroLibraryPath != null) {
                            menuMacroLibraryPath = macroLibraryPath;
                        }
                    }
                }
            }
        }
        ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(UtilProperties.getPropertyValue("widget", getName()
                + ".name"), screenMacroLibraryPath);
        if (!formMacroLibraryPath.isEmpty()) {
            FormStringRenderer formStringRenderer = new MacroFormRenderer(formMacroLibraryPath, request, response);
            context.put("formStringRenderer", formStringRenderer);
        }
        if (!treeMacroLibraryPath.isEmpty()) {
            TreeStringRenderer treeStringRenderer = new MacroTreeRenderer(treeMacroLibraryPath, writer);
            context.put("treeStringRenderer", treeStringRenderer);
        }
        if (!menuMacroLibraryPath.isEmpty()) {
            MenuStringRenderer menuStringRenderer = new MacroMenuRenderer(menuMacroLibraryPath, request, response);
            context.put("menuStringRenderer", menuStringRenderer);
        }
        return screenStringRenderer;
    }

    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request, HttpServletResponse response) throws ViewHandlerException {
        try {
            Writer writer = response.getWriter();
            Delegator delegator = (Delegator) request.getAttribute("delegator");
            // compress output if configured to do so
            if (UtilValidate.isEmpty(encoding)) {
                encoding = EntityUtilProperties.getPropertyValue("widget", getName() + ".default.encoding", "none", delegator);
            }
            boolean compressOutput = "compressed".equals(encoding);
            if (!compressOutput) {
                compressOutput = "true".equals(EntityUtilProperties.getPropertyValue("widget", getName() + ".compress", delegator));
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
            context.put("simpleEncoder", UtilCodec.getEncoder(UtilProperties.getPropertyValue("widget", getName() + ".encoder")));
            screenStringRenderer.renderScreenBegin(writer, context);
            screens.render(page);
            screenStringRenderer.renderScreenEnd(writer, context);
            writer.flush();
        } catch (TemplateException e) {
            Debug.logError(e, "Error initializing screen renderer", module);
            throw new ViewHandlerException(e.getMessage());
        } catch (IOException e) {
            throw new ViewHandlerException("Error in the response writer/output stream: " + e.toString(), e);
        } catch (SAXException e) {
            throw new ViewHandlerException("XML Error rendering page: " + e.toString(), e);
        } catch (ParserConfigurationException e) {
            throw new ViewHandlerException("XML Error rendering page: " + e.toString(), e);
        } catch (GeneralException e) {
            throw new ViewHandlerException("Lower level error rendering page: " + e.toString(), e);
        }
    }
}
