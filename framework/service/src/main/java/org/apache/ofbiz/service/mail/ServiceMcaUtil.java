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
package org.apache.ofbiz.service.mail;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

public final class ServiceMcaUtil {

    private static final String MODULE = ServiceMcaUtil.class.getName();
    private static final UtilCache<String, ServiceMcaRule> MCA_CACHE = UtilCache.createUtilCache("service.ServiceMCAs", 0, 0, false);

    private ServiceMcaUtil() { }

    public static void reloadConfig() {
        MCA_CACHE.clear();
        readConfig();
    }

    public static void readConfig() {
        // TODO: Missing in XSD file.

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.ServiceResourceInfo componentResourceInfo: ComponentConfig.getAllServiceResourceInfos("mca")) {
            addMcaDefinitions(componentResourceInfo.createResourceHandler());
        }
    }

    public static void addMcaDefinitions(ResourceHandler handler) {
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, MODULE);
            return;
        }

        int numDefs = 0;
        for (Element e: UtilXml.childElementList(rootElement, "mca")) {
            String ruleName = e.getAttribute("mail-rule-name");
            MCA_CACHE.put(ruleName, new ServiceMcaRule(e));
            numDefs++;
        }

        if (Debug.importantOn()) {
            String resourceLocation = handler.getLocation();
            try {
                resourceLocation = handler.getURL().toExternalForm();
            } catch (GenericConfigException e) {
                Debug.logError(e, "Could not get resource URL", MODULE);
            }
            Debug.logImportant("Loaded " + numDefs + " Service MCA definitions from " + resourceLocation, MODULE);
        }
    }

    public static Collection<ServiceMcaRule> getServiceMcaRules() {
        if (MCA_CACHE.isEmpty()) {
            readConfig();
        }
        return MCA_CACHE.values();
    }

    public static void evalRules(LocalDispatcher dispatcher, MimeMessageWrapper wrapper, GenericValue userLogin) throws GenericServiceException {
        Set<String> actionsRun = new TreeSet<>();
        for (ServiceMcaRule rule: getServiceMcaRules()) {
            rule.eval(dispatcher, wrapper, actionsRun, userLogin);
        }
    }
}
