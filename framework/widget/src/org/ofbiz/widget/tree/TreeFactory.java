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
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.Delegator;
import org.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


/**
 * Widget Library - Tree factory class
 */
public class TreeFactory {

    public static final String module = TreeFactory.class.getName();

    public static final UtilCache<String, Map<String, ModelTree>> treeLocationCache = UtilCache.createUtilCache("widget.tree.locationResource", 0, 0, false);
    public static final UtilCache<String, Map<String, ModelTree>> treeWebappCache = UtilCache.createUtilCache("widget.tree.webappResource", 0, 0, false);

    public static ModelTree getTreeFromLocation(String resourceName, String treeName, Delegator delegator, LocalDispatcher dispatcher)
            throws IOException, SAXException, ParserConfigurationException {
        Map<String, ModelTree> modelTreeMap = treeLocationCache.get(resourceName);
        if (modelTreeMap == null) {
            synchronized (TreeFactory.class) {
                modelTreeMap = treeLocationCache.get(resourceName);
                if (modelTreeMap == null) {
                    ClassLoader loader = Thread.currentThread().getContextClassLoader();
                    if (loader == null) {
                        loader = TreeFactory.class.getClassLoader();
                    }

                    URL treeFileUrl = null;
                    treeFileUrl = FlexibleLocation.resolveLocation(resourceName); //, loader);
                    Document treeFileDoc = UtilXml.readXmlDocument(treeFileUrl, true);
                    modelTreeMap = readTreeDocument(treeFileDoc, delegator, dispatcher, resourceName);
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


        Map<String, ModelTree> modelTreeMap = treeWebappCache.get(cacheKey);
        if (modelTreeMap == null) {
            synchronized (TreeFactory.class) {
                modelTreeMap = treeWebappCache.get(cacheKey);
                if (modelTreeMap == null) {
                    ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
                    Delegator delegator = (Delegator) request.getAttribute("delegator");
                    LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

                    URL treeFileUrl = servletContext.getResource(resourceName);
                    Document treeFileDoc = UtilXml.readXmlDocument(treeFileUrl, true);
                    modelTreeMap = readTreeDocument(treeFileDoc, delegator, dispatcher, cacheKey);
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

    public static Map<String, ModelTree> readTreeDocument(Document treeFileDoc, Delegator delegator, LocalDispatcher dispatcher, String treeLocation) {
        Map<String, ModelTree> modelTreeMap = new HashMap<String, ModelTree>();
        if (treeFileDoc != null) {
            // read document and construct ModelTree for each tree element
            Element rootElement = treeFileDoc.getDocumentElement();
            for (Element treeElement: UtilXml.childElementList(rootElement, "tree")) {
                ModelTree modelTree = new ModelTree(treeElement, delegator, dispatcher);
                modelTree.setTreeLocation(treeLocation);
                modelTreeMap.put(modelTree.getName(), modelTree);
            }
        }
        return modelTreeMap;
    }
}
