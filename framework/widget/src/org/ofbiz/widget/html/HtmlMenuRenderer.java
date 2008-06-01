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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.RequestHandler;
import org.ofbiz.webapp.taglib.ContentUrlTag;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.menu.ModelMenu;
import org.ofbiz.widget.menu.ModelMenuItem;
import org.ofbiz.widget.menu.ModelMenuItem.Image;
import org.ofbiz.widget.menu.ModelMenuItem.Link;

/**
 * Widget Library - HTML Menu Renderer implementation
 */
public class HtmlMenuRenderer extends HtmlWidgetRenderer implements MenuStringRenderer {

    HttpServletRequest request;
    HttpServletResponse response;
    protected String userLoginIdAtPermGrant;
    protected boolean userLoginIdHasChanged = true;
    protected String permissionErrorMessage = "";

    public static final String module = HtmlMenuRenderer.class.getName();

    protected HtmlMenuRenderer() {}

    public HtmlMenuRenderer(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public void appendOfbizUrl(Writer writer, String location) throws IOException {
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
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        if (delegator == null) {
                //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl, delegator is null(5)", "");
        }
        RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
        // make and append the link
        String s = rh.makeLink(this.request, this.response, location);
        if (s.indexOf("null") >= 0) {
            //if (Debug.infoOn()) Debug.logInfo("in appendOfbizUrl(3), url: " + s, "");
        }
        writer.write(s);
    }

    public void appendContentUrl(Writer writer, String location) throws IOException {
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
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        if (delegator == null) {
                //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, delegator is null(6)", "");
        }
        StringBuffer buffer = new StringBuffer();
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        writer.write(buffer.toString());
        writer.write(location);
    }

    public void appendTooltip(Writer writer, Map<String, Object> context, ModelMenuItem modelMenuItem) throws IOException {
        // render the tooltip
        String tooltip = modelMenuItem.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            writer.write("<span class=\"");
            String tooltipStyle = modelMenuItem.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                writer.write(tooltipStyle);
            } else {
                writer.write("tooltip");
            }
            writer.write("\"");
            writer.write(tooltip);
            writer.write("</span>");
        }
    }

    public void renderFormatSimpleWrapperRows(Writer writer, Map<String, Object> context, Object menuObj) throws IOException {

        List menuItemList = ((ModelMenu)menuObj).getMenuItemList();
        Iterator menuItemIter = menuItemList.iterator();
        ModelMenuItem currentMenuItem = null;

        while (menuItemIter.hasNext()) {
            currentMenuItem = (ModelMenuItem)menuItemIter.next();
            renderMenuItem(writer, context, currentMenuItem);
        }
    }

    public void renderMenuItem(Writer writer, Map<String, Object> context, ModelMenuItem menuItem) throws IOException {
        
        //Debug.logInfo("in renderMenuItem, menuItem:" + menuItem.getName() + " context:" + context ,"");
        boolean hideThisItem = isHideIfSelected(menuItem, context);
        //if (Debug.infoOn()) Debug.logInfo("in HtmlMenuRendererImage, hideThisItem:" + hideThisItem,"");
        if (hideThisItem)
            return;

        String style = null;
        
        if (menuItem.isSelected(context)) {
            style = menuItem.getSelectedStyle();
            if (UtilValidate.isEmpty(style)) {
                style = "selected";
            }
        }
        
        if (menuItem.getDisabled()) {
            style = menuItem.getDisabledTitleStyle();
        }
        
        writer.write("  <li");
        String alignStyle = menuItem.getAlignStyle();
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(alignStyle)) {
            writer.write(" class=\"");
            if (UtilValidate.isNotEmpty(style)) {
                writer.write(style + " ");
            }
            if (UtilValidate.isNotEmpty(alignStyle)) {
                writer.write(alignStyle);
            }
            writer.write("\"");
        }
        writer.write(">");
        
        Link link = menuItem.getLink();
        //if (Debug.infoOn()) Debug.logInfo("in HtmlMenuRendererImage, link(0):" + link,"");
        if (link != null) {
            renderLink(writer, context, link);
        } 

        writer.write("</li>");
        
        appendWhitespace(writer);
    }

    public boolean isDisableIfEmpty(ModelMenuItem menuItem, Map<String, Object> context) {

        boolean disabled = false;
        String disableIfEmpty = menuItem.getDisableIfEmpty();
        if (UtilValidate.isNotEmpty(disableIfEmpty)) {
            List keys = StringUtil.split(disableIfEmpty, "|");
            Iterator iter = keys.iterator();
            while (iter.hasNext()) {
                Object obj = context.get(disableIfEmpty);
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
    public void renderMenuOpen(Writer writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {

        if (!userLoginIdHasChanged) {
            userLoginIdHasChanged = userLoginIdHasChanged();
        }

            //Debug.logInfo("in HtmlMenuRenderer, userLoginIdHasChanged:" + userLoginIdHasChanged,"");
        renderBeginningBoundaryComment(writer, "Menu Widget", modelMenu);
        writer.write("<div");
        String menuId = modelMenu.getId();
        if (UtilValidate.isNotEmpty(menuId)) {
            writer.write(" id=\"" + menuId + "\"");
        } else {
            // TODO: Remove else after UI refactor - allow both id and style
            String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
            if (UtilValidate.isNotEmpty(menuContainerStyle)) {
                writer.write(" class=\"" + menuContainerStyle + "\"");
            }
        }
        String menuWidth = modelMenu.getMenuWidth();
        // TODO: Eliminate embedded styling after refactor
        if (UtilValidate.isNotEmpty(menuWidth)) {
            writer.write(" style=\"width:" + menuWidth + ";\"");
        }
        writer.write(">");
        String menuTitle = modelMenu.getTitle(context);
        if (UtilValidate.isNotEmpty(menuTitle)) {
            appendWhitespace(writer);
            writer.write(" <h2>" + menuTitle + "</h2>");
        }
        appendWhitespace(writer);
        writer.write(" <ul>");
        
        appendWhitespace(writer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.menu.MenuStringRenderer#renderMenuClose(java.io.Writer, java.util.Map, org.ofbiz.widget.menu.ModelMenu)
     */
    public void renderMenuClose(Writer writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        String fillStyle = modelMenu.getFillStyle();
        if (UtilValidate.isNotEmpty(fillStyle)) {
            writer.write("<div class=\"" + fillStyle + "\">&nbsp;</div>");
        }
        //String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
        writer.write(" </ul>");
        appendWhitespace(writer);
        writer.write(" <br class=\"clear\"/>");
        appendWhitespace(writer);
        writer.write("</div>");
        appendWhitespace(writer);
        renderEndingBoundaryComment(writer, "Menu Widget", modelMenu);
        
        userLoginIdHasChanged = userLoginIdHasChanged(); 
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

    public void renderFormatSimpleWrapperOpen(Writer writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
        //appendWhitespace(writer);
    }

    public void renderFormatSimpleWrapperClose(Writer writer, Map<String, Object> context, ModelMenu modelMenu) throws IOException {
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

    /**
     * @return
     */
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


    public void setUserLoginIdHasChanged(boolean b) {
        userLoginIdHasChanged = b;
    }


    public String getTitle(ModelMenuItem menuItem, Map<String, Object> context) {
        String title = null;
        title = menuItem.getTitle(context);
        return title;
    }

    public void renderLink(Writer writer, Map<String, Object> context, ModelMenuItem.Link link) throws IOException {
        ModelMenuItem menuItem = link.getLinkMenuItem();
        String target = link.getTarget(context);
        if (menuItem.getDisabled()) {
            target = null;
        }
        if (UtilValidate.isNotEmpty(target)) {
            // open tag
            writer.write("<a");
            String id = link.getId(context);
            if (UtilValidate.isNotEmpty(id)) {
                writer.write(" id=\"");
                writer.write(id);
                writer.write("\"");
            }
        
/*
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
            writer.write(" class=\"");
            writer.write(style);
            writer.write("\"");
        }
*/
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
            writer.write(" href=\"");
            String urlMode = link.getUrlMode();
            String prefix = link.getPrefix(context);
            boolean fullPath = link.getFullPath();
            boolean secure = link.getSecure();
            boolean encode = link.getEncode();
            HttpServletResponse res = (HttpServletResponse) context.get("response");
            HttpServletRequest req = (HttpServletRequest) context.get("request");
            if (urlMode != null && urlMode.equalsIgnoreCase("intra-app")) {
                if (req != null && res != null) {
                    ServletContext ctx = (ServletContext) req.getAttribute("servletContext");
                    RequestHandler rh = (RequestHandler) ctx.getAttribute("_REQUEST_HANDLER_");
                    String urlString = rh.makeLink(req, res, target, fullPath, secure, encode);
                    writer.write(urlString);
                } else if (prefix != null) {
                    writer.write(prefix + target);
                } else {
                    writer.write(target);
                }
            } else if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                StringBuffer newURL = new StringBuffer();
                ContentUrlTag.appendContentPrefix(req, newURL);
                newURL.append(target);
                writer.write(newURL.toString());
            } else if ("inter-app".equalsIgnoreCase(urlMode) && req != null) {
                String externalLoginKey = (String) req.getAttribute("externalLoginKey");
                if (UtilValidate.isNotEmpty(externalLoginKey)) {
                    if (target.contains("?")) {
                        target += "&externalLoginKey=" + externalLoginKey;
                    } else {
                        target += "?externalLoginKey=" + externalLoginKey;
                    }
                    writer.write(target);
                }
            } else {
                writer.write(target);
            }
            writer.write("\">");
        }
        
        // the text
        Image img = link.getImage();
        if (img == null)
            writer.write(link.getText(context));
        else
            renderImage(writer, context, img);
        
        if (UtilValidate.isNotEmpty(target)) {
            // close tag
            writer.write("</a>");
        }
    }

    public void renderImage(Writer writer, Map<String, Object> context, ModelMenuItem.Image image) throws IOException {
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
            if (urlMode != null && urlMode.equalsIgnoreCase("ofbiz")) {
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
    }
}


