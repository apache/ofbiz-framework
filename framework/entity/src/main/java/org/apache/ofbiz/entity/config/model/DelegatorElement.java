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
package org.apache.ofbiz.entity.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;delegator&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class DelegatorElement {

    private final String name; // type = xs:string
    private final String entityModelReader; // type = xs:string
    private final String entityGroupReader; // type = xs:string
    private final String entityEcaReader; // type = xs:string
    private final boolean entityEcaEnabled;
    private final String entityEcaHandlerClassName; // type = xs:string
    private final boolean distributedCacheClearEnabled;
    private final String distributedCacheClearClassName; // type = xs:string
    private final String distributedCacheClearUserLoginId; // type = xs:string
    private final String sequencedIdPrefix; // type = xs:string
    private final String defaultGroupName; // type = xs:string
    private final String keyEncryptingKey; // type = xs:string
    private final List<GroupMap> groupMapList; // <group-map>
    private final Map<String, String> groupMapMap; // <group-map>

    DelegatorElement(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<delegator> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        String entityModelReader = element.getAttribute("entity-model-reader").intern();
        if (entityModelReader.isEmpty()) {
            throw new GenericEntityConfException("<delegator> element entity-model-reader attribute is empty" + lineNumberText);
        }
        this.entityModelReader = entityModelReader;
        String entityGroupReader = element.getAttribute("entity-group-reader").intern();
        if (entityGroupReader.isEmpty()) {
            throw new GenericEntityConfException("<delegator> element entity-group-reader attribute is empty" + lineNumberText);
        }
        this.entityGroupReader = entityGroupReader;
        this.entityEcaReader = element.getAttribute("entity-eca-reader").intern();
        this.entityEcaEnabled = !"false".equalsIgnoreCase(element.getAttribute("entity-eca-enabled"));
        String entityEcaHandlerClassName = element.getAttribute("entity-eca-handler-class-name").intern();
        if (entityEcaHandlerClassName.isEmpty()) {
            entityEcaHandlerClassName = "org.apache.ofbiz.entityext.eca.DelegatorEcaHandler";
        }
        this.entityEcaHandlerClassName = entityEcaHandlerClassName;
        this.distributedCacheClearEnabled = "true".equalsIgnoreCase(element.getAttribute("distributed-cache-clear-enabled"));
        String distributedCacheClearClassName = element.getAttribute("distributed-cache-clear-class-name").intern();
        if (distributedCacheClearClassName.isEmpty()) {
            distributedCacheClearClassName = "org.apache.ofbiz.entityext.cache.EntityCacheServices";
        }
        this.distributedCacheClearClassName = distributedCacheClearClassName;
        String distributedCacheClearUserLoginId = element.getAttribute("distributed-cache-clear-user-login-id").intern();
        if (distributedCacheClearUserLoginId.isEmpty()) {
            distributedCacheClearUserLoginId = "system";
        }
        this.distributedCacheClearUserLoginId = distributedCacheClearUserLoginId;
        this.sequencedIdPrefix = element.getAttribute("sequenced-id-prefix").intern();
        String defaultGroupName = element.getAttribute("default-group-name").intern();
        if (defaultGroupName.isEmpty()) {
            defaultGroupName = "org.apache.ofbiz";
        }
        this.defaultGroupName = defaultGroupName;
        this.keyEncryptingKey = element.getAttribute("key-encrypting-key").intern();
        List<? extends Element> groupMapElementList = UtilXml.childElementList(element, "group-map");
        if (groupMapElementList.isEmpty()) {
            throw new GenericEntityConfException("<delegator> element child elements <group-map> are missing" + lineNumberText);
        } else {
            List<GroupMap> groupMapList = new ArrayList<GroupMap>(groupMapElementList.size());
            Map<String, String> groupMapMap = new HashMap<String, String>();
            for (Element groupMapElement : groupMapElementList) {
                GroupMap groupMap = new GroupMap(groupMapElement);
                groupMapList.add(groupMap);
                groupMapMap.put(groupMap.getGroupName(), groupMap.getDatasourceName());
            }
            this.groupMapList = Collections.unmodifiableList(groupMapList);
            this.groupMapMap = Collections.unmodifiableMap(groupMapMap);
        }
    }

    /** Returns the value of the <code>name</code> attribute. */
    public String getName() {
        return this.name;
    }

    /** Returns the value of the <code>entity-model-reader</code> attribute. */
    public String getEntityModelReader() {
        return this.entityModelReader;
    }

    /** Returns the value of the <code>entity-group-reader</code> attribute. */
    public String getEntityGroupReader() {
        return this.entityGroupReader;
    }

    /** Returns the value of the <code>entity-eca-reader</code> attribute. */
    public String getEntityEcaReader() {
        return this.entityEcaReader;
    }

    /** Returns the value of the <code>entity-eca-enabled</code> attribute. */
    public boolean getEntityEcaEnabled() {
        return this.entityEcaEnabled;
    }

    /** Returns the value of the <code>entity-eca-handler-class-name</code> attribute. */
    public String getEntityEcaHandlerClassName() {
        return this.entityEcaHandlerClassName;
    }

    /** Returns the value of the <code>distributed-cache-clear-enabled</code> attribute. */
    public boolean getDistributedCacheClearEnabled() {
        return this.distributedCacheClearEnabled;
    }

    /** Returns the value of the <code>distributed-cache-clear-class-name</code> attribute. */
    public String getDistributedCacheClearClassName() {
        return this.distributedCacheClearClassName;
    }

    /** Returns the value of the <code>distributed-cache-clear-user-login-id</code> attribute. */
    public String getDistributedCacheClearUserLoginId() {
        return this.distributedCacheClearUserLoginId;
    }

    /** Returns the value of the <code>sequenced-id-prefix</code> attribute. */
    public String getSequencedIdPrefix() {
        return this.sequencedIdPrefix;
    }

    /** Returns the value of the <code>default-group-name</code> attribute. */
    public String getDefaultGroupName() {
        return this.defaultGroupName;
    }

    /** Returns the value of the <code>key-encrypting-key</code> attribute. */
    public String getKeyEncryptingKey() {
        return this.keyEncryptingKey;
    }

    /** Returns the <code>&lt;group-map&gt;</code> child elements. */
    public List<GroupMap> getGroupMapList() {
        return this.groupMapList;
    }

    /** Returns the specified <code>&lt;group-map&gt; datasource-name</code> attribute value,
     * or <code>null</code> if the <code>&lt;group-map&gt;</code> element does not exist . */
    public String getGroupDataSource(String groupName) {
        return this.groupMapMap.get(groupName);
    }
}
