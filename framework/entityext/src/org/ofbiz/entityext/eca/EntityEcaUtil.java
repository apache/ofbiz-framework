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
package org.ofbiz.entityext.eca;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Collection;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.EntityEcaReaderInfo;
import org.ofbiz.entity.Delegator;
import org.w3c.dom.Element;

/**
 * EntityEcaUtil
 */
public class EntityEcaUtil {

    public static final String module = EntityEcaUtil.class.getName();

    public static UtilCache<String, Map<String, Map<String, List<EntityEcaRule>>>> entityEcaReaders = UtilCache.createUtilCache("entity.EcaReaders", 0, 0, false);

    public static Map<String, Map<String, List<EntityEcaRule>>> getEntityEcaCache(String entityEcaReaderName) {
        Map<String, Map<String, List<EntityEcaRule>>> ecaCache = entityEcaReaders.get(entityEcaReaderName);
        if (ecaCache == null) {
            synchronized (EntityEcaUtil.class) {
                ecaCache = entityEcaReaders.get(entityEcaReaderName);
                if (ecaCache == null) {
                    ecaCache = FastMap.newInstance();
                    readConfig(entityEcaReaderName, ecaCache);
                    entityEcaReaders.put(entityEcaReaderName, ecaCache);
                }
            }
        }
        return ecaCache;
    }

    public static String getEntityEcaReaderName(String delegatorName) {
        DelegatorInfo delegatorInfo = EntityConfigUtil.getDelegatorInfo(delegatorName);
        if (delegatorInfo == null) {
            Debug.logError("BAD ERROR: Could not find delegator config with name: " + delegatorName, module);
            return null;
        }
        return delegatorInfo.entityEcaReader;
    }

    protected static void readConfig(String entityEcaReaderName, Map<String, Map<String, List<EntityEcaRule>>> ecaCache) {
        EntityEcaReaderInfo entityEcaReaderInfo = EntityConfigUtil.getEntityEcaReaderInfo(entityEcaReaderName);
        if (entityEcaReaderInfo == null) {
            Debug.logError("BAD ERROR: Could not find entity-eca-reader config with name: " + entityEcaReaderName, module);
            return;
        }

        for (Element eecaResourceElement: entityEcaReaderInfo.resourceElements) {
            ResourceHandler handler = new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, eecaResourceElement);
            addEcaDefinitions(handler, ecaCache);
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.EntityResourceInfo componentResourceInfo: ComponentConfig.getAllEntityResourceInfos("eca")) {
            if (entityEcaReaderName.equals(componentResourceInfo.readerName)) {
                addEcaDefinitions(componentResourceInfo.createResourceHandler(), ecaCache);
            }
        }
    }

    protected static void addEcaDefinitions(ResourceHandler handler, Map<String, Map<String, List<EntityEcaRule>>> ecaCache) {
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }

        int numDefs = 0;
        for (Element e: UtilXml.childElementList(rootElement, "eca")) {
            String entityName = e.getAttribute("entity");
            String eventName = e.getAttribute("event");
            Map<String, List<EntityEcaRule>> eventMap = ecaCache.get(entityName);
            List<EntityEcaRule> rules = null;
            if (eventMap == null) {
                eventMap = FastMap.newInstance();
                rules = FastList.newInstance();
                ecaCache.put(entityName, eventMap);
                eventMap.put(eventName, rules);
            } else {
                rules = eventMap.get(eventName);
                if (rules == null) {
                    rules = FastList.newInstance();
                    eventMap.put(eventName, rules);
                }
            }
            rules.add(new EntityEcaRule(e));
            numDefs++;
        }
        try {
            Debug.logImportant("Loaded [" + numDefs + "] Entity ECA definitions from " + handler.getFullLocation() + " in loader " + handler.getLoaderName(), module);
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return;
        }
    }

    public static Collection<EntityEcaRule> getEntityEcaRules(Delegator delegator, String entityName, String event) {
        Map<String, Map<String, List<EntityEcaRule>>> ecaCache = EntityEcaUtil.getEntityEcaCache(EntityEcaUtil.getEntityEcaReaderName(delegator.getDelegatorName()));
        Map<String, List<EntityEcaRule>> eventMap = ecaCache.get(entityName);
        if (eventMap != null) {
            if (event != null) {
                return eventMap.get(event);
            }
        }
        return null;
    }
}
