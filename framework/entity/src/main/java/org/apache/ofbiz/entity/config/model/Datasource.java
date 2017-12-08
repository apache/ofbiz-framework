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

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;datasource&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class Datasource {

/*
    public static final int TYPE_JNDI_JDBC = 1;
    public static final int TYPE_INLINE_JDBC = 2;
    public static final int TYPE_TYREX_DATA_SOURCE = 3;
    public static final int TYPE_OTHER = 4;
*/

    private final String name; // type = xs:string
    private final String helperClass; // type = xs:string
    private final String fieldTypeName; // type = xs:string
    private final boolean useSchemas;
    private final String schemaName; // type = xs:string
    private final boolean checkOnStart;
    private final boolean addMissingOnStart;
    private final boolean usePkConstraintNames;
    private final boolean checkPksOnStart;
    private final int constraintNameClipLength; // type = xs:nonNegativeInteger
    private final boolean useProxyCursor;
    private final String proxyCursorName; // type = xs:string
    private final int resultFetchSize; // type = xs:integer
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
    private final String tableType; // type = xs:string
    private final String characterSet; // type = xs:string
    private final String collate; // type = xs:string
    private final int maxWorkerPoolSize; // type = xs:integer
    private final List<SqlLoadPath> sqlLoadPathList; // <sql-load-path>
    private final List<ReadData> readDataList; // <read-data>
    private final InlineJdbc inlineJdbc; // <inline-jdbc>
    private final JndiJdbc jndiJdbc; // <jndi-jdbc>
    private final TyrexDataSource tyrexDataSource; // <tyrex-dataSource>

    Datasource(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<datasource> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        String helperClass = element.getAttribute("helper-class").intern();
        if (helperClass.isEmpty()) {
            throw new GenericEntityConfException("<datasource> element helper-class attribute is empty" + lineNumberText);
        }
        this.helperClass = helperClass;
        String fieldTypeName = element.getAttribute("field-type-name").intern();
        if (fieldTypeName.isEmpty()) {
            throw new GenericEntityConfException("<datasource> element field-type-name attribute is empty" + lineNumberText);
        }
        this.fieldTypeName = fieldTypeName;
        this.useSchemas = !"false".equals(element.getAttribute("use-schemas"));
        this.schemaName = element.getAttribute("schema-name").intern();
        this.checkOnStart = !"false".equals(element.getAttribute("check-on-start"));
        this.addMissingOnStart = "true".equals(element.getAttribute("add-missing-on-start"));
        this.usePkConstraintNames = !"false".equals(element.getAttribute("use-pk-constraint-names"));
        this.checkPksOnStart = !"false".equals(element.getAttribute("check-pks-on-start"));
        String constraintNameClipLength = element.getAttribute("constraint-name-clip-length");
        if (constraintNameClipLength.isEmpty()) {
            this.constraintNameClipLength = 30;
        } else {
            try {
                this.constraintNameClipLength = Integer.parseInt(constraintNameClipLength);
            } catch (Exception e) {
                throw new GenericEntityConfException("<datasource> element constraint-name-clip-length attribute is invalid" + lineNumberText);
            }
        }
        this.useProxyCursor = "true".equalsIgnoreCase(element.getAttribute("use-proxy-cursor"));
        String proxyCursorName = element.getAttribute("proxy-cursor-name").intern();
        if (proxyCursorName.isEmpty()) {
            proxyCursorName = "p_cursor";
        }
        this.proxyCursorName = proxyCursorName;
        String resultFetchSize = element.getAttribute("result-fetch-size");
        if (resultFetchSize.isEmpty()) {
            this.resultFetchSize = -1;
        } else {
            try {
                this.resultFetchSize = Integer.parseInt(resultFetchSize);
            } catch (Exception e) {
                throw new GenericEntityConfException("<datasource> element result-fetch-size attribute is invalid" + lineNumberText);
            }
        }
        this.useForeignKeys = !"false".equals(element.getAttribute("use-foreign-keys"));
        this.useForeignKeyIndices = !"false".equals(element.getAttribute("use-foreign-key-indices"));
        this.checkFksOnStart = "true".equals(element.getAttribute("check-fks-on-start"));
        this.checkFkIndicesOnStart = "true".equals(element.getAttribute("check-fk-indices-on-start"));
        String fkStyle = element.getAttribute("fk-style").intern();
        if (fkStyle.isEmpty()) {
            fkStyle = "name_constraint";
        }
        this.fkStyle = fkStyle;
        this.useFkInitiallyDeferred = "true".equals(element.getAttribute("use-fk-initially-deferred"));
        this.useIndices = !"false".equals(element.getAttribute("use-indices"));
        this.useIndicesUnique = !"false".equals(element.getAttribute("use-indices-unique"));
        this.checkIndicesOnStart = "true".equals(element.getAttribute("check-indices-on-start"));
        String joinStyle = element.getAttribute("join-style").intern();
        if (joinStyle.isEmpty()) {
            joinStyle = "ansi";
        }
        this.joinStyle = joinStyle;
        this.aliasViewColumns = "true".equals(element.getAttribute("alias-view-columns"));
        this.alwaysUseConstraintKeyword = "true".equals(element.getAttribute("always-use-constraint-keyword"));
        this.dropFkUseForeignKeyKeyword = "true".equals(element.getAttribute("drop-fk-use-foreign-key-keyword"));
        this.useBinaryTypeForBlob = "true".equals(element.getAttribute("use-binary-type-for-blob"));
        this.useOrderByNulls = "true".equals(element.getAttribute("use-order-by-nulls"));
        String offsetStyle = element.getAttribute("offset-style").intern();
        if (offsetStyle.isEmpty()) {
            offsetStyle = "none";
        }
        this.offsetStyle = offsetStyle;
        this.tableType = element.getAttribute("table-type").intern();
        this.characterSet = element.getAttribute("character-set").intern();
        this.collate = element.getAttribute("collate").intern();
        String maxWorkerPoolSize = element.getAttribute("max-worker-pool-size").intern();
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
        List<? extends Element> sqlLoadPathElementList = UtilXml.childElementList(element, "sql-load-path");
        if (sqlLoadPathElementList.isEmpty()) {
            this.sqlLoadPathList = Collections.emptyList();
        } else {
            List<SqlLoadPath> sqlLoadPathList = new ArrayList<SqlLoadPath>(sqlLoadPathElementList.size());
            for (Element sqlLoadPathElement : sqlLoadPathElementList) {
                sqlLoadPathList.add(new SqlLoadPath(sqlLoadPathElement));
            }
            this.sqlLoadPathList = Collections.unmodifiableList(sqlLoadPathList);
        }
        List<? extends Element> readDataElementList = UtilXml.childElementList(element, "read-data");
        if (readDataElementList.isEmpty()) {
            this.readDataList = Collections.emptyList();
        } else {
            List<ReadData> readDataList = new ArrayList<ReadData>(readDataElementList.size());
            for (Element readDataElement : readDataElementList) {
                readDataList.add(new ReadData(readDataElement));
            }
            this.readDataList = Collections.unmodifiableList(readDataList);
        }
        int jdbcElementCount = 0;
        Element inlineJdbcElement = UtilXml.firstChildElement(element, "inline-jdbc");
        if (inlineJdbcElement == null) {
            this.inlineJdbc = null;
        } else {
            this.inlineJdbc = new InlineJdbc(inlineJdbcElement);
            jdbcElementCount++;
        }
        Element jndiJdbcElement = UtilXml.firstChildElement(element, "jndi-jdbc");
        if (jndiJdbcElement == null) {
            this.jndiJdbc = null;
        } else {
            this.jndiJdbc = new JndiJdbc(jndiJdbcElement);
            jdbcElementCount++;
        }
        Element tyrexElement = UtilXml.firstChildElement(element, "tyrex-dataSource");
        if (tyrexElement == null) {
            this.tyrexDataSource = null;
        } else {
            this.tyrexDataSource = new TyrexDataSource(tyrexElement);
            jdbcElementCount++;
        }
        if (jdbcElementCount > 1) {
            throw new GenericEntityConfException("<datasource> element is invalid: Only one of <inline-jdbc>, <jndi-jdbc>, <tyrex-dataSource> is allowed" + lineNumberText);
        }
    }

    /** Returns the value of the <code>name</code> attribute. */
    public String getName() {
        return this.name;
    }

    /** Returns the value of the <code>helper-class</code> attribute. */
    public String getHelperClass() {
        return this.helperClass;
    }

    /** Returns the value of the <code>field-type-name</code> attribute. */
    public String getFieldTypeName() {
        return this.fieldTypeName;
    }

    /** Returns the value of the <code>use-schemas</code> attribute. */
    public boolean getUseSchemas() {
        return this.useSchemas;
    }

    /** Returns the value of the <code>schema-name</code> attribute. */
    public String getSchemaName() {
        return this.schemaName;
    }

    /** Returns the value of the <code>check-on-start</code> attribute. */
    public boolean getCheckOnStart() {
        return this.checkOnStart;
    }

    /** Returns the value of the <code>add-missing-on-start</code> attribute. */
    public boolean getAddMissingOnStart() {
        return this.addMissingOnStart;
    }

    /** Returns the value of the <code>use-pk-constraint-names</code> attribute. */
    public boolean getUsePkConstraintNames() {
        return this.usePkConstraintNames;
    }

    /** Returns the value of the <code>check-pks-on-start</code> attribute. */
    public boolean getCheckPksOnStart() {
        return this.checkPksOnStart;
    }

    /** Returns the value of the <code>constraint-name-clip-length</code> attribute. */
    public int getConstraintNameClipLength() {
        return this.constraintNameClipLength;
    }

    /** Returns the value of the <code>use-proxy-cursor</code> attribute. */
    public boolean getUseProxyCursor() {
        return this.useProxyCursor;
    }

    /** Returns the value of the <code>proxy-cursor-name</code> attribute. */
    public String getProxyCursorName() {
        return this.proxyCursorName;
    }

    /** Returns the value of the <code>result-fetch-size</code> attribute. */
    public int getResultFetchSize() {
        return this.resultFetchSize;
    }

    /** Returns the value of the <code>use-foreign-keys</code> attribute. */
    public boolean getUseForeignKeys() {
        return this.useForeignKeys;
    }

    /** Returns the value of the <code>use-foreign-key-indices</code> attribute. */
    public boolean getUseForeignKeyIndices() {
        return this.useForeignKeyIndices;
    }

    /** Returns the value of the <code>check-fks-on-start</code> attribute. */
    public boolean getCheckFksOnStart() {
        return this.checkFksOnStart;
    }

    /** Returns the value of the <code>check-fk-indices-on-start</code> attribute. */
    public boolean getCheckFkIndicesOnStart() {
        return this.checkFkIndicesOnStart;
    }

    /** Returns the value of the <code>fk-style</code> attribute. */
    public String getFkStyle() {
        return this.fkStyle;
    }

    /** Returns the value of the <code>use-fk-initially-deferred</code> attribute. */
    public boolean getUseFkInitiallyDeferred() {
        return this.useFkInitiallyDeferred;
    }

    /** Returns the value of the <code>use-indices</code> attribute. */
    public boolean getUseIndices() {
        return this.useIndices;
    }

    /** Returns the value of the <code>use-indices-unique</code> attribute. */
    public boolean getUseIndicesUnique() {
        return this.useIndicesUnique;
    }

    /** Returns the value of the <code>check-indices-on-start</code> attribute. */
    public boolean getCheckIndicesOnStart() {
        return this.checkIndicesOnStart;
    }

    /** Returns the value of the <code>join-style</code> attribute. */
    public String getJoinStyle() {
        return this.joinStyle;
    }

    /** Returns the value of the <code>alias-view-columns</code> attribute. */
    public boolean getAliasViewColumns() {
        return this.aliasViewColumns;
    }

    /** Returns the value of the <code>always-use-constraint-keyword</code> attribute. */
    public boolean getAlwaysUseConstraintKeyword() {
        return this.alwaysUseConstraintKeyword;
    }

    /** Returns the value of the <code>drop-fk-use-foreign-key-keyword</code> attribute. */
    public boolean getDropFkUseForeignKeyKeyword() {
        return this.dropFkUseForeignKeyKeyword;
    }

    /** Returns the value of the <code>use-binary-type-for-blob</code> attribute. */
    public boolean getUseBinaryTypeForBlob() {
        return this.useBinaryTypeForBlob;
    }

    /** Returns the value of the <code>use-order-by-nulls</code> attribute. */
    public boolean getUseOrderByNulls() {
        return this.useOrderByNulls;
    }

    /** Returns the value of the <code>offset-style</code> attribute. */
    public String getOffsetStyle() {
        return this.offsetStyle;
    }

    /** Returns the value of the <code>table-type</code> attribute. */
    public String getTableType() {
        return this.tableType;
    }

    /** Returns the value of the <code>character-set</code> attribute. */
    public String getCharacterSet() {
        return this.characterSet;
    }

    /** Returns the value of the <code>collate</code> attribute. */
    public String getCollate() {
        return this.collate;
    }

    /** Returns the value of the <code>max-worker-pool-size</code> attribute. */
    public int getMaxWorkerPoolSize() {
        return this.maxWorkerPoolSize;
    }

    /** Returns the <code>&lt;sql-load-path&gt;</code> child elements. */
    public List<SqlLoadPath> getSqlLoadPathList() {
        return this.sqlLoadPathList;
    }

    /** Returns the <code>&lt;read-data&gt;</code> child elements. */
    public List<ReadData> getReadDataList() {
        return this.readDataList;
    }

    /** Returns the <code>&lt;inline-jdbc&gt;</code> child element. */
    public InlineJdbc getInlineJdbc() {
        return this.inlineJdbc;
    }

    /** Returns the <code>&lt;jndi-jdbc&gt;</code> child element. */
    public JndiJdbc getJndiJdbc() {
        return this.jndiJdbc;
    }

    /** Returns the <code>&lt;tyrex-dataSource&gt;</code> child element. */
    public TyrexDataSource getTyrexDataSource() {
        return this.tyrexDataSource;
    }
}
