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
package org.apache.ofbiz.entity.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.config.MainResourceHandler;
import org.apache.ofbiz.base.config.ResourceHandler;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.apache.ofbiz.entity.config.model.DelegatorElement;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.config.model.EntityGroupReader;
import org.apache.ofbiz.entity.config.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generic Entity - Entity Group Definition Reader
 *
 */
@SuppressWarnings("serial")
public class ModelGroupReader implements Serializable {

    public static final String module = ModelGroupReader.class.getName();
    private static final UtilCache<String, ModelGroupReader> readers = UtilCache.createUtilCache("entity.ModelGroupReader", 0, 0);

    private volatile Map<String, String> groupCache = null;
    private Set<String> groupNames = null;

    public String modelName;
    public List<ResourceHandler> entityGroupResourceHandlers = new LinkedList<>();

    public static ModelGroupReader getModelGroupReader(String delegatorName) throws GenericEntityConfException {
        DelegatorElement delegatorInfo = EntityConfig.getInstance().getDelegator(delegatorName);

        if (delegatorInfo == null) {
            throw new GenericEntityConfException("Could not find a delegator with the name " + delegatorName);
        }

        String tempModelName = delegatorInfo.getEntityGroupReader();
        ModelGroupReader reader = readers.get(tempModelName);

        if (reader == null) {
            reader = readers.putIfAbsentAndGet(tempModelName, new ModelGroupReader(delegatorName, tempModelName));
        }
        return reader;
    }

    public ModelGroupReader(String delegatorName, String modelName) throws GenericEntityConfException {
        this.modelName = modelName;
        EntityGroupReader entityGroupReaderInfo = EntityConfig.getInstance().getEntityGroupReader(modelName);

        if (entityGroupReaderInfo == null) {
            throw new GenericEntityConfException("Cound not find an entity-group-reader with the name " + modelName);
        }
        for (Resource resourceElement: entityGroupReaderInfo.getResourceList()) {
            this.entityGroupResourceHandlers.add(new MainResourceHandler(EntityConfig.ENTITY_ENGINE_XML_FILENAME, resourceElement.getLoader(), resourceElement.getLocation()));
        }

        // get all of the component resource group stuff, ie specified in each ofbiz-component.xml file
        for (ComponentConfig.EntityResourceInfo componentResourceInfo: ComponentConfig.getAllEntityResourceInfos("group")) {
            if (modelName.equals(componentResourceInfo.readerName)) {
                this.entityGroupResourceHandlers.add(componentResourceInfo.createResourceHandler());
            }
        }

        // preload caches...
        getGroupCache(delegatorName);
    }

