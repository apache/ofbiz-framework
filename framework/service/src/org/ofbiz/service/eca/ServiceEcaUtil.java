/*
 * $Id: ServiceEcaUtil.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2002-2003 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.eca;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;

import org.w3c.dom.Element;

/**
 * ServiceEcaUtil
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ServiceEcaUtil {

    public static final String module = ServiceEcaUtil.class.getName();

    public static UtilCache ecaCache = new UtilCache("service.ServiceECAs", 0, 0, false);

    public static void reloadConfig() {
        ecaCache.clear();
        readConfig();
    }

    public static void readConfig() {
        Element rootElement = null;
        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
            return;
        }

        List serviceEcasElements = UtilXml.childElementList(rootElement, "service-ecas");
        Iterator secasIter = serviceEcasElements.iterator();
        while (secasIter.hasNext()) {
            Element serviceEcasElement = (Element) secasIter.next();
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, serviceEcasElement);
            addEcaDefinitions(handler);
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        List componentResourceInfos = ComponentConfig.getAllServiceResourceInfos("eca");
        Iterator componentResourceInfoIter = componentResourceInfos.iterator();
        while (componentResourceInfoIter.hasNext()) {
            ComponentConfig.ServiceResourceInfo componentResourceInfo = (ComponentConfig.ServiceResourceInfo) componentResourceInfoIter.next();
            addEcaDefinitions(componentResourceInfo.createResourceHandler());
        }
    }

    public static void addEcaDefinitions(ResourceHandler handler) {
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }

        List ecaList = UtilXml.childElementList(rootElement, "eca");
        Iterator ecaIt = ecaList.iterator();
        int numDefs = 0;
        while (ecaIt.hasNext()) {
            Element e = (Element) ecaIt.next();
            String serviceName = e.getAttribute("service");
            String eventName = e.getAttribute("event");
            Map eventMap = (Map) ecaCache.get(serviceName);
            List rules = null;

            if (eventMap == null) {
                eventMap = new HashMap();
                rules = new LinkedList();
                ecaCache.put(serviceName, eventMap);
                eventMap.put(eventName, rules);
            } else {
                rules = (List) eventMap.get(eventName);
                if (rules == null) {
                    rules = new LinkedList();
                    eventMap.put(eventName, rules);
                }
            }
            rules.add(new ServiceEcaRule(e));
            numDefs++;
        }
        if (Debug.importantOn()) {
			String resourceLocation = handler.getLocation();
			try {
				resourceLocation = handler.getURL().toExternalForm();
			} catch (GenericConfigException e) {
				Debug.logError(e, "Could not get resource URL", module);
			}
			Debug.logImportant("Loaded " + numDefs + " Service ECA definitions from " + resourceLocation, module);
        }
    }

    public static Map getServiceEventMap(String serviceName) {
        if (ServiceEcaUtil.ecaCache == null) ServiceEcaUtil.readConfig();
        return (Map) ServiceEcaUtil.ecaCache.get(serviceName);
    }

    public static Collection getServiceEventRules(String serviceName, String event) {
        Map eventMap = getServiceEventMap(serviceName);
        if (eventMap != null) {
            if (event != null) {
                return (Collection) eventMap.get(event);
            } else {
                return eventMap.values();
            }
        }
        return null;
    }

    public static void evalRules(String serviceName, Map eventMap, String event, DispatchContext dctx, Map context, Map result, boolean isError, boolean isFailure) throws GenericServiceException {
        // if the eventMap is passed we save a HashMap lookup, but if not that's okay we'll just look it up now
        if (eventMap == null) eventMap = getServiceEventMap(serviceName);
        if (eventMap == null || eventMap.size() == 0) {
            return;
        }

        List rules = (List) eventMap.get(event);
        if (rules == null || rules.size() == 0) {
            return;
        }

        Iterator i = rules.iterator();
        if (i.hasNext() && Debug.verboseOn()) Debug.logVerbose("Running ECA (" + event + ").", module);
        Set actionsRun = new TreeSet();
        while (i.hasNext()) {
            ServiceEcaRule eca = (ServiceEcaRule) i.next();
            eca.eval(serviceName, dctx, context, result, isError, isFailure, actionsRun);
        }
    }
}
