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
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetContentWorker;
import org.ofbiz.widget.WidgetDataResourceWorker;
import org.ofbiz.widget.WidgetWorker;
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
    private WeakHashMap<Appendable, Environment> environments = new WeakHashMap<Appendable, Environment>();
    private String rendererName;
    private int elementId = 999;

    public MacroScreenRenderer(String name, String macroLibraryPath) throws TemplateException, IOException {
        macroLibrary = FreeMarkerWorker.getTemplate(macroLibraryPath);
        rendererName = name;
    }

    @Deprecated
    public MacroScreenRenderer(String name, String macroLibraryPath, Appendable writer) throws TemplateException, IOException {
        this(name, macroLibraryPath);
    }

    private String getNextElementId() {
        elementId++;
        return "hsr" + elementId;
    }

    private void executeMacro(Appendable writer, String macro) throws IOException {
        try {
            Environment environment = getEnvironment(writer);
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

    private Environment getEnvironment(Appendable writer) throws TemplateException, IOException {
        Environment environment = environments.get(writer);
        if (environment == null) {
            Map<String, Object> input = UtilMisc.toMap("key", null);
            environment = FreeMarkerWorker.renderTemplate(macroLibrary, input, writer);
            environments.put(writer, environment);
        }
        return environment;
    }

    public String getRendererName() {
        return rendererName;
    }

    public void renderScreenBegin(Appendable writer, Map<String, Object> context) throws IOException {
        executeMacro(writer, "<@renderScreenBegin/>");
    }

    public void renderScreenEnd(Appendable writer, Map<String, Object> context) throws IOException {
        executeMacro(writer, "<@renderScreenEnd/>");
    }

    public void renderSectionBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        if (section.boundaryCommentsEnabled()) {
            StringWriter sr = new StringWriter();
            sr.append("<@renderSectionBegin ");
            sr.append("boundaryComment=\"Begin ");
            sr.append(section.isMainSection ? "Screen " : "Section Widget ");
            sr.append(section.getBoundaryCommentName());
            sr.append("\"/>");
            executeMacro(writer, sr.toString());
        }
    }
    public void renderSectionEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Section section) throws IOException {
        if (section.boundaryCommentsEnabled()) {
            StringWriter sr = new StringWriter();
            sr.append("<@renderSectionEnd ");
            sr.append("boundaryComment=\"End ");
            sr.append(section.isMainSection ? "Screen " : "Section Widget ");
            sr.append(section.getBoundaryCommentName());
            sr.append("\"/>");
            executeMacro(writer, sr.toString());
        }
    }

    public void renderContainerBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Container container) throws IOException {
        String containerId = container.getId(context);
        String autoUpdateTarget = container.getAutoUpdateTargetExdr(context);
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        String autoUpdateLink = "";
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
        sr.append("id=\"");
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
        StringWriter sr = new StringWriter();
        sr.append("<@renderLabel ");
        sr.append("text=\"");
        sr.append(labelText);
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
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        String targetWindow = link.getTargetWindow(context);
        String target = link.getTarget(context);

        String uniqueItemName = link.getModelScreen().getName() + "_LF_" + UtilMisc.<String>addToBigDecimalInMap(context, "screenUniqueItemIndex", BigDecimal.ONE);

        String linkType = WidgetWorker.determineAutoLinkType(link.getLinkType(), target, link.getUrlMode(), request);
        String linkUrl = "";
        String actionUrl = "";
        StringBuilder parameters=new StringBuilder();
        if ("hidden-form".equals(linkType)) {
            StringBuilder sb = new StringBuilder();
            WidgetWorker.buildHyperlinkUrl(sb, target, link.getUrlMode(), null, link.getPrefix(context),
                    link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
            actionUrl = sb.toString();
            parameters.append("[");
            for (Map.Entry<String, String> parameter: link.getParameterMap(context).entrySet()) {
                if (parameters.length() >1) {
                    parameters.append(",");
                }
                parameters.append("{'name':'");
                parameters.append(parameter.getKey());
                parameters.append("'");
                parameters.append(",'value':'");
                parameters.append(parameter.getValue());
                parameters.append("'}");
            }
            parameters.append("]");

        }
        String id = link.getId(context);
        String style = link.getStyle(context);
        String name = link.getName(context);
        String text = link.getText(context);
        if (UtilValidate.isNotEmpty(target)) {
            if (!"hidden-form".equals(linkType)) {
                StringBuilder sb = new StringBuilder();
                WidgetWorker.buildHyperlinkUrl(sb, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                        link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
                linkUrl = sb.toString();
            }
        }
        String imgStr = "";
        ModelScreenWidget.Image img = link.getImage();
        if (img != null) {
            StringWriter sw = new StringWriter();
            renderImage(sw, context, img);
            imgStr = sw.toString();
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderLink ");
        sr.append("parameterList=");
        sr.append(parameters.length()==0?"\"\"":parameters.toString());
        sr.append(" targetWindow=\"");
        sr.append(targetWindow);
        sr.append("\" target=\"");
        sr.append(target);
        sr.append("\" uniqueItemName=\"");
        sr.append(uniqueItemName);
        sr.append("\" linkType=\"");
        sr.append(linkType);
        sr.append("\" actionUrl=\"");
        sr.append(actionUrl);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" name=\"");
        sr.append(name);
        sr.append("\" linkUrl=\"");
        sr.append(linkUrl);
        sr.append("\" text=\"");
        sr.append(text);
        sr.append("\" imgStr=\"");
        sr.append(imgStr.replaceAll("\"", "\\\\\""));
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderImage(Appendable writer, Map<String, Object> context, ModelScreenWidget.Image image) throws IOException {
        if (image == null)
            return ;
        String src = image.getSrc(context);
        String id = image.getId(context);
        String style = image.getStyle(context);
        String wid = image.getWidth(context);
        String hgt = image.getHeight(context);
        String border = image.getBorder(context);
        String alt = image.getAlt(context);

        String urlMode = image.getUrlMode();
        boolean fullPath = false;
        boolean secure = false;
        boolean encode = false;
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        String urlString = "";
        if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
            if (request != null && response != null) {
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                urlString = rh.makeLink(request, response, src, fullPath, secure, encode);
            } else {
                urlString = src;
            }
        } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
            if (request != null && response != null) {
                StringBuilder newURL = new StringBuilder();
                ContentUrlTag.appendContentPrefix(request, newURL);
                newURL.append(src);
                urlString = newURL.toString();
            }
        } else {
            urlString = src;
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderImage ");
        sr.append("src=\"");
        sr.append(src);
        sr.append("\" id=\"");
        sr.append(id);
        sr.append("\" style=\"");
        sr.append(style);
        sr.append("\" wid=\"");
        sr.append(wid);
        sr.append("\" hgt=\"");
        sr.append(hgt);
        sr.append("\" border=\"");
        sr.append(border);
        sr.append("\" alt=\"");
        sr.append(alt);
        sr.append("\" urlString=\"");
        sr.append(urlString);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
         String editRequest = content.getEditRequest(context);
         String editContainerStyle = content.getEditContainerStyle(context);
         String enableEditName = content.getEnableEditName(context);
         String enableEditValue = (String)context.get(enableEditName);

         if (Debug.verboseOn()) Debug.logVerbose("directEditRequest:" + editRequest, module);

         StringWriter sr = new StringWriter();
         sr.append("<@renderContentBegin ");
         sr.append("editRequest=\"");
         sr.append(editRequest);
         sr.append("\" enableEditValue=\"");
         sr.append(enableEditValue);
         sr.append("\" editContainerStyle=\"");
         sr.append(editContainerStyle);
         sr.append("\" />");
         executeMacro(writer, sr.toString());
    }

    public void renderContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        //Boolean nullThruDatesOnly = Boolean.valueOf(false);
        String mimeTypeId = "text/html";
        String expandedContentId = content.getContentId(context);
        String expandedDataResourceId = content.getDataResourceId(context);
        String renderedContent = null;
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        Delegator delegator = (Delegator) context.get("delegator");

        // make a new map for content rendering; so our current map does not get clobbered
        Map<String, Object> contentContext = FastMap.newInstance();
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

    public void renderContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        String expandedContentId = content.getContentId(context);
        String editMode = "Edit";
        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        String urlString = "";
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
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                urlString = rh.makeLink(request, response, editRequest, false, false, false);
            }

            StringWriter sr = new StringWriter();
            sr.append("<@renderContentEnd ");
            sr.append("urlString=\"");
            sr.append(urlString);
            sr.append("\" editMode=\"");
            sr.append(editMode);
            sr.append("\" editContainerStyle=\"");
            sr.append(editContainerStyle);
            sr.append("\" editRequest=\"");
            sr.append(editRequest);
            sr.append("\" enableEditValue=\"");
            sr.append(enableEditValue);
            sr.append("\" />");
            executeMacro(writer, sr.toString());
        }
    }

    public void renderContentFrame(Appendable writer, Map<String, Object> context, ModelScreenWidget.Content content) throws IOException {
        String dataResourceId = content.getDataResourceId(context);
        String urlString = "/ViewSimpleContent?dataResourceId=" + dataResourceId;
        String width = content.getWidth();
        String height = content.getHeight();
        String border = content.getBorder();
        String fullUrlString = "";
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        if (request != null && response != null) {
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
            fullUrlString = rh.makeLink(request, response, urlString, true, false, false);
        }
        StringWriter sr = new StringWriter();
        sr.append("<@renderContentFrame ");
        sr.append("fullUrl=\"");
        sr.append(fullUrlString);
        sr.append("\" width=\"");
        sr.append(width);
        sr.append("\" height=\"");
        sr.append(height);
        sr.append("\" border=\"");
        sr.append(border);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

    public void renderSubContentBegin(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
         String editRequest = content.getEditRequest(context);
         String editContainerStyle = content.getEditContainerStyle(context);
         String enableEditName = content.getEnableEditName(context);
         String enableEditValue = (String)context.get(enableEditName);

         StringWriter sr = new StringWriter();
         sr.append("<@renderSubContentBegin ");
         sr.append(" editContainerStyle=\"");
         sr.append(editContainerStyle);
         sr.append("\" editRequest=\"");
         sr.append(editRequest);
         sr.append("\" enableEditValue=\"");
         sr.append(enableEditValue);
         sr.append("\" />");
         executeMacro(writer, sr.toString());
    }

    public void renderSubContentBody(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
         Locale locale = UtilMisc.ensureLocale(context.get("locale"));
         String mimeTypeId = "text/html";
         String expandedContentId = content.getContentId(context);
         String expandedMapKey = content.getMapKey(context);
         String renderedContent = "";
         LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
         Delegator delegator = (Delegator) context.get("delegator");

         // create a new map for the content rendering; so our current context does not get overwritten!
         Map<String, Object> contentContext = FastMap.newInstance();
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

    public void renderSubContentEnd(Appendable writer, Map<String, Object> context, ModelScreenWidget.SubContent content) throws IOException {
         String editMode = "Edit";
         String editRequest = content.getEditRequest(context);
         String editContainerStyle = content.getEditContainerStyle(context);
         String enableEditName = content.getEnableEditName(context);
         String enableEditValue = (String)context.get(enableEditName);
         String expandedContentId = content.getContentId(context);
         String expandedMapKey = content.getMapKey(context);
         String urlString = "";
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
                 ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                 RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                 urlString = rh.makeLink(request, response, editRequest, false, false, false);
             }
         }

         StringWriter sr = new StringWriter();
         sr.append("<@renderSubContentEnd ");
         sr.append("urlString=\"");
         sr.append(urlString);
         sr.append("\" editMode=\"");
         sr.append(editMode);
         sr.append("\" editContainerStyle=\"");
         sr.append(editContainerStyle);
         sr.append("\" editRequest=\"");
         sr.append(editRequest);
         sr.append("\" enableEditValue=\"");
         sr.append(enableEditValue);
         sr.append("\" />");
         executeMacro(writer, sr.toString());
    }


    public void renderScreenletBegin(Appendable writer, Map<String, Object> context, boolean collapsed, ModelScreenWidget.Screenlet screenlet) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        boolean javaScriptEnabled = UtilHttp.isJavaScriptEnabled(request);
        ModelScreenWidget.Menu tabMenu = screenlet.getTabMenu();
        if (tabMenu != null) {
            tabMenu.renderWidgetString(writer, context, this);
        }

        String title = screenlet.getTitle(context);
        boolean collapsible = screenlet.collapsible();
        ModelScreenWidget.Menu navMenu = screenlet.getNavigationMenu();
        ModelScreenWidget.Form navForm = screenlet.getNavigationForm();
        String collapsibleAreaId = "";
        String expandToolTip = "";
        String collapseToolTip = "";
        String fullUrlString = "";
        boolean padded = screenlet.padded();
        String menuString = "";
        boolean showMore = false;
        if (UtilValidate.isNotEmpty(title) || navMenu != null || navForm != null || collapsible) {
            showMore = true;
            if (collapsible) {
                collapsibleAreaId = this.getNextElementId();
                Map<String, Object> uiLabelMap = UtilGenerics.checkMap(context.get("uiLabelMap"));
                Map<String, Object> paramMap = UtilGenerics.checkMap(context.get("requestParameters"));
                Map<String, Object> requestParameters = new HashMap<String, Object>(paramMap);
                if (uiLabelMap != null) {
                    expandToolTip = (String) uiLabelMap.get("CommonExpand");
                    collapseToolTip = (String) uiLabelMap.get("CommonCollapse");
                }
                if (!javaScriptEnabled) {
                    requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "false");
                    String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                    fullUrlString = request.getRequestURI() + "?" + queryString;
                }
            }
            if (!collapsed) {
                StringWriter sb = new StringWriter();
                if (navMenu != null) {
                    MenuStringRenderer savedRenderer = (MenuStringRenderer) context.get("menuStringRenderer");
                    MenuStringRenderer renderer = new ScreenletMenuRenderer(request, response);
                    context.put("menuStringRenderer", renderer);
                    navMenu.renderWidgetString(sb, context, this);
                    context.put("menuStringRenderer", savedRenderer);
                } else if (navForm != null) {
                    renderScreenletPaginateMenu(sb, context, navForm);
                }
                menuString = sb.toString();
            }
        }

        StringWriter sr = new StringWriter();
        sr.append("<@renderScreenletBegin ");
        sr.append("id=\"");
        sr.append(screenlet.getId(context));
        sr.append("\" title=\"");
        sr.append(title);
        sr.append("\" collapsible=");
        sr.append(Boolean.toString(collapsible));
        sr.append(" saveCollapsed=");
        sr.append(Boolean.toString(screenlet.saveCollapsed()));
        sr.append(" collapsibleAreaId=\"");
        sr.append(collapsibleAreaId);
        sr.append("\" expandToolTip=\"");
        sr.append(expandToolTip);
        sr.append("\" collapseToolTip=\"");
        sr.append(collapseToolTip);
        sr.append("\" fullUrlString=\"");
        sr.append(fullUrlString);
        sr.append("\" padded=");
        sr.append(Boolean.toString(padded));
        sr.append(" menuString=\"");
        sr.append(menuString.replaceAll("\"", "\\\\\""));
        sr.append("\" showMore=");
        sr.append(Boolean.toString(showMore));
        sr.append(" collapsed=");
        sr.append(Boolean.toString(collapsed));
        sr.append(" javaScriptEnabled=");
        sr.append(Boolean.toString(javaScriptEnabled));
        sr.append(" />");
        executeMacro(writer, sr.toString());
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
        String viewIndexParam = modelForm.getMultiPaginateIndexField(context);
        String viewSizeParam = modelForm.getMultiPaginateSizeField(context);

        int viewIndex = modelForm.getViewIndex(context);
        int viewSize = modelForm.getViewSize(context);
        int listSize = modelForm.getListSize(context);

        int lowIndex = modelForm.getLowIndex(context);
        int highIndex = modelForm.getHighIndex(context);
        int actualPageSize = modelForm.getActualPageSize(context);

        // if this is all there seems to be (if listSize < 0, then size is unknown)
        if (actualPageSize >= listSize && listSize >= 0) return;

        // needed for the "Page" and "rows" labels
        Map<String, String> uiLabelMap = UtilGenerics.cast(context.get("uiLabelMap"));
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


        // The current screenlet title bar navigation syling requires rendering
        // these links in reverse order
        // Last button
        String paginateLastStyle = modelForm.getPaginateLastStyle();
        String paginateLastLabel = modelForm.getPaginateLastLabel(context);
        String lastLinkUrl = "";
        if (highIndex < listSize) {
            int page = (listSize / viewSize);
            linkText = prepLinkText + page + anchor;
            lastLinkUrl = rh.makeLink(request, response, linkText);
        }
        String paginateNextStyle = modelForm.getPaginateNextStyle();
        String paginateNextLabel = modelForm.getPaginateNextLabel(context);
        String nextLinkUrl = "";
        if (highIndex < listSize) {
            linkText = prepLinkText + (viewIndex + 1) + anchor;
            // - make the link
            nextLinkUrl = rh.makeLink(request, response, linkText);
        }
        String paginatePreviousStyle = modelForm.getPaginatePreviousStyle();
        String paginatePreviousLabel = modelForm.getPaginatePreviousLabel(context);
        String previousLinkUrl = "";
        if (viewIndex > 0) {
            linkText = prepLinkText + (viewIndex - 1) + anchor;
            previousLinkUrl = rh.makeLink(request, response, linkText);
        }
        String paginateFirstStyle = modelForm.getPaginateFirstStyle();
        String paginateFirstLabel = modelForm.getPaginateFirstLabel(context);
        String firstLinkUrl = "";
        if (viewIndex > 0) {
            linkText = prepLinkText + 0 + anchor;
            firstLinkUrl = rh.makeLink(request, response, linkText);
        }

        StringWriter sr = new StringWriter();
        sr.append("<@renderScreenletPaginateMenu ");
        sr.append("lowIndex=\"");
        sr.append(Integer.toString(lowIndex));
        sr.append("\" actualPageSize=\"");
        sr.append(Integer.toString(actualPageSize));
        sr.append("\" ofLabel=\"");
        sr.append(ofLabel);
        sr.append("\" listSize=\"");
        sr.append(Integer.toString(listSize));
        sr.append("\" paginateLastStyle=\"");
        sr.append(paginateLastStyle);
        sr.append("\" lastLinkUrl=\"");
        sr.append(lastLinkUrl);
        sr.append("\" paginateLastLabel=\"");
        sr.append(paginateLastLabel);
        sr.append("\" paginateNextStyle=\"");
        sr.append(paginateNextStyle);
        sr.append("\" nextLinkUrl=\"");
        sr.append(nextLinkUrl);
        sr.append("\" paginateNextLabel=\"");
        sr.append(paginateNextLabel);
        sr.append("\" paginatePreviousStyle=\"");
        sr.append(paginatePreviousStyle);
        sr.append("\" paginatePreviousLabel=\"");
        sr.append(paginatePreviousLabel);
        sr.append("\" previousLinkUrl=\"");
        sr.append(previousLinkUrl);
        sr.append("\" paginateFirstStyle=\"");
        sr.append(paginateFirstStyle);
        sr.append("\" paginateFirstLabel=\"");
        sr.append(paginateFirstLabel);
        sr.append("\" firstLinkUrl=\"");
        sr.append(firstLinkUrl);
        sr.append("\" />");
        executeMacro(writer, sr.toString());
    }

}
