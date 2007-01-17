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
package org.ofbiz.entity.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.MainResourceHandler;
import org.ofbiz.base.config.ResourceHandler;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.config.DelegatorInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.config.EntityGroupReaderInfo;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.UtilTimer;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generic Entity - Entity Group Definition Reader
 *
 */
public class ModelGroupReader implements Serializable {

    public static final String module = ModelGroupReader.class.getName();
    public static UtilCache readers = new UtilCache("entity.ModelGroupReader", 0, 0);

    private Map groupCache = null;
    private Set groupNames = null;

    public String modelName;
    public List entityGroupResourceHandlers = new LinkedList();

    public static ModelGroupReader getModelGroupReader(String delegatorName) throws GenericEntityConfException {
        DelegatorInfo delegatorInfo = EntityConfigUtil.getDelegatorInfo(delegatorName);

        if (delegatorInfo == null) {
            throw new GenericEntityConfException("Could not find a delegator with the name " + delegatorName);
        }

        String tempModelName = delegatorInfo.entityGroupReader;
        ModelGroupReader reader = (ModelGroupReader) readers.get(tempModelName);

        if (reader == null) { // don't want to block here
            synchronized (ModelGroupReader.class) {
                // must check if null again as one of the blocked threads can still enter
                reader = (ModelGroupReader) readers.get(tempModelName);
                if (reader == null) {
                    reader = new ModelGroupReader(tempModelName);
                    readers.put(tempModelName, reader);
                }
            }
        }
        return reader;
    }

    public ModelGroupReader(String modelName) throws GenericEntityConfException {
        this.modelName = modelName;
        EntityGroupReaderInfo entityGroupReaderInfo = EntityConfigUtil.getEntityGroupReaderInfo(modelName);

        if (entityGroupReaderInfo == null) {
            throw new GenericEntityConfException("Cound not find an entity-group-reader with the name " + modelName);
        }
        Iterator resourceElementIter = entityGroupReaderInfo.resourceElements.iterator();
        while (resourceElementIter.hasNext()) {
            Element resourceElement = (Element) resourceElementIter.next();
            this.entityGroupResourceHandlers.add(new MainResourceHandler(EntityConfigUtil.ENTITY_ENGINE_XML_FILENAME, resourceElement));
        }

        // get all of the component resource group stuff, ie specified in each ofbiz-component.xml file
        List componentResourceInfos = ComponentConfig.getAllEntityResourceInfos("group");
        Iterator componentResourceInfoIter = componentResourceInfos.iterator();
        while (componentResourceInfoIter.hasNext()) {
            ComponentConfig.EntityResourceInfo componentResourceInfo = (ComponentConfig.EntityResourceInfo) componentResourceInfoIter.next();
            if (modelName.equals(componentResourceInfo.readerName)) {
                this.entityGroupResourceHandlers.add(componentResourceInfo.createResourceHandler());
            }
        }

        // preload caches...
        getGroupCache();
    }

    public Map getGroupCache() {
        if (this.groupCache == null) // don't want to block here
        {
            synchronized (ModelGroupReader.class) {
                // must check if null again as one of the blocked threads can still enter
                if (this.groupCache == null) {
                    // now it's safe
                    this.groupCache = new HashMap();
                    this.groupNames = new TreeSet();

                    UtilTimer utilTimer = new UtilTimer();
                    // utilTimer.timerString("[ModelGroupReader.getGroupCache] Before getDocument");

                    int i = 0;
                    Iterator entityGroupResourceHandlerIter = this.entityGroupResourceHandlers.iterator();
                    while (entityGroupResourceHandlerIter.hasNext()) {
                        ResourceHandler entityGroupResourceHandler = (ResourceHandler) entityGroupResourceHandlerIter.next();
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
                                    String entityName = UtilXml.checkEmpty(curEntity.getAttribute("entity"));
                                    String groupName = UtilXml.checkEmpty(curEntity.getAttribute("group"));

                                    if (groupName == null || entityName == null) continue;
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
    public String getEntityGroupName(String entityName) {
        Map gc = getGroupCache();

        if (gc != null)
            return (String) gc.get(entityName);
        else
            return null;
    }

    /** Creates a Collection with all of the groupNames defined in the specified XML Entity Group Descriptor file.
     * @return A Collection of groupNames Strings
     */
    public Collection getGroupNames() {
        getGroupCache();
        if (this.groupNames == null) return null;
        return new ArrayList(this.groupNames);
    }

    /** Creates a Collection with names of all of the entities for a given group
     * @param groupName
     * @return A Collection of entityName Strings
     */
    public Collection getEntityNamesByGroup(String groupName) {
        Map gc = getGroupCache();
        Collection enames = new LinkedList();

        if (groupName == null || groupName.length() <= 0) return enames;
        if (gc == null || gc.size() < 0) return enames;
        Set gcEntries = gc.entrySet();
        Iterator gcIter = gcEntries.iterator();

        while (gcIter.hasNext()) {
            Map.Entry entry = (Map.Entry) gcIter.next();

            if (groupName.equals(entry.getValue())) enames.add(entry.getKey());
        }
        return enames;
    }
}
