/*
 * $Id: ComponentLoaderConfig.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
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
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.0
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
