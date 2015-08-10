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
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilCodec;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.WidgetWorker;
import org.ofbiz.widget.model.CommonWidgetModels.Image;
import org.ofbiz.widget.model.ModelMenu;
import org.ofbiz.widget.model.ModelMenuItem;
import org.ofbiz.widget.model.ModelMenuItem.MenuLink;
import org.ofbiz.widget.model.ModelWidget;
import org.ofbiz.widget.renderer.MenuStringRenderer;

/**
 * Widget Library - HTML Menu Renderer implementation
 */
public class HtmlMenuRenderer extends HtmlWidgetRenderer implements MenuStringRenderer {

    HttpServletRequest request;
    HttpServletResponse response;
    protected String userLoginIdAtPermGrant;
    protected String permissionErrorMessage = "";

    public static final String module = HtmlMenuRenderer.class.getName();

    protected HtmlMenuRenderer() {}

    public HtmlMenuRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void appendOfbizUrl(Appendable writer, String location) throws IOException {
        ServletContext ctx = (ServletContext) request.getAttribute("servletContext");
        if (ctx == null) {
            //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl, ctx is null(0): buffer=" + buffer.toString() + " location:" + location, "");
            HttpSession session = request.getSession();
            if (session != null) {
                ctx = session.getServletContext();
            } else {
                //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl, session is null(1)", "");
            }
            if (ctx == null) {
                throw new RuntimeException("ctx is null. location:" + location);
            }
                //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl, ctx is NOT null(2)", "");
        }
        Delegator delegator = (Delegator)request.getAttribute("delegator");
        if (delegator == null) {
                //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl, delegator is null(5)", "");
        }
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        String s = rh.makeLink(this.request, this.response, location);
        if (s.indexOf("null") >= 0) {
            //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl(3), url: " + s, "");
        }
        writer.append(s);
    }

    public void appendContentUrl(Appendable writer, String location) throws IOException {
        ServletContext ctx = (ServletContext) this.request.getAttribute("servletContext");
        if (ctx == null) {
            //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, ctx is null(0): buffer=" + buffer.toString() + " location:" + location, "");
            HttpSession session = request.getSession();
            if (session != null) {
                ctx = session.getServletContext();
            } else {
                //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, session is null(1)", "");
            }
            if (ctx == null) {
                throw new RuntimeException("ctx is null. location:" + location);
            }
            //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, ctx is NOT null(2)", "");
            this.request.setAttribute("servletContext", ctx);
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
                //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, delegator is null(6)", "");
        }
        StringBuilder buffer = new StringBuilder();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    public void appendTooltip(Appendable writer, Map<String, Object> context, ModelMenuItem modelMenuItem) throws IOException {
        // render the tooltip
        String tooltip = modelMenuItem.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            writer.append("<span class=\"");
            String tooltipStyle = modelMenuItem.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                writer.append(tooltipStyle);
            } else {
                writer.append("tooltip");
            }
            writer.append("\"");
            writer.append(tooltip);
            writer.append("</span>");
        }
    }

    public void renderFormatSimpleWrapperRows(Appendable writer, Map<String, Object> context, Object menuObj) throws IOException {
        List<ModelMenuItem> menuItemList = ((ModelMenu) menuObj).getMenuItemList();
        for (ModelMenuItem currentMenuItem: menuItemList) {
            renderMenuItem(writer, context, currentMenuItem);
        }
    }

    public void renderMenuItem(Appendable writer, Map<String, Object> context, ModelMenuItem menuItem) throws IOException {

        //Debug.logInfo("in renderMenuItem, menuItem:" + menuItem.getName() + " context:" + context ,"");
        boolean hideThisItem = isHideIfSelected(menuItem, context);
        //if (Debug.infoOn()) Debug.logInfo("in HtmlMenuRendererImage, hideThisItem:" + hideThisItem,"");
        if (hideThisItem)
            return;

        String style = menuItem.getWidgetStyle();

        if (menuItem.isSelected(context)) {
            style = menuItem.getSelectedStyle();
            if (UtilValidate.isEmpty(style)) {
                style = "selected";
            }
        }

        if (this.isDisableIfEmpty(menuItem, context)) {
            style = menuItem.getDisabledTitleStyle();
        }

        writer.append("  <li");
        String alignStyle = menuItem.getAlignStyle();
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(alignStyle)) {
            writer.append(" class=\"");
            if (UtilValidate.isNotEmpty(style)) {
                writer.append(style).append(" ");
            }
            if (UtilValidate.isNotEmpty(alignStyle)) {
                writer.append(alignStyle);
            }
            writer.append("\"");
        }
        String toolTip = menuItem.getTooltip(context);
        if (UtilValidate.isNotEmpty(toolTip)) {
            writer.append(" title=\"").append(toolTip).append("\"");
        }
        writer.append(">");

        MenuLink link = menuItem.getLink();
        //if (Debug.infoOn()) Debug.logInfo("in HtmlMenuRendererImage, link(0):" + link,"");
        if (link != null) {
            renderLink(writer, context, link);
        } else {
            String txt = menuItem.getTitle(context);
            UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
            if (simpleEncoder != null) {
                txt = simpleEncoder.encode(txt);
            }
            writer.append(txt);

        }
        if (!menuItem.getMenuItemList().isEmpty()) {
            appendWhitespace(writer);
            writer.append("    <ul>");
            appendWhitespace(writer);
            for (ModelMenuItem childMenuItem : menuItem.getMenuItemList()) {
                childMenuItem.renderMenuItemString(writer, context, this);
            }
            writer.append("    </ul>");
            appendWhitespace(writer);
        }

        writer.append("</li>");

        appendWhitespace(writer);
    }

    public boolean isDisableIfEmpty(ModelMenuItem menuItem, Map<String, Object> context) {
        boolean disabled = false;
        String disableIfEmpty = menuItem.getDisableIfEmpty();
        if (UtilValidate.isNotEmpty(disableIfEmpty)) {
            List<String> keys = StringUtil.split(disableIfEmpty, "|");
            for (String key: keys) {
                Object obj = context.get(key);
                if (obj == null) {
                    disabled = true;
                    break;
                }
            }
        }
        return disabled;
    }

