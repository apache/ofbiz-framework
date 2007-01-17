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
package org.ofbiz.widget.tree;

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
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.LocalDispatcher;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Tree factory class
 */
public class TreeFactory {
    
    public static final String module = TreeFactory.class.getName();

    public static final UtilCache treeLocationCache = new UtilCache("widget.tree.locationResource", 0, 0, false);
    public static final UtilCache treeWebappCache = new UtilCache("widget.tree.webappResource", 0, 0, false);
    
    public static ModelTree getTreeFromLocation(String resourceName, String treeName, GenericDelegator delegator, LocalDispatcher dispatcher) 
            throws IOException, SAXException, ParserConfigurationException {
        Map modelTreeMap = (Map) treeLocationCache.get(resourceName);
        if (modelTreeMap == null) {
            synchronized (TreeFactory.class) {
                modelTreeMap = (Map) treeLocationCache.get(resourceName);
                if (modelTreeMap == null) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader == null) {
                        loader = TreeFactory.class.getClassLoader();
                    }
                    
                    URL treeFileUrl = null;
                    treeFileUrl = FlexibleLocation.resolveLocation(resourceName); //, loader);
                    Document treeFileDoc = UtilXml.readXmlDocument(treeFileUrl, true);
                    modelTreeMap = readTreeDocument(treeFileDoc, delegator, dispatcher);
                    treeLocationCache.put(resourceName, modelTreeMap);
                }
            }
        }
        
        ModelTree modelTree = (ModelTree) modelTreeMap.get(treeName);
        if (modelTree == null) {
            throw new IllegalArgumentException("Could not find tree with name [" + treeName + "] in class resource [" + resourceName + "]");
        }
        return modelTree;
    }
    
    public static ModelTree getTreeFromWebappContext(String resourceName, String treeName, HttpServletRequest request) 
            throws IOException, SAXException, ParserConfigurationException {
        String webappName = UtilHttp.getApplicationName(request);
        String cacheKey = webappName + "::" + resourceName;
        
        
        Map modelTreeMap = (Map) treeWebappCache.get(cacheKey);
        if (modelTreeMap == null) {
            synchronized (TreeFactory.class) {
                modelTreeMap = (Map) treeWebappCache.get(cacheKey);
                if (modelTreeMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
                    GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
                    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
                    
                    URL treeFileUrl = servletContext.getResource(resourceName);
                    Document treeFileDoc = UtilXml.readXmlDocument(treeFileUrl, true);
                    modelTreeMap = readTreeDocument(treeFileDoc, delegator, dispatcher);
                    treeWebappCache.put(cacheKey, modelTreeMap);
                }
            }
        }
        
        ModelTree modelTree = (ModelTree) modelTreeMap.get(treeName);
        if (modelTree == null) {
            throw new IllegalArgumentException("Could not find tree with name [" + treeName + "] in webapp resource [" + resourceName + "] in the webapp [" + webappName + "]");
        }
        return modelTree;
    }
    
    public static Map readTreeDocument(Document treeFileDoc, GenericDelegator delegator, LocalDispatcher dispatcher) {
        Map modelTreeMap = new HashMap();
        if (treeFileDoc != null) {
            // read document and construct ModelTree for each tree element
            Element rootElement = treeFileDoc.getDocumentElement();
            List treeElements = UtilXml.childElementList(rootElement, "tree");
            Iterator treeElementIter = treeElements.iterator();
            while (treeElementIter.hasNext()) {
                Element treeElement = (Element) treeElementIter.next();
                ModelTree modelTree = new ModelTree(treeElement, delegator, dispatcher);
                modelTreeMap.put(modelTree.getName(), modelTree);
            }
        }
        return modelTreeMap;
    }
}
