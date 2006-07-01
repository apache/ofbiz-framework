/*
 * $Id: ServiceConfigUtil.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
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
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
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
