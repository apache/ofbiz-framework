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
package org.ofbiz.widget.menu;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.LocalDispatcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Menu factory class
 */
public class MenuFactory {
    
    public static final String module = MenuFactory.class.getName();

    public static final UtilCache menuWebappCache = new UtilCache("widget.menu.webappResource", 0, 0, false);
    public static final UtilCache menuLocationCache = new UtilCache("widget.menu.locationResource", 0, 0, false);
    
    public static ModelMenu getMenuFromWebappContext(String resourceName, String menuName, HttpServletRequest request) 
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        String cacheKey = webappName + "::" + resourceName;
        
        
        Map modelMenuMap = (Map) menuWebappCache.get(cacheKey);
        if (modelMenuMap == null) {
            synchronized (MenuFactory.class) {
                modelMenuMap = (Map) menuWebappCache.get(cacheKey);
                if (modelMenuMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
                    GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
                    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
                    
                    URL menuFileUrl = servletContext.getResource(resourceName);
                    Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true);
                    modelMenuMap = readMenuDocument(menuFileDoc, delegator, dispatcher);
                    menuWebappCache.put(cacheKey, modelMenuMap);
                }
            }
        }
        
        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        
        ModelMenu modelMenu = (ModelMenu) modelMenuMap.get(menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        return modelMenu;
    }
    
    public static Map readMenuDocument(Document menuFileDoc, GenericDelegator delegator, LocalDispatcher dispatcher) {
        Map modelMenuMap = new HashMap();
        if (menuFileDoc != null) {
            // read document and construct ModelMenu for each menu element
            Element rootElement = menuFileDoc.getDocumentElement();
            List menuElements = UtilXml.childElementList(rootElement, "menu");
            Iterator menuElementIter = menuElements.iterator();
            while (menuElementIter.hasNext()) {
                Element menuElement = (Element) menuElementIter.next();
                ModelMenu modelMenu = new ModelMenu(menuElement, delegator, dispatcher);
                modelMenuMap.put(modelMenu.getName(), modelMenu);
            }
        }
        return modelMenuMap;
    }

    public static ModelMenu getMenuFromLocation(String resourceName, String menuName, GenericDelegator delegator, LocalDispatcher dispatcher) 
            throws IOException, SAXException, ParserConfigurationException {
        Map modelMenuMap = (Map) menuLocationCache.get(resourceName);
        if (modelMenuMap == null) {
            synchronized (MenuFactory.class) {
                modelMenuMap = (Map) menuLocationCache.get(resourceName);
                if (modelMenuMap == null) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader == null) {
                        loader = MenuFactory.class.getClassLoader();
                    }
                    
                    URL menuFileUrl = null;
                    menuFileUrl = FlexibleLocation.resolveLocation(resourceName); //, loader);
                    Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true);
                    modelMenuMap = readMenuDocument(menuFileDoc, delegator, dispatcher);
                    menuLocationCache.put(resourceName, modelMenuMap);
                }
            }
        }

        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in location [" + resourceName + "]");
        }
        
        ModelMenu modelMenu = (ModelMenu) modelMenuMap.get(menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in location [" + resourceName + "]");
        }
        return modelMenu;
    }
    
}
