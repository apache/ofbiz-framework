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
package org.ofbiz.base.component;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ComponentLoaderConfig - Component Loader configuration
 *
 */
public class ComponentLoaderConfig {
    
    public static final String module = ComponentLoaderConfig.class.getName();    
    public static final String COMPONENT_LOAD_XML_FILENAME = "component-load.xml";
    
    public static final int SINGLE_COMPONENT = 0;
    public static final int COMPONENT_DIRECTORY = 1;
    
    protected static List componentsToLoad = null;
    
    public static List getRootComponents(String configFile) throws ComponentException {
        if (componentsToLoad == null) {
            synchronized (ComponentLoaderConfig.class) {
                if (componentsToLoad ==  null) {
                    if (configFile == null) {
                        configFile = COMPONENT_LOAD_XML_FILENAME;
                    }
                    URL xmlUrl = UtilURL.fromResource(configFile);
                    ComponentLoaderConfig.componentsToLoad = ComponentLoaderConfig.getComponentsFromConfig(xmlUrl);
                }                
            }
        }
        return componentsToLoad;
    }

    public static List getComponentsFromConfig(URL configUrl) throws ComponentException {
        if (configUrl == null) {
            throw new ComponentException("Component config file does not exist: " + configUrl);
        }

        List componentsFromConfig = null;
        Document document = null;
        try {
            document = UtilXml.readXmlDocument(configUrl, true);
        } catch (SAXException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        } catch (IOException e) {
            throw new ComponentException("Error reading the component config file: " + configUrl, e);
        }

        Element root = document.getDocumentElement();
        List toLoad = UtilXml.childElementList(root);
        if (toLoad != null && toLoad.size() > 0) {
            componentsFromConfig = new LinkedList();
            Iterator i = toLoad.iterator();
            while (i.hasNext()) {
                Element element = (Element) i.next();
                componentsFromConfig.add(new ComponentDef(element));
            }
        }
        return componentsFromConfig;
    }

    public static class ComponentDef {
        public String name;
        public String location;
        public int type = -1;
        
        public ComponentDef(Element element) {            
            Properties systemProps = System.getProperties();
            if ("load-component".equals(element.getNodeName())) {
                name = element.getAttribute("component-name");
                location = FlexibleStringExpander.expandString(element.getAttribute("component-location"), systemProps);
                type = SINGLE_COMPONENT;
            } else if ("load-components".equals(element.getNodeName())) {
                name = null;
                location = FlexibleStringExpander.expandString(element.getAttribute("parent-directory"), systemProps);
                type = COMPONENT_DIRECTORY;
            }
        }                
    }
}
