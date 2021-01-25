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
package org.apache.ofbiz.widget.model;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ofbiz.base.location.FlexibleLocation;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Menu factory class
 */
public class MenuFactory {

    private static final String MODULE = MenuFactory.class.getName();

    public static final UtilCache<String, Map<String, ModelMenu>> MENU_WEBAPP_CACHE =
            UtilCache.createUtilCache("widget.menu.webappResource", 0, 0, false);
    public static final UtilCache<String, Map<String, ModelMenu>> MENU_LOCATION_CACHE =
            UtilCache.createUtilCache("widget.menu.locationResource", 0, 0, false);

    public static ModelMenu getMenuFromWebappContext(String resourceName, String menuName, HttpServletRequest request)
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        VisualTheme visualTheme = ThemeFactory.resolveVisualTheme(request);
        String location = webappName + "::" + resourceName;
        String cacheKey = location;

        if (UtilValidate.isNotEmpty(visualTheme)) {
            cacheKey += "::" + visualTheme.getVisualThemeId();
        }

        Map<String, ModelMenu> modelMenuMap = MENU_WEBAPP_CACHE.get(cacheKey);
        if (modelMenuMap == null) {
            ServletContext servletContext = request.getServletContext();

            URL menuFileUrl = servletContext.getResource(resourceName);
            Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true, true);
            modelMenuMap = readMenuDocument(menuFileDoc, location, visualTheme);
            MENU_WEBAPP_CACHE.putIfAbsent(cacheKey, modelMenuMap);
            modelMenuMap = MENU_WEBAPP_CACHE.get(cacheKey);
        }

        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in webapp resource [" + resourceName + "] in the webapp ["
                    + webappName + "]");
        }

        ModelMenu modelMenu = modelMenuMap.get(menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in webapp resource [" + resourceName
                    + "] in the webapp [" + webappName + "]");
        }
        return modelMenu;
    }

    public static Map<String, ModelMenu> readMenuDocument(Document menuFileDoc, String menuLocation, VisualTheme visualTheme) {
        Map<String, ModelMenu> modelMenuMap = new HashMap<>();
        if (menuFileDoc != null) {
            // read document and construct ModelMenu for each menu element
            Element rootElement = menuFileDoc.getDocumentElement();
            if (!"menus".equalsIgnoreCase(rootElement.getTagName())) {
                rootElement = UtilXml.firstChildElement(rootElement, "menus");
            }
            for (Element menuElement: UtilXml.childElementList(rootElement, "menu")) {
                ModelMenu modelMenu = new ModelMenu(menuElement, menuLocation, visualTheme);
                modelMenuMap.put(modelMenu.getName(), modelMenu);
            }
        }
        return modelMenuMap;
    }

    public static ModelMenu getMenuFromLocation(String resourceName, String menuName, VisualTheme visualTheme)
            throws IOException, SAXException, ParserConfigurationException {
        String keyName = resourceName + "::" + visualTheme.getVisualThemeId();
        Map<String, ModelMenu> modelMenuMap = MENU_LOCATION_CACHE.get(keyName);
        if (modelMenuMap == null) {
            URL menuFileUrl = FlexibleLocation.resolveLocation(resourceName);
            Document menuFileDoc = UtilXml.readXmlDocument(menuFileUrl, true, true);
            modelMenuMap = readMenuDocument(menuFileDoc, resourceName, visualTheme);
            MENU_LOCATION_CACHE.putIfAbsent(keyName, modelMenuMap);
            modelMenuMap = MENU_LOCATION_CACHE.get(keyName);
        }

        if (UtilValidate.isEmpty(modelMenuMap)) {
            throw new IllegalArgumentException("Could not find menu file in location [" + resourceName + "]");
        }

        ModelMenu modelMenu = modelMenuMap.get(menuName);
        if (modelMenu == null) {
            throw new IllegalArgumentException("Could not find menu with name [" + menuName + "] in location [" + resourceName + "]");
        }
        return modelMenu;
    }
}
