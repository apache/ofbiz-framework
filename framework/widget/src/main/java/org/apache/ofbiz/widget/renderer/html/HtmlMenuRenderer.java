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
package org.apache.ofbiz.widget.renderer.html;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilCodec;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.webapp.control.RequestHandler;
import org.apache.ofbiz.webapp.taglib.ContentUrlTag;
import org.apache.ofbiz.widget.WidgetWorker;
import org.apache.ofbiz.widget.model.CommonWidgetModels.Image;
import org.apache.ofbiz.widget.model.ModelMenu;
import org.apache.ofbiz.widget.model.ModelMenuItem;
import org.apache.ofbiz.widget.model.ModelMenuItem.MenuLink;
import org.apache.ofbiz.widget.model.ModelWidget;
import org.apache.ofbiz.widget.renderer.MenuStringRenderer;

/**
 * Widget Library - HTML Menu Renderer implementation
 */
public class HtmlMenuRenderer extends HtmlWidgetRenderer implements MenuStringRenderer {

    private HttpServletRequest request;
    private HttpServletResponse response;

    /**
     * Gets request.
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Gets response.
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    private String userLoginIdAtPermGrant;
    private String permissionErrorMessage = "";

    protected static final String MODULE = HtmlMenuRenderer.class.getName();

    protected HtmlMenuRenderer() { }

    public HtmlMenuRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    /**
     * Append ofbiz url.
     * @param writer   the writer
     * @param location the location
     * @throws IOException the io exception
     */
    public void appendOfbizUrl(Appendable writer, String location) throws IOException {
        ServletContext ctx = request.getServletContext();
        if (ctx == null) {
            HttpSession session = request.getSession();
            if (session != null) {
                ctx = session.getServletContext();
            }
            if (ctx == null) {
                throw new RuntimeException("ctx is null. location:" + location);
            }
        }
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        String s = rh.makeLink(this.request, this.response, location);
        writer.append(s);
    }

    /**
     * Append content url.
     * @param writer   the writer
     * @param location the location
     * @throws IOException the io exception
     */
    public void appendContentUrl(Appendable writer, String location) throws IOException {
        ServletContext ctx = request.getServletContext();
        if (ctx == null) {
            HttpSession session = request.getSession();
            if (session != null) {
                ctx = session.getServletContext();
            }
            if (ctx == null) {
                throw new RuntimeException("ctx is null. location:" + location);
            }
            this.request.setAttribute("servletContext", ctx);
        }
        StringBuilder buffer = new StringBuilder();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.append(buffer.toString());
        writer.append(location);
    }

    /**
     * Append tooltip.
     * @param writer the writer
     * @param context the context
     * @param modelMenuItem the model menu item
     * @throws IOException the io exception
     */
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

    @Override
    public void renderFormatSimpleWrapperRows(Appendable writer, Map<String, Object> context, Object menuObj) throws IOException {
        List<ModelMenuItem> menuItemList = ((ModelMenu) menuObj).getMenuItemList();
        for (ModelMenuItem currentMenuItem: menuItemList) {
            renderMenuItem(writer, context, currentMenuItem);
        }
    }

