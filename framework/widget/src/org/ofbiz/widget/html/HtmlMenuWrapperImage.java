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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.widget.menu.MenuStringRenderer;
import org.ofbiz.widget.menu.ModelMenuItem;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.xml.sax.SAXException;

/**
 * Widget Library - HTML Menu Wrapper class - makes it easy to do the setup and render of a menu
 */
public class HtmlMenuWrapperImage extends HtmlMenuWrapper {
    
    public static final String module = HtmlMenuWrapperImage.class.getName();

    protected HtmlMenuWrapperImage() {}

    public HtmlMenuWrapperImage(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response) 
            throws IOException, SAXException, ParserConfigurationException {
        super(resourceName, menuName, request, response);
    }

    public MenuStringRenderer getMenuRenderer() {
        return new HtmlMenuRendererImage(request, response);
    }
    
    public void init(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)  
            throws IOException, SAXException, ParserConfigurationException {
        
        super.init(resourceName, menuName, request, response);
        String pubPt = (String)request.getAttribute("pubPt");
        //if (Debug.infoOn()) Debug.logInfo("in init, pubPt:" + pubPt, module);
        Map dummyMap = new HashMap();
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        //if (Debug.infoOn()) Debug.logInfo("in init, delegator:" + delegator, module);
        try {
            List menuItemList = modelMenu.getMenuItemList();
            Iterator iter = menuItemList.iterator();
            while (iter.hasNext()) {
               ModelMenuItem menuItem = (ModelMenuItem)iter.next();
               String contentId = menuItem.getAssociatedContentId(dummyMap);
               //if (Debug.infoOn()) Debug.logInfo("in init, contentId:" + contentId, module);
               GenericValue webSitePublishPoint = delegator.findByPrimaryKeyCache("WebSitePublishPoint", UtilMisc.toMap("contentId", contentId));
               String menuItemName = menuItem.getName();
               //if (Debug.infoOn()) Debug.logInfo("in init, menuItemName:" + menuItemName, module);
               //if (Debug.infoOn()) Debug.logInfo("in init, webSitePublishPoint:" + webSitePublishPoint, module);
               putInContext(menuItemName, "WebSitePublishPoint", webSitePublishPoint);
            }
        } catch (GenericEntityException e) {
            return;
        }
    }
}