/*
    public String buildDivStr(ModelMenuItem menuItem, Map<String, Object> context) {
        String divStr = "";
        divStr =  menuItem.getTitle(context);
        return divStr;
    }
*/
    public void renderMenuOpen(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {

            //Debug.logInfo("in HtmlMenuRenderer, userLoginIdHasChanged:" + userLoginIdHasChanged,"");
        this.widgetCommentsEnabled = ModelWidget.widgetBoundaryCommentsEnabled(context);
        renderBeginningBoundaryComment(writer, "Menu Widget", modelMenu);
        writer.append("<div");
        String menuId = modelMenu.getId();
        if (UtilValidate.isNotEmpty(menuId)) {
            writer.append(" id=\"").append(menuId).append("\"");
        } else {
            // TODO: Remove else after UI refactor - allow both id and style
            String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
            if (UtilValidate.isNotEmpty(menuContainerStyle)) {
                writer.append(" class=\"").append(menuContainerStyle).append("\"");
            }
        }
        String menuWidth = modelMenu.getMenuWidth();
        // TODO: Eliminate embedded styling after refactor
        if (UtilValidate.isNotEmpty(menuWidth)) {
            writer.append(" style=\"width:").append(menuWidth).append(";\"");
        }
        writer.append(">");
        appendWhitespace(writer);
        String menuTitle = modelMenu.getTitle(context);
        if (UtilValidate.isNotEmpty(menuTitle)) {
            writer.append("<h2>").append(menuTitle).append("</h2>");
            appendWhitespace(writer);
        }
        if (modelMenu.renderedMenuItemCount(context) > 0) {
            writer.append("<ul>");
            appendWhitespace(writer);
            writer.append("<li>");
            appendWhitespace(writer);
            writer.append(" <ul>");
            appendWhitespace(writer);
        }
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.menu.MenuStringRenderer#renderMenuClose(java.io.Writer, java.util.Map, org.ofbiz.widget.menu.ModelMenu)
     */
    public void renderMenuClose(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        // TODO: div can't be directly inside an UL
        String fillStyle = modelMenu.getFillStyle();
        if (UtilValidate.isNotEmpty(fillStyle)) {
            writer.append("<div class=\"").append(fillStyle).append("\">&nbsp;</div>");
        }
        //String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
        if (modelMenu.renderedMenuItemCount(context) > 0) {      
            writer.append(" </ul>");
            appendWhitespace(writer);
            writer.append("</li>");
            appendWhitespace(writer);
            writer.append("</ul>");
            appendWhitespace(writer);
        }
        writer.append(" <br class=\"clear\"/>");
        appendWhitespace(writer);
        writer.append("</div>");
        appendWhitespace(writer);
        renderEndingBoundaryComment(writer, "Menu Widget", modelMenu);

        GenericValue userLogin = (GenericValue)request.getSession().getAttribute("userLogin");
        if (userLogin != null) {
            String userLoginId = userLogin.getString("userLoginId");
            //request.getSession().setAttribute("userLoginIdAtPermGrant", userLoginId);
            setUserLoginIdAtPermGrant(userLoginId);
            //Debug.logInfo("in HtmlMenuRenderer, userLoginId(Close):" + userLoginId + " userLoginIdAtPermGrant:" + request.getSession().getAttribute("userLoginIdAtPermGrant"),"");
        } else {
            request.getSession().setAttribute("userLoginIdAtPermGrant", null);
        }
    }

    public void renderFormatSimpleWrapperOpen(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        //appendWhitespace(writer);
    }

    public void renderFormatSimpleWrapperClose(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        //appendWhitespace(writer);
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }


    /**
     * @param string
     */
    public void setUserLoginIdAtPermGrant(String string) {
            //Debug.logInfo("in HtmlMenuRenderer,  userLoginIdAtPermGrant(setUserLoginIdAtPermGrant):" + string,"");
        this.userLoginIdAtPermGrant = string;
    }

    public String getUserLoginIdAtPermGrant() {
        return this.userLoginIdAtPermGrant;
    }

    public boolean isHideIfSelected(ModelMenuItem menuItem, Map<String, Object> context) {
        ModelMenu menu = menuItem.getModelMenu();
        String currentMenuItemName = menu.getSelectedMenuItemContextFieldName(context);
        String currentItemName = menuItem.getName();
        Boolean hideIfSelected = menuItem.getHideIfSelected();
            //Debug.logInfo("in HtmlMenuRenderer, currentMenuItemName:" + currentMenuItemName + " currentItemName:" + currentItemName + " hideIfSelected:" + hideIfSelected,"");
        return (hideIfSelected != null && hideIfSelected.booleanValue() && currentMenuItemName != null && currentMenuItemName.equals(currentItemName));
    }


    public boolean userLoginIdHasChanged() {
        boolean hasChanged = false;
        GenericValue userLogin = (GenericValue)request.getSession().getAttribute("userLogin");
        userLoginIdAtPermGrant = getUserLoginIdAtPermGrant();
        //userLoginIdAtPermGrant = (String)request.getSession().getAttribute("userLoginIdAtPermGrant");
        String userLoginId = null;
        if (userLogin != null)
            userLoginId = userLogin.getString("userLoginId");
            //Debug.logInfo("in HtmlMenuRenderer, userLoginId:" + userLoginId + " userLoginIdAtPermGrant:" + userLoginIdAtPermGrant ,"");
        if ((userLoginId == null && userLoginIdAtPermGrant != null)
           || (userLoginId != null && userLoginIdAtPermGrant == null)
           || ((userLoginId != null && userLoginIdAtPermGrant != null)
              && !userLoginId.equals(userLoginIdAtPermGrant))) {
            hasChanged = true;
        } else {
            if (userLoginIdAtPermGrant != null)
               hasChanged = true;
            else
               hasChanged = false;

            userLoginIdAtPermGrant = null;
        }
        return hasChanged;
    }

    public String getTitle(ModelMenuItem menuItem, Map<String, Object> context) {
        String title = null;
        title = menuItem.getTitle(context);
        return title;
    }

    public void renderLink(Appendable writer, Map<String, Object> context, ModelMenuItem.MenuLink link) throws IOException {
        String target = link.getTarget(context);
        ModelMenuItem menuItem = link.getLinkMenuItem();
        if (isDisableIfEmpty(menuItem, context)) {
            target = null;
        }

        if (UtilValidate.isNotEmpty(target)) {
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");

            String targetWindow = link.getTargetWindow(context);
            String uniqueItemName = menuItem.getModelMenu().getName() + "_" + menuItem.getName() + "_LF_" + UtilMisc.<String>addToBigDecimalInMap(context, "menuUniqueItemIndex", BigDecimal.ONE);

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

                writer.append(" name=\"");
                writer.append(uniqueItemName);
                writer.append("\">");

                UtilCodec.SimpleEncoder simpleEncoder = (UtilCodec.SimpleEncoder) context.get("simpleEncoder");
                for (Map.Entry<String, String> parameter: link.getParameterMap(context).entrySet()) {
                    writer.append("<input name=\"");
                    writer.append(parameter.getKey());
                    writer.append("\" value=\"");
                    if (simpleEncoder != null) {
                        writer.append(simpleEncoder.encode(parameter.getValue()));
                    } else {
                        writer.append(parameter.getValue());
                    }
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
            if (!"hidden-form".equals(linkType)) {
                if (UtilValidate.isNotEmpty(targetWindow)) {
                    writer.append(" target=\"");
                    writer.append(targetWindow);
                    writer.append("\"");
                }
            }

            writer.append(" href=\"");
            String confirmationMsg = null;
            if ("hidden-form".equals(linkType)) {
                if (UtilValidate.isNotEmpty(confirmationMsg)) {
                    writer.append("javascript:confirmActionFormLink('");
                    writer.append(confirmationMsg);
                    writer.append("', '");
                    writer.append(uniqueItemName);
                    writer.append("')");
                } else {
                    writer.append("javascript:document.");
                    writer.append(uniqueItemName);
                    writer.append(".submit()");
                }
            } else {
                if (UtilValidate.isNotEmpty(confirmationMsg)) {
                    writer.append("javascript:confirmActionLink('");
                    writer.append(confirmationMsg);
                    writer.append("', '");
                    WidgetWorker.buildHyperlinkUrl(writer, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                            link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
                    writer.append("')");
                } else {
                WidgetWorker.buildHyperlinkUrl(writer, target, link.getUrlMode(), link.getParameterMap(context), link.getPrefix(context),
                        link.getFullPath(), link.getSecure(), link.getEncode(), request, response, context);
                }
            }
            writer.append("\">");
        }

        // the text
        Image img = link.getImage();
        if (img != null) {
            renderImage(writer, context, img);
            writer.append("&nbsp;" + link.getText(context));
        } else {
            writer.append(link.getText(context));
        }

        if (UtilValidate.isNotEmpty(target)) {
            // close tag
            writer.append("</a>");
        }

        /* NOTE DEJ20090316: This was here as a comment and not sure what it is for or if it is useful... can probably be safely removed in the future if still not used/needed
        boolean isSelected = menuItem.isSelected(context);

        String style = null;

        if (isSelected) {
        style = menuItem.getSelectedStyle();
        } else {
        style = link.getStyle(context);
        if (UtilValidate.isEmpty(style))
        style = menuItem.getTitleStyle();
        if (UtilValidate.isEmpty(style))
        style = menuItem.getWidgetStyle();
        }

        if (menuItem.getDisabled()) {
        style = menuItem.getDisabledTitleStyle();
        }

        if (UtilValidate.isNotEmpty(style)) {
        writer.append(" class=\"");
        writer.append(style);
        writer.append("\"");
        }
        */
    }

    public void renderImage(Appendable writer, Map<String, Object> context, Image image) throws IOException {
        // open tag
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
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src)) {
            writer.append(" src=\"");
            String urlMode = image.getUrlMode();
            boolean fullPath = false;
            boolean secure = false;
            boolean encode = false;
            HttpServletResponse response = (HttpServletResponse) context.get("response");
            HttpServletRequest request = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("ofbiz")) {
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

            writer.append("\"");
        }
        writer.append("/>");
    }
}
