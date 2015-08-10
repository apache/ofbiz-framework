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
package org.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.content.WidgetContentWorker;
import org.ofbiz.widget.content.WidgetDataResourceWorker;
import org.ofbiz.widget.model.ModelForm;
import org.ofbiz.widget.model.ModelMenu;
import org.ofbiz.widget.model.ModelScreenWidget;
import org.ofbiz.widget.model.ModelScreenWidget.ColumnContainer;
import org.ofbiz.widget.model.ModelScreenWidget.Container;
import org.ofbiz.widget.model.ModelScreenWidget.Content;
import org.ofbiz.widget.model.ModelScreenWidget.Form;
import org.ofbiz.widget.model.ModelScreenWidget.HorizontalSeparator;
import org.ofbiz.widget.model.ModelScreenWidget.Label;
import org.ofbiz.widget.model.ModelScreenWidget.Menu;
import org.ofbiz.widget.model.ModelScreenWidget.PortalPage;
import org.ofbiz.widget.model.ModelScreenWidget.ScreenImage;
import org.ofbiz.widget.model.ModelScreenWidget.ScreenLink;
import org.ofbiz.widget.model.ModelScreenWidget.Screenlet;
import org.ofbiz.widget.model.ModelScreenWidget.Section;
import org.ofbiz.widget.model.ModelScreenWidget.SubContent;
import org.ofbiz.widget.model.ModelWidget;
import org.ofbiz.widget.renderer.FormStringRenderer;
import org.ofbiz.widget.renderer.MenuStringRenderer;
import org.ofbiz.widget.renderer.Paginator;
import org.ofbiz.widget.renderer.ScreenStringRenderer;

/**
 * Widget Library - HTML Form Renderer implementation
 * @deprecated Use MacroScreenRenderer.
 */
public class HtmlScreenRenderer extends HtmlWidgetRenderer implements ScreenStringRenderer {

    public static final String module = HtmlScreenRenderer.class.getName();
    protected int elementId = 999;

    public HtmlScreenRenderer() {}

    protected String getNextElementId() {
        elementId++;
        return "hsr" + elementId;
    }

    public String getRendererName() {
        return "html";
    }

    public void renderScreenBegin(Appendable writer, Map<String, Object> context) throws IOException {
        writer.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        appendWhitespace(writer);
    }

    public void renderScreenEnd(Appendable writer, Map<String, Object> context) throws IOException {
    }

    public void renderSectionBegin(Appendable writer, Map<String, Object> context, Section section) throws IOException {
        if (section.isMainSection()) {
            this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        }
        renderBeginningBoundaryComment(writer, section.isMainSection()?"Screen":"Section Widget", section);
    }

    public void renderSectionEnd(Appendable writer, Map<String, Object> context, Section section) throws IOException {
        renderEndingBoundaryComment(writer, section.isMainSection()?"Screen":"Section Widget", section);
    }

    public void renderContainerBegin(Appendable writer, Map<String, Object> context, Container container) throws IOException {
        String containerId = container.getId(context);
        String autoUpdateTarget = container.getAutoUpdateTargetExdr(context);
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (UtilValidate.isNotEmpty(autoUpdateTarget) && UtilHttp.isJavaScriptEnabled(request)) {
            if (UtilValidate.isEmpty(containerId)) {
                containerId = getNextElementId();
            }
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");

            writer.append("<script type=\"text/javascript\">ajaxUpdateAreaPeriodic('");
            writer.append(containerId);
            writer.append("', '");
            writer.append(rh.makeLink(request, response, autoUpdateTarget));
            writer.append("', '");
            writer.append("', '").append(container.getAutoUpdateInterval(context)).append("');</script>");
            appendWhitespace(writer);
        }
        writer.append("<div");

        if (UtilValidate.isNotEmpty(containerId)) {
            writer.append(" id=\"");
            writer.append(containerId);
            writer.append("\"");
        }

        String style = container.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }

