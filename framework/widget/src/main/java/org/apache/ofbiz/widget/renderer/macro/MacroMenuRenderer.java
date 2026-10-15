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
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilRandom;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
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

import freemarker.template.TemplateException;
import org.apache.ofbiz.widget.renderer.html.HtmlWidgetRenderer;

public class MacroMenuRenderer implements MenuStringRenderer {

    private final FtlWriter ftlWriter;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public MacroMenuRenderer(String macroLibraryPath, HttpServletRequest request, HttpServletResponse response)
            throws TemplateException, IOException {
        this.request = request;
        this.response = response;
        this.ftlWriter = new FtlWriter(macroLibraryPath, ThemeFactory.resolveVisualTheme(request));
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
                RequestHandler rh = RequestHandler.from(request);
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

    private void executeMacro(Appendable writer, String macroName, Map<String, Object> macroParameters) {
        Map<String, Object> parameters = new HashMap<>();
        if (macroParameters != null) {
            macroParameters.forEach((name, value) -> parameters.put(name, value == null ? "" : value));
        }
        ftlWriter.processMacroCall(writer, null, macroName, parameters);
    }

    private static boolean isDisableIfEmpty(ModelMenuItem menuItem, Map<String, Object> context) {
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

    private static boolean isHideIfSelected(ModelMenuItem menuItem, Map<String, Object> context) {
        ModelMenu menu = menuItem.getModelMenu();
        String currentMenuItemName = menu.getSelectedMenuItemContextFieldName(context);
        String currentItemName = menuItem.getName();
        Boolean hideIfSelected = menuItem.getHideIfSelected();
        return (hideIfSelected != null && hideIfSelected && currentMenuItemName != null && currentMenuItemName.equals(currentItemName));
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
        executeMacro(writer, "renderImage", parameters);
    }

    @Override
    public void renderLink(Appendable writer, Map<String, Object> context, MenuLink link) throws IOException {
        String target = link.getTarget(context);
        ModelMenuItem menuItem = link.getLinkMenuItem();
        if (isDisableIfEmpty(menuItem, context)) {
            target = null;
        }
        Map<String, Object> parameters = UtilMisc.toMap(
                        "id", link.getId(context),
                "style", link.getStyle(context),
                "name", link.getName(context),
                "text", link.getText(context),
                "targetWindow", link.getTargetWindow(context));

        String linkHeight = link.getHeight();
        if (UtilValidate.isNotEmpty(linkHeight)) parameters.put("height", linkHeight);

        String linkWidth = link.getWidth();
        if (UtilValidate.isNotEmpty(linkWidth)) parameters.put("width", linkWidth);

        String uniqueItemName = UtilRandom.getUnique(menuItem.getModelMenu().getName()
                + "_" + menuItem.getName() + "_", true);
        parameters.put("uniqueItemName", uniqueItemName);

        String linkType = "";
        if (UtilValidate.isNotEmpty(target)) {
            linkType = WidgetWorker.determineAutoLinkType(link.getLinkType(), target, link.getUrlMode(), request);
        }
        parameters.put("linkType", linkType);
        String actionUrl = "";
        List<Map<String, String>> targetParameters = new ArrayList<>();

        String confirmationMessage = link.getLink().getConfirmationMsg(context);
        if (link.getLink().getRequestConfirmation() && UtilValidate.isEmpty(confirmationMessage)) {
            String defaultMessage = UtilProperties.getPropertyValue("general", "default.confirmation.message",
                    "${uiLabelMap.CommonConfirm}");
            confirmationMessage = FlexibleStringExpander.expandString(defaultMessage, context);
        }
        parameters.put("confirmation", confirmationMessage);

        boolean isModal = "layered-modal".equals(linkType);
        if ("hidden-form".equals(linkType) || isModal) {
            final URI actionUri = WidgetWorker.buildHyperlinkUri(target, link.getUrlMode(), null,
                    link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(),
                    request, response);
            actionUrl = actionUri.toString();

            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            // Callback propagation only if displaying a modal
            for (Map.Entry<String, String> parameter : link.getParameterMap(context, isModal).entrySet()) {
                String value = parameter.getValue() == null ? "" : parameter.getValue();
                targetParameters.add(UtilMisc.toMap("name", parameter.getKey(),
                        "value", simpleEncoder != null ? simpleEncoder.encode(value) : value));
            }

        }
        parameters.put("linkUrl", MacroCommonRenderer.getLinkUrl(link.getLink(), linkType, context));
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
        executeMacro(writer, "renderLink", parameters);
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
        executeMacro(writer, "renderMenuEnd", parameters);
        if (HtmlWidgetRenderer.NAMED_BORDER_TYPE != ModelWidget.NamedBorderType.NONE) {
            writer.append(HtmlWidgetRenderer.endNamedBorder("Menu", menu.getBoundaryCommentName()));
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
                style += " ";
            }
            style += selectedStyle;
        }
        if (isDisableIfEmpty(menuItem, context)) {
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
        executeMacro(writer, "renderMenuItemBegin", parameters);
        if (containsNestedMenus) {
            for (ModelMenuItem childMenuItem : menuItem.getMenuItemList()) {
                childMenuItem.renderMenuItemString(writer, context, this);
            }
        }
        parameters.clear();
        parameters.put("containsNestedMenus", containsNestedMenus);
        executeMacro(writer, "renderMenuItemEnd", parameters);
    }

    @Override
    public void renderMenuOpen(Appendable writer, Map<String, Object> context, ModelMenu menu) throws IOException {
        if (HtmlWidgetRenderer.NAMED_BORDER_TYPE != ModelWidget.NamedBorderType.NONE) {
            writer.append(HtmlWidgetRenderer.beginNamedBorder("Menu",
                    menu.getBoundaryCommentName(), ((HttpServletRequest) context.get("request")).getContextPath()));
        }
        Map<String, Object> parameters = new HashMap<>();
        if (ModelWidget.widgetBoundaryCommentsEnabled(context)) {
            StringBuilder sb = new StringBuilder("Begin Menu Widget ");
            sb.append(menu.getBoundaryCommentName());
            parameters.put("boundaryComment", sb.toString());
        }
        parameters.put("id", menu.getId());
        parameters.put("style", menu.getMenuContainerStyle(context));
        parameters.put("title", menu.getTitle(context));
        executeMacro(writer, "renderMenuBegin", parameters);
    }
}