    public Map<String, String> getGroupCache(String delegatorName) {
        if (this.groupCache == null) { // don't want to block here
            synchronized (ModelGroupReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (this.groupCache == null) {
                    // now it's safe
                    this.groupCache = new HashMap<>();
                    this.groupNames = new TreeSet<>();

                    UtilTimer utilTimer = new UtilTimer();
                    // utilTimer.timerString("[ModelGroupReader.getGroupCache] Before getDocument");

                    int i = 0;
                    for (ResourceHandler entityGroupResourceHandler: this.entityGroupResourceHandlers) {
                        Document document = null;

                        try {
                            document = entityGroupResourceHandler.getDocument();
                        } catch (GenericConfigException e) {
                            Debug.logError(e, "Error loading entity group model", module);
                        }
                        if (document == null) {
                            this.groupCache = null;
                            return null;
                        }

                        // utilTimer.timerString("[ModelGroupReader.getGroupCache] Before getDocumentElement");
                        Element docElement = document.getDocumentElement();
                        if (docElement == null) {
                            continue;
                        }
                        docElement.normalize();

                        Node curChild = docElement.getFirstChild();
                        if (curChild != null) {
                            utilTimer.timerString("[ModelGroupReader.getGroupCache] Before start of entity loop");
                            do {
                                if (curChild.getNodeType() == Node.ELEMENT_NODE && "entity-group".equals(curChild.getNodeName())) {
                                    Element curEntity = (Element) curChild;
                                    String entityName = UtilXml.checkEmpty(curEntity.getAttribute("entity")).intern();
                                    String groupName = UtilXml.checkEmpty(curEntity.getAttribute("group")).intern();

                                    try {
                                        if (null == EntityConfig.getInstance().getDelegator(delegatorName).getGroupDataSource(groupName)) {
                                            Debug.logError("The declared group name " + groupName + " has no corresponding group-map in entityengine.xml: ", module);
                                        }
                                    } catch (GenericEntityConfException e) {
                                        Debug.logWarning(e, "Exception thrown while getting group name: ", module);
                                    }
                                    this.groupNames.add(groupName);
                                    this.groupCache.put(entityName, groupName);
                                    // utilTimer.timerString("  After entityEntityName -- " + i + " --");
                                    i++;
                                }
                            } while ((curChild = curChild.getNextSibling()) != null);
                        } else {
                            Debug.logWarning("[ModelGroupReader.getGroupCache] No child nodes found.", module);
                        }
                    }
                    utilTimer.timerString("[ModelGroupReader.getGroupCache] FINISHED - Total Entity-Groups: " + i + " FINISHED");
                }
            }
        }
        return this.groupCache;
    }

    /** Gets a group name based on a definition from the specified XML Entity Group descriptor file.
     * @param entityName The entityName of the Entity Group definition to use.
     * @return A group name
     */
    public String getEntityGroupName(String entityName, String delegatorBaseName) {
        Map<String, String> gc = getGroupCache(delegatorBaseName);

        if (gc != null) {
            String groupName = gc.get(entityName);
            if (groupName == null) {
                DelegatorElement delegatorInfo = null;
                try {
                    delegatorInfo = EntityConfig.getInstance().getDelegator(delegatorBaseName);
                } catch (GenericEntityConfException e) {
                    Debug.logWarning(e, "Exception thrown while getting delegator config: ", module);
                }
                if (delegatorInfo == null) {
                    throw new RuntimeException("Could not find DelegatorInfo for delegatorBaseName [" + delegatorBaseName + "]");
                }
                groupName = delegatorInfo.getDefaultGroupName();
            }
            return groupName;
        } else {
            return null;
        }
    }

    /** Creates a Set with all of the groupNames defined in the specified XML Entity Group Descriptor file.
     * @return A Set of groupNames Strings
     */
    public Set<String> getGroupNames(String delegatorBaseName) {
        if (delegatorBaseName.indexOf('#') >= 0) {
            delegatorBaseName = delegatorBaseName.substring(0, delegatorBaseName.indexOf('#'));
        }
        getGroupCache(delegatorBaseName);
        if (this.groupNames == null) return null;
        Set<String> newSet = new HashSet<>();
        try {
            newSet.add(EntityConfig.getInstance().getDelegator(delegatorBaseName).getDefaultGroupName());
        } catch (GenericEntityConfException e) {
            Debug.logWarning(e, "Exception thrown while getting delegator config: ", module);
        }
        newSet.addAll(this.groupNames);
        return newSet;
    }

    /** Creates a Set with names of all of the entities for a given group
     * @param groupName
     * @return A Set of entityName Strings
     */
    public Set<String> getEntityNamesByGroup(String delegatorBaseName, String groupName) {
        Map<String, String> gc = getGroupCache(delegatorBaseName);
        Set<String> enames = new HashSet<>();

        if (UtilValidate.isEmpty(groupName)) return enames;
        if (UtilValidate.isEmpty(gc)) return enames;
        for (Map.Entry<String, String> entry: gc.entrySet()) {
            if (groupName.equals(entry.getValue())) enames.add(entry.getKey());
        }
        return enames;
    }
}
