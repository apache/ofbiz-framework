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
 * An object that models the <code>&lt;datasource&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class Datasource {

    private final String name; // type = xs:string
    private final String helperClass; // type = xs:string
    private final String fieldTypeName; // type = xs:string
    private final String useSchemas;
    private final String schemaName; // type = xs:string
    private final String checkOnStart;
    private final String addMissingOnStart;
    private final String usePkConstraintNames;
    private final String checkPksOnStart;
    private final String constraintNameClipLength; // type = xs:nonNegativeInteger
    private final String useProxyCursor;
    private final String proxyCursorName; // type = xs:string
    private final String resultFetchSize; // type = xs:integer
    private final String useForeignKeys;
    private final String useForeignKeyIndices;
    private final String checkFksOnStart;
    private final String checkFkIndicesOnStart;
    private final String fkStyle;
    private final String useFkInitiallyDeferred;
    private final String useIndices;
    private final String useIndicesUnique;
    private final String checkIndicesOnStart;
    private final String joinStyle;
    private final String aliasViewColumns;
    private final String alwaysUseConstraintKeyword;
    private final String dropFkUseForeignKeyKeyword;
    private final String useBinaryTypeForBlob;
    private final String useOrderByNulls;
    private final String offsetStyle;
    private final String tableType; // type = xs:string
    private final String characterSet; // type = xs:string
    private final String collate; // type = xs:string
    private final String maxWorkerPoolSize; // type = xs:integer
    private final List<SqlLoadPath> sqlLoadPathList; // <sql-load-path>
    private final List<ReadData> readDataList; // <read-data>

    public Datasource(Element element) throws GenericEntityConfException {
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element name attribute is empty");
        }
        this.name = name;
        String helperClass = element.getAttribute("helper-class").intern();
        if (helperClass.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element helper-class attribute is empty");
        }
        this.helperClass = helperClass;
        String fieldTypeName = element.getAttribute("field-type-name").intern();
        if (fieldTypeName.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element field-type-name attribute is empty");
        }
        this.fieldTypeName = fieldTypeName;
        String useSchemas = element.getAttribute("use-schemas").intern();
        if (useSchemas.isEmpty()) {
            useSchemas = "true";
        }
        this.useSchemas = useSchemas;
        this.schemaName = element.getAttribute("schema-name").intern();
        String checkOnStart = element.getAttribute("check-on-start").intern();
        if (checkOnStart.isEmpty()) {
            checkOnStart = "true";
        }
        this.checkOnStart = checkOnStart;
        String addMissingOnStart = element.getAttribute("add-missing-on-start").intern();
        if (addMissingOnStart.isEmpty()) {
            addMissingOnStart = "false";
        }
        this.addMissingOnStart = addMissingOnStart;
        String usePkConstraintNames = element.getAttribute("use-pk-constraint-names").intern();
        if (usePkConstraintNames.isEmpty()) {
            usePkConstraintNames = "true";
        }
        this.usePkConstraintNames = usePkConstraintNames;
        String checkPksOnStart = element.getAttribute("check-pks-on-start").intern();
        if (checkPksOnStart.isEmpty()) {
            checkPksOnStart = "true";
        }
        this.checkPksOnStart = checkPksOnStart;
        String constraintNameClipLength = element.getAttribute("constraint-name-clip-length").intern();
        if (constraintNameClipLength.isEmpty()) {
            constraintNameClipLength = "30";
        }
        this.constraintNameClipLength = constraintNameClipLength;
        String useProxyCursor = element.getAttribute("use-proxy-cursor").intern();
        if (useProxyCursor.isEmpty()) {
            useProxyCursor = "false";
        }
        this.useProxyCursor = useProxyCursor;
        String proxyCursorName = element.getAttribute("proxy-cursor-name").intern();
        if (proxyCursorName.isEmpty()) {
            proxyCursorName = "p_cursor";
        }
        this.proxyCursorName = proxyCursorName;
        String resultFetchSize = element.getAttribute("result-fetch-size").intern();
        if (resultFetchSize.isEmpty()) {
            resultFetchSize = "-1";
        }
        this.resultFetchSize = resultFetchSize;
        String useForeignKeys = element.getAttribute("use-foreign-keys").intern();
        if (useForeignKeys.isEmpty()) {
            useForeignKeys = "true";
        }
        this.useForeignKeys = useForeignKeys;
        String useForeignKeyIndices = element.getAttribute("use-foreign-key-indices").intern();
        if (useForeignKeyIndices.isEmpty()) {
            useForeignKeyIndices = "true";
        }
        this.useForeignKeyIndices = useForeignKeyIndices;
        String checkFksOnStart = element.getAttribute("check-fks-on-start").intern();
        if (checkFksOnStart.isEmpty()) {
            checkFksOnStart = "false";
        }
        this.checkFksOnStart = checkFksOnStart;
        String checkFkIndicesOnStart = element.getAttribute("check-fk-indices-on-start").intern();
        if (checkFkIndicesOnStart.isEmpty()) {
            checkFkIndicesOnStart = "false";
        }
        this.checkFkIndicesOnStart = checkFkIndicesOnStart;
        String fkStyle = element.getAttribute("fk-style").intern();
        if (fkStyle.isEmpty()) {
            fkStyle = "name_constraint";
        }
        this.fkStyle = fkStyle;
        String useFkInitiallyDeferred = element.getAttribute("use-fk-initially-deferred").intern();
        if (useFkInitiallyDeferred.isEmpty()) {
            useFkInitiallyDeferred = "false";
        }
        this.useFkInitiallyDeferred = useFkInitiallyDeferred;
        String useIndices = element.getAttribute("use-indices").intern();
        if (useIndices.isEmpty()) {
            useIndices = "true";
        }
        this.useIndices = useIndices;
        String useIndicesUnique = element.getAttribute("use-indices-unique").intern();
        if (useIndicesUnique.isEmpty()) {
            useIndicesUnique = "true";
        }
        this.useIndicesUnique = useIndicesUnique;
        String checkIndicesOnStart = element.getAttribute("check-indices-on-start").intern();
        if (checkIndicesOnStart.isEmpty()) {
            checkIndicesOnStart = "false";
        }
        this.checkIndicesOnStart = checkIndicesOnStart;
        String joinStyle = element.getAttribute("join-style").intern();
        if (joinStyle.isEmpty()) {
            joinStyle = "ansi";
        }
        this.joinStyle = joinStyle;
        String aliasViewColumns = element.getAttribute("alias-view-columns").intern();
        if (aliasViewColumns.isEmpty()) {
            aliasViewColumns = "false";
        }
        this.aliasViewColumns = aliasViewColumns;
        String alwaysUseConstraintKeyword = element.getAttribute("always-use-constraint-keyword").intern();
        if (alwaysUseConstraintKeyword.isEmpty()) {
            alwaysUseConstraintKeyword = "false";
        }
        this.alwaysUseConstraintKeyword = alwaysUseConstraintKeyword;
        String dropFkUseForeignKeyKeyword = element.getAttribute("drop-fk-use-foreign-key-keyword").intern();
        if (dropFkUseForeignKeyKeyword.isEmpty()) {
            dropFkUseForeignKeyKeyword = "false";
        }
        this.dropFkUseForeignKeyKeyword = dropFkUseForeignKeyKeyword;
        String useBinaryTypeForBlob = element.getAttribute("use-binary-type-for-blob").intern();
        if (useBinaryTypeForBlob.isEmpty()) {
            useBinaryTypeForBlob = "false";
        }
        this.useBinaryTypeForBlob = useBinaryTypeForBlob;
        String useOrderByNulls = element.getAttribute("use-order-by-nulls").intern();
        if (useOrderByNulls.isEmpty()) {
            useOrderByNulls = "false";
        }
        this.useOrderByNulls = useOrderByNulls;
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
            maxWorkerPoolSize = "0";
        }
        this.maxWorkerPoolSize = maxWorkerPoolSize;
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
    public String getUseSchemas() {
        return this.useSchemas;
    }

    /** Returns the value of the <code>schema-name</code> attribute. */
    public String getSchemaName() {
        return this.schemaName;
    }

    /** Returns the value of the <code>check-on-start</code> attribute. */
    public String getCheckOnStart() {
        return this.checkOnStart;
    }

    /** Returns the value of the <code>add-missing-on-start</code> attribute. */
    public String getAddMissingOnStart() {
        return this.addMissingOnStart;
    }

    /** Returns the value of the <code>use-pk-constraint-names</code> attribute. */
    public String getUsePkConstraintNames() {
        return this.usePkConstraintNames;
    }

    /** Returns the value of the <code>check-pks-on-start</code> attribute. */
    public String getCheckPksOnStart() {
        return this.checkPksOnStart;
    }

    /** Returns the value of the <code>constraint-name-clip-length</code> attribute. */
    public String getConstraintNameClipLength() {
        return this.constraintNameClipLength;
    }

    /** Returns the value of the <code>use-proxy-cursor</code> attribute. */
    public String getUseProxyCursor() {
        return this.useProxyCursor;
    }

    /** Returns the value of the <code>proxy-cursor-name</code> attribute. */
    public String getProxyCursorName() {
        return this.proxyCursorName;
    }

    /** Returns the value of the <code>result-fetch-size</code> attribute. */
    public String getResultFetchSize() {
        return this.resultFetchSize;
    }

    /** Returns the value of the <code>use-foreign-keys</code> attribute. */
    public String getUseForeignKeys() {
        return this.useForeignKeys;
    }

    /** Returns the value of the <code>use-foreign-key-indices</code> attribute. */
    public String getUseForeignKeyIndices() {
        return this.useForeignKeyIndices;
    }

    /** Returns the value of the <code>check-fks-on-start</code> attribute. */
    public String getCheckFksOnStart() {
        return this.checkFksOnStart;
    }

    /** Returns the value of the <code>check-fk-indices-on-start</code> attribute. */
    public String getCheckFkIndicesOnStart() {
        return this.checkFkIndicesOnStart;
    }

    /** Returns the value of the <code>fk-style</code> attribute. */
    public String getFkStyle() {
        return this.fkStyle;
    }

    /** Returns the value of the <code>use-fk-initially-deferred</code> attribute. */
    public String getUseFkInitiallyDeferred() {
        return this.useFkInitiallyDeferred;
    }

    /** Returns the value of the <code>use-indices</code> attribute. */
    public String getUseIndices() {
        return this.useIndices;
    }

    /** Returns the value of the <code>use-indices-unique</code> attribute. */
    public String getUseIndicesUnique() {
        return this.useIndicesUnique;
    }

    /** Returns the value of the <code>check-indices-on-start</code> attribute. */
    public String getCheckIndicesOnStart() {
        return this.checkIndicesOnStart;
    }

    /** Returns the value of the <code>join-style</code> attribute. */
    public String getJoinStyle() {
        return this.joinStyle;
    }

    /** Returns the value of the <code>alias-view-columns</code> attribute. */
    public String getAliasViewColumns() {
        return this.aliasViewColumns;
    }

    /** Returns the value of the <code>always-use-constraint-keyword</code> attribute. */
    public String getAlwaysUseConstraintKeyword() {
        return this.alwaysUseConstraintKeyword;
    }

    /** Returns the value of the <code>drop-fk-use-foreign-key-keyword</code> attribute. */
    public String getDropFkUseForeignKeyKeyword() {
        return this.dropFkUseForeignKeyKeyword;
    }

    /** Returns the value of the <code>use-binary-type-for-blob</code> attribute. */
    public String getUseBinaryTypeForBlob() {
        return this.useBinaryTypeForBlob;
    }

    /** Returns the value of the <code>use-order-by-nulls</code> attribute. */
    public String getUseOrderByNulls() {
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
    public String getMaxWorkerPoolSize() {
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
}
