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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;delegator&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class DelegatorElement extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "delegator";
    private final String xPath;

    private final String name;
    private final String entityModelReader;
    private final String entityGroupReader;
    private final String entityEcaReader;
    private final boolean entityEcaEnabled;
    private final String entityEcaHandlerClassName;
    private final boolean distributedCacheClearEnabled;
    private final String distributedCacheClearClassName;
    private final String distributedCacheClearUserLoginId;
    private final String sequencedIdPrefix;
    private final String defaultGroupName;
    private final String keyEncryptingKey;
    private final List<GroupMap> groupMapList;
    private final Map<String, GroupMap> groupMapMap;

    DelegatorElement(Element element, String xPathParent) throws GenericEntityConfException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        this.xPath = xPathParent.concat("/delegator[@name='" + name + "']");
        String entityModelReader = config.getValue(this.xPath + "/@entity-model-reader");
        if (entityModelReader.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element entity-model-reader attribute is empty" + lineNumberText);
        }
        this.entityModelReader = entityModelReader;
        String entityGroupReader = config.getValue(this.xPath + "/@entity-group-reader");
        if (entityGroupReader.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element entity-group-reader attribute is empty" + lineNumberText);
        }
        this.entityGroupReader = entityGroupReader;
        entityEcaReader = config.getValue(this.xPath + "/@entity-eca-reader");
        entityEcaEnabled = !"false".equalsIgnoreCase(config.getValue(this.xPath + "/@entity-eca-enabled"));
        entityEcaHandlerClassName = config.getValue(this.xPath + "/@entity-eca-handler-class-name",
                "org.apache.ofbiz.entityext.eca.DelegatorEcaHandler", String.class);
        distributedCacheClearEnabled = "true".equalsIgnoreCase(config.getValue(this.xPath + "/@distributed-cache-clear-enabled"));
        distributedCacheClearClassName = config.getValue(
                this.xPath + "/@distributed-cache-clear-class-name", "org.apache.ofbiz.entityext.cache.EntityCacheServices", String.class);
        distributedCacheClearUserLoginId = config.getValue(this.xPath
                + "/@distributed-cache-clear-user-login-id", "system", String.class);
        sequencedIdPrefix = config.getValue(this.xPath + "/@sequenced-id-prefix");
        defaultGroupName = config.getValue(this.xPath + "/@default-group-name", "org.apache.ofbiz", String.class);
        keyEncryptingKey = config.getValue(this.xPath + "/@key-encrypting-key");
        List<GroupMap> groupMapList = config.getSubElementsAsListEntries(this.xPath, element, GroupMap.class);
        if (groupMapList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element child elements <group-map> are missing" + lineNumberText);
        }
        this.groupMapList = Collections.unmodifiableList(groupMapList);
        groupMapMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(this.xPath, element, GroupMap.class));
    }

    DelegatorElement(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        this.xPath = xPath;
        String name = getNameFromXPath(xPath);
        if (name.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element name attribute is empty");
        }
        this.name = name;
        String entityModelReader = config.getValue(configObject, "/@entity-model-reader");
        if (entityModelReader.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element entity-model-reader attribute is empty");
        }
        this.entityModelReader = entityModelReader;
        String entityGroupReader = config.getValue(configObject, "/@entity-group-reader");
        if (entityGroupReader.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element entity-group-reader attribute is empty");
        }
        this.entityGroupReader = entityGroupReader;
        entityEcaReader = config.getValue(configObject, "/@entity-eca-reader");
        entityEcaEnabled = !"false".equalsIgnoreCase(config.getValue(configObject, "/@entity-eca-enabled"));
        entityEcaHandlerClassName = config.getValue(configObject, "/@entity-eca-handler-class-name",
                "org.apache.ofbiz.entityext.eca.DelegatorEcaHandler", String.class);
        distributedCacheClearEnabled = "true".equalsIgnoreCase(config.getValue(configObject, "/@distributed-cache-clear-enabled"));
        distributedCacheClearClassName = config.getValue(configObject, "/@distributed-cache-clear-class-name",
                "org.apache.ofbiz.entityext.cache.EntityCacheServices", String.class);
        distributedCacheClearUserLoginId = config.getValue(configObject,
                "/@distributed-cache-clear-user-login-id", "system", String.class);
        sequencedIdPrefix = config.getValue(configObject, "/@sequenced-id-prefix");
        defaultGroupName = config.getValue(configObject, "/@default-group-name", "org.apache.ofbiz", String.class);
        keyEncryptingKey = config.getValue(configObject, "/@key-encrypting-key");

        List<GroupMap> groupMapList = config.getSubElementsAsListEntries(this.xPath, null, GroupMap.class);
        if (groupMapList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<delegator> element child elements <group-map> are missing");
        }
        this.groupMapList = Collections.unmodifiableList(groupMapList);
        this.groupMapMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(this.xPath, null, GroupMap.class));
    }

    public static DelegatorElement loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new DelegatorElement(element, xPathParent);
    }

    public static DelegatorElement loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new DelegatorElement(configMap, xPath);
    }

    public String getEntityModelReader() {
        return entityModelReader;
    }

    public String getEntityGroupReader() {
        return entityGroupReader;
    }

    public String getEntityEcaReader() {
        return entityEcaReader;
    }

    public boolean getEntityEcaEnabled() {
        return entityEcaEnabled;
    }

    public String getEntityEcaHandlerClassName() {
        return entityEcaHandlerClassName;
    }

    public boolean getDistributedCacheClearEnabled() {
        return distributedCacheClearEnabled;
    }

    public String getDistributedCacheClearClassName() {
        return distributedCacheClearClassName;
    }

    public String getDistributedCacheClearUserLoginId() {
        return distributedCacheClearUserLoginId;
    }

    public String getSequencedIdPrefix() {
        return sequencedIdPrefix;
    }

    public String getDefaultGroupName() {
        return defaultGroupName;
    }

    public String getKeyEncryptingKey() {
        return keyEncryptingKey;
    }

    public List<GroupMap> getGroupMapList() {
        return groupMapList;
    }

    public GroupMap getGroupDataSource(String groupName) {
        return groupMapMap.get(groupName);
    }

    @Override
    public String getName() {
        return name;
    }
}
