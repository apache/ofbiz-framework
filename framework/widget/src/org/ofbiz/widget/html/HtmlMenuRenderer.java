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
public class HtmlMenuRenderer implements MenuStringRenderer {

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

    public void appendWhitespace(StringBuffer buffer) {
        // appending line ends for now, but this could be replaced with a simple space or something
        buffer.append("\r\n");
        //buffer.append(' ');
    }

    public void appendOfbizUrl(StringBuffer buffer, String location) {
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
                throw new RuntimeException("ctx is null. buffer=" + buffer.toString() + " location:" + location);
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
        buffer.append(s);
    }

    public void appendContentUrl(StringBuffer buffer, String location) {
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
                throw new RuntimeException("ctx is null. buffer=" + buffer.toString() + " location:" + location);
            }
            //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, ctx is NOT null(2)", "");
            this.request.setAttribute("servletContext", ctx);
        }
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        if (delegator == null) {
                //if (Debug.infoOn()) Debug.logInfo("in appendContentUrl, delegator is null(6)", "");
        }
        ContentUrlTag.appendContentPrefix(this.request, buffer);
        buffer.append(location);
    }

    public void appendTooltip(StringBuffer buffer, Map context, ModelMenuItem modelMenuItem) {
        // render the tooltip
        String tooltip = modelMenuItem.getTooltip(context);
        if (UtilValidate.isNotEmpty(tooltip)) {
            buffer.append("<span class=\"");
            String tooltipStyle = modelMenuItem.getTooltipStyle();
            if (UtilValidate.isNotEmpty(tooltipStyle)) {
                buffer.append(tooltipStyle);
            } else {
                buffer.append("tooltip");
            }
            buffer.append("\"");
            buffer.append(tooltip);
            buffer.append("</span>");
        }
    }

    public void renderFormatSimpleWrapperRows(StringBuffer buffer, Map context, Object menuObj) {

        List menuItemList = ((ModelMenu)menuObj).getMenuItemList();
        Iterator menuItemIter = menuItemList.iterator();
        ModelMenuItem currentMenuItem = null;

        while (menuItemIter.hasNext()) {
            currentMenuItem = (ModelMenuItem)menuItemIter.next();
            renderMenuItem(buffer, context, currentMenuItem);
        }
    }

    public void renderMenuItem(StringBuffer buffer, Map context, ModelMenuItem menuItem) {
        
        //Debug.logInfo("in renderMenuItem, menuItem:" + menuItem.getName() + " context:" + context ,"");
        boolean hideThisItem = isHideIfSelected(menuItem);
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
        
        buffer.append("  <li");
        String alignStyle = menuItem.getAlignStyle();
        if (UtilValidate.isNotEmpty(style) || UtilValidate.isNotEmpty(alignStyle)) {
            buffer.append(" class=\"");
            if (UtilValidate.isNotEmpty(style)) {
                buffer.append(style + " ");
            }
            if (UtilValidate.isNotEmpty(alignStyle)) {
                buffer.append(alignStyle);
            }
            buffer.append("\"");
        }
        buffer.append(">");
        
        Link link = menuItem.getLink();
        //if (Debug.infoOn()) Debug.logInfo("in HtmlMenuRendererImage, link(0):" + link,"");
        if (link != null) {
            renderLink(buffer, context, link);
        } 

        buffer.append("</li>");
        
        this.appendWhitespace(buffer);
    }

    public boolean isDisableIfEmpty(ModelMenuItem menuItem, Map context) {

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
    public String buildDivStr(ModelMenuItem menuItem, Map context) {
        String divStr = "";
        divStr =  menuItem.getTitle(context);
        return divStr;
    }
*/
    public void renderMenuOpen(StringBuffer buffer, Map context, ModelMenu modelMenu) {

        if (!userLoginIdHasChanged) {
            userLoginIdHasChanged = userLoginIdHasChanged();
        }

            //Debug.logInfo("in HtmlMenuRenderer, userLoginIdHasChanged:" + userLoginIdHasChanged,"");
        buffer.append("<!-- begin menu widget -->");
        this.appendWhitespace(buffer);
        buffer.append("<div");
        String menuId = modelMenu.getId();
        if (UtilValidate.isNotEmpty(menuId)) {
            buffer.append(" id=\"" + menuId + "\"");
        } else {
            // TODO: Remove else after UI refactor - allow both id and style
            String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
            if (UtilValidate.isNotEmpty(menuContainerStyle)) {
                buffer.append(" class=\"" + menuContainerStyle + "\"");
            }
        }
        String menuWidth = modelMenu.getMenuWidth();
        // TODO: Eliminate embedded styling after refactor
        if (UtilValidate.isNotEmpty(menuWidth)) {
            buffer.append(" style=\"width:" + menuWidth + ";\"");
        }
        buffer.append(">");
        String menuTitle = modelMenu.getTitle(context);
        if (UtilValidate.isNotEmpty(menuTitle)) {
            this.appendWhitespace(buffer);
            buffer.append(" <h2>" + menuTitle + "</h2>");
        }
        this.appendWhitespace(buffer);
        buffer.append(" <ul>");
        
        this.appendWhitespace(buffer);
    }

    /* (non-Javadoc)
     * @see org.ofbiz.widget.menu.MenuStringRenderer#renderMenuClose(java.lang.StringBuffer, java.util.Map, org.ofbiz.widget.menu.ModelMenu)
     */
    public void renderMenuClose(StringBuffer buffer, Map context, ModelMenu modelMenu) {
        String fillStyle = modelMenu.getFillStyle();
        if (UtilValidate.isNotEmpty(fillStyle)) {
            buffer.append("<div class=\"" + fillStyle + "\">&nbsp;</div>");
        }
        //String menuContainerStyle = modelMenu.getMenuContainerStyle(context);
        buffer.append(" </ul>");
        this.appendWhitespace(buffer);
        buffer.append(" <br class=\"clear\" />");
        this.appendWhitespace(buffer);
        buffer.append("</div>");
        this.appendWhitespace(buffer);
        buffer.append("<!-- end menu widget -->");
        this.appendWhitespace(buffer);
        
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

    public void renderFormatSimpleWrapperOpen(StringBuffer buffer, Map context, ModelMenu modelMenu) {
        //this.appendWhitespace(buffer);
    }

    public void renderFormatSimpleWrapperClose(StringBuffer buffer, Map context, ModelMenu modelMenu) {
        //this.appendWhitespace(buffer);
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

    public boolean isHideIfSelected( ModelMenuItem menuItem) {
        ModelMenu menu = menuItem.getModelMenu();
        String currentMenuItemName = menu.getCurrentMenuItemName();
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


    public String getTitle(ModelMenuItem menuItem, Map context) {
        String title = null;
        title = menuItem.getTitle(context);
        return title;
    }

    public void renderLink(StringBuffer buffer, Map context, ModelMenuItem.Link link) {
        // open tag
        buffer.append("<a");
        String id = link.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            buffer.append(" id=\"");
            buffer.append(id);
            buffer.append("\"");
        }
        
        ModelMenuItem menuItem = link.getLinkMenuItem();
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
            buffer.append(" class=\"");
            buffer.append(style);
            buffer.append("\"");
        }
*/
        String name = link.getName(context);
        if (UtilValidate.isNotEmpty(name)) {
            buffer.append(" name=\"");
            buffer.append(name);
            buffer.append("\"");
        }
        String targetWindow = link.getTargetWindow(context);
        if (UtilValidate.isNotEmpty(targetWindow)) {
            buffer.append(" target=\"");
            buffer.append(targetWindow);
            buffer.append("\"");
        }
        String target = link.getTarget(context);
        if (menuItem.getDisabled()) {
            target = null;
        }
        if (UtilValidate.isNotEmpty(target)) {
            buffer.append(" href=\"");
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
                    buffer.append(urlString);
                } else if (prefix != null) {
                    buffer.append(prefix + target);
                } else {
                    buffer.append(target);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                StringBuffer newURL = new StringBuffer();
                ContentUrlTag.appendContentPrefix(req, newURL);
                newURL.append(target);
                buffer.append(newURL.toString());
            } else {
                buffer.append(target);
            }

            buffer.append("\"");
        }
        buffer.append(">");
        
        // the text
        Image img = link.getImage();
        if (img == null)
            buffer.append(link.getText(context));
        else
            renderImage(buffer, context, img);
        
        // close tag
        buffer.append("</a>");
    }

    public void renderImage(StringBuffer buffer, Map context, ModelMenuItem.Image image) {
        // open tag
        buffer.append("<img ");
        String id = image.getId(context);
        if (UtilValidate.isNotEmpty(id)) {
            buffer.append(" id=\"");
            buffer.append(id);
            buffer.append("\"");
        }
        String style = image.getStyle(context);
        if (UtilValidate.isNotEmpty(style)) {
            buffer.append(" class=\"");
            buffer.append(style);
            buffer.append("\"");
        }
        String wid = image.getWidth(context);
        if (UtilValidate.isNotEmpty(wid)) {
            buffer.append(" width=\"");
            buffer.append(wid);
            buffer.append("\"");
        }
        String hgt = image.getHeight(context);
        if (UtilValidate.isNotEmpty(hgt)) {
            buffer.append(" height=\"");
            buffer.append(hgt);
            buffer.append("\"");
        }
        String border = image.getBorder(context);
        if (UtilValidate.isNotEmpty(border)) {
            buffer.append(" border=\"");
            buffer.append(border);
            buffer.append("\"");
        }
        String src = image.getSrc(context);
        if (UtilValidate.isNotEmpty(src)) {
            buffer.append(" src=\"");
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
                    buffer.append(urlString);
                } else {
                    buffer.append(src);
                }
            } else  if (urlMode != null && urlMode.equalsIgnoreCase("content")) {
                if (request != null && response != null) {
                    StringBuffer newURL = new StringBuffer();
                    ContentUrlTag.appendContentPrefix(request, newURL);
                    newURL.append(src);
                    buffer.append(newURL.toString());
                }
            } else {
                buffer.append(src);
            }

            buffer.append("\"");
        }
        buffer.append("/>");
    }
}

