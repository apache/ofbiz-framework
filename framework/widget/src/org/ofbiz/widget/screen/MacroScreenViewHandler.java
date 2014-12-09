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
package org.ofbiz.widget.screen;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtilProperties;
import org.ofbiz.webapp.view.AbstractViewHandler;
import org.ofbiz.webapp.view.ViewHandlerException;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.MacroFormRenderer;
import org.ofbiz.widget.menu.MacroMenuRenderer;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.tree.MacroTreeRenderer;
import org.ofbiz.widget.tree.TreeStringRenderer;
import org.python.modules.re;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;
import freemarker.template.utility.StandardCompress;

public class MacroScreenViewHandler extends AbstractViewHandler {

    public static final String module = MacroScreenViewHandler.class.getName();

    protected ServletContext servletContext = null;

    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
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
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(EntityUtilProperties.getPropertyValue("widget", getName() + ".name", delegator), EntityUtilProperties.getPropertyValue("widget", getName() + ".screenrenderer", delegator));
            ScreenRenderer screens = new ScreenRenderer(writer, null, screenStringRenderer);
            screens.populateContextForRequest(request, response, servletContext);
            String macroLibraryPath = EntityUtilProperties.getPropertyValue("widget", getName() + ".formrenderer", delegator);
            if (UtilValidate.isNotEmpty(macroLibraryPath)) {
                FormStringRenderer formStringRenderer = new MacroFormRenderer(macroLibraryPath, request, response);
                screens.getContext().put("formStringRenderer", formStringRenderer);
            }
            macroLibraryPath = EntityUtilProperties.getPropertyValue("widget", getName() + ".treerenderer", delegator);
            if (UtilValidate.isNotEmpty(macroLibraryPath)) {
                TreeStringRenderer treeStringRenderer = new MacroTreeRenderer(macroLibraryPath, writer);
                screens.getContext().put("treeStringRenderer", treeStringRenderer);
            }
            macroLibraryPath = EntityUtilProperties.getPropertyValue("widget", getName() + ".menurenderer", delegator);
            if (UtilValidate.isNotEmpty(macroLibraryPath)) {
                MenuStringRenderer menuStringRenderer = new MacroMenuRenderer(macroLibraryPath, request, response);
                screens.getContext().put("menuStringRenderer", menuStringRenderer);
            }
            screens.getContext().put("simpleEncoder", StringUtil.getEncoder(EntityUtilProperties.getPropertyValue("widget", getName() + ".encoder", delegator)));
            screenStringRenderer.renderScreenBegin(writer, screens.getContext());
            screens.render(page);
            screenStringRenderer.renderScreenEnd(writer, screens.getContext());
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
