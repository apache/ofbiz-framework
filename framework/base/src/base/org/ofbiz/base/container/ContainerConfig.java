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
package org.ofbiz.base.container;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;

import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;

import org.apache.commons.collections.map.LinkedMap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * ContainerConfig - Container configuration for ofbiz.xml
 *
 */
public class ContainerConfig {
    
    public static final String module = ContainerConfig.class.getName();
    
    protected static Map containers = new LinkedMap();
    
    public static Container getContainer(String containerName, String configFile) throws ContainerException {
        Container container = (Container) containers.get(containerName);
        if (container == null) {            
            synchronized (ContainerConfig.class) {
                container = (Container) containers.get(containerName);
                if (container == null) {
                    if (configFile == null) {
                        throw new ContainerException("Container config file cannot be null");
                    }
                    new ContainerConfig(configFile);
                    container = (Container) containers.get(containerName);
                }                
            }
            if (container == null) {
                throw new ContainerException("No container found with the name : " + containerName);
            }            
        }
        return container;
    }
    
    public static Collection getContainers(String configFile) throws ContainerException {
        if (containers.size() == 0) {
            synchronized (ContainerConfig.class) {                
                if (containers.size() == 0) {
                    if (configFile == null) {
                        throw new ContainerException("Container config file cannot be null");
                    }
                    new ContainerConfig(configFile);                    
                }                
            }
            if (containers.size() == 0) {
                throw new ContainerException("No containers loaded; problem with configuration");
            }            
        }
        return containers.values();
    }

    public static String getPropertyValue(ContainerConfig.Container parentProp, String name, String defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return prop.value;
        }
    }

    public static int getPropertyValue(ContainerConfig.Container parentProp, String name, int defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            int num = defaultValue;
            try {
                num = Integer.parseInt(prop.value);
            } catch (Exception e) {
                return defaultValue;
            }
            return num;
        }
    }

    public static boolean getPropertyValue(ContainerConfig.Container parentProp, String name, boolean defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return "true".equalsIgnoreCase(prop.value);
        }
    }

    public static String getPropertyValue(ContainerConfig.Container.Property parentProp, String name, String defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return prop.value;
        }
    }

    public static int getPropertyValue(ContainerConfig.Container.Property parentProp, String name, int defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            int num = defaultValue;
            try {
                num = Integer.parseInt(prop.value);
            } catch (Exception e) {
                return defaultValue;
            }
            return num;
        }
    }

    public static boolean getPropertyValue(ContainerConfig.Container.Property parentProp, String name, boolean defaultValue) {
        ContainerConfig.Container.Property prop = parentProp.getProperty(name);
        if (prop == null || UtilValidate.isEmpty(prop.value)) {
            return defaultValue;
        } else {
            return "true".equalsIgnoreCase(prop.value);
        }
    }

    protected ContainerConfig() {}
    
    protected ContainerConfig(String configFileLocation) throws ContainerException {        
        // load the config file
        URL xmlUrl = UtilURL.fromResource(configFileLocation);
        if (xmlUrl == null) {
            throw new ContainerException("Could not find " + configFileLocation + " master OFBiz container configuration");
        }
        
        // read the document
        Document containerDocument = null;
        try {
            containerDocument = UtilXml.readXmlDocument(xmlUrl, true);
        } catch (SAXException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        } catch (ParserConfigurationException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        } catch (IOException e) {
            throw new ContainerException("Error reading the container config file: " + xmlUrl, e);
        } 
        
        // root element
        Element root = containerDocument.getDocumentElement();        
          
        // containers
        Iterator elementIter = UtilXml.childElementList(root, "container").iterator();
        while (elementIter.hasNext()) {
            Element curElement = (Element) elementIter.next();
            Container container = new Container(curElement);
            containers.put(container.name, container);    
        }                          
    }
    
    public static class Container {
        public String name;
        public String className;
        public Map properties;
        
        public Container(Element element) {
            this.name = element.getAttribute("name");
            this.className = element.getAttribute("class");
            
            properties = new LinkedMap();
            Iterator elementIter = UtilXml.childElementList(element, "property").iterator();
            while (elementIter.hasNext()) {
                Element curElement = (Element) elementIter.next();
                Property property = new Property(curElement);
                properties.put(property.name, property);
            }                       
        }
        
        public Property getProperty(String name) {
            return (Property) properties.get(name);
        }

        public List getPropertiesWithValue(String value) {
            List props = new LinkedList();
            if (properties != null && properties.size() > 0) {
                Iterator i = properties.entrySet().iterator();
                while (i.hasNext()) {
                    Map.Entry e = (Map.Entry) i.next();
                    Property p = (Property) e.getValue();
                    if (p != null && value.equals(p.value)) {
                        props.add(p);
                    }
                }
            }
            return props;
        }

        public static class Property {
            public String name;
            public String value;
            public Map properties;
            
            public Property(Element element) {
                this.name = element.getAttribute("name");
                this.value = element.getAttribute("value");
                if (UtilValidate.isEmpty(this.value)) {
                    this.value = UtilXml.childElementValue(element, "property-value");                    
                }

                properties = new LinkedMap();
                Iterator elementIter = UtilXml.childElementList(element, "property").iterator();
                while (elementIter.hasNext()) {
                    Element curElement = (Element) elementIter.next();
                    Property property = new Property(curElement);
                    properties.put(property.name, property);                    
                }                    
            }
            
            public Property getProperty(String name) {
                return (Property) properties.get(name);
            }

            public List getPropertiesWithValue(String value) {
                List props = new LinkedList();
                if (properties != null && properties.size() > 0) {
                    Iterator i = properties.entrySet().iterator();
                    while (i.hasNext()) {
                        Map.Entry e = (Map.Entry) i.next();
                        Property p = (Property) e.getValue();
                        if (p != null && value.equals(p.value)) {
                            props.add(p);
                        }
                    }
                }
                return props;
            }
        }
    }                        
}
