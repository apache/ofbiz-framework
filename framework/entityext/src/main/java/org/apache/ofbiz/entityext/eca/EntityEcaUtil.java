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
package org.apache.ofbiz.entityext.eca;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.MainResourceHandler;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.config.model.DelegatorElement;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.config.model.EntityEcaReader;
import org.apache.ofbiz.entity.config.model.Resource;
import org.w3c.dom.Element;

/**
 * EntityEcaUtil
 */
public final class EntityEcaUtil {

    public static final String module = EntityEcaUtil.class.getName();

    private static final UtilCache<String, Map<String, Map<String, List<EntityEcaRule>>>> entityEcaReaders = UtilCache.createUtilCache("entity.EcaReaders", 0, 0, false);

    private EntityEcaUtil () {}

    public static Map<String, Map<String, List<EntityEcaRule>>> getEntityEcaCache(String entityEcaReaderName) {
        Map<String, Map<String, List<EntityEcaRule>>> ecaCache = entityEcaReaders.get(entityEcaReaderName);
        if (ecaCache == null) {
            // FIXME: Collections are not thread safe
            ecaCache = new HashMap<String, Map<String, List<EntityEcaRule>>>();
            readConfig(entityEcaReaderName, ecaCache);
            ecaCache = entityEcaReaders.putIfAbsentAndGet(entityEcaReaderName, ecaCache);
        }
        return ecaCache;
    }

    public static String getEntityEcaReaderName(String delegatorName) {
        DelegatorElement delegatorInfo = null;
        try {
            delegatorInfo = EntityConfig.getInstance().getDelegator(delegatorName);
        } catch (GenericEntityConfException e) {
            Debug.logWarning(e, "Exception thrown while getting field type config: ", module);
        }
        if (delegatorInfo == null) {
            Debug.logError("BAD ERROR: Could not find delegator config with name: " + delegatorName, module);
            return null;
        }
        return delegatorInfo.getEntityEcaReader();
    }

    private static void readConfig(String entityEcaReaderName, Map<String, Map<String, List<EntityEcaRule>>> ecaCache) {
        EntityEcaReader entityEcaReaderInfo = null;
        try {
            entityEcaReaderInfo = EntityConfig.getInstance().getEntityEcaReader(entityEcaReaderName);
        } catch (GenericEntityConfException e) {
            Debug.logError(e, "Exception thrown while getting entity-eca-reader config with name: " + entityEcaReaderName, module);
        }
        if (entityEcaReaderInfo == null) {
            Debug.logError("BAD ERROR: Could not find entity-eca-reader config with name: " + entityEcaReaderName, module);
            return;
        }

        List<Future<List<EntityEcaRule>>> futures = new LinkedList<Future<List<EntityEcaRule>>>();
        for (Resource eecaResourceElement : entityEcaReaderInfo.getResourceList()) {
            ResourceHandler handler = new MainResourceHandler(EntityConfig.ENTITY_ENGINE_XML_FILENAME, eecaResourceElement.getLoader(), eecaResourceElement.getLocation());
            futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(createEcaLoaderCallable(handler)));
        }

        // get all of the component resource eca stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.EntityResourceInfo componentResourceInfo: ComponentConfig.getAllEntityResourceInfos("eca")) {
            if (entityEcaReaderName.equals(componentResourceInfo.readerName)) {
                futures.add(ExecutionPool.GLOBAL_FORK_JOIN.submit(createEcaLoaderCallable(componentResourceInfo.createResourceHandler())));
            }
        }

        for (List<EntityEcaRule> oneFileRules: ExecutionPool.getAllFutures(futures)) {
            for (EntityEcaRule rule: oneFileRules) {
                String entityName = rule.getEntityName();
                String eventName = rule.getEventName();
                Map<String, List<EntityEcaRule>> eventMap = ecaCache.get(entityName);
                List<EntityEcaRule> rules = null;
                if (eventMap == null) {
                    eventMap = new HashMap<String, List<EntityEcaRule>>();
                    rules = new LinkedList<EntityEcaRule>();
                    ecaCache.put(entityName, eventMap);
                    eventMap.put(eventName, rules);
                } else {
                    rules = eventMap.get(eventName);
                    if (rules == null) {
                        rules = new LinkedList<EntityEcaRule>();
                        eventMap.put(eventName, rules);
                    }
                }
                //remove the old rule if found and keep the recent one
                //This will prevent duplicate rule execution along with enabled/disabled eca workflow
                if (rules.remove(rule)) {
                    Debug.logWarning("Duplicate Entity ECA [" + entityName + "]" + "for operation [ "+ rule.getOperationName() + "] " + "on [" + eventName + "] ", module);
                }
                rules.add(rule);
            }
        }
    }

    private static List<EntityEcaRule> getEcaDefinitions(ResourceHandler handler) {
        List<EntityEcaRule> rules = new LinkedList<EntityEcaRule>();
        Element rootElement = null;
        try {
            rootElement = handler.getDocument().getDocumentElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
            return rules;
        }
        for (Element e: UtilXml.childElementList(rootElement, "eca")) {
            rules.add(new EntityEcaRule(e));
        }
        try {
            Debug.logInfo("Loaded [" + rules.size() + "] Entity ECA definitions from " + handler.getFullLocation() + " in loader " + handler.getLoaderName(), module);
        } catch (GenericConfigException e) {
            Debug.logError(e, module);
        }
        return rules;
    }

    private static Callable<List<EntityEcaRule>> createEcaLoaderCallable(final ResourceHandler handler) {
        return new Callable<List<EntityEcaRule>>() {
            public List<EntityEcaRule> call() throws Exception {
                return getEcaDefinitions(handler);
            }
        };
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
