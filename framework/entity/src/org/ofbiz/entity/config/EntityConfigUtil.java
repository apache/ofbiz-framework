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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilURL;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.config.model.Datasource;
import org.ofbiz.entity.config.model.DelegatorElement;
import org.ofbiz.entity.config.model.EntityConfig;
import org.ofbiz.entity.config.model.EntityDataReader;
import org.ofbiz.entity.config.model.EntityEcaReader;
import org.ofbiz.entity.config.model.EntityGroupReader;
import org.ofbiz.entity.config.model.EntityModelReader;
import org.ofbiz.entity.config.model.FieldType;
import org.ofbiz.entity.config.model.InlineJdbc;
import org.ofbiz.entity.config.model.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public final class EntityConfigUtil {

    public static final String module = EntityConfigUtil.class.getName();
    public static final String ENTITY_ENGINE_XML_FILENAME = "entityengine.xml";
    private static final AtomicReference<EntityConfig> configRef = new AtomicReference<EntityConfig>(null);

    public static String createConfigFileLineNumberText(Element element) {
        if (element.getUserData("startLine") != null) {
            return " [" + ENTITY_ENGINE_XML_FILENAME + " line " + element.getUserData("startLine") + "]";
        }
        return "";
    }

    /**
     * Returns the <code>EntityConfig</code> instance.
     * @throws GenericEntityConfException
     */
    public static EntityConfig getEntityConfig() throws GenericEntityConfException {
        EntityConfig instance = configRef.get();
        if (instance == null) {
            Element entityConfigElement = getXmlDocument().getDocumentElement();
            instance = new EntityConfig(entityConfigElement);
            if (!configRef.compareAndSet(null, instance)) {
                instance = configRef.get();
            }
        }
        return instance;
    }

    private static Document getXmlDocument() throws GenericEntityConfException {
        URL confUrl = UtilURL.fromResource(ENTITY_ENGINE_XML_FILENAME);
        if (confUrl == null) {
            throw new GenericEntityConfException("Could not find the " + ENTITY_ENGINE_XML_FILENAME + " file");
        }
        try {
            return UtilXml.readXmlDocument(confUrl, true, true);
        } catch (Exception e) {
            throw new GenericEntityConfException("Exception thrown while reading " + ENTITY_ENGINE_XML_FILENAME + ": ", e);
        }
    }

    public static String getTxFactoryClass() throws GenericEntityConfException {
        return getEntityConfig().getTransactionFactory().getClassName();
    }

    public static String getTxFactoryUserTxJndiName() throws GenericEntityConfException {
        return getEntityConfig().getTransactionFactory().getUserTransactionJndi().getJndiName();
    }

    public static String getTxFactoryUserTxJndiServerName() throws GenericEntityConfException {
        return getEntityConfig().getTransactionFactory().getUserTransactionJndi().getJndiServerName();
    }

    public static String getTxFactoryTxMgrJndiName() throws GenericEntityConfException {
        return getEntityConfig().getTransactionFactory().getTransactionManagerJndi().getJndiName();
    }

    public static String getTxFactoryTxMgrJndiServerName() throws GenericEntityConfException {
        return getEntityConfig().getTransactionFactory().getTransactionManagerJndi().getJndiServerName();
    }
    
    /**
     * @return true Create Begin stacktrace when enlisting transactions
     * @throws GenericEntityConfException 
     */
    public static boolean isDebugXAResource() throws GenericEntityConfException {
        return getEntityConfig().getDebugXaResources().getValue();
    }

    public static String getConnectionFactoryClass() throws GenericEntityConfException {
        return getEntityConfig().getConnectionFactory().getClassName();
    }

    public static ResourceLoader getResourceLoader(String name) throws GenericEntityConfException {
        return getEntityConfig().getResourceLoader(name);
    }

    public static DelegatorElement getDelegator(String name) throws GenericEntityConfException {
        return getEntityConfig().getDelegator(name);
    }

    public static EntityModelReader getEntityModelReader(String name) throws GenericEntityConfException {
        return getEntityConfig().getEntityModelReader(name);
    }

    public static EntityGroupReader getEntityGroupReader(String name) throws GenericEntityConfException {
        return getEntityConfig().getEntityGroupReader(name);
    }

    public static EntityEcaReader getEntityEcaReader(String name) throws GenericEntityConfException {
        return getEntityConfig().getEntityEcaReader(name);
    }

    public static EntityDataReader getEntityDataReader(String name) throws GenericEntityConfException {
        return getEntityConfig().getEntityDataReader(name);
    }

    public static FieldType getFieldType(String name) throws GenericEntityConfException {
        return getEntityConfig().getFieldType(name);
    }

    public static Datasource getDatasource(String name) {
        try {
            return getEntityConfig().getDatasource(name);
        } catch (GenericEntityConfException e) {
            // FIXME: Doing this so we don't have to rewrite the entire API.
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Datasource> getDatasources() throws GenericEntityConfException {
        return getEntityConfig().getDatasourceMap();
    }

    public static String getJdbcPassword(InlineJdbc inlineJdbcElement) {
        String jdbcPassword = inlineJdbcElement.getJdbcPassword();
        if (UtilValidate.isNotEmpty(jdbcPassword)) {
            return jdbcPassword;
        }
        String jdbcPasswordLookup = inlineJdbcElement.getJdbcPasswordLookup();
        if (UtilValidate.isEmpty(jdbcPasswordLookup)) {
            // FIXME: Include line number in model
            // Debug.logError("no @jdbc-password or @jdbc-password-lookup specified for inline-jdbc element: %s@%d:%d", module, inlineJdbcElement.getUserData("systemId"), inlineJdbcElement.getUserData("startLine"), inlineJdbcElement.getUserData("startColumn"));
            Debug.logError("no jdbc-password or jdbc-password-lookup specified for inline-jdbc element", module);
            return null;
        }
        String key = "jdbc-password." + jdbcPasswordLookup;
        jdbcPassword = UtilProperties.getPropertyValue("passwords.properties", key);
        if (UtilValidate.isEmpty(jdbcPassword)) {
            // FIXME: Include line number in model
            // Debug.logError("no @jdbc-password or @jdbc-password-lookup specified for inline-jdbc element: %s@%d:%d", module, inlineJdbcElement.getUserData("systemId"), inlineJdbcElement.getUserData("startLine"), inlineJdbcElement.getUserData("startColumn"));
            Debug.logError("no jdbc-password or jdbc-password-lookup specified for inline-jdbc element", module);
        }
        return jdbcPassword;
    }

    private EntityConfigUtil() {}
}
