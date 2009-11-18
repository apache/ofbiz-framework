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

import java.util.Map;

import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.GenericEntityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class EntityConfigUtil {

    public static final String module = EntityConfigUtil.class.getName();
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";

    // ========== engine info fields ==========
    protected static String txFactoryClass;
    protected static String txFactoryUserTxJndiName;
    protected static String txFactoryUserTxJndiServerName;
    protected static String txFactoryTxMgrJndiName;
    protected static String txFactoryTxMgrJndiServerName;
    protected static String connFactoryClass;

    protected static Map<String, ResourceLoaderInfo> resourceLoaderInfos = FastMap.newInstance();
    protected static Map<String, DelegatorInfo> delegatorInfos = FastMap.newInstance();
    protected static Map<String, EntityModelReaderInfo> entityModelReaderInfos = FastMap.newInstance();
    protected static Map<String, EntityGroupReaderInfo> entityGroupReaderInfos = FastMap.newInstance();
    protected static Map<String, EntityEcaReaderInfo> entityEcaReaderInfos = FastMap.newInstance();
    protected static Map<String, EntityDataReaderInfo> entityDataReaderInfos = FastMap.newInstance();
    protected static Map<String, FieldTypeInfo> fieldTypeInfos = FastMap.newInstance();
    protected static Map<String, DatasourceInfo> datasourceInfos = FastMap.newInstance();

    protected static Element getXmlRootElement() throws GenericEntityConfException {
        try {
            return ResourceLoader.getXmlRootElement(ENTITY_ENGINE_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericEntityConfException("Could not get entity engine XML root element", e);
        }
    }

    protected static Document getXmlDocument() throws GenericEntityConfException {
        try {
            return ResourceLoader.getXmlDocument(ENTITY_ENGINE_XML_FILENAME);
        } catch (GenericConfigException e) {
            throw new GenericEntityConfException("Could not get entity engine XML document", e);
        }
    }

    static {
        try {
            initialize(getXmlRootElement());
        } catch (Exception e) {
            Debug.logError(e, "Error loading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, module);
        }
    }

    public static synchronized void reinitialize() throws GenericEntityException {
        try {
            ResourceLoader.invalidateDocument(ENTITY_ENGINE_XML_FILENAME);
            initialize(getXmlRootElement());
        } catch (Exception e) {
            throw new GenericEntityException("Error reloading entity config XML file " + ENTITY_ENGINE_XML_FILENAME, e);
        }
    }

    public static void initialize(Element rootElement) throws GenericEntityException {
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
        return txFactoryClass;
    }

    public static String getTxFactoryUserTxJndiName() {
        return txFactoryUserTxJndiName;
    }

    public static String getTxFactoryUserTxJndiServerName() {
        return txFactoryUserTxJndiServerName;
    }

    public static String getTxFactoryTxMgrJndiName() {
        return txFactoryTxMgrJndiName;
    }

    public static String getTxFactoryTxMgrJndiServerName() {
        return txFactoryTxMgrJndiServerName;
    }

    public static String getConnectionFactoryClass() {
        return connFactoryClass;
    }

    public static ResourceLoaderInfo getResourceLoaderInfo(String name) {
        return resourceLoaderInfos.get(name);
    }

    public static DelegatorInfo getDelegatorInfo(String name) {
        return delegatorInfos.get(name);
    }

    public static EntityModelReaderInfo getEntityModelReaderInfo(String name) {
        return entityModelReaderInfos.get(name);
    }

    public static EntityGroupReaderInfo getEntityGroupReaderInfo(String name) {
        return entityGroupReaderInfos.get(name);
    }

    public static EntityEcaReaderInfo getEntityEcaReaderInfo(String name) {
        return entityEcaReaderInfos.get(name);
    }

    public static EntityDataReaderInfo getEntityDataReaderInfo(String name) {
        return entityDataReaderInfos.get(name);
    }

    public static FieldTypeInfo getFieldTypeInfo(String name) {
        return fieldTypeInfos.get(name);
    }

    public static DatasourceInfo getDatasourceInfo(String name) {
        return datasourceInfos.get(name);
    }

    public static Map<String, DatasourceInfo> getDatasourceInfos() {
        return datasourceInfos;
    }
}
