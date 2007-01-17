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
package org.ofbiz.service.config;

import java.util.Iterator;
import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the serviceengine.xml file
 */
public class ServiceConfigUtil {
    
    public static final String module = ServiceConfigUtil.class.getName();    
    public static final String SERVICE_ENGINE_XML_FILENAME = "serviceengine.xml";

    public static Element getXmlRootElement() throws GenericConfigException {
        return ResourceLoader.getXmlRootElement(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME);
    }

    public static Document getXmlDocument() throws GenericConfigException {
        return ResourceLoader.getXmlDocument(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME);
    }

    public static Element getElement(String elementName) {
        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
        }
        return  UtilXml.firstChildElement(rootElement, elementName);       
    }
    
    public static String getElementAttr(String elementName, String attrName) {        
        Element element = getElement(elementName);

        if (element == null) return null;
        return element.getAttribute(attrName);
    }

    public static String getSendPool() {
        return getElementAttr("thread-pool", "send-to-pool");        
    }
    
    public static List getRunPools() {
        List readPools = null;
        
        Element threadPool = getElement("thread-pool");
        List readPoolElements = UtilXml.childElementList(threadPool, "run-from-pool");
        if (readPoolElements != null) {
            readPools = FastList.newInstance();        
            Iterator i = readPoolElements.iterator();
        
            while (i.hasNext()) {                
                Element e = (Element) i.next();
                readPools.add(e.getAttribute("name"));
            }
        }
        return readPools;
    }
    
    public static int getPurgeJobDays() {
        String days = getElementAttr("thread-pool", "purge-job-days");
        int purgeDays = 0;
        try {
            purgeDays = Integer.parseInt(days);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Cannot read the number of days to keep jobs; not purging", module);
            purgeDays = 0;
        }
        return purgeDays;
    }

    public static int getFailedRetryMin() {
        String minString = getElementAttr("thread-pool", "failed-retry-min");
        int retryMin = 30;
        try {
            retryMin = Integer.parseInt(minString);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse retry minutes; using default of 30", module);
            retryMin = 30;
        }
        return retryMin;
    }    
}
