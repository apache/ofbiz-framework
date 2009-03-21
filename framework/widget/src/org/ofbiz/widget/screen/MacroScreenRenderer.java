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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.FileUtil;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.html.HtmlFormRenderer;
import org.ofbiz.widget.html.HtmlScreenRenderer.ScreenletMenuRenderer;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.screen.ScreenStringRenderer;

import freemarker.core.Environment;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class MacroScreenRenderer implements ScreenStringRenderer {

    public static final String module = MacroScreenRenderer.class.getName();
    private Template macroLibrary;
    private Environment environment;
    private int elementId = 999;
 
    public MacroScreenRenderer(String macroLibraryPath, Appendable writer) throws TemplateException, IOException {
        macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        Map<String, Object> input = UtilMisc.toMap("key", null);
        environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
    }

    private String getNextElementId() {
        elementId++;
        return "hsr" + elementId;
    }

    private void executeMacro(Appendable writer, String macro) throws IOException {
        try {
            Reader templateReader = new StringReader(macro);
            // FIXME: I am using a Date as an hack to provide a unique name for the template...
            Template template = new Template((new java.util.Date()).toString(), templateReader, FreeMarkerWorker.getDefaultOfbizConfig());
            templateReader.close();
            environment.include(template);
        } catch (TemplateException e) {
            Debug.logError(e, "Error rendering screen thru ftl", module);
        } catch (IOException e) {
            Debug.logError(e, "Error rendering screen thru ftl", module);
        }
    }

    public void renderSectionBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        // TODO: not implemented FIXME
    }
    public void renderSectionEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        // TODO: not implemented FIXME
    }

    public void renderContainerBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
        String containerId = container.getId(context);
        String autoUpdateTarget = container.getAutoUpdateTargetExdr(context);
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        String autoUpdateLink = null;
        if (UtilValidate.isNotEmpty(autoUpdateTarget) && UtilHttp.isJavaScriptEnabled(request)) {
            if (UtilValidate.isEmpty(containerId)) {
                containerId = getNextElementId();
            }
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
            autoUpdateLink = rh.makeLink(request, response, autoUpdateTarget);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderContainerBegin ");
        sr.append("containerId=\"");
        sr.append(containerId);
        sr.append("\" style=\"");
        sr.append(container.getStyle(context));
        sr.append("\" autoUpdateLink=\"");
        sr.append(autoUpdateLink);
        sr.append("\" autoUpdateInterval=\"");
        sr.append(container.getAutoUpdateInterval());
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderContainerEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderContainerEnd/>");
        executeMacro(writer, sr.toString());
    }

    public void renderLabel(Appendable writer, Map<String, Object> context, ModelScreenWidget.Label label) throws IOException {
        String labelText = label.getText(context);
        String style = label.getStyle(context);
        String id = label.getId(context);
        StringWriter sr = new StringWriter();
        sr.append("<@renderLabel ");
        sr.append("text=\"");
        sr.append(label.getText(context));
        sr.append("\" id=\"");
        sr.append(label.getId(context));
        sr.append("\" style=\"");
        sr.append(label.getStyle(context));
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderHorizontalSeparator(Appendable writer, Map<String, Object> context, ModelScreenWidget.HorizontalSeparator separator) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderHorizontalSeparator ");
        sr.append("id=\"");
        sr.append(separator.getId(context));
        sr.append("\" style=\"");
        sr.append(separator.getStyle(context));
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderLink(Appendable writer, Map<String, Object> context, ModelScreenWidget.Link link) throws IOException {
        // TODO: not implemented
    }

    public void renderImage(Appendable writer, Map<String, Object> context, ModelScreenWidget.Image image) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderContentFrame(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void renderSubContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
        // TODO: not implemented
    }

    public void appendWhitespace(Appendable writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.append("\r\n");
    }
    public void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, ModelScreenWidget.Screenlet screenlet) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        boolean javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        ModelScreenWidget.Menu tabMenu = screenlet.getTabMenu();
        if (tabMenu != null) {
            tabMenu.renderWidgetString(writer, context, this);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderScreenletBegin ");
        sr.append("id=\"");
        sr.append(screenlet.getId(context));
        sr.append("\" />");
        executeMacro(writer, sr.toString());

        String title = screenlet.getTitle(context);
        boolean collapsible = screenlet.collapsible();
        ModelScreenWidget.Menu navMenu = screenlet.getNavigationMenu();
        ModelScreenWidget.Form navForm = screenlet.getNavigationForm();
        String collapsibleAreaId = null;
        if (UtilValidate.isNotEmpty(title) || navMenu != null || navForm != null || collapsible) {
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
            if (collapsible) {
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
                        writer.append("onclick=\"javascript:toggleScreenlet(this, '" + collapsibleAreaId + "', '" + expandToolTip + "', '" + collapseToolTip + "');\"");
                    } else {
                        requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "false");
                        String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                        writer.append("href=\"" + request.getRequestURI() + "?" + queryString + "\"");
                    }
                    if (UtilValidate.isNotEmpty(expandToolTip)) {
                        writer.append(" title=\"" + expandToolTip + "\"");
                    }
                } else {
                    writer.append("expanded\"><a ");
                    if (javaScriptEnabled) {
                        writer.append("onclick=\"javascript:toggleScreenlet(this, '" + collapsibleAreaId + "', '" + expandToolTip + "', '" + collapseToolTip + "');\"");
                    } else {
                        requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "true");
                        String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                        writer.append("href=\"" + request.getRequestURI() + "?" + queryString + "\"");
                    }
                    if (UtilValidate.isNotEmpty(collapseToolTip)) {
                        writer.append(" title=\"" + collapseToolTip + "\"");
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
                writer.append(" id=\"" + collapsibleAreaId + "\"");
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

    public void renderScreenletSubWidget(Appendable writer, Map<String, Object> context, ModelScreenWidget subWidget, ModelScreenWidget.Screenlet screenlet) throws GeneralException, IOException  {
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
    public void renderScreenletEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Screenlet screenlet) throws IOException {
        StringWriter sr = new StringWriter();
        sr.append("<@renderScreenletEnd/>");
        executeMacro(writer, sr.toString());
    }

    protected void renderScreenletPaginateMenu(Appendable writer, Map<String, Object> context, ModelScreenWidget.Form form) throws IOException {
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        ModelForm modelForm = form.getModelForm(context);
        modelForm.runFormActions(context);
        modelForm.preparePager(context);
        String targetService = modelForm.getPaginateTarget(context);
        if (targetService == null) {
            targetService = "${targetService}";
        }

        // get the parametrized pagination index and size fields
        int paginatorNumber = modelForm.getPaginatorNumber(context);
        String viewIndexParam = modelForm.getPaginateIndexField(context);
        String viewSizeParam = modelForm.getPaginateSizeField(context);

        int viewIndex = modelForm.getViewIndex(context);
        int viewSize = modelForm.getViewSize(context);
        int listSize = modelForm.getListSize(context);

        int lowIndex = modelForm.getLowIndex(context);
        int highIndex = modelForm.getHighIndex(context);
        int actualPageSize = modelForm.getActualPageSize(context);

        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize >= 0) return;

        // needed for the "Page" and "rows" labels
        Map uiLabelMap = (Map) context.get("uiLabelMap");
        String ofLabel = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        } else {
            ofLabel = (String) uiLabelMap.get("CommonOf");
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
        String prepLinkText = "";
        prepLinkText = targetService;
        if (prepLinkText.indexOf("?") < 0) {
            prepLinkText += "?";
        } else if (!prepLinkText.endsWith("?")) {
            prepLinkText += "&amp;";
        }
        if (!UtilValidate.isEmpty(queryString) && !queryString.equals("null")) {
            prepLinkText += queryString + "&amp;";
        }
        prepLinkText += viewSizeParam + "=" + viewSize + "&amp;" + viewIndexParam + "=";

        String linkText;

        appendWhitespace(writer);
        // The current screenlet title bar navigation syling requires rendering
        // these links in reverse order
        // Last button
        writer.append("<li class=\"" + modelForm.getPaginateLastStyle());
        if (highIndex < listSize) {
            writer.append("\"><a href=\"");
            int page = (listSize / viewSize) - 1;
            linkText = prepLinkText + page + anchor;
            // - make the link
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">" + modelForm.getPaginateLastLabel(context) + "</a>");
        } else {
            // disabled button
            writer.append(" disabled\">" + modelForm.getPaginateLastLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        // Next button
        writer.append("<li class=\"" + modelForm.getPaginateNextStyle());
        if (highIndex < listSize) {
            writer.append("\"><a href=\"");
            linkText = prepLinkText + (viewIndex + 1) + anchor;
            // - make the link
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">" + modelForm.getPaginateNextLabel(context) + "</a>");
        } else {
            // disabled button
            writer.append(" disabled\">" + modelForm.getPaginateNextLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        if (listSize > 0) {
            writer.append("<li>");
            writer.append((lowIndex + 1) + " - " + (lowIndex + actualPageSize ) + " " + ofLabel + " " + listSize);
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
            writer.append("\">" + modelForm.getPaginatePreviousLabel(context) + "</a>");
        } else {
            // disabled button
            writer.append(" disabled\">" + modelForm.getPaginatePreviousLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
        // First button
        writer.append("<li class=\"nav-first");
        if (viewIndex > 0) {
            writer.append("\"><a href=\"");
            linkText = prepLinkText + 0 + anchor;
            writer.append(rh.makeLink(request, response, linkText));
            writer.append("\">" + modelForm.getPaginateFirstLabel(context) + "</a>");
        } else {
            writer.append(" disabled\">" + modelForm.getPaginateFirstLabel(context));
        }
        writer.append("</li>");
        appendWhitespace(writer);
    }

}
