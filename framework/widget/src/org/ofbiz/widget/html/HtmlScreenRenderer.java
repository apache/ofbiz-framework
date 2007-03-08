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
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetContentWorker;
import org.ofbiz.widget.screen.ModelScreenWidget;
import org.ofbiz.widget.screen.ScreenStringRenderer;
import org.ofbiz.service.LocalDispatcher;
import javolution.util.FastMap;

/**
 * Widget Library - HTML Form Renderer implementation
 */
public class HtmlScreenRenderer implements ScreenStringRenderer {

    public static final String module = HtmlScreenRenderer.class.getName();

    public HtmlScreenRenderer() {}

    public void renderSectionBegin(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        // do nothing, this is just a place holder container for HTML
    }
    public void renderSectionEnd(Writer writer, Map context, ModelScreenWidget.Section section) throws IOException {
        // do nothing, this is just a place holder container for HTML
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
        String renderedContent = null;
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        GenericDelegator delegator = (GenericDelegator) context.get("delegator");

        // make a new map for content rendering; so our current map does not get clobbered
        Map contentContext = FastMap.newInstance();
        contentContext.putAll(context);

        if (Debug.verboseOn()) Debug.logVerbose("expandedContentId:" + expandedContentId, module);
        
        try {
            if (UtilValidate.isNotEmpty(expandedContentId)) {
                if (WidgetContentWorker.contentWorker != null) {
                    renderedContent = WidgetContentWorker.contentWorker.renderContentAsTextExt(dispatcher, delegator, expandedContentId, contentContext, locale, mimeTypeId, true);
                } else {
                    Debug.logError("Not rendering content, not ContentWorker found.", module);
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

    public void appendWhitespace(Writer writer) throws IOException {
        // appending line ends for now, but this could be replaced with a simple space or something
        writer.write("\r\n");
        //writer.write(' ');
    }
}
