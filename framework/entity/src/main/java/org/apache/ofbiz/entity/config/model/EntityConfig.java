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

import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.config.XmlFileReader;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.secret.SecretProviderFactory;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * A singleton class that models the <code>&lt;entity-config&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityConfig {
    private static final String MODULE = EntityConfig.class.getName();
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";
    private final EntityConfigGetter config = EntityConfigGetter.getInstance();

    private final List<ResourceLoader> resourceLoaderList;
    private final Map<String, ResourceLoader> resourceLoaderMap;
    private final TransactionFactory transactionFactory;
    private final ConnectionFactory connectionFactory;
    private final DebugXaResources debugXaResources;
    private final List<DelegatorElement> delegatorList;
    private final Map<String, DelegatorElement> delegatorMap;
    private final List<EntityModelReader> entityModelReaderList;
    private final Map<String, EntityModelReader> entityModelReaderMap;
    private final List<EntityGroupReader> entityGroupReaderList;
    private final Map<String, EntityGroupReader> entityGroupReaderMap;
    private final List<EntityEcaReader> entityEcaReaderList;
    private final Map<String, EntityEcaReader> entityEcaReaderMap;
    private final List<EntityDataReader> entityDataReaderList;
    private final Map<String, EntityDataReader> entityDataReaderMap;
    private final List<FieldType> fieldTypeList;
    private final Map<String, FieldType> fieldTypeMap;
    private final List<Datasource> datasourceList;
    private final Map<String, Datasource> datasourceMap;

    public EntityConfig() throws GenericEntityConfException {
        Element entityConfigRootXmlElement = XmlFileReader.read(ENTITY_ENGINE_XML_FILENAME);
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        List<ResourceLoader> resourceLoaderList = config.getSubElementsAsListEntries(entityConfigRootXmlElement, ResourceLoader.class);
        if (resourceLoaderList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <resource-loader> are missing");
        }
        this.resourceLoaderList = Collections.unmodifiableList(resourceLoaderList);
        resourceLoaderMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement,
                ResourceLoader.class));

        TransactionFactory transactionFactory = config.getObjectSubElement(entityConfigRootXmlElement, TransactionFactory.class);
        if (transactionFactory == null && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child element <transaction-factory> is missing");
        }
        this.transactionFactory = transactionFactory;
        connectionFactory = config.getObjectSubElement(entityConfigRootXmlElement, ConnectionFactory.class);

        DebugXaResources debugXaResources = config.getObjectSubElement(entityConfigRootXmlElement, DebugXaResources.class);
        if (debugXaResources == null && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child element <debug-xa-resources> is missing");
        }
        this.debugXaResources = debugXaResources;

        List<DelegatorElement> delegatorList = config.getSubElementsAsListEntries(entityConfigRootXmlElement, DelegatorElement.class);
        if (delegatorList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <delegator> are missing");
        }
        this.delegatorList = Collections.unmodifiableList(delegatorList);
        delegatorMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement, DelegatorElement.class));

        List<EntityModelReader> entityModelReaderList = config.getSubElementsAsListEntries(entityConfigRootXmlElement,
                EntityModelReader.class);
        if (entityModelReaderList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <entity-model-reader> are missing");
        }
        this.entityModelReaderList = Collections.unmodifiableList(entityModelReaderList);
        entityModelReaderMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement,
                EntityModelReader.class));

        List<EntityGroupReader> entityGroupReaderList = config.getSubElementsAsListEntries(entityConfigRootXmlElement,
                EntityGroupReader.class);
        if (entityGroupReaderList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <entity-group-reader> are missing");
        }
        this.entityGroupReaderList = Collections.unmodifiableList(entityGroupReaderList);
        entityGroupReaderMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement,
                EntityGroupReader.class));

        List<EntityEcaReader> entityEcaReaderList = config.getSubElementsAsListEntries(entityConfigRootXmlElement, EntityEcaReader.class);
        this.entityEcaReaderList = Collections.unmodifiableList(entityEcaReaderList);
        if (entityEcaReaderList.isEmpty() && checkStructure) {
            entityEcaReaderMap = Map.of();
        } else {
            entityEcaReaderMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement,
                    EntityEcaReader.class));
        }
        List<EntityDataReader> entityDataReaderList = config.getSubElementsAsListEntries(entityConfigRootXmlElement,
                EntityDataReader.class);
        this.entityDataReaderList = Collections.unmodifiableList(entityDataReaderList);
        if (entityDataReaderList.isEmpty()) {
            entityDataReaderMap = Map.of();
        } else {
            entityDataReaderMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement,
                    EntityDataReader.class));
        }
        List<FieldType> fieldTypeList = config.getSubElementsAsListEntries(entityConfigRootXmlElement, FieldType.class);
        if (fieldTypeList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <field-type> are missing");
        }
        this.fieldTypeList = Collections.unmodifiableList(fieldTypeList);
        fieldTypeMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement, FieldType.class));

        List<Datasource> datasourceList = config.getSubElementsAsListEntries(entityConfigRootXmlElement, Datasource.class);
        if (datasourceList.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<entity-config> element child elements <datasource> are missing");
        }
        this.datasourceList = Collections.unmodifiableList(datasourceList);
        datasourceMap = Collections.unmodifiableMap(config.getSubElementsAsMapValues(entityConfigRootXmlElement, Datasource.class));
    }

    public static EntityConfig getInstance() throws GenericEntityConfException {
        EntityConfig entityConfig = EntityConfigFactory.getInstance();
        if (entityConfig == null) {
            throw new GenericEntityConfException("EntityConfig is not initialized.");
        }
        return entityConfig;
    }

    public static String createConfigFileLineNumberText(Element element) {
        if (element.getUserData("startLine") != null) {
            return " [" + ENTITY_ENGINE_XML_FILENAME + " line " + element.getUserData("startLine") + "]";
        }
        return "";
    }

    public ResourceLoader getResourceLoader(String name) {
        return this.resourceLoaderMap.get(name);
    }

    public List<ResourceLoader> getResourceLoaderList() {
        return this.resourceLoaderList;
    }

    public TransactionFactory getTransactionFactory() {
        return this.transactionFactory;
    }

    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    public DebugXaResources getDebugXaResources() {
        return this.debugXaResources;
    }

    public DelegatorElement getDelegator(String name) {
        return this.delegatorMap.get(name);
    }

    public List<DelegatorElement> getDelegatorList() {
        return this.delegatorList;
    }

    public EntityModelReader getEntityModelReader(String name) {
        return this.entityModelReaderMap.get(name);
    }

    public List<EntityModelReader> getEntityModelReaderList() {
        return this.entityModelReaderList;
    }

    public EntityGroupReader getEntityGroupReader(String name) {
        return this.entityGroupReaderMap.get(name);
    }

    public List<EntityGroupReader> getEntityGroupReaderList() {
        return this.entityGroupReaderList;
    }

    public EntityEcaReader getEntityEcaReader(String name) {
        return this.entityEcaReaderMap.get(name);
    }

    public List<EntityEcaReader> getEntityEcaReaderList() {
        return this.entityEcaReaderList;
    }

    public EntityDataReader getEntityDataReader(String name) {
        return this.entityDataReaderMap.get(name);
    }

    public List<EntityDataReader> getEntityDataReaderList() {
        return this.entityDataReaderList;
    }

    public FieldType getFieldType(String name) {
        return this.fieldTypeMap.get(name);
    }

    public List<FieldType> getFieldTypeList() {
        return this.fieldTypeList;
    }

    public List<Datasource> getDatasourceList() {
        return this.datasourceList;
    }

    public static Datasource getDatasource(String name) {
        try {
            return getInstance().datasourceMap.get(name);
        } catch (GenericEntityConfException e) {
            // FIXME: Doing this so we don't have to rewrite the entire API.
            throw new RuntimeException(e);
        }
    }

    public static String getJdbcPassword(InlineJdbc inlineJdbcElement) throws GenericEntityConfException {
        String jdbcPassword = inlineJdbcElement.getJdbcPassword();
        if (!jdbcPassword.isEmpty()) {
            return jdbcPassword;
        }
        String jdbcPasswordLookup = inlineJdbcElement.getJdbcPasswordLookup();
        if (jdbcPasswordLookup.isEmpty()) {
            throw new GenericEntityConfException("No jdbc-password or jdbc-password-lookup specified for inline-jdbc element, line: "
                    + inlineJdbcElement.getLineNumber());
        }
        String key = "jdbc-password.".concat(jdbcPasswordLookup);
        try {
            return SecretProviderFactory.getInstance().getSecret(key);
        } catch (GeneralException e) {
            throw new GenericEntityConfException("Secret not found for key '" + key + "' for inline-jdbc element, line: "
                    + inlineJdbcElement.getLineNumber() + " - " + e.getMessage());
        }
    }

    public Map<String, Datasource> getDatasourceMap() {
        return this.datasourceMap;
    }
}
