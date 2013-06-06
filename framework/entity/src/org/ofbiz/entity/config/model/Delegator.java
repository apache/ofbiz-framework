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
package org.ofbiz.entity.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;delegator&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class Delegator {

    private final String name; // type = xs:string
    private final String entityModelReader; // type = xs:string
    private final String entityGroupReader; // type = xs:string
    private final String entityEcaReader; // type = xs:string
    private final String entityEcaEnabled;
    private final String entityEcaHandlerClassName; // type = xs:string
    private final String distributedCacheClearEnabled;
    private final String distributedCacheClearClassName; // type = xs:string
    private final String distributedCacheClearUserLoginId; // type = xs:string
    private final String sequencedIdPrefix; // type = xs:string
    private final String defaultGroupName; // type = xs:string
    private final String keyEncryptingKey; // type = xs:string
    private final List<GroupMap> groupMapList; // <group-map>

    public Delegator(Element element) throws GenericEntityConfException {
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element name attribute is empty");
        }
        this.name = name;
        String entityModelReader = element.getAttribute("entity-model-reader").intern();
        if (entityModelReader.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element entity-model-reader attribute is empty");
        }
        this.entityModelReader = entityModelReader;
        String entityGroupReader = element.getAttribute("entity-group-reader").intern();
        if (entityGroupReader.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element entity-group-reader attribute is empty");
        }
        this.entityGroupReader = entityGroupReader;
        this.entityEcaReader = element.getAttribute("entity-eca-reader").intern();
        String entityEcaEnabled = element.getAttribute("entity-eca-enabled").intern();
        if (entityEcaEnabled.isEmpty()) {
            entityEcaEnabled = "true";
        }
        this.entityEcaEnabled = entityEcaEnabled;
        String entityEcaHandlerClassName = element.getAttribute("entity-eca-handler-class-name").intern();
        if (entityEcaHandlerClassName.isEmpty()) {
            entityEcaHandlerClassName = "org.ofbiz.entityext.eca.DelegatorEcaHandler";
        }
        this.entityEcaHandlerClassName = entityEcaHandlerClassName;
        String distributedCacheClearEnabled = element.getAttribute("distributed-cache-clear-enabled").intern();
        if (distributedCacheClearEnabled.isEmpty()) {
            distributedCacheClearEnabled = "false";
        }
        this.distributedCacheClearEnabled = distributedCacheClearEnabled;
        String distributedCacheClearClassName = element.getAttribute("distributed-cache-clear-class-name").intern();
        if (distributedCacheClearClassName.isEmpty()) {
            distributedCacheClearClassName = "org.ofbiz.entityext.cache.EntityCacheServices";
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
            defaultGroupName = "org.ofbiz";
        }
        this.defaultGroupName = defaultGroupName;
        this.keyEncryptingKey = element.getAttribute("key-encrypting-key").intern();
        List<? extends Element> groupMapElementList = UtilXml.childElementList(element, "group-map");
        if (groupMapElementList.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element child elements <group-map> are missing");
        } else {
            List<GroupMap> groupMapList = new ArrayList<GroupMap>(groupMapElementList.size());
            for (Element groupMapElement : groupMapElementList) {
                groupMapList.add(new GroupMap(groupMapElement));
            }
            this.groupMapList = Collections.unmodifiableList(groupMapList);
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
    public String getEntityEcaEnabled() {
        return this.entityEcaEnabled;
    }

    /** Returns the value of the <code>entity-eca-handler-class-name</code> attribute. */
    public String getEntityEcaHandlerClassName() {
        return this.entityEcaHandlerClassName;
    }

    /** Returns the value of the <code>distributed-cache-clear-enabled</code> attribute. */
    public String getDistributedCacheClearEnabled() {
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
}
