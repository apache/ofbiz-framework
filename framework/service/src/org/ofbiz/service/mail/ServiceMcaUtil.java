/*
 * $Id: ServiceMcaUtil.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.service.mail;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Collection;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.entity.GenericValue;

import org.w3c.dom.Element;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.3
 */
public class ServiceMcaUtil {

    public static final String module = ServiceMcaUtil.class.getName();
    public static UtilCache mcaCache = new UtilCache("service.ServiceMCAs", 0, 0, false);

    public static void reloadConfig() {
        mcaCache.clear();
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

        List serviceMcasElements = UtilXml.childElementList(rootElement, "service-mcas");
        Iterator secasIter = serviceMcasElements.iterator();
        while (secasIter.hasNext()) {
            Element serviceMcasElement = (Element) secasIter.next();
            ResourceHandler handler = new MainResourceHandler(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME, serviceMcasElement);
            addMcaDefinitions(handler);
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        List componentResourceInfos = ComponentConfig.getAllServiceResourceInfos("mca");
        Iterator componentResourceInfoIter = componentResourceInfos.iterator();
        while (componentResourceInfoIter.hasNext()) {
            ComponentConfig.ServiceResourceInfo componentResourceInfo = (ComponentConfig.ServiceResourceInfo) componentResourceInfoIter.next();
            addMcaDefinitions(componentResourceInfo.createResourceHandler());
        }
    }

    public static void addMcaDefinitions(ResourceHandler handler) {
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }

        List ecaList = UtilXml.childElementList(rootElement, "mca");
        Iterator ecaIt = ecaList.iterator();
        int numDefs = 0;

        while (ecaIt.hasNext()) {
            Element e = (Element) ecaIt.next();
            String ruleName = e.getAttribute("mail-rule-name");
            mcaCache.put(ruleName, new ServiceMcaRule(e));
            numDefs++;
        }

        if (Debug.importantOn()) {
			String resourceLocation = handler.getLocation();
			try {
				resourceLocation = handler.getURL().toExternalForm();
			} catch (GenericConfigException e) {
				Debug.logError(e, "Could not get resource URL", module);
			}
			Debug.logImportant("Loaded " + numDefs + " Service MCA definitions from " + resourceLocation, module);
        }
    }

    public static Collection getServiceMcaRules() {
        if (mcaCache != null) {
            return mcaCache.values();
        }
        return null;
    }

    public static void evalRules(LocalDispatcher dispatcher, MimeMessageWrapper wrapper, GenericValue userLogin) throws GenericServiceException {
        List rules = mcaCache.values();
        Set actionsRun = new TreeSet();
        if (rules != null) {
            Iterator i = rules.iterator();
            while (i.hasNext()) {
                ServiceMcaRule rule = (ServiceMcaRule) i.next();
                rule.eval(dispatcher, wrapper, actionsRun, userLogin);
            }
        }
    }
}