        writer.append(">");
        appendWhitespace(writer);
    }
    public void renderContainerEnd(Appendable writer, Map<String, Object> context, Container container) throws IOException {
        writer.append("</div>");
        appendWhitespace(writer);
    }

    public void renderHorizontalSeparator(Appendable writer, Map<String, Object> context, HorizontalSeparator separator) throws IOException {
        writer.append("<hr");
        String className = separator.getStyle(context);
        if (UtilValidate.isNotEmpty(className)) {
            writer.append(" class=\"").append(className).append("\"");
        }
        String idName = separator.getId(context);
        if (UtilValidate.isNotEmpty(idName)) {
            writer.append(" id=\"").append(idName).append("\"");
        }
        writer.append("/>");
        appendWhitespace(writer);
    }

    public void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, Screenlet screenlet) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        boolean javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        Menu tabMenu = screenlet.getTabMenu();
        if (tabMenu != null) {
            tabMenu.renderWidgetString(writer, context, this);
        }
        writer.append("<div class=\"screenlet\"");
        String id = screenlet.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        writer.append(">");
        appendWhitespace(writer);

        String title = screenlet.getTitle(context);
        Menu navMenu = screenlet.getNavigationMenu();
        Form navForm = screenlet.getNavigationForm();
        String collapsibleAreaId = null;
        if (UtilValidate.isNotEmpty(title) || navMenu != null || navForm != null || screenlet.collapsible()) {
            writer.append("<div class=\"screenlet-title-bar\">");
            appendWhitespace(writer);
            writer.append("<ul>");
            appendWhitespace(writer);
            if (UtilValidate.isNotEmpty(title)) {
                writer.append("<li class=\"h3\">");
                writer.append(title);
                writer.append("</li>");
                appendWhitespace(writer);
            }
            if (screenlet.collapsible()) {
                collapsibleAreaId = this.getNextElementId();
                String expandToolTip = null;
                String collapseToolTip = null;
                Map<String, Object> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
                Map<String, Object> paramMap = UtilGenerics.checkMap(context.get("requestParameters"));
                Map<String, Object> requestParameters = new HashMap<String, Object>(paramMap);
                if (uiLabelMap != null) {
                    expandToolTip = (String) uiLabelMap.get("CommonExpand");
                    collapseToolTip = (String) uiLabelMap.get("CommonCollapse");
                }
                writer.append("<li class=\"");
                if (collapsed) {
                    writer.append("collapsed\"><a ");
                    if (javaScriptEnabled) {
                        writer.append("onclick=\"javascript:toggleScreenlet(this, '").append(collapsibleAreaId).append("', '").append(expandToolTip).append("', '").append(collapseToolTip).append("');\"");
                    } else {
                        requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "false");
                        String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                        writer.append("href=\"").append(request.getRequestURI()).append("?").append(queryString).append("\"");
                    }
                    if (UtilValidate.isNotEmpty(expandToolTip)) {
                        writer.append(" title=\"").append(expandToolTip).append("\"");
                    }
                } else {
                    writer.append("expanded\"><a ");
                    if (javaScriptEnabled) {
                        writer.append("onclick=\"javascript:toggleScreenlet(this, '").append(collapsibleAreaId).append("', '").append(expandToolTip).append("', '").append(collapseToolTip).append("');\"");
                    } else {
                        requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "true");
                        String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                        writer.append("href=\"").append(request.getRequestURI()).append("?").append(queryString).append("\"");
                    }
                    if (UtilValidate.isNotEmpty(collapseToolTip)) {
                        writer.append(" title=\"").append(collapseToolTip).append("\"");
                    }
                }
                writer.append(">&nbsp</a></li>");
                appendWhitespace(writer);
            }
            if (!collapsed) {
                if (navMenu != null) {
                    MenuStringRenderer savedRenderer = (MenuStringRenderer) context.get("menuStringRenderer");
                    MenuStringRenderer renderer = new ScreenletMenuRenderer(request, response);
                    context.put("menuStringRenderer", renderer);
                    navMenu.renderWidgetString(writer, context, this);
                    context.put("menuStringRenderer", savedRenderer);
                } else if (navForm != null) {
                    renderScreenletPaginateMenu(writer, context, navForm);
                }
            }
            writer.append("</ul>");
            appendWhitespace(writer);
            writer.append("<br class=\"clear\" />");
            appendWhitespace(writer);
            writer.append("</div>");
            appendWhitespace(writer);
            writer.append("<div");
            if (UtilValidate.isNotEmpty(collapsibleAreaId)) {
                writer.append(" id=\"").append(collapsibleAreaId).append("\"");
                if (collapsed) {
                    writer.append(" style=\"display: none;\"");
                }
            }
            if (screenlet.padded()) {
                writer.append(" class=\"screenlet-body\"");
            }
            writer.append(">");
            appendWhitespace(writer);
        }
    }

    private int getActualPageSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("actualPageSize");
        return value != null ? value.intValue() : (getHighIndex(context) - getLowIndex(context));
    }

    private int getHighIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("highIndex");
        return value != null ? value.intValue() : 0;
    }

    private int getListSize(Map<String, Object> context) {
        Integer value = (Integer) context.get("listSize");
        return value != null ? value.intValue() : 0;
    }

    private int getLowIndex(Map<String, Object> context) {
        Integer value = (Integer) context.get("lowIndex");
        return value != null ? value.intValue() : 0;
    }

    protected void renderScreenletPaginateMenu(Appendable writer, Map<String, Object> context, Form form) throws IOException {
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        ModelForm modelForm;
        try {
            modelForm = form.getModelForm(context);
        } catch (Exception e) {
            throw new IOException(e);
        }
        modelForm.runFormActions(context);
        Paginator.preparePager(modelForm, context);
        String targetService = modelForm.getPaginateTarget(context);
        if (targetService == null) {
            targetService = "${targetService}";
        }

        // get the parametrized pagination index and size fields
        int paginatorNumber = WidgetWorker.getPaginatorNumber(context);
        String viewIndexParam = modelForm.getMultiPaginateIndexField(context);
        String viewSizeParam = modelForm.getMultiPaginateSizeField(context);

        int viewIndex = Paginator.getViewIndex(modelForm, context);
        int viewSize = Paginator.getViewSize(modelForm, context);
        int listSize = getListSize(context);

        int lowIndex = getLowIndex(context);
        int highIndex = getHighIndex(context);
        int actualPageSize = getActualPageSize(context);

        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize >= 0) return;

        // needed for the "Page" and "rows" labels
        Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
        String ofLabel = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        } else {
            ofLabel = uiLabelMap.get("CommonOf");
            ofLabel = ofLabel.toLowerCase();
        }

        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (viewIndexParam.equals("viewIndex" + "_" + paginatorNumber)) viewIndexParam = "VIEW_INDEX" + "_" + paginatorNumber;
        if (viewSizeParam.equals("viewSize" + "_" + paginatorNumber)) viewSizeParam = "VIEW_SIZE" + "_" + paginatorNumber;

        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");

        Map<String, Object> inputFields = UtilGenerics.toMap(context.get("requestParameters"));
        // strip out any multi form fields if the form is of type multi
        if (modelForm.getType().equals("multi")) {
            inputFields = UtilHttp.removeMultiFormParameters(inputFields);
        }
        String queryString = UtilHttp.urlEncodeArgs(inputFields);
        // strip legacy viewIndex/viewSize params from the query string
        queryString = UtilHttp.stripViewParamsFromQueryString(queryString, "" + paginatorNumber);
        // strip parametrized index/size params from the query string
        HashSet<String> paramNames = new HashSet<String>();
        paramNames.add(viewIndexParam);
        paramNames.add(viewSizeParam);
        queryString = UtilHttp.stripNamedParamsFromQueryString(queryString, paramNames);

        String anchor = "";
        String paginateAnchor = modelForm.getPaginateTargetAnchor();
        if (paginateAnchor != null) anchor = "#" + paginateAnchor;

        // preparing the link text, so that later in the code we can reuse this and just add the viewIndex
        StringBuilder prepLinkTextBuffer = new StringBuilder(targetService);
        if (prepLinkTextBuffer.indexOf("?") < 0) {
            prepLinkTextBuffer.append("?");
        } else if (prepLinkTextBuffer.indexOf("?", prepLinkTextBuffer.length() - 1) > 0) {
            prepLinkTextBuffer.append("&amp;");
        }
        if (!UtilValidate.isEmpty(queryString) && !queryString.equals("null")) {
            prepLinkTextBuffer.append(queryString).append("&amp;");
        }
        prepLinkTextBuffer.append(viewSizeParam).append("=").append(viewSize).append("&amp;").append(viewIndexParam).append("=");
        String prepLinkText = prepLinkTextBuffer.toString();

        String linkText;

        appendWhitespace(writer);
        // The current screenlet title bar navigation syling requires rendering
        // these links in reverse order
        // Last button
        writer.append("<li class=\"").append(modelForm.getPaginateLastStyle());
        if (highIndex < listSize) {
            writer.append("\"><a href=\"");
            int lastIndex = UtilMisc.getViewLastIndex(listSize, viewSize);
            linkText = prepLinkText + lastIndex + anchor;
            // - make the link
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">").append(modelForm.getPaginateLastLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append(" disabled\">").append(modelForm.getPaginateLastLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        // Next button
        writer.append("<li class=\"").append(modelForm.getPaginateNextStyle());
        if (highIndex < listSize) {
            writer.append("\"><a href=\"");
            linkText = prepLinkText + (viewIndex + 1) + anchor;
            // - make the link
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">").append(modelForm.getPaginateNextLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append(" disabled\">").append(modelForm.getPaginateNextLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        if (listSize > 0) {
            writer.append("<li>");
            writer.append(Integer.toString(lowIndex + 1)).append(" - ").append(Integer.toString(lowIndex + actualPageSize)).append(" ").append(ofLabel).append(" ").append(Integer.toString(listSize));
            writer.append("</li>");
            appendWhitespace(writer);
        }
        // Previous button
        writer.append("<li class=\"nav-previous");
        if (viewIndex > 0) {
            writer.append("\"><a href=\"");
            linkText = prepLinkText + (viewIndex - 1) + anchor;
            // - make the link
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">").append(modelForm.getPaginatePreviousLabel(context)).append("</a>");
        } else {
            // disabled button
            writer.append(" disabled\">").append(modelForm.getPaginatePreviousLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        // First button
        writer.append("<li class=\"nav-first");
        if (viewIndex > 0) {
            writer.append("\"><a href=\"");
            linkText = prepLinkText + 0 + anchor;
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">").append(modelForm.getPaginateFirstLabel(context)).append("</a>");
        } else {
            writer.append(" disabled\">").append(modelForm.getPaginateFirstLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
    }

    public void renderScreenletSubWidget(Appendable writer, Map<String, Object> context, ModelScreenWidget subWidget, Screenlet screenlet) throws GeneralException, IOException {
        if (subWidget.equals(screenlet.getNavigationForm())) {
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            if (request != null && response != null) {
                Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
                globalCtx.put("NO_PAGINATOR", true);
                FormStringRenderer savedRenderer = (FormStringRenderer) context.get("formStringRenderer");
                HtmlFormRenderer renderer = new HtmlFormRenderer(request, response);
                renderer.setRenderPagination(false);
                context.put("formStringRenderer", renderer);
                subWidget.renderWidgetString(writer, context, this);
                context.put("formStringRenderer", savedRenderer);
            }
        } else {
            subWidget.renderWidgetString(writer, context, this);
        }
    }

    public void renderScreenletEnd(Appendable writer, Map<String, Object> context, Screenlet screenlet) throws IOException {
        writer.append("</div>");
        appendWhitespace(writer);
        writer.append("</div>");
        appendWhitespace(writer);
    }

    public static class ScreenletMenuRenderer extends HtmlMenuRenderer {
        public ScreenletMenuRenderer(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }
        @Override
        public void renderMenuOpen(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) {}
        @Override
        public void renderMenuClose(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) {}
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, Label label) throws IOException {
        String labelText = label.getText(context);
        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return;
        }
        // open tag
        String style = label.getStyle(context);
        String id = label.getId(context);
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id)) {
               writer.append("<span");

            if (UtilValidate.isNotEmpty(id)) {
                writer.append(" id=\"");
                writer.append(id);
                writer.append("\"");
            }
            if (UtilValidate.isNotEmpty(style)) {
                writer.append(" class=\"");
                writer.append(style);
                writer.append("\"");
            }
            writer.append(">");

            // the text
            writer.append(labelText);

            // close tag
               writer.append("</span>");

        } else {
            writer.append(labelText);
        }

        appendWhitespace(writer);
    }

    public void renderLink(Appendable writer, Map<String, Object> context, ScreenLink link) throws IOException {
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        String targetWindow = link.getTargetWindow(context);
        String target = link.getTarget(context);

        String uniqueItemName = link.getModelScreen().getName() + "_LF_" + UtilMisc.<String>addToBigDecimalInMap(context, "screenUniqueItemIndex", BigDecimal.ONE);

        String linkType = WidgetWorker.determineAutoLinkType(link.getLinkType(), target, link.getUrlMode(), request);
        if ("hidden-form".equals(linkType)) {
            writer.append("<form method=\"post\"");
            writer.append(" action=\"");
            // note that this passes null for the parameterList on purpose so they won't be put into the URL
            WidgetWorker.buildHyperlinkUrl(writer, target, link.getUrlMode(), null, link.getPrefix(context),
                    link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
            writer.append("\"");

            if (UtilValidate.isNotEmpty(targetWindow)) {
                writer.append(" target=\"");
                writer.append(targetWindow);
                writer.append("\"");
            }

            writer.append(" onsubmit=\"javascript:submitFormDisableSubmits(this)\"");

            writer.append(" name=\"");
            writer.append(uniqueItemName);
            writer.append("\">");

            for (Map.Entry<String, String> parameter: link.getParameterMap(context).entrySet()) {
                writer.append("<input name=\"");
                writer.append(parameter.getKey());
                writer.append("\" value=\"");
                writer.append(parameter.getValue());
                writer.append("\" type=\"hidden\"/>");
            }

            writer.append("</form>");
        }

        writer.append("<a");
        String id = link.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        String style = link.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }
        String name = link.getName(context);
        if (UtilValidate.isNotEmpty(name)) {
            writer.append(" name=\"");
            writer.append(name);
            writer.append("\"");
        }
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.append(" target=\"");
            writer.append(targetWindow);
            writer.append("\"");
        }
        if (UtilValidate.isNotEmpty(target)) {
            writer.append(" href=\"");
            if ("hidden-form".equals(linkType)) {
                writer.append("javascript:document.");
                writer.append(uniqueItemName);
                writer.append(".submit()");
            } else {
                WidgetWorker.buildHyperlinkUrl(writer, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                        link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
            }
            writer.append("\"");
        }
        writer.append(">");

        // the text
        ScreenImage img = link.getImage();
        if (img == null) {
            writer.append(link.getText(context));
        } else {
            renderImage(writer, context, img);
        }

        // close tag
        writer.append("</a>");

        appendWhitespace(writer);
    }

    public void renderImage(Appendable writer, Map<String, Object> context, ScreenImage image) throws IOException {
        // open tag
        String src = image.getSrc(context);
        if (UtilValidate.isEmpty(src)) {
            return;
        }
        writer.append("<img ");
        String id = image.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.append(" id=\"");
            writer.append(id);
            writer.append("\"");
        }
        String style = image.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.append(" class=\"");
            writer.append(style);
            writer.append("\"");
        }
        String wid = image.getWidth(context);
        if (UtilValidate.isNotEmpty(wid)) {
            writer.append(" width=\"");
            writer.append(wid);
            writer.append("\"");
        }
        String hgt = image.getHeight(context);
        if (UtilValidate.isNotEmpty(hgt)) {
            writer.append(" height=\"");
            writer.append(hgt);
            writer.append("\"");
        }
        String border = image.getBorder(context);
        if (UtilValidate.isNotEmpty(border)) {
            writer.append(" border=\"");
            writer.append(border);
            writer.append("\"");
        }
        String alt = image.getAlt(context);
        if (UtilValidate.isNotEmpty(alt)) {
            writer.append(" alt=\"");
            writer.append(alt);
            writer.append("\"");
        }

        writer.append(" src=\"");
        String urlMode = image.getUrlMode();
        boolean fullPath = false;
        boolean secure = false;
        boolean encode = false;
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
            if (request != null && response != null) {
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                String urlString = rh.makeLink(request, response, src, fullPath, secure, encode);
                writer.append(urlString);
            } else {
                writer.append(src);
            }
        } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
            if (request != null && response != null) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, newURL);
                newURL.append(src);
                writer.append(newURL.toString());
            }
        } else {
            writer.append(src);
        }

        writer.append("\"/>");

        appendWhitespace(writer);
    }

    public void renderContentBegin(Appendable writer, Map<String, Object> context, Content content) throws IOException {
        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);

        if (Debug.verboseOn()) Debug.logVerbose("directEditRequest:" + editRequest, module);

        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            writer.append("<div");
            writer.append(" class=\"").append(editContainerStyle).append("\"> ");
            appendWhitespace(writer);
        }
    }

    public void renderContentBody(Appendable writer, Map<String, Object> context, Content content) throws IOException {
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        //Boolean nullThruDatesOnly = Boolean.valueOf(false);
        String mimeTypeId = "text/html";
        String expandedContentId = content.getContentId(context);
        String expandedDataResourceId = content.getDataResourceId(context);
        String renderedContent = null;
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        Delegator delegator = (Delegator) context.get("delegator");

        // make a new map for content rendering; so our current map does not get clobbered
        Map<String, Object> contentContext = new HashMap<String, Object>();
        contentContext.putAll(context);
        String dataResourceId = (String)contentContext.get("dataResourceId");
        if (Debug.verboseOn()) Debug.logVerbose("expandedContentId:" + expandedContentId, module);

        try {
            if (UtilValidate.isNotEmpty(dataResourceId)) {
                if (WidgetDataResourceWorker.dataresourceWorker != null) {
                    renderedContent = WidgetDataResourceWorker.dataresourceWorker.renderDataResourceAsTextExt(delegator, dataResourceId, contentContext, locale, mimeTypeId, false);
                } else {
                    Debug.logError("Not rendering content, WidgetDataResourceWorker.dataresourceWorker not found.", module);
                }
            } else if (UtilValidate.isNotEmpty(expandedContentId)) {
                if (WidgetContentWorker.contentWorker != null) {
                    renderedContent = WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, expandedContentId, contentContext, locale, mimeTypeId, true);
                } else {
                    Debug.logError("Not rendering content, WidgetContentWorker.contentWorker not found.", module);
                }
            } else if (UtilValidate.isNotEmpty(expandedDataResourceId)) {
                if (WidgetDataResourceWorker.dataresourceWorker != null) {
                    renderedContent = WidgetDataResourceWorker.dataresourceWorker.renderDataResourceAsTextExt(delegator, expandedDataResourceId, contentContext, locale, mimeTypeId, false);
                } else {
                    Debug.logError("Not rendering content, WidgetDataResourceWorker.dataresourceWorker not found.", module);
                }
            }
            if (UtilValidate.isEmpty(renderedContent)) {
                String editRequest = content.getEditRequest(context);
                if (UtilValidate.isNotEmpty(editRequest)) {
                    if (WidgetContentWorker.contentWorker != null) {
                        WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, "NOCONTENTFOUND", writer, contentContext, locale, mimeTypeId, true);
                    } else {
                        Debug.logError("Not rendering content, WidgetContentWorker.contentWorker not found.", module);
                    }
                }
            } else {
                if (content.xmlEscape()) {
                    renderedContent = UtilFormatOut.encodeXmlValue(renderedContent);
                }

                writer.append(renderedContent);
            }

        } catch (GeneralException e) {
            String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e.toString();
            Debug.logError(e, errMsg, module);
            //throw new RuntimeException(errMsg);
        } catch (IOException e2) {
            String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e2.toString();
            Debug.logError(e2, errMsg, module);
            //throw new RuntimeException(errMsg);
        }
    }

    public void renderContentEnd(Appendable writer, Map<String, Object> context, Content content) throws IOException {

                //Debug.logInfo("renderContentEnd, context:" + context, module);
        String expandedContentId = content.getContentId(context);
        String editMode = "Edit";
        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        if (editRequest != null && editRequest.toUpperCase().indexOf("IMAGE") > 0) {
            editMode += " Image";
        }
        //String editRequestWithParams = editRequest + "?contentId=${currentValue.contentId}&drDataResourceId=${currentValue.drDataResourceId}&directEditRequest=${directEditRequest}&indirectEditRequest=${indirectEditRequest}&caContentIdTo=${currentValue.caContentIdTo}&caFromDate=${currentValue.caFromDate}&caContentAssocTypeId=${currentValue.caContentAssocTypeId}";

        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (request != null && response != null) {
                if (editRequest.indexOf("?") < 0)  editRequest += "?";
                else editRequest += "&amp;";
                editRequest += "contentId=" + expandedContentId;
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                writer.append("<a href=\"").append(rh.makeLink(request, response, editRequest, false, false, false)).append("\">").append(editMode).append("</a>");
            }
            if (UtilValidate.isNotEmpty(editContainerStyle)) {
                writer.append("</div>");
            }
            appendWhitespace(writer);
        }
    }

    public void renderContentFrame(Appendable writer, Map<String, Object> context, Content content) throws IOException {


        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        if (request != null && response != null) {
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
            String dataResourceId = content.getDataResourceId(context);
//          String urlString = "/content/control/ViewSimpleContent?dataResourceId=" + dataResourceId;
            String urlString = "/ViewSimpleContent?dataResourceId=" + dataResourceId;

            writer.append("<iframe src=\"").append(rh.makeLink(request, response, urlString, true, false, false)).append("\" ");
            writer.append(" width=\"").append(content.getWidth()).append("\"");
            writer.append(" height=\"").append(content.getHeight()).append("\"");
            String border = content.getBorder();
            if (UtilValidate.isNotEmpty(border)) {
                writer.append(" border=\"").append(border).append("\"");
            }
            writer.append(" />");
        }

    }

    public void renderSubContentBegin(Appendable writer, Map<String, Object> context, SubContent content) throws IOException {

        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            writer.append("<div");
            writer.append(" class=\"").append(editContainerStyle).append("\"> ");

            appendWhitespace(writer);
        }
    }

    public void renderSubContentBody(Appendable writer, Map<String, Object> context, SubContent content) throws IOException {
            Locale locale = Locale.getDefault();
            String mimeTypeId = "text/html";
            String expandedContentId = content.getContentId(context);
            String expandedMapKey = content.getMapKey(context);
            String renderedContent = null;
            LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
            Delegator delegator = (Delegator) context.get("delegator");

            // create a new map for the content rendering; so our current context does not get overwritten!
            Map<String, Object> contentContext = new HashMap<String, Object>();
            contentContext.putAll(context);

            try {
                if (WidgetContentWorker.contentWorker != null) {
                    renderedContent = WidgetContentWorker.contentWorker.renderSubContentAsTextExt(dispatcher, delegator, expandedContentId, expandedMapKey, contentContext, locale, mimeTypeId, true);
                    //Debug.logInfo("renderedContent=" + renderedContent, module);
                } else {
                    Debug.logError("Not rendering content, WidgetContentWorker.contentWorker not found.", module);
                }
                if (UtilValidate.isEmpty(renderedContent)) {
                    String editRequest = content.getEditRequest(context);
                    if (UtilValidate.isNotEmpty(editRequest)) {
                        if (WidgetContentWorker.contentWorker != null) {
                            WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, "NOCONTENTFOUND", writer, contentContext, locale, mimeTypeId, true);
                        } else {
                            Debug.logError("Not rendering content, WidgetContentWorker.contentWorker not found.", module);
                        }
                    }
                } else {
                    if (content.xmlEscape()) {
                        renderedContent = UtilFormatOut.encodeXmlValue(renderedContent);
                    }

                    writer.append(renderedContent);
                }

            } catch (GeneralException e) {
                String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e.toString();
                Debug.logError(e, errMsg, module);
                //throw new RuntimeException(errMsg);
            } catch (IOException e2) {
                String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e2.toString();
                Debug.logError(e2, errMsg, module);
                //throw new RuntimeException(errMsg);
            }
    }

    public void renderSubContentEnd(Appendable writer, Map<String, Object> context, SubContent content) throws IOException {

        String editMode = "Edit";
        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        String expandedContentId = content.getContentId(context);
        String expandedMapKey = content.getMapKey(context);
        if (editRequest != null && editRequest.toUpperCase().indexOf("IMAGE") > 0) {
            editMode += " Image";
        }
        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (request != null && response != null) {
                if (editRequest.indexOf("?") < 0)  editRequest += "?";
                else editRequest += "&amp;";
                editRequest += "contentId=" + expandedContentId;
                if (UtilValidate.isNotEmpty(expandedMapKey)) {
                    editRequest += "&amp;mapKey=" + expandedMapKey;
                }
                //HttpSession session = request.getSession();
                //GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
                /* don't know why this is here. might come to me later. -amb
                Delegator delegator = (Delegator)request.getAttribute("delegator");
                String contentIdTo = content.getContentId(context);
                String mapKey = content.getAssocName(context);
                GenericValue view = null;
                try {
                    view = ContentWorker.getSubContentCache(delegator, contentIdTo, mapKey, userLogin, null, UtilDateTime.nowTimestamp(), Boolean.valueOf(false), null);
                } catch (GenericEntityException e) {
                    throw new IOException("Originally a GenericEntityException. " + e.getMessage());
                }
                */
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                writer.append("<a href=\"").append(rh.makeLink(request, response, editRequest, false, false, false)).append("\">").append(editMode).append("</a>");
            }
            if (UtilValidate.isNotEmpty(editContainerStyle)) {
                writer.append("</div>");
            }
            appendWhitespace(writer);
        }
    }

    public void renderPortalPageBegin(Appendable writer, Map<String, Object> context, PortalPage portalPage) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageEnd(Appendable writer, Map<String, Object> context, PortalPage portalPage) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageColumnBegin(Appendable writer, Map<String, Object> context, PortalPage portalPage, GenericValue portalPageColumn) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPageColumnEnd(Appendable writer, Map<String, Object> context, PortalPage portalPage, GenericValue portalPageColumn) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletBegin(Appendable writer, Map<String, Object> context, PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletEnd(Appendable writer, Map<String, Object> context, PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }
    public void renderPortalPagePortletBody(Appendable writer, Map<String, Object> context, PortalPage portalPage, GenericValue portalPortlet) throws GeneralException, IOException {
        // TODO: not implemented
    }

    @Override
    public void renderColumnContainer(Appendable writer, Map<String, Object> context, ColumnContainer columnContainer) throws IOException {
        // TODO: not implemented
    }
}