    @Override
    public void renderMenuItem(Appendable writer, Map<String, Object> context, ModelMenuItem menuItem) throws IOException {
        boolean hideThisItem = isHideIfSelected(menuItem, context);

        if (hideThisItem) {
            return;
        }

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

    /**
     * Is disable if empty boolean.
     * @param menuItem the menu item
     * @param context the context
     * @return the boolean
     */
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

    @Override
    public void renderMenuOpen(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {

        this.setWidgetCommentsEnabled(ModelWidget.widgetBoundaryCommentsEnabled(context));
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

    @Override
    public void renderMenuClose(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        // TODO: div can't be directly inside an UL
        String fillStyle = modelMenu.getFillStyle();
        if (UtilValidate.isNotEmpty(fillStyle)) {
            writer.append("<div class=\"").append(fillStyle).append("\">&nbsp;</div>");
        }
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

        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        if (userLogin != null) {
            String userLoginId = userLogin.getString("userLoginId");
            setUserLoginIdAtPermGrant(userLoginId);
        } else {
            request.getSession().setAttribute("userLoginIdAtPermGrant", null);
        }
    }

    @Override
    public void renderFormatSimpleWrapperOpen(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
    }

    @Override
    public void renderFormatSimpleWrapperClose(Appendable writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
    }

    /**
     * Sets request.
     * @param request the request
     */
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Sets response.
     * @param response the response
     */
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }


    /**
     * @param string
     */
    public void setUserLoginIdAtPermGrant(String string) {
        this.userLoginIdAtPermGrant = string;
    }

    /**
     * Gets user login id at perm grant.
     * @return the user login id at perm grant
     */
    public String getUserLoginIdAtPermGrant() {
        return this.userLoginIdAtPermGrant;
    }

    /**
     * Is hide if selected boolean.
     * @param menuItem the menu item
     * @param context  the context
     * @return the boolean
     */
    public boolean isHideIfSelected(ModelMenuItem menuItem, Map<String, Object> context) {
        ModelMenu menu = menuItem.getModelMenu();
        String currentMenuItemName = menu.getSelectedMenuItemContextFieldName(context);
        String currentItemName = menuItem.getName();
        Boolean hideIfSelected = menuItem.getHideIfSelected();
        return (hideIfSelected != null && hideIfSelected && currentMenuItemName != null && currentMenuItemName.equals(currentItemName));
    }

    /**
     * User login id has changed boolean.
     * @return the boolean
     */
    public boolean userLoginIdHasChanged() {
        boolean hasChanged = false;
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        userLoginIdAtPermGrant = getUserLoginIdAtPermGrant();
        String userLoginId = null;
        if (userLogin != null) {
            userLoginId = userLogin.getString("userLoginId");
        }
        if ((userLoginId == null && userLoginIdAtPermGrant != null)
                || (userLoginId != null && userLoginIdAtPermGrant == null)
                || ((userLoginId != null && userLoginIdAtPermGrant != null)
                && !userLoginId.equals(userLoginIdAtPermGrant))) {
            hasChanged = true;
        } else {
            if (userLoginIdAtPermGrant != null) {
                hasChanged = true;
            } else {
                hasChanged = false;
            }

            userLoginIdAtPermGrant = null;
        }
        return hasChanged;
    }

    /**
     * Gets title.
     * @param menuItem the menu item
     * @param context the context
     * @return the title
     */
    public String getTitle(ModelMenuItem menuItem, Map<String, Object> context) {
        String title = null;
        title = menuItem.getTitle(context);
        return title;
    }

    @Override
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
            String uniqueItemName = menuItem.getModelMenu().getName() + "_" + menuItem.getName() + "_LF_"
                    + UtilMisc.<String>addToBigDecimalInMap(context, "menuUniqueItemIndex", BigDecimal.ONE);

            String linkType = WidgetWorker.determineAutoLinkType(link.getLinkType(), target, link.getUrlMode(), request);
            boolean isHiddenForm = "hidden-form".equals(linkType);
            if (isHiddenForm) {
                writer.append("<form method=\"post\"");
                writer.append(" action=\"");
                // note that this passes null for the parameterList on purpose so they won't be put into the URL
                final URI uri = WidgetWorker.buildHyperlinkUri(target, link.getUrlMode(), null,
                        link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(),
                        request, response);
                writer.append(uri.toString());
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
                for (Map.Entry<String, String> parameter: link.getParameterMap(context, false).entrySet()) {
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
            if (!isHiddenForm) {
                if (UtilValidate.isNotEmpty(targetWindow)) {
                    writer.append(" target=\"");
                    writer.append(targetWindow);
                    writer.append("\"");
                }
            }

            writer.append(" href=\"");
            if (isHiddenForm) {
                writer.append("javascript:document.");
                writer.append(uniqueItemName);
                writer.append(".submit()");
            } else {
                final URI uri = WidgetWorker.buildHyperlinkUri(target, link.getUrlMode(), link.getParameterMap(context),
                        link.getPrefix(context), link.getFullPath(), link.getSecure(), link.getEncode(),
                        request, response);
                writer.append(uri.toString());
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

    }

    @Override
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
            if (urlMode != null && "ofbiz".equalsIgnoreCase(urlMode)) {
                if (request != null && response != null) {
                    RequestHandler rh = (RequestHandler) request.getServletContext().getAttribute("_REQUEST_HANDLER_");
                    String urlString = rh.makeLink(request, response, src, fullPath, secure, encode);
                    writer.append(urlString);
                } else {
                    writer.append(src);
                }
            } else if (urlMode != null && "content".equalsIgnoreCase(urlMode)) {
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
