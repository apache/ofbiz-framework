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
package org.apache.ofbiz.widget.renderer.xlsx;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.MapStack;
import org.apache.ofbiz.webapp.control.ConfigXMLReader;
import org.apache.ofbiz.webapp.view.AbstractViewHandler;
import org.apache.ofbiz.webapp.view.ViewHandlerException;
import org.apache.ofbiz.widget.model.ModelTheme;
import org.apache.ofbiz.widget.renderer.ScreenRenderer;
import org.apache.ofbiz.widget.renderer.ScreenStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.apache.ofbiz.widget.renderer.macro.MacroScreenRenderer;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.xml.sax.SAXException;

import freemarker.template.TemplateException;

/**
 * Renders a screen's form/grid content as a genuine binary .xlsx workbook via Apache POI,
 * bypassing the FreeMarker macro pipeline entirely (unlike {@code ScreenFopViewHandler},
 * which still renders XSL-FO text via macros and only converts to binary as a final step).
 */
public class ScreenXlsxViewHandler extends AbstractViewHandler {
    private static final String MODULE = ScreenXlsxViewHandler.class.getName();
    private static final String DEFAULT_ERROR_TEMPLATE = "component://common/widget/CommonScreens.xml#FoError";
    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private ServletContext servletContext;

    @Override
    public void init(ServletContext context) throws ViewHandlerException {
        this.servletContext = context;
    }

    @Override
    public Map<String, Object> prepareViewContext(HttpServletRequest request, HttpServletResponse response,
            ConfigXMLReader.ViewMap viewMap) {
        MapStack<String> context = MapStack.create();
        ScreenRenderer.populateContextForRequest(context, null, request, response, servletContext, viewMap.isSecureContext());
        return context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void render(String name, String page, String info, String contentType, String encoding, HttpServletRequest request,
            HttpServletResponse response, Map<String, Object> context) throws ViewHandlerException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            Writer writer = new StringWriter();
            XlsxFormRenderer xlsxFormRenderer = new XlsxFormRenderer(workbook);
            ScreenStringRenderer screenStringRenderer = new XlsxScreenStringRenderer();
            ScreenRenderer screens = new ScreenRenderer(writer, UtilGenerics.cast(context), screenStringRenderer);
            screens.getContext().put("formStringRenderer", xlsxFormRenderer);
            screens.getContext().put("screens", screens);
            screens.render(page);

            String responseContentType = (UtilValidate.isEmpty(contentType) || "text/html".equals(contentType)) ? CONTENT_TYPE : contentType;
            response.setContentType(responseContentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".xlsx\"");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException | GeneralException | SAXException | ParserConfigurationException e) {
            renderError("Problems rendering the xlsx export", e, request, response, context);
        }
    }

    /**
     * Render error.
     * @param msg      the msg
     * @param e        the e
     * @param request  the request
     * @param response the response
     * @param context  the context
     * @throws ViewHandlerException the view handler exception
     */
    protected void renderError(String msg, Exception e, HttpServletRequest request, HttpServletResponse response,
            Map<String, Object> context) throws ViewHandlerException {
        Debug.logError(msg + ": " + e, MODULE);
        try {
            Writer writer = new StringWriter();
            VisualTheme visualTheme = UtilHttp.getVisualTheme(request);
            ModelTheme modelTheme = visualTheme.getModelTheme();
            ScreenStringRenderer screenStringRenderer = new MacroScreenRenderer(modelTheme.getType("screen"),
                    modelTheme.getScreenRendererLocation("screen"));
            ScreenRenderer screens = new ScreenRenderer(writer, UtilGenerics.cast(context), screenStringRenderer);
            screens.getContext().put("errorMessage", msg + ": " + e);
            screens.render(DEFAULT_ERROR_TEMPLATE);
            response.setContentType("text/html");
            response.getWriter().write(writer.toString());
            writer.close();
        } catch (IOException | GeneralException | SAXException | ParserConfigurationException | TemplateException x) {
            Debug.logError("Multiple errors rendering xlsx export", MODULE);
            throw new ViewHandlerException("Multiple errors rendering xlsx export", x);
        }
    }
}
