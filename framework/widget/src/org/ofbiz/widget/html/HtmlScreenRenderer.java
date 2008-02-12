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
package org.ofbiz.widget.html;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.form.FormStringRenderer;
import org.ofbiz.widget.form.ModelForm;
import org.ofbiz.widget.html.HtmlWidgetRenderer;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.menu.ModelMenu;
import org.ofbiz.widget.WidgetContentWorker;
import org.ofbiz.widget.WidgetDataResourceWorker;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.ofbiz.service.LocalDispatcher;
import javolution.util.FastMap;

/**
 * Widget Library - HTML Form Renderer implementation
 */
public class HtmlScreenRenderer extends HtmlWidgetRenderer implements ScreenStringRenderer {

    public static final String module = HtmlScreenRenderer.class.getName();

    public HtmlScreenRenderer() {}

    public void renderSectionBegin(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        renderBeginningBoundaryComment(writer, section.isMainSection?"Screen":"Section Widget", section);
    }
    public void renderSectionEnd(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        renderEndingBoundaryComment(writer, section.isMainSection?"Screen":"Section Widget", section);
    }

    public void renderContainerBegin(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException {
        writer.write("<div");

        String id = container.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        
        String style = container.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        
        writer.write(">");
        appendWhitespace(writer);
    }
    public void renderContainerEnd(Writer writer, Map context, ModelScreenWidget.Container container) throws IOException {
        writer.write("</div>");
        appendWhitespace(writer);
    }

    public void renderScreenletBegin(Writer writer, Map context, boolean collapsed, ModelScreenWidget.Screenlet screenlet) throws IOException {
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        ModelScreenWidget.Menu tabMenu = screenlet.getTabMenu();
        if (tabMenu != null) {
            tabMenu.renderWidgetString(writer, context, this);
        }
        writer.write("<div class=\"screenlet\"");
        String id = screenlet.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        writer.write(">");
        appendWhitespace(writer);

        String title = screenlet.getTitle(context);
        ModelScreenWidget.Menu navMenu = screenlet.getNavigationMenu();
        ModelScreenWidget.Form navForm = screenlet.getNavigationForm();
        if (UtilValidate.isNotEmpty(title) || navMenu != null || navForm != null || screenlet.collapsible()) {
            writer.write("<div class=\"screenlet-title-bar\">");
            appendWhitespace(writer);
            writer.write("<ul>");
            appendWhitespace(writer);
            if (UtilValidate.isNotEmpty(title)) {
                writer.write("<li class=\"head3\">");
                writer.write(title);
                writer.write("</li>");
                appendWhitespace(writer);
            }
            if (screenlet.collapsible()) {
                String toolTip = null;
                Map uiLabelMap = (Map) context.get("uiLabelMap");
                Map requestParameters = new HashMap((Map)context.get("requestParameters"));
                writer.write("<li class=\"");
                if (collapsed) {
                    requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "false");
                    String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                    writer.write("collapsed\"><a href=\"");
                    writer.write(request.getRequestURI() + "?" + queryString);
                    if (uiLabelMap != null) {
                        toolTip = (String) uiLabelMap.get("CommonExpand");
                    }
                } else {
                    requestParameters.put(screenlet.getPreferenceKey(context) + "_collapsed", "true");
                    String queryString = UtilHttp.urlEncodeArgs(requestParameters);
                    writer.write("expanded\"><a href=\"");
                    writer.write(request.getRequestURI() + "?" + queryString);
                    if (uiLabelMap != null) {
                        toolTip = (String) uiLabelMap.get("CommonCollapse");
                    }
                }
                writer.write("\"");
                if (UtilValidate.isNotEmpty(toolTip)) {
                    writer.write(" title=\"" + toolTip + "\"");
                }
                writer.write(">&nbsp</a></li>");
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
            writer.write("</ul>");
            appendWhitespace(writer);
            writer.write("<br class=\"clear\" />");
            appendWhitespace(writer);
            writer.write("</div>");
            appendWhitespace(writer);
            if (screenlet.padded()) {
                writer.write("<div class=\"screenlet-body\">");
                appendWhitespace(writer);
            }
        }
    }
    
    protected void renderScreenletPaginateMenu(Writer writer, Map context, ModelScreenWidget.Form form) throws IOException {
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
        String pageLabel = "";
        String rowsLabel = "";
        String ofLabel = "";
        if (uiLabelMap == null) {
            Debug.logWarning("Could not find uiLabelMap in context", module);
        } else {
            pageLabel = (String) uiLabelMap.get("CommonPage");
            rowsLabel = (String) uiLabelMap.get("CommonRows");
            ofLabel = (String) uiLabelMap.get("CommonOf");
            ofLabel = ofLabel.toLowerCase();
        }

        // for legacy support, the viewSizeParam is VIEW_SIZE and viewIndexParam is VIEW_INDEX when the fields are "viewSize" and "viewIndex"
        if (viewIndexParam.equals("viewIndex")) viewIndexParam = "VIEW_INDEX";
        if (viewSizeParam.equals("viewSize")) viewSizeParam = "VIEW_SIZE";

        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");

        Map inputFields = (Map) context.get("requestParameters");
        // strip out any multi form fields if the form is of type multi
        if (modelForm.getType().equals("multi")) {
            inputFields = UtilHttp.removeMultiFormParameters(inputFields);
        }
        String queryString = UtilHttp.urlEncodeArgs(inputFields);
        // strip legacy viewIndex/viewSize params from the query string
        queryString = UtilHttp.stripViewParamsFromQueryString(queryString);
        // strip parametrized index/size params from the query string
        HashSet paramNames = new HashSet();
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
        writer.write("<li class=\"" + modelForm.getPaginateLastStyle());
        if (highIndex < listSize) {
            writer.write("\"><a href=\"");
            int page = (listSize / viewSize) - 1;
            linkText = prepLinkText + page + anchor;
            // - make the link
            writer.write(rh.makeLink(request, response, linkText));
            writer.write("\">" + modelForm.getPaginateLastLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write(" disabled\">" + modelForm.getPaginateLastLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);
        // Next button
        writer.write("<li class=\"" + modelForm.getPaginateNextStyle());
        if (highIndex < listSize) {
            writer.write("\"><a href=\"");
            linkText = prepLinkText + (viewIndex + 1) + anchor;
            // - make the link
            writer.write(rh.makeLink(request, response, linkText));
            writer.write("\">" + modelForm.getPaginateNextLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write(" disabled\">" + modelForm.getPaginateNextLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);
        if (listSize > 0) {
            writer.write("<li>");
            writer.write((lowIndex + 1) + " - " + (lowIndex + actualPageSize ) + " " + ofLabel + " " + listSize);
            writer.write("</li>");
            appendWhitespace(writer);
        }
        // Previous button
        writer.write("<li class=\"nav-previous");
        if (viewIndex > 0) {
            writer.write("\"><a href=\"");
            linkText = prepLinkText + (viewIndex - 1) + anchor;
            // - make the link
            writer.write(rh.makeLink(request, response, linkText));
            writer.write("\">" + modelForm.getPaginatePreviousLabel(context) + "</a>");
        } else {
            // disabled button
            writer.write(" disabled\">" + modelForm.getPaginatePreviousLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);
        // First button
        writer.write("<li class=\"nav-first");
        if (viewIndex > 0) {
            writer.write("\"><a href=\"");
            linkText = prepLinkText + 0 + anchor;
            writer.write(rh.makeLink(request, response, linkText));
            writer.write("\">" + modelForm.getPaginateFirstLabel(context) + "</a>");
        } else {
            writer.write(" disabled\">" + modelForm.getPaginateFirstLabel(context));
        }
        writer.write("</li>");
        appendWhitespace(writer);
    }
    
    public void renderScreenletSubWidget(Writer writer, Map context, ModelScreenWidget subWidget, ModelScreenWidget.Screenlet screenlet) throws GeneralException {
        if (subWidget.equals(screenlet.getNavigationForm())) {
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            if (request != null && response != null) {
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

    public void renderScreenletEnd(Writer writer, Map context, ModelScreenWidget.Screenlet screenlet) throws IOException {
        if (screenlet.padded()) {
            writer.write("</div>");
            appendWhitespace(writer);
        }
        writer.write("</div>");
        appendWhitespace(writer);
    }

    public static class ScreenletMenuRenderer extends HtmlMenuRenderer {
        public ScreenletMenuRenderer(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }
        public void renderMenuOpen(StringBuffer buffer, Map context, ModelMenu modelMenu) {}
        public void renderMenuClose(StringBuffer buffer, Map context, ModelMenu modelMenu) {}
    }

    public void renderLabel(Writer writer, Map context, ModelScreenWidget.Label label) throws IOException {
        String labelText = label.getText(context);
        if (UtilValidate.isEmpty(labelText)) {
            // nothing to render
            return;
        }
        // open tag
        String style = label.getStyle(context);
        String id = label.getId(context);
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(id) ) {
               writer.write("<span");
            
            if (UtilValidate.isNotEmpty(id)) {
                writer.write(" id=\"");
                writer.write(id);
                writer.write("\"");
            }
            if (UtilValidate.isNotEmpty(style)) {
                writer.write(" class=\"");
                writer.write(style);
                writer.write("\"");
            }
            writer.write(">");
            
            // the text
            writer.write(labelText);
            
            // close tag
               writer.write("</span>");
            
        } else {
            writer.write(labelText);
        }
        
        appendWhitespace(writer);
    }

    public void renderLink(Writer writer, Map context, ModelScreenWidget.Link link) throws IOException {
        // open tag
        writer.write("<a");
        String id = link.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        String style = link.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        String name = link.getName(context);
        if (UtilValidate.isNotEmpty(name)) {
            writer.write(" name=\"");
            writer.write(name);
            writer.write("\"");
        }
        String targetWindow = link.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            writer.write(" target=\"");
            writer.write(targetWindow);
            writer.write("\"");
        }
        String target = link.getTarget(context);
        if (UtilValidate.isNotEmpty(target)) {
            writer.write(" href=\"");
            String urlMode = link.getUrlMode();
            String prefix = link.getPrefix(context);
            boolean fullPath = link.getFullPath();
            boolean secure = link.getSecure();
            boolean encode = link.getEncode();
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
                if (request != null && response != null) {
                    ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                    RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                    String urlString = rh.makeLink(request, response, target, fullPath, secure, encode);
                    writer.write(urlString);
                } else if (prefix != null) {
                    writer.write(prefix + target);
                } else {
                    writer.write(target);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                if (request != null && response != null) {
                    StringBuffer newURL = new StringBuffer();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    newURL.append(target);
                    writer.write(newURL.toString());
                }
            } else {
                writer.write(target);
            }

            writer.write("\"");
        }
        writer.write(">");
        
        // the text
        ModelScreenWidget.Image img = link.getImage();
        if (img == null)
            writer.write(link.getText(context));
        else
            renderImage(writer, context, img);
        
        // close tag
        writer.write("</a>");
        
        appendWhitespace(writer);
    }

    public void renderImage(Writer writer, Map context, ModelScreenWidget.Image image) throws IOException {
        // open tag
        writer.write("<img ");
        String id = image.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            writer.write(" id=\"");
            writer.write(id);
            writer.write("\"");
        }
        String style = image.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
        String wid = image.getWidth(context);
        if (UtilValidate.isNotEmpty(wid)) {
            writer.write(" width=\"");
            writer.write(wid);
            writer.write("\"");
        }
        String hgt = image.getHeight(context);
        if (UtilValidate.isNotEmpty(hgt)) {
            writer.write(" height=\"");
            writer.write(hgt);
            writer.write("\"");
        }
        String border = image.getBorder(context);
        if (UtilValidate.isNotEmpty(border)) {
            writer.write(" border=\"");
            writer.write(border);
            writer.write("\"");
        }
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src)) {
            writer.write(" src=\"");
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
                    writer.write(urlString);
                } else {
                    writer.write(src);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                if (request != null && response != null) {
                    StringBuffer newURL = new StringBuffer();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    newURL.append(src);
                    writer.write(newURL.toString());
                }
            } else {
                writer.write(src);
            }

            writer.write("\"");
        }
        writer.write("/>");
        
        
        appendWhitespace(writer);
    }

    public void renderContentBegin(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        
        if (Debug.verboseOn()) Debug.logVerbose("directEditRequest:" + editRequest, module);
        
        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            writer.write("<div");
            writer.write(" class=\"" + editContainerStyle + "\"> ");
            appendWhitespace(writer);
        }
    }

    public void renderContentBody(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        Locale locale = UtilMisc.ensureLocale(context.get("locale"));
        //Boolean nullThruDatesOnly = new Boolean(false);
        String mimeTypeId = "text/html";
        String expandedContentId = content.getContentId(context);
        String expandedDataResourceId = content.getDataResourceId(context);
        String renderedContent = null;
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        GenericDelegator delegator = (GenericDelegator) context.get("delegator");

        // make a new map for content rendering; so our current map does not get clobbered
        Map contentContext = FastMap.newInstance();
        contentContext.putAll(context);
        String dataResourceId = (String)contentContext.get("dataResourceId");
        if (Debug.verboseOn()) Debug.logVerbose("expandedContentId:" + expandedContentId, module);
        
        try {
            if (UtilValidate.isNotEmpty(dataResourceId)) {
                if (WidgetDataResourceWorker.dataresourceWorker != null) {
                    renderedContent = WidgetDataResourceWorker.dataresourceWorker.renderDataResourceAsTextExt(delegator, dataResourceId, contentContext, locale, mimeTypeId, false);
                } else {
                    Debug.logError("Not rendering content, not WidgetDataResourceWorker.dataresourceWorker found.", module);
                }
            } else if (UtilValidate.isNotEmpty(expandedContentId)) {
                if (WidgetContentWorker.contentWorker != null) {
                    renderedContent = WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, expandedContentId, contentContext, locale, mimeTypeId, true);
                } else {
                    Debug.logError("Not rendering content, not ContentWorker found.", module);
                }
            } else if (UtilValidate.isNotEmpty(expandedDataResourceId)) {
                if (WidgetDataResourceWorker.dataresourceWorker != null) {
                    renderedContent = WidgetDataResourceWorker.dataresourceWorker.renderDataResourceAsTextExt(delegator, expandedDataResourceId, contentContext, locale, mimeTypeId, false);
                } else {
                    Debug.logError("Not rendering content, not WidgetDataResourceWorker.dataresourceWorker found.", module);
                }
            }
            if (UtilValidate.isEmpty(renderedContent)) {
                String editRequest = content.getEditRequest(context);
                if (UtilValidate.isNotEmpty(editRequest)) {
                    if (WidgetContentWorker.contentWorker != null) {
                        WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, "NOCONTENTFOUND", writer, contentContext, locale, mimeTypeId, true);
                    } else {
                        Debug.logError("Not rendering content, not ContentWorker found.", module);
                    }
                }
            } else {
                if (content.xmlEscape()) {
                    renderedContent = UtilFormatOut.encodeXmlValue(renderedContent);
                }
                
                writer.write(renderedContent);
            }

        } catch(GeneralException e) {
            String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e.toString();
            Debug.logError(e, errMsg, module);
            //throw new RuntimeException(errMsg);
        } catch(IOException e2) {
            String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e2.toString();
            Debug.logError(e2, errMsg, module);
            //throw new RuntimeException(errMsg);
        }
    }

