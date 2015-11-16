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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * A singleton class that models the <code>&lt;entity-config&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityConfig {
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";

    private static final String module = EntityConfig.class.getName();

    private static final EntityConfig instance = createNewInstance();
    private final List<ResourceLoader> resourceLoaderList; // <resource-loader>
    private final Map<String, ResourceLoader> resourceLoaderMap; // <resource-loader>
    private final TransactionFactory transactionFactory; // <transaction-factory>
    private final ConnectionFactory connectionFactory; // <connection-factory>
    private final DebugXaResources debugXaResources; // <debug-xa-resources>
    private final List<DelegatorElement> delegatorList; // <delegator>
    private final Map<String, DelegatorElement> delegatorMap; // <delegator>
    private final List<EntityModelReader> entityModelReaderList; // <entity-model-reader>
    private final Map<String, EntityModelReader> entityModelReaderMap; // <entity-model-reader>
    private final List<EntityGroupReader> entityGroupReaderList; // <entity-group-reader>
    private final Map<String, EntityGroupReader> entityGroupReaderMap; // <entity-group-reader>
    private final List<EntityEcaReader> entityEcaReaderList; // <entity-eca-reader>
    private final Map<String, EntityEcaReader> entityEcaReaderMap; // <entity-eca-reader>
    private final List<EntityDataReader> entityDataReaderList; // <entity-data-reader>
    private final Map<String, EntityDataReader> entityDataReaderMap; // <entity-data-reader>
    private final List<FieldType> fieldTypeList; // <field-type>
    private final Map<String, FieldType> fieldTypeMap; // <field-type>
    private final List<Datasource> datasourceList; // <datasource>
    private final Map<String, Datasource> datasourceMap;

    private EntityConfig() throws GenericEntityConfException {
        Element element;
        URL confUrl = UtilURL.fromResource(ENTITY_ENGINE_XML_FILENAME);
        if (confUrl == null) {
            throw new GenericEntityConfException("Could not find the " + ENTITY_ENGINE_XML_FILENAME + " file");
        }
        try {
            element = UtilXml.readXmlDocument(confUrl, true, true).getDocumentElement();
        } catch (Exception e) {
            throw new GenericEntityConfException("Exception thrown while reading " + ENTITY_ENGINE_XML_FILENAME + ": ", e);
        }

        List<? extends Element> resourceLoaderElementList = UtilXml.childElementList(element, "resource-loader");
        if (resourceLoaderElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <resource-loader> are missing");
        } else {
            List<ResourceLoader> resourceLoaderList = new ArrayList<ResourceLoader>(resourceLoaderElementList.size());
            Map<String, ResourceLoader> resourceLoaderMap = new HashMap<String, ResourceLoader>();
            for (Element resourceLoaderElement : resourceLoaderElementList) {
                ResourceLoader resourceLoader = new ResourceLoader(resourceLoaderElement);
                resourceLoaderList.add(resourceLoader);
                resourceLoaderMap.put(resourceLoader.getName(), resourceLoader);
            }
            this.resourceLoaderList = Collections.unmodifiableList(resourceLoaderList);
            this.resourceLoaderMap = Collections.unmodifiableMap(resourceLoaderMap);
        }
        Element transactionFactoryElement = UtilXml.firstChildElement(element, "transaction-factory");
        if (transactionFactoryElement == null) {
            throw new GenericEntityConfException("<entity-config> element child element <transaction-factory> is missing");
        } else {
            this.transactionFactory = new TransactionFactory(transactionFactoryElement);
        }
        Element connectionFactoryElement = UtilXml.firstChildElement(element, "connection-factory");
        if (connectionFactoryElement != null) {
            this.connectionFactory = new ConnectionFactory(connectionFactoryElement);
        } else {
            this.connectionFactory = null;
        }
        Element debugXaResourcesElement = UtilXml.firstChildElement(element, "debug-xa-resources");
        if (debugXaResourcesElement == null) {
            throw new GenericEntityConfException("<entity-config> element child element <debug-xa-resources> is missing");
        } else {
            this.debugXaResources = new DebugXaResources(debugXaResourcesElement);
        }
        List<? extends Element> delegatorElementList = UtilXml.childElementList(element, "delegator");
        if (delegatorElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <delegator> are missing");
        } else {
            List<DelegatorElement> delegatorList = new ArrayList<DelegatorElement>(delegatorElementList.size());
            Map<String, DelegatorElement> delegatorMap = new HashMap<String, DelegatorElement>();
            for (Element delegatorElement : delegatorElementList) {
                DelegatorElement delegator = new DelegatorElement(delegatorElement);
                delegatorList.add(delegator);
                delegatorMap.put(delegator.getName(), delegator);
            }
            this.delegatorList = Collections.unmodifiableList(delegatorList);
            this.delegatorMap = Collections.unmodifiableMap(delegatorMap);
        }
        List<? extends Element> entityModelReaderElementList = UtilXml.childElementList(element, "entity-model-reader");
        if (entityModelReaderElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <entity-model-reader> are missing");
        } else {
            List<EntityModelReader> entityModelReaderList = new ArrayList<EntityModelReader>(entityModelReaderElementList.size());
            Map<String, EntityModelReader> entityModelReaderMap = new HashMap<String, EntityModelReader>();
            for (Element entityModelReaderElement : entityModelReaderElementList) {
                EntityModelReader entityModelReader = new EntityModelReader(entityModelReaderElement);
                entityModelReaderList.add(entityModelReader);
                entityModelReaderMap.put(entityModelReader.getName(), entityModelReader);
            }
            this.entityModelReaderList = Collections.unmodifiableList(entityModelReaderList);
            this.entityModelReaderMap = Collections.unmodifiableMap(entityModelReaderMap);
        }
        List<? extends Element> entityGroupReaderElementList = UtilXml.childElementList(element, "entity-group-reader");
        if (entityGroupReaderElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <entity-group-reader> are missing");
        } else {
            List<EntityGroupReader> entityGroupReaderList = new ArrayList<EntityGroupReader>(entityGroupReaderElementList.size());
            Map<String, EntityGroupReader> entityGroupReaderMap = new HashMap<String, EntityGroupReader>();
            for (Element entityGroupReaderElement : entityGroupReaderElementList) {
                EntityGroupReader entityGroupReader = new EntityGroupReader(entityGroupReaderElement);
                entityGroupReaderList.add(entityGroupReader);
                entityGroupReaderMap.put(entityGroupReader.getName(), entityGroupReader);
            }
            this.entityGroupReaderList = Collections.unmodifiableList(entityGroupReaderList);
            this.entityGroupReaderMap = Collections.unmodifiableMap(entityGroupReaderMap);
        }
        List<? extends Element> entityEcaReaderElementList = UtilXml.childElementList(element, "entity-eca-reader");
        if (entityEcaReaderElementList.isEmpty()) {
            this.entityEcaReaderList = Collections.emptyList();
            this.entityEcaReaderMap = Collections.emptyMap();
        } else {
            List<EntityEcaReader> entityEcaReaderList = new ArrayList<EntityEcaReader>(entityEcaReaderElementList.size());
            Map<String, EntityEcaReader> entityEcaReaderMap = new HashMap<String, EntityEcaReader>();
            for (Element entityEcaReaderElement : entityEcaReaderElementList) {
                EntityEcaReader entityEcaReader = new EntityEcaReader(entityEcaReaderElement);
                entityEcaReaderList.add(new EntityEcaReader(entityEcaReaderElement));
                entityEcaReaderMap.put(entityEcaReader.getName(), entityEcaReader);
            }
            this.entityEcaReaderList = Collections.unmodifiableList(entityEcaReaderList);
            this.entityEcaReaderMap = Collections.unmodifiableMap(entityEcaReaderMap);
        }
        List<? extends Element> entityDataReaderElementList = UtilXml.childElementList(element, "entity-data-reader");
        if (entityDataReaderElementList.isEmpty()) {
            this.entityDataReaderList = Collections.emptyList();
            this.entityDataReaderMap = Collections.emptyMap();
        } else {
            List<EntityDataReader> entityDataReaderList = new ArrayList<EntityDataReader>(entityDataReaderElementList.size());
            Map<String, EntityDataReader> entityDataReaderMap = new HashMap<String, EntityDataReader>();
            for (Element entityDataReaderElement : entityDataReaderElementList) {
                EntityDataReader entityDataReader = new EntityDataReader(entityDataReaderElement);
                entityDataReaderList.add(entityDataReader);
                entityDataReaderMap.put(entityDataReader.getName(), entityDataReader);
            }
            this.entityDataReaderList = Collections.unmodifiableList(entityDataReaderList);
            this.entityDataReaderMap = Collections.unmodifiableMap(entityDataReaderMap);
        }
        List<? extends Element> fieldTypeElementList = UtilXml.childElementList(element, "field-type");
        if (fieldTypeElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <field-type> are missing");
        } else {
            List<FieldType> fieldTypeList = new ArrayList<FieldType>(fieldTypeElementList.size());
            Map<String, FieldType> fieldTypeMap = new HashMap<String, FieldType>();
            for (Element fieldTypeElement : fieldTypeElementList) {
                FieldType fieldType = new FieldType(fieldTypeElement);
                fieldTypeList.add(fieldType);
                fieldTypeMap.put(fieldType.getName(), fieldType);
            }
            this.fieldTypeList = Collections.unmodifiableList(fieldTypeList);
            this.fieldTypeMap = Collections.unmodifiableMap(fieldTypeMap);
        }
        List<? extends Element> datasourceElementList = UtilXml.childElementList(element, "datasource");
        if (datasourceElementList.isEmpty()) {
            throw new GenericEntityConfException("<entity-config> element child elements <datasource> are missing");
        } else {
            List<Datasource> datasourceList = new ArrayList<Datasource>(datasourceElementList.size());
            Map<String, Datasource> datasourceMap = new HashMap<String, Datasource>();
            for (Element datasourceElement : datasourceElementList) {
                Datasource datasource = new Datasource(datasourceElement);
                datasourceList.add(datasource);
                datasourceMap.put(datasource.getName(), datasource);
            }
            this.datasourceList = Collections.unmodifiableList(datasourceList);
            this.datasourceMap = Collections.unmodifiableMap(datasourceMap);
        }
    }

    private static EntityConfig createNewInstance() {
        EntityConfig entityConfig = null;
        try {
            entityConfig = new EntityConfig();
        } catch (GenericEntityConfException gece) {
            Debug.logError(gece, module);
        }
        return entityConfig;
    }

    public static EntityConfig getInstance() throws GenericEntityConfException {
        if (instance == null) {
            throw new GenericEntityConfException("EntityConfig is not initialized.");
        }
        return instance;
    }

    public static String createConfigFileLineNumberText(Element element) {
        if (element.getUserData("startLine") != null) {
            return " [" + ENTITY_ENGINE_XML_FILENAME + " line " + element.getUserData("startLine") + "]";
        }
        return "";
    }

    /** Returns the specified <code>&lt;resource-loader&gt;</code> child element, or <code>null</code> if no child element was found. */
    public ResourceLoader getResourceLoader(String name) {
        return this.resourceLoaderMap.get(name);
    }

    /** Returns the <code>&lt;resource-loader&gt;</code> child elements. */
    public List<ResourceLoader> getResourceLoaderList() {
        return this.resourceLoaderList;
    }

    /** Returns the <code>&lt;transaction-factory&gt;</code> child element, or <code>null</code> if no child element was found. */
    public TransactionFactory getTransactionFactory() {
        return this.transactionFactory;
    }

    /** Returns the <code>&lt;connection-factory&gt;</code> child element, or <code>null</code> if no child element was found. */
    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    /** Returns the <code>&lt;debug-xa-resources&gt;</code> child element, or <code>null</code> if no child element was found. */
    public DebugXaResources getDebugXaResources() {
        return this.debugXaResources;
    }

    /** Returns the specified <code>&lt;delegator&gt;</code> child element, or <code>null</code> if no child element was found. */
    public DelegatorElement getDelegator(String name) {
        return this.delegatorMap.get(name);
    }

    /** Returns the <code>&lt;delegator&gt;</code> child elements. */
    public List<DelegatorElement> getDelegatorList() {
        return this.delegatorList;
    }

    /** Returns the specified <code>&lt;entity-model-reader&gt;</code> child element, or <code>null</code> if no child element was found. */
    public EntityModelReader getEntityModelReader(String name) {
        return this.entityModelReaderMap.get(name);
    }

    /** Returns the <code>&lt;entity-model-reader&gt;</code> child elements. */
    public List<EntityModelReader> getEntityModelReaderList() {
        return this.entityModelReaderList;
    }

    /** Returns the specified <code>&lt;entity-group-reader&gt;</code> child element, or <code>null</code> if no child element was found. */
    public EntityGroupReader getEntityGroupReader(String name) {
        return this.entityGroupReaderMap.get(name);
    }

    /** Returns the <code>&lt;entity-group-reader&gt;</code> child elements. */
    public List<EntityGroupReader> getEntityGroupReaderList() {
        return this.entityGroupReaderList;
    }

    /** Returns the specified <code>&lt;entity-eca-reader&gt;</code> child element, or <code>null</code> if no child element was found. */
    public EntityEcaReader getEntityEcaReader(String name) {
        return this.entityEcaReaderMap.get(name);
    }

    /** Returns the <code>&lt;entity-eca-reader&gt;</code> child elements. */
    public List<EntityEcaReader> getEntityEcaReaderList() {
        return this.entityEcaReaderList;
    }

    /** Returns the specified <code>&lt;entity-data-reader&gt;</code> child element, or <code>null</code> if no child element was found. */
    public EntityDataReader getEntityDataReader(String name) {
        return this.entityDataReaderMap.get(name);
    }

    /** Returns the <code>&lt;entity-data-reader&gt;</code> child elements. */
    public List<EntityDataReader> getEntityDataReaderList() {
        return this.entityDataReaderList;
    }

    /** Returns the specified <code>&lt;field-type&gt;</code> child element, or <code>null</code> if no child element was found. */
    public FieldType getFieldType(String name) {
        return this.fieldTypeMap.get(name);
    }

    /** Returns the <code>&lt;field-type&gt;</code> child elements. */
    public List<FieldType> getFieldTypeList() {
        return this.fieldTypeList;
    }

    /** Returns the <code>&lt;datasource&gt;</code> child elements. */
    public List<Datasource> getDatasourceList() {
        return this.datasourceList;
    }

    /** Returns the specified <code>&lt;datasource&gt;</code> child element or <code>null</code> if it does not exist. */
    /*
    public Datasource getDatasource(String name) {
        return this.datasourceMap.get(name);
    }
    */
    public static Datasource getDatasource(String name) {
        try {
            return getInstance().datasourceMap.get(name);
        } catch (GenericEntityConfException e) {
            // FIXME: Doing this so we don't have to rewrite the entire API.
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the configured JDBC password.
     *
     * @param inlineJdbcElement
     * @return The configured JDBC password.
     * @throws GenericEntityConfException If the password was not found.
     *
     * @see <code>entity-config.xsd</code>
     */
    public static String getJdbcPassword(InlineJdbc inlineJdbcElement) throws GenericEntityConfException {
        String jdbcPassword = inlineJdbcElement.getJdbcPassword();
        if (!jdbcPassword.isEmpty()) {
            return jdbcPassword;
        }
        String jdbcPasswordLookup = inlineJdbcElement.getJdbcPasswordLookup();
        if (jdbcPasswordLookup.isEmpty()) {
            throw new GenericEntityConfException("No jdbc-password or jdbc-password-lookup specified for inline-jdbc element, line: " + inlineJdbcElement.getLineNumber());
        }
        String key = "jdbc-password.".concat(jdbcPasswordLookup);
        jdbcPassword = UtilProperties.getPropertyValue("passwords", key);
        if (jdbcPassword.isEmpty()) {
            throw new GenericEntityConfException("'" + key + "' property not found in passwords.properties file for inline-jdbc element, line: " + inlineJdbcElement.getLineNumber());
        }
        return jdbcPassword;
    }

    /** Returns the <code>&lt;datasource&gt;</code> child elements as a <code>Map</code>. */
    public Map<String, Datasource> getDatasourceMap() {
        return this.datasourceMap;
    }
}
