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
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Screen factory class
 */
public class ScreenFactory {
    
    public static final String module = ScreenFactory.class.getName();

    public static final UtilCache screenLocationCache = new UtilCache("widget.screen.locationResource", 0, 0, false);
    public static final UtilCache screenWebappCache = new UtilCache("widget.screen.webappResource", 0, 0, false);

    public static boolean isCombinedName(String combinedName) {
        int numSignIndex = combinedName.lastIndexOf("#");
        if (numSignIndex == -1) {
            return false;
        }
        if (numSignIndex + 1 >= combinedName.length()) {
            return false;
        }
        return true;
    }
    
    public static String getResourceNameFromCombined(String combinedName) {
        // split out the name on the last "#"
        int numSignIndex = combinedName.lastIndexOf("#");
        if (numSignIndex == -1) {
            throw new IllegalArgumentException("Error in screen location/name: no \"#\" found to separate the location from the name; correct example: component://product/screen/product/ProductScreens.xml#EditProduct");
        }
        if (numSignIndex + 1 >= combinedName.length()) {
            throw new IllegalArgumentException("Error in screen location/name: the \"#\" was at the end with no screen name after it; correct example: component://product/screen/product/ProductScreens.xml#EditProduct");
        }
        String resourceName = combinedName.substring(0, numSignIndex);
        return resourceName;
    }
    
    public static String getScreenNameFromCombined(String combinedName) {
        // split out the name on the last "#"
        int numSignIndex = combinedName.lastIndexOf("#");
        if (numSignIndex == -1) {
            throw new IllegalArgumentException("Error in screen location/name: no \"#\" found to separate the location from the name; correct example: component://product/screen/product/ProductScreens.xml#EditProduct");
        }
        if (numSignIndex + 1 >= combinedName.length()) {
            throw new IllegalArgumentException("Error in screen location/name: the \"#\" was at the end with no screen name after it; correct example: component://product/screen/product/ProductScreens.xml#EditProduct");
        }
        String screenName = combinedName.substring(numSignIndex + 1);
        return screenName;
    }
    
    public static ModelScreen getScreenFromLocation(String combinedName) 
            throws IOException, SAXException, ParserConfigurationException {
        String resourceName = getResourceNameFromCombined(combinedName);
        String screenName = getScreenNameFromCombined(combinedName);
        return getScreenFromLocation(resourceName, screenName);
    }
    
    public static ModelScreen getScreenFromLocation(String resourceName, String screenName) 
            throws IOException, SAXException, ParserConfigurationException {
        Map modelScreenMap = (Map) screenLocationCache.get(resourceName);
        if (modelScreenMap == null) {
            synchronized (ScreenFactory.class) {
                modelScreenMap = (Map) screenLocationCache.get(resourceName);
                if (modelScreenMap == null) {
                    long startTime = System.currentTimeMillis();
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader == null) {
                        loader = ScreenFactory.class.getClassLoader();
                    }
                    
                    URL screenFileUrl = null;
                    screenFileUrl = FlexibleLocation.resolveLocation(resourceName, loader);
                    if (screenFileUrl == null) {
                        throw new IllegalArgumentException("Could not resolve location to URL: " + resourceName);
                    }
                    Document screenFileDoc = UtilXml.readXmlDocument(screenFileUrl, true);
                    modelScreenMap = readScreenDocument(screenFileDoc, resourceName);
                    screenLocationCache.put(resourceName, modelScreenMap);
                    double totalSeconds = (System.currentTimeMillis() - startTime)/1000.0;
                    Debug.logInfo("Got " + modelScreenMap.size() + " screens in " + totalSeconds + "s from: " + screenFileUrl.toExternalForm(), module);
                }
            }
        }
        
        ModelScreen modelScreen = (ModelScreen) modelScreenMap.get(screenName);
        if (modelScreen == null) {
            throw new IllegalArgumentException("Could not find screen with name [" + screenName + "] in class resource [" + resourceName + "]");
        }
        return modelScreen;
    }
    
    public static ModelScreen getScreenFromWebappContext(String resourceName, String screenName, HttpServletRequest request) 
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        String cacheKey = webappName + "::" + resourceName;
        
        
        Map modelScreenMap = (Map) screenWebappCache.get(cacheKey);
        if (modelScreenMap == null) {
            synchronized (ScreenFactory.class) {
                modelScreenMap = (Map) screenWebappCache.get(cacheKey);
                if (modelScreenMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
                    
                    URL screenFileUrl = servletContext.getResource(resourceName);
                    Document screenFileDoc = UtilXml.readXmlDocument(screenFileUrl, true);
                    modelScreenMap = readScreenDocument(screenFileDoc, resourceName);
                    screenWebappCache.put(cacheKey, modelScreenMap);
                }
            }
        }
        
        ModelScreen modelScreen = (ModelScreen) modelScreenMap.get(screenName);
        if (modelScreen == null) {
            throw new IllegalArgumentException("Could not find screen with name [" + screenName + "] in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        return modelScreen;
    }
    
    public static Map readScreenDocument(Document screenFileDoc, String sourceLocation) {
        Map modelScreenMap = new HashMap();
        if (screenFileDoc != null) {
            // read document and construct ModelScreen for each screen element
            Element rootElement = screenFileDoc.getDocumentElement();
            List screenElements = UtilXml.childElementList(rootElement, "screen");
            Iterator screenElementIter = screenElements.iterator();
            while (screenElementIter.hasNext()) {
                Element screenElement = (Element) screenElementIter.next();
                ModelScreen modelScreen = new ModelScreen(screenElement, modelScreenMap, sourceLocation);
                //Debug.logInfo("Read Screen with name: " + modelScreen.getName(), module);
                modelScreenMap.put(modelScreen.getName(), modelScreen);
            }
        }
        return modelScreenMap;
    }
}
