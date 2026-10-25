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

import org.apache.ofbiz.base.config.AbstractConfigElement;
import org.apache.ofbiz.base.config.ConfigHelper;
import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;datasource&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class Datasource extends AbstractConfigElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "datasource";
    private final String xPath;

    private final String name;
    private final String helperClass;
    private final String fieldTypeName;
    private final boolean useSchemas;
    private final String schemaName;
    private final boolean checkOnStart;
    private final boolean addMissingOnStart;
    private final boolean usePkConstraintNames;
    private final boolean checkPksOnStart;
    private final int constraintNameClipLength;
    private final boolean useProxyCursor;
    private final String proxyCursorName;
    private final int resultFetchSize;
    private final boolean useForeignKeys;
    private final boolean useForeignKeyIndices;
    private final boolean checkFksOnStart;
    private final boolean checkFkIndicesOnStart;
    private final String fkStyle;
    private final boolean useFkInitiallyDeferred;
    private final boolean useIndices;
    private final boolean useIndicesUnique;
    private final boolean checkIndicesOnStart;
    private final String joinStyle;
    private final boolean aliasViewColumns;
    private final boolean alwaysUseConstraintKeyword;
    private final boolean dropFkUseForeignKeyKeyword;
    private final boolean useBinaryTypeForBlob;
    private final boolean useOrderByNulls;
    private final String offsetStyle;
    private final String tableType;
    private final String characterSet;
    private final String collate;
    private final int maxWorkerPoolSize;
    private final List<SqlLoadPath> sqlLoadPathList;
    private final List<ReadData> readDataList;
    private final InlineJdbc inlineJdbc;
    private final JndiJdbc jndiJdbc;
    private final TyrexDataSource tyrexDataSource;

    Datasource(Element element, String xPathParent) throws GenericEntityConfException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name");
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<datasource> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        this.xPath = xPathParent.concat("/datasource[@name='" + name + "']");
        String helperClass = config.getValue(this.xPath + "/@helper-class");
        if (helperClass.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<datasource> element helper-class attribute is empty" + lineNumberText);
        }
        this.helperClass = helperClass;
        String fieldTypeName = config.getValue(this.xPath + "/@field-type-name");
        if (fieldTypeName.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<datasource> element field-type-name attribute is empty" + lineNumberText);
        }
        this.fieldTypeName = fieldTypeName;
        useSchemas = "true".equals(config.getValue(this.xPath + "/@use-schemas"));
        schemaName = config.getValue(this.xPath + "/@schema-name");
        checkOnStart = "true".equals(config.getValue(this.xPath + "/@check-on-start"));
        addMissingOnStart = "true".equals(config.getValue(this.xPath + "/@add-missing-on-start"));
        usePkConstraintNames = "true".equals(config.getValue(this.xPath + "/@use-pk-constraint-names"));
        checkPksOnStart = "true".equals(config.getValue(this.xPath + "/@check-pks-on-start"));
        constraintNameClipLength = config.getValue(this.xPath + "/@constraint-name-clip-length", 30, Integer.class);
        useProxyCursor = "true".equalsIgnoreCase(config.getValue(this.xPath + "/@use-proxy-cursor"));
        proxyCursorName = config.getValue(this.xPath + "/@proxy-cursor-name", "p_cursor", String.class);
        resultFetchSize = config.getValue(this.xPath + "/@result-fetch-size", -1, Integer.class);
        useForeignKeys = "true".equals(config.getValue(this.xPath + "/@use-foreign-keys"));
        useForeignKeyIndices = "true".equals(config.getValue(this.xPath + "/@use-foreign-key-indices"));
        checkFksOnStart = "true".equals(config.getValue(this.xPath + "/@check-fks-on-start"));
        checkFkIndicesOnStart = "true".equals(config.getValue(this.xPath + "/@check-fks-indices-on-start"));
        fkStyle = config.getValue(this.xPath + "/@fk-style", "name_constraint", String.class);
        useFkInitiallyDeferred = "true".equals(config.getValue(this.xPath + "/@use-fk-initially-deferred"));
        useIndices = "true".equals(config.getValue(this.xPath + "/@use-indices"));
        useIndicesUnique = "true".equals(config.getValue(this.xPath + "/@use-indices-unique"));
        checkIndicesOnStart = "true".equals(config.getValue(this.xPath + "/@check-indices-on-start"));
        joinStyle = config.getValue(this.xPath + "/@join-style", "ansi", String.class);
        aliasViewColumns = "true".equals(config.getValue(this.xPath + "/@alias-view-columns"));
        alwaysUseConstraintKeyword = "true".equals(config.getValue(this.xPath + "/@always-use-constraint-keyword"));
        dropFkUseForeignKeyKeyword = "true".equals(config.getValue(this.xPath + "/@drop-fk-use-foreign-key-keyword"));
        useBinaryTypeForBlob = "true".equals(config.getValue(this.xPath + "/@use-binary-type-for-blob"));
        useOrderByNulls = "true".equals(config.getValue(this.xPath + "/@use-order-by-nulls"));
        offsetStyle = config.getValue(this.xPath + "/@offset-style", "none", String.class);
        tableType = config.getValue(this.xPath + "/@table-type");
        characterSet = config.getValue(this.xPath + "/@character-set");
        collate = config.getValue(this.xPath + "/@collate");
        String maxWorkerPoolSize = config.getValue(this.xPath + "/@max-worker-pool-size");
        if (maxWorkerPoolSize.isEmpty()) {
            this.maxWorkerPoolSize = 1;
        } else {
            try {
                int maxWorkerPoolSizeInt = Integer.parseInt(maxWorkerPoolSize);
                if (maxWorkerPoolSizeInt == 0) {
                    maxWorkerPoolSizeInt = 1;
                } else if (maxWorkerPoolSizeInt < 0) {
                    maxWorkerPoolSizeInt = Math.abs(maxWorkerPoolSizeInt) * Runtime.getRuntime().availableProcessors();
                }
                this.maxWorkerPoolSize = maxWorkerPoolSizeInt;
            } catch (NumberFormatException e) {
                throw new GenericEntityConfException("<datasource> element max-worker-pool-size attribute is invalid" + lineNumberText);
            }
        }
        List<SqlLoadPath> sqlLoadPathList = config.getSubElementsAsListEntries(xPath, element, SqlLoadPath.class);
        if (sqlLoadPathList.isEmpty()) {
            this.sqlLoadPathList = Collections.emptyList();
        } else {
            this.sqlLoadPathList = Collections.unmodifiableList(sqlLoadPathList);
        }
        List<ReadData> readDataList = config.getSubElementsAsListEntries(xPath, element, ReadData.class);
        if (readDataList.isEmpty()) {
            this.readDataList = Collections.emptyList();
        } else {
            this.readDataList = Collections.unmodifiableList(readDataList);
        }
        int jdbcElementCount = 0;
        InlineJdbc inlineJdbc = config.getObjectSubElement(xPath, element, InlineJdbc.class);
        if (inlineJdbc == null) {
            this.inlineJdbc = null;
        } else {
            this.inlineJdbc = inlineJdbc;
            jdbcElementCount++;
        }
        JndiJdbc jndiJdbc = config.getObjectSubElement(xPath, element, JndiJdbc.class);
        if (jndiJdbc == null) {
            this.jndiJdbc = null;
        } else {
            this.jndiJdbc = jndiJdbc;
            jdbcElementCount++;
        }
        TyrexDataSource tyrex = config.getObjectSubElement(xPath, element, TyrexDataSource.class);
        if (tyrex == null) {
            tyrexDataSource = null;
        } else {
            this.tyrexDataSource = tyrex;
            jdbcElementCount++;
        }
        if (jdbcElementCount > 1) {
            throw new GenericEntityConfException("<datasource> element is invalid: Only one of <inline-jdbc>, <jndi-jdbc>, "
                    + "<tyrex-dataSource> is allowed" + lineNumberText);
        }
    }

    Datasource(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        boolean checkStructure = ConfigHelper.checkStrictXmlStructure();
        this.xPath = xPath;
        name = getNameFromXPath(xPath);
        if (name.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<datasource> element name attribute is empty");
        }
        String helperClass = config.getValue(configObject, "/@helper-class");
        if (helperClass.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<datasource> element helper-class attribute is empty");
        }
        this.helperClass = helperClass;
        String fieldTypeName = config.getValue(configObject, "/@field-type-name");
        if (fieldTypeName.isEmpty() && checkStructure) {
            throw new GenericEntityConfException("<datasource> element field-type-name attribute is empty");
        }
        this.fieldTypeName = fieldTypeName;
        useSchemas = "true".equals(config.getValue(configObject, "/@use-schemas"));
        schemaName = config.getValue(configObject, "/@schema-name");
        checkOnStart = "true".equals(config.getValue(configObject, "/@check-on-start"));
        addMissingOnStart = "true".equals(config.getValue(configObject, "/@add-missing-on-start"));
        usePkConstraintNames = "true".equals(config.getValue(configObject, "/@use-pk-constraint-names"));
        checkPksOnStart = "true".equals(config.getValue(configObject, "/@check-pks-on-start"));
        constraintNameClipLength = config.getValue(configObject, "/@constraint-name-clip-length", 30, Integer.class);
        useProxyCursor = "true".equalsIgnoreCase(config.getValue(configObject, "/@use-proxy-cursor"));
        proxyCursorName = config.getValue(configObject, "/@proxy-cursor-name", "p_cursor", String.class);
        resultFetchSize = config.getValue(configObject, "/@result-fetch-size", -1, Integer.class);
        useForeignKeys = "true".equals(config.getValue(configObject, "/@use-foreign-keys"));
        useForeignKeyIndices = "true".equals(config.getValue(configObject, "/@use-foreign-key-indices"));
        checkFksOnStart = "true".equals(config.getValue(configObject, "/@check-fks-on-start"));
        checkFkIndicesOnStart = "true".equals(config.getValue(configObject, "/@check-fks-indices-on-start"));
        fkStyle = config.getValue(configObject, "/@fk-style", "name_constraint", String.class);
        useFkInitiallyDeferred = "true".equals(config.getValue(configObject, "/@use-fk-initially-deferred"));
        useIndices = "true".equals(config.getValue(configObject, "/@use-indices"));
        useIndicesUnique = "true".equals(config.getValue(configObject, "/@use-indices-unique"));
        checkIndicesOnStart = "true".equals(config.getValue(configObject, "/@check-indices-on-start"));
        joinStyle = config.getValue(configObject, "/@join-style", "ansi", String.class);
        aliasViewColumns = "true".equals(config.getValue(configObject, "/@alias-view-columns"));
        alwaysUseConstraintKeyword = "true".equals(config.getValue(configObject, "/@always-use-constraint-keyword"));
        dropFkUseForeignKeyKeyword = "true".equals(config.getValue(configObject, "/@drop-fk-use-foreign-keyword"));
        useBinaryTypeForBlob = "true".equals(config.getValue(configObject, "/@use-binary-type-for-blob"));
        useOrderByNulls = "true".equals(config.getValue(configObject, "/@use-order-by-nulls"));
        offsetStyle = config.getValue(configObject, "/@offset-style", "none", String.class);
        tableType = config.getValue(configObject, "/@table-type");
        characterSet = config.getValue(configObject, "/@character-set");
        collate = config.getValue(configObject, "/@collate");
        String maxWorkerPoolSize = config.getValue(configObject, "/@max-worker-pool-size");
        if (maxWorkerPoolSize.isEmpty()) {
            this.maxWorkerPoolSize = 1;
        } else {
            try {
                int maxWorkerPoolSizeInt = Integer.parseInt(maxWorkerPoolSize);
                if (maxWorkerPoolSizeInt == 0) {
                    maxWorkerPoolSizeInt = 1;
                } else if (maxWorkerPoolSizeInt < 0) {
                    maxWorkerPoolSizeInt = Math.abs(maxWorkerPoolSizeInt) * Runtime.getRuntime().availableProcessors();
                }
                this.maxWorkerPoolSize = maxWorkerPoolSizeInt;
            } catch (NumberFormatException e) {
                throw new GenericEntityConfException("<datasource> element max-worker-pool-size attribute is invalid");
            }
        }
        List<SqlLoadPath> sqlLoadPathList = config.getSubElementsAsListEntries(xPath.concat("/sql-load-path"), null, SqlLoadPath.class);
        this.sqlLoadPathList = sqlLoadPathList.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(sqlLoadPathList);

        List<ReadData> readDataList = new ArrayList<>();
        List<Map<String, Object>> readDataConfs = UtilGenerics.cast(configObject.get("read-data"));
        if (UtilValidate.isNotEmpty(readDataConfs)) {
            for (Map<String, Object> readDataConf : readDataConfs) {
                readDataList.add(ReadData.loadFromConfig(readDataConf, "/read-data"));
            }
        }

        this.readDataList = readDataList.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(readDataList);

        int jdbcElementCount = 0;

        Map<String, Object> inlineJdbcConfig = UtilGenerics.cast(configObject.get("inline-jdbc"));
        if (UtilValidate.isNotEmpty(inlineJdbcConfig)) {
            this.inlineJdbc = InlineJdbc.loadFromConfig(inlineJdbcConfig, "/inline-jdbc");
            jdbcElementCount++;
        } else {
            this.inlineJdbc = null;
        }

        Map<String, Object> jndiJdbcConfig = UtilGenerics.cast(configObject.get("jndi-jdbc"));
        if (UtilValidate.isNotEmpty(jndiJdbcConfig)) {
            this.jndiJdbc = JndiJdbc.loadFromConfig(jndiJdbcConfig, "/jndi-jdbc");
            jdbcElementCount++;
        } else {
            this.jndiJdbc = null;
        }

        Map<String, Object> tyrexJdbcConfig = UtilGenerics.cast(configObject.get("tyrex-jdbc"));
        if (UtilValidate.isNotEmpty(tyrexJdbcConfig)) {
            this.tyrexDataSource = TyrexDataSource.loadFromConfig(tyrexJdbcConfig, "/tyrex-jdbc");
            jdbcElementCount++;
        } else {
            this.tyrexDataSource = null;
        }

        if (jdbcElementCount > 1) {
            throw new GenericEntityConfException("<datasource> element is invalid: Only one of <inline-jdbc>, <jndi-jdbc>, "
                    + "<tyrex-dataSource> is allowed");
        }
    }

    public static Datasource loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new Datasource(element, xPathParent);
    }

    public static Datasource loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new Datasource(configMap, xPath);
    }

    public String getHelperClass() {
        return helperClass;
    }

    public String getFieldTypeName() {
        return fieldTypeName;
    }

    public boolean getUseSchemas() {
        return useSchemas;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public boolean getCheckOnStart() {
        return checkOnStart;
    }

    public boolean getAddMissingOnStart() {
        return addMissingOnStart;
    }

    public boolean getUsePkConstraintNames() {
        return usePkConstraintNames;
    }

    public boolean getCheckPksOnStart() {
        return checkPksOnStart;
    }

    public int getConstraintNameClipLength() {
        return constraintNameClipLength;
    }

    public boolean getUseProxyCursor() {
        return useProxyCursor;
    }

    public String getProxyCursorName() {
        return proxyCursorName;
    }

    public int getResultFetchSize() {
        return resultFetchSize;
    }

    public boolean getUseForeignKeys() {
        return useForeignKeys;
    }

    public boolean getUseForeignKeyIndices() {
        return useForeignKeyIndices;
    }

    public boolean getCheckFksOnStart() {
        return checkFksOnStart;
    }

    public boolean getCheckFkIndicesOnStart() {
        return checkFkIndicesOnStart;
    }

    public String getFkStyle() {
        return fkStyle;
    }

    public boolean getUseFkInitiallyDeferred() {
        return useFkInitiallyDeferred;
    }

    public boolean getUseIndices() {
        return useIndices;
    }

    public boolean getUseIndicesUnique() {
        return useIndicesUnique;
    }

    public boolean getCheckIndicesOnStart() {
        return checkIndicesOnStart;
    }

    public String getJoinStyle() {
        return joinStyle;
    }

    public boolean getAliasViewColumns() {
        return aliasViewColumns;
    }

    public boolean getAlwaysUseConstraintKeyword() {
        return alwaysUseConstraintKeyword;
    }

    public boolean getDropFkUseForeignKeyKeyword() {
        return dropFkUseForeignKeyKeyword;
    }

    public boolean getUseBinaryTypeForBlob() {
        return useBinaryTypeForBlob;
    }

    public boolean getUseOrderByNulls() {
        return useOrderByNulls;
    }

    public String getOffsetStyle() {
        return offsetStyle;
    }

    public String getTableType() {
        return tableType;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public String getCollate() {
        return collate;
    }

    public int getMaxWorkerPoolSize() {
        return maxWorkerPoolSize;
    }

    public List<SqlLoadPath> getSqlLoadPathList() {
        return sqlLoadPathList;
    }

    public List<ReadData> getReadDataList() {
        return readDataList;
    }

    public InlineJdbc getInlineJdbc() {
        return inlineJdbc;
    }

    public JndiJdbc getJndiJdbc() {
        return jndiJdbc;
    }

    public TyrexDataSource getTyrexDataSource() {
        return tyrexDataSource;
    }

    @Override
    public String getName() {
        return name;
    }

}
