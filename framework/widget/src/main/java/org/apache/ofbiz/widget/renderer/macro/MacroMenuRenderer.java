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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.template.FreeMarkerWorker;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Image;
import org.apache.ofbiz.widget.model.ModelMenu;
import org.apache.ofbiz.widget.model.ModelMenuItem;
import org.apache.ofbiz.widget.model.ModelMenuItem.MenuLink;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.model.ThemeFactory;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;
import org.apache.ofbiz.widget.renderer.VisualTheme;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MacroMenuRenderer implements MenuStringRenderer {

    public static final String module = MacroMenuRenderer.class.getName();
    private int macroCount = 999;
    private final Map<Appendable, Environment> environments = new HashMap<>();
    private final Template macroLibrary;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final VisualTheme visualTheme;

    public MacroMenuRenderer(String macroLibraryPath, HttpServletRequest request, HttpServletResponse response) throws TemplateException, IOException {
        this.macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        this.request = request;
        this.response = response;
        this.visualTheme = ThemeFactory.resolveVisualTheme(request);
    }

    // Made this a separate method so it can be externalized and reused.
    private Map<String, Object> createImageParameters(Map<String, Object> context, Image image) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", image.getId(context));
        parameters.put("style", image.getStyle(context));
        parameters.put("width", image.getWidth(context));
        parameters.put("height", image.getHeight(context));
        parameters.put("border", image.getBorder(context));
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src) && request != null && response != null) {
            String urlMode = image.getUrlMode();
            if ("ofbiz".equalsIgnoreCase(urlMode)) {
                boolean fullPath = false;
                boolean secure = false;
                boolean encode = false;
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                src = rh.makeLink(request, response, src, fullPath, secure, encode);
            } else if ("content".equalsIgnoreCase(urlMode)) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, newURL);
                newURL.append(src);
                src = newURL.toString();
            }
        }
        parameters.put("src", src);
        return parameters;
    }

    private void executeMacro(Appendable writer, String macro) throws IOException, TemplateException {
        Environment environment = getEnvironment(writer);
        environment.setVariable("visualTheme", FreeMarkerWorker.autoWrap(visualTheme, environment));
        Reader templateReader = new StringReader(macro);
        macroCount++;
        String templateName = toString().concat("_") + macroCount;
        Template template = new Template(templateName, templateReader, FreeMarkerWorker.getDefaultOfbizConfig());
        templateReader.close();
        environment.include(template);
    }

    private void executeMacro(Appendable writer, String macroName, Map<String, Object> macroParameters) throws IOException, TemplateException {
        StringBuilder sb = new StringBuilder("<@");
        sb.append(macroName);
        if (macroParameters != null) {
            for (Map.Entry<String, Object> parameter : macroParameters.entrySet()) {
                sb.append(' ');
                sb.append(parameter.getKey());
                sb.append("=");
                Object value = parameter.getValue();
                if (value instanceof String) {
                    sb.append('"');
                    sb.append(((String) value).replaceAll("\"", "\\\\\""));
                    sb.append('"');
                } else {
                    sb.append(value);
                }
            }
        }
        sb.append(" />");
        if (Debug.verboseOn()) {
            Debug.logVerbose("Executing macro: " + sb, module);
        }
        executeMacro(writer, sb.toString());
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

    private boolean isDisableIfEmpty(ModelMenuItem menuItem, Map<String, Object> context) {
        boolean disabled = false;
        String disableIfEmpty = menuItem.getDisableIfEmpty();
        if (UtilValidate.isNotEmpty(disableIfEmpty)) {
            List<String> keys = StringUtil.split(disableIfEmpty, "|");
            for (String key : keys) {
                Object obj = context.get(key);
                if (UtilValidate.isEmpty(obj)) {
                    disabled = true;
                    break;
                }
            }
        }
        return disabled;
    }

    private boolean isHideIfSelected(ModelMenuItem menuItem, Map<String, Object> context) {
        ModelMenu menu = menuItem.getModelMenu();
        String currentMenuItemName = menu.getSelectedMenuItemContextFieldName(context);
        String currentItemName = menuItem.getName();
        Boolean hideIfSelected = menuItem.getHideIfSelected();
        return (hideIfSelected != null && hideIfSelected.booleanValue() && currentMenuItemName != null && currentMenuItemName.equals(currentItemName));
    }

    @Override
    public void renderFormatSimpleWrapperClose(Appendable writer, Map<String, Object> context, ModelMenu menu) throws IOException {
        // Nothing to do.
    }

    @Override
    public void renderFormatSimpleWrapperOpen(Appendable writer, Map<String, Object> context, ModelMenu menu) throws IOException {
        // Nothing to do.
    }

    @Override
    public void renderFormatSimpleWrapperRows(Appendable writer, Map<String, Object> context, Object menu) throws IOException {
        List<ModelMenuItem> menuItemList = ((ModelMenu) menu).getMenuItemList();
        for (ModelMenuItem currentMenuItem : menuItemList) {
            renderMenuItem(writer, context, currentMenuItem);
        }
    }

    @Override
    public void renderImage(Appendable writer, Map<String, Object> context, Image image) throws IOException {
        Map<String, Object> parameters = createImageParameters(context, image);
        try {
            executeMacro(writer, "renderImage", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void renderLink(Appendable writer, Map<String, Object> context, MenuLink link) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        String target = link.getTarget(context);
        ModelMenuItem menuItem = link.getLinkMenuItem();
        if (isDisableIfEmpty(menuItem, context)) {
            target = null;
        }
        parameters.put("id", link.getId(context));
        parameters.put("style", link.getStyle(context));
        parameters.put("name", link.getName(context));
        parameters.put("text", link.getText(context));
        parameters.put("height", link.getHeight());
        parameters.put("width", link.getWidth());
        parameters.put("targetWindow", link.getTargetWindow(context));
        StringBuffer uniqueItemName = new StringBuffer(menuItem.getModelMenu().getName());
        uniqueItemName.append("_").append(menuItem.getName()).append("_LF_").append(UtilMisc.<String> addToBigDecimalInMap(context, "menuUniqueItemIndex", BigDecimal.ONE));
        if (menuItem.getModelMenu().getExtraIndex(context) != null) {
            uniqueItemName.append("_").append(menuItem.getModelMenu().getExtraIndex(context));
        }
        if (context.containsKey("itemIndex")) {
            if (context.containsKey("parentItemIndex")) {
                uniqueItemName.append(context.get("parentItemIndex")).append("_").append(context.get("itemIndex"));
            } else {
                uniqueItemName.append("_").append(context.get("itemIndex"));
            }
        }
        parameters.put("uniqueItemName", uniqueItemName.toString());
        String linkType = "";
        if (UtilValidate.isNotEmpty(target)) {
            linkType = WidgetWorker.determineAutoLinkType(link.getLinkType(), target, link.getUrlMode(), request);
        }
        parameters.put("linkType", linkType);
        String linkUrl = "";
        String actionUrl = "";
        StringBuilder targetParameters = new StringBuilder();
        if ("hidden-form".equals(linkType) || "layered-modal".equals(linkType)) {
            StringBuilder sb = new StringBuilder();
            WidgetWorker.buildHyperlinkUrl(sb, target, link.getUrlMode(), null, link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
            actionUrl = sb.toString();
            targetParameters.append("[");
            for (Map.Entry<String, String> parameter : link.getParameterMap(context).entrySet()) {
                if (targetParameters.length() > 1) {
                    targetParameters.append(",");
                }
                targetParameters.append("{'name':'");
                targetParameters.append(parameter.getKey());
                targetParameters.append("'");
                targetParameters.append(",'value':'");
                targetParameters.append(parameter.getValue());
                targetParameters.append("'}");
            }
            targetParameters.append("]");

        }
        if (targetParameters.length() == 0) {
            targetParameters.append("\"\"");
        }
        if (UtilValidate.isNotEmpty(target)) {
            if (!"hidden-form".equals(linkType)) {
                StringBuilder sb = new StringBuilder();
                WidgetWorker.buildHyperlinkUrl(sb, target, link.getUrlMode(), "layered-modal".equals(linkType)?null:link.getParameterMap(context), link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
                linkUrl = sb.toString();
            }
        }
        parameters.put("linkUrl", linkUrl);
        parameters.put("actionUrl", actionUrl);
        parameters.put("parameterList", targetParameters);
        String imgStr = "";
        Image img = link.getImage();
        if (img != null) {
            StringWriter sw = new StringWriter();
            renderImage(sw, context, img);
            imgStr = sw.toString();
        }
        parameters.put("imgStr", imgStr);
        try {
            executeMacro(writer, "renderLink", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void renderMenuClose(Appendable writer, Map<String, Object> context, ModelMenu menu) throws IOException {
        Map<String, Object> parameters = null;
        if (ModelWidget.widgetBoundaryCommentsEnabled(context)) {
            parameters = new HashMap<>();
            StringBuilder sb = new StringBuilder("End Menu Widget ");
            sb.append(menu.getBoundaryCommentName());
            parameters.put("boundaryComment", sb.toString());
        }
        try {
            executeMacro(writer, "renderMenuEnd", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void renderMenuItem(Appendable writer, Map<String, Object> context, ModelMenuItem menuItem) throws IOException {
        if (isHideIfSelected(menuItem, context)) {
            return;
        }
        Map<String, Object> parameters = new HashMap<>();
        String style = menuItem.getWidgetStyle();
        if (menuItem.isSelected(context)) {
            String selectedStyle = menuItem.getSelectedStyle();
            if (UtilValidate.isEmpty(selectedStyle)) {
                selectedStyle = "selected";
            }
            if (UtilValidate.isNotEmpty(style)) {
                style += " " ;
            }
            style += selectedStyle ;
        }
        if (this.isDisableIfEmpty(menuItem, context)) {
            style = menuItem.getDisabledTitleStyle();
        }
        if (style == null) {
            style = "";
        }
        String alignStyle = menuItem.getAlignStyle();
        if (UtilValidate.isNotEmpty(alignStyle)) {
            style = style.concat(" ").concat(alignStyle);
        }
        parameters.put("style", style);
        parameters.put("toolTip", menuItem.getTooltip(context));
        String linkStr = "";
        MenuLink link = menuItem.getLink();
        if (link != null) {
            StringWriter sw = new StringWriter();
            renderLink(sw, context, link);
            linkStr = sw.toString();
        } else {
            linkStr = menuItem.getTitle(context);
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                linkStr = simpleEncoder.encode(linkStr);
            }
        }
        parameters.put("linkStr", linkStr);
        boolean containsNestedMenus = !menuItem.getMenuItemList().isEmpty();
        parameters.put("containsNestedMenus", containsNestedMenus);
        try {
            executeMacro(writer, "renderMenuItemBegin", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
        if (containsNestedMenus) {
            for (ModelMenuItem childMenuItem : menuItem.getMenuItemList()) {
                childMenuItem.renderMenuItemString(writer, context, this);
            }
        }
        parameters.clear();
        parameters.put("containsNestedMenus", containsNestedMenus);
        try {
            executeMacro(writer, "renderMenuItemEnd", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void renderMenuOpen(Appendable writer, Map<String, Object> context, ModelMenu menu) throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        if (ModelWidget.widgetBoundaryCommentsEnabled(context)) {
            StringBuilder sb = new StringBuilder("Begin Menu Widget ");
            sb.append(menu.getBoundaryCommentName());
            parameters.put("boundaryComment", sb.toString());
        }
        parameters.put("id", menu.getId());
        parameters.put("style", menu.getMenuContainerStyle(context));
        parameters.put("title", menu.getTitle(context));
        try {
            executeMacro(writer, "renderMenuBegin", parameters);
        } catch (TemplateException e) {
            throw new IOException(e);
        }
    }
}
