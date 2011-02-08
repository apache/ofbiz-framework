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
package org.ofbiz.entity.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.GenericEntityException;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class EntityConfigUtil {

    public static final String module = EntityConfigUtil.class.getName();
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";

    private static volatile AtomicReference<EntityConfigUtil> configRef = new AtomicReference<EntityConfigUtil>();

    // ========== engine info fields ==========
    private final String txFactoryClass;
    private final String txFactoryUserTxJndiName;
    private final String txFactoryUserTxJndiServerName;
    private final String txFactoryTxMgrJndiName;
    private final String txFactoryTxMgrJndiServerName;
    private final String connFactoryClass;

    private final Map<String, ResourceLoaderInfo> resourceLoaderInfos = new HashMap<String, ResourceLoaderInfo>();
    private final Map<String, DelegatorInfo> delegatorInfos = new HashMap<String, DelegatorInfo>();
    private final Map<String, EntityModelReaderInfo> entityModelReaderInfos = new HashMap<String, EntityModelReaderInfo>();
    private final Map<String, EntityGroupReaderInfo> entityGroupReaderInfos = new HashMap<String, EntityGroupReaderInfo>();
    private final Map<String, EntityEcaReaderInfo> entityEcaReaderInfos = new HashMap<String, EntityEcaReaderInfo>();
    private final Map<String, EntityDataReaderInfo> entityDataReaderInfos = new HashMap<String, EntityDataReaderInfo>();
    private final Map<String, FieldTypeInfo> fieldTypeInfos = new HashMap<String, FieldTypeInfo>();
    private final Map<String, DatasourceInfo> datasourceInfos = new HashMap<String, DatasourceInfo>();

    private static Element getXmlRootElement() throws GenericEntityConfException {
        try {
            return ResourceLoader.getXmlRootElement(ENTITY_ENGINE_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericEntityConfException("Could not get entity engine XML root element", e);
        }
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            Debug.logError(e, "Error loading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, module);
        }
    }

    public static void reinitialize() throws GenericEntityException {
        try {
            ResourceLoader.invalidateDocument(ENTITY_ENGINE_XML_FILENAME);
            initialize(getXmlRootElement());
        } catch (Exception e) {
            throw new GenericEntityException("Error reloading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, e);
        }
    }

    public static void initialize(Element rootElement) throws GenericEntityException {
        configRef.set(new EntityConfigUtil(rootElement));
    }

    private EntityConfigUtil(Element rootElement) throws GenericEntityException {
        // load the transaction factory
        Element transactionFactoryElement = UtilXml.firstChildElement(rootElement, "transaction-factory");
        if (transactionFactoryElement == null) {
            throw new GenericEntityConfException("ERROR: no transaction-factory definition was found in " + ENTITY_ENGINE_XML_FILENAME);
        }

        txFactoryClass = transactionFactoryElement.getAttribute("class");

        Element userTxJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "user-transaction-jndi");
        if (userTxJndiElement != null) {
            txFactoryUserTxJndiName = userTxJndiElement.getAttribute("jndi-name");
            txFactoryUserTxJndiServerName = userTxJndiElement.getAttribute("jndi-server-name");
        } else {
            txFactoryUserTxJndiName = null;
            txFactoryUserTxJndiServerName = null;
        }

        Element txMgrJndiElement = UtilXml.firstChildElement(transactionFactoryElement, "transaction-manager-jndi");
        if (txMgrJndiElement != null) {
            txFactoryTxMgrJndiName = txMgrJndiElement.getAttribute("jndi-name");
            txFactoryTxMgrJndiServerName = txMgrJndiElement.getAttribute("jndi-server-name");
        } else {
            txFactoryTxMgrJndiName = null;
            txFactoryTxMgrJndiServerName = null;
        }

        // load the connection factory
        Element connectionFactoryElement = UtilXml.firstChildElement(rootElement, "connection-factory");
        if (connectionFactoryElement == null) {
            throw new GenericEntityConfException("ERROR: no connection-factory definition was found in " + ENTITY_ENGINE_XML_FILENAME);
        }

        connFactoryClass = connectionFactoryElement.getAttribute("class");

        // not load all of the maps...

        // resource-loader - resourceLoaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "resource-loader")) {
            ResourceLoaderInfo resourceLoaderInfo = new ResourceLoaderInfo(curElement);
            resourceLoaderInfos.put(resourceLoaderInfo.name, resourceLoaderInfo);
        }

        // delegator - delegatorInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "delegator")) {
            DelegatorInfo delegatorInfo = new DelegatorInfo(curElement);
            delegatorInfos.put(delegatorInfo.name, delegatorInfo);
        }

        // entity-model-reader - entityModelReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-model-reader")) {
            EntityModelReaderInfo entityModelReaderInfo = new EntityModelReaderInfo(curElement);
            entityModelReaderInfos.put(entityModelReaderInfo.name, entityModelReaderInfo);
        }

        // entity-group-reader - entityGroupReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-group-reader")) {
            EntityGroupReaderInfo entityGroupReaderInfo = new EntityGroupReaderInfo(curElement);
            entityGroupReaderInfos.put(entityGroupReaderInfo.name, entityGroupReaderInfo);
        }

        // entity-eca-reader - entityEcaReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-eca-reader")) {
            EntityEcaReaderInfo entityEcaReaderInfo = new EntityEcaReaderInfo(curElement);
            entityEcaReaderInfos.put(entityEcaReaderInfo.name, entityEcaReaderInfo);
        }

        // entity-data-reader - entityDataReaderInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "entity-data-reader")) {
            EntityDataReaderInfo entityDataReaderInfo = new EntityDataReaderInfo(curElement);
            entityDataReaderInfos.put(entityDataReaderInfo.name, entityDataReaderInfo);
        }

        // field-type - fieldTypeInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "field-type")) {
            FieldTypeInfo fieldTypeInfo = new FieldTypeInfo(curElement);
            fieldTypeInfos.put(fieldTypeInfo.name, fieldTypeInfo);
        }

        // datasource - datasourceInfos
        for (Element curElement: UtilXml.childElementList(rootElement, "datasource")) {
            DatasourceInfo datasourceInfo = new DatasourceInfo(curElement);
            datasourceInfos.put(datasourceInfo.name, datasourceInfo);
        }
    }

    public static String getTxFactoryClass() {
        return configRef.get().txFactoryClass;
    }

    public static String getTxFactoryUserTxJndiName() {
        return configRef.get().txFactoryUserTxJndiName;
    }

    public static String getTxFactoryUserTxJndiServerName() {
        return configRef.get().txFactoryUserTxJndiServerName;
    }

    public static String getTxFactoryTxMgrJndiName() {
        return configRef.get().txFactoryTxMgrJndiName;
    }

    public static String getTxFactoryTxMgrJndiServerName() {
        return configRef.get().txFactoryTxMgrJndiServerName;
    }

    public static String getConnectionFactoryClass() {
        return configRef.get().connFactoryClass;
    }

    public static ResourceLoaderInfo getResourceLoaderInfo(String name) {
        return configRef.get().resourceLoaderInfos.get(name);
    }

    public static DelegatorInfo getDelegatorInfo(String name) {
        return configRef.get().delegatorInfos.get(name);
    }

    public static EntityModelReaderInfo getEntityModelReaderInfo(String name) {
        return configRef.get().entityModelReaderInfos.get(name);
    }

    public static EntityGroupReaderInfo getEntityGroupReaderInfo(String name) {
        return configRef.get().entityGroupReaderInfos.get(name);
    }

    public static EntityEcaReaderInfo getEntityEcaReaderInfo(String name) {
        return configRef.get().entityEcaReaderInfos.get(name);
    }

    public static EntityDataReaderInfo getEntityDataReaderInfo(String name) {
        return configRef.get().entityDataReaderInfos.get(name);
    }

    public static FieldTypeInfo getFieldTypeInfo(String name) {
        return configRef.get().fieldTypeInfos.get(name);
    }

    public static DatasourceInfo getDatasourceInfo(String name) {
        return configRef.get().datasourceInfos.get(name);
    }

    public static Map<String, DatasourceInfo> getDatasourceInfos() {
        return configRef.get().datasourceInfos;
    }
}