    public void renderContentEnd(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {

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
                String urlString = rh.makeLink(request, response, editRequest, false, false, false);
                String linkString = "<a href=\"" + urlString + "\">" + editMode + "</a>";
                writer.write(linkString);
            }
            if (UtilValidate.isNotEmpty(editContainerStyle)) {
                writer.write("</div>");
            }
            appendWhitespace(writer);
        }
    }

    public void renderContentFrame(Writer writer, Map context, ModelScreenWidget.Content content) throws IOException {
        
        String dataResourceId = content.getDataResourceId(context);
//        String urlString = "/content/control/ViewSimpleContent?dataResourceId=" + dataResourceId;
        String urlString = "/ViewSimpleContent?dataResourceId=" + dataResourceId;
        
        String width = content.getWidth();
        String widthString=" width=\"" + width + "\"";
        String height = content.getHeight();
        String heightString=" height=\"" + height + "\"";
        String border = content.getBorder();
        String borderString = (UtilValidate.isNotEmpty(border)) ? " border=\"" + border + "\"" : "";
        
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        HttpServletResponse response = (HttpServletResponse) context.get("response");
        if (request != null && response != null) {
            ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
            RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
            String fullUrlString = rh.makeLink(request, response, urlString, true, false, false);
            String linkString = "<iframe src=\"" + fullUrlString + "\" " + widthString + heightString + borderString + " />";
            writer.write(linkString);
        }
        
    }

    public void renderSubContentBegin(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {

        String editRequest = content.getEditRequest(context);
        String editContainerStyle = content.getEditContainerStyle(context);
        String enableEditName = content.getEnableEditName(context);
        String enableEditValue = (String)context.get(enableEditName);
        if (UtilValidate.isNotEmpty(editRequest) && "true".equals(enableEditValue)) {
            writer.write("<div");
            writer.write(" class=\"" + editContainerStyle + "\"> ");
    
            appendWhitespace(writer);
        }
    }

    public void renderSubContentBody(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {
            Locale locale = Locale.getDefault();
            String mimeTypeId = "text/html";
            String expandedContentId = content.getContentId(context);
            String expandedMapKey = content.getMapKey(context);
            String renderedContent = null;
            LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
            GenericDelegator delegator = (GenericDelegator) context.get("delegator");

            // create a new map for the content rendering; so our current context does not get overwritten!
            Map contentContext = FastMap.newInstance();
            contentContext.putAll(context);

            try {
                if (WidgetContentWorker.contentWorker != null) {
                    renderedContent = WidgetContentWorker.contentWorker.renderSubContentAsTextExt(dispatcher, delegator, expandedContentId, expandedMapKey, contentContext, locale, mimeTypeId, true);
                    //Debug.logInfo("renderedContent=" + renderedContent, module);
                } else {
                    Debug.logError("Not rendering content, not ContentWorker found.", module);
                }
                if (UtilValidate.isEmpty(renderedContent)) {
                    String editRequest = content.getEditRequest(context);
                    if (UtilValidate.isNotEmpty(editRequest)) {
                        if (WidgetContentWorker.contentWorker != null) {
                            WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, "NOCONTENTFOUND", writer, contentContext, locale, mimeTypeId, true);
                        } else {
                            Debug.logError("Not rendering content, ContentWorker not found.", module);
                        }
                    }
                } else {
                    if (content.xmlEscape()) {
                        renderedContent = UtilFormatOut.encodeXmlValue(renderedContent);
                    }
                        
                    writer.write(renderedContent);
                }

            } catch(GeneralException e) {
                String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e.toString();
                Debug.logError(e, errMsg, module);
                //throw new RuntimeException(errMsg);
            } catch(IOException e2) {
                String errMsg = "Error rendering included content with id [" + expandedContentId + "] : " + e2.toString();
                Debug.logError(e2, errMsg, module);
                //throw new RuntimeException(errMsg);
            }
    }

    public void renderSubContentEnd(Writer writer, Map context, ModelScreenWidget.SubContent content) throws IOException {

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
                HttpSession session = request.getSession();
                GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
                /* don't know why this is here. might come to me later. -amb
                GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
                String contentIdTo = content.getContentId(context);
                String mapKey = content.getAssocName(context);
                GenericValue view = null;
                try {
                    view = ContentWorker.getSubContentCache(delegator, contentIdTo, mapKey, userLogin, null, UtilDateTime.nowTimestamp(), new Boolean(false), null);
                } catch(GenericEntityException e) {
                    throw new IOException("Originally a GenericEntityException. " + e.getMessage());
                }
                */
                ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
                RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                String urlString = rh.makeLink(request, response, editRequest, false, false, false);
                String linkString = "<a href=\"" + urlString + "\">" + editMode + "</a>";
                writer.write(linkString);
            }
            if (UtilValidate.isNotEmpty(editContainerStyle)) {
                writer.write("</div>");
            }
            appendWhitespace(writer);
        }
    }
}
