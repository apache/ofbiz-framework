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

import java.util.LinkedList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the entityengine.xml file
 *
 */
public class DatasourceInfo {
    public static final String module = DatasourceInfo.class.getName();

    public String name;
    public String helperClass;
    public String fieldTypeName;
    public List sqlLoadPaths = new LinkedList();
    public List readDatas = new LinkedList();
    public Element datasourceElement;
    
    public static final int TYPE_JNDI_JDBC = 1;        
    public static final int TYPE_INLINE_JDBC = 2;
    public static final int TYPE_TYREX_DATA_SOURCE = 3;
    public static final int TYPE_OTHER = 4;
            
    public Element jndiJdbcElement;
    public Element tyrexDataSourceElement;
    public Element inlineJdbcElement;

    public String schemaName = null;
    public boolean useSchemas = true;
    public boolean checkOnStart = true;
    public boolean addMissingOnStart = false;
    public boolean useFks = true;
    public boolean useFkIndices = true;
    public boolean checkPrimaryKeysOnStart = false;
    public boolean checkForeignKeysOnStart = false;
    public boolean checkFkIndicesOnStart = false;
    public boolean usePkConstraintNames = true;
    public int constraintNameClipLength = 30;
    public boolean useProxyCursor = false;
    public String cursorName = "p_cursor";
    public int resultFetchSize = -1;
    public String fkStyle = null;
    public boolean useFkInitiallyDeferred = true;
    public boolean useIndices = true;
    public boolean checkIndicesOnStart = false;
    public String joinStyle = null;
    public boolean aliasViews = true;
    public boolean alwaysUseConstraintKeyword = false;
    public boolean dropFkUseForeignKeyKeyword = false;
    public boolean useBinaryTypeForBlob = false;
    public String tableType = null;
    public String characterSet = null;
    public String collate = null;

    public DatasourceInfo(Element element) {
        this.name = element.getAttribute("name");
        this.helperClass = element.getAttribute("helper-class");
        this.fieldTypeName = element.getAttribute("field-type-name");

        sqlLoadPaths = UtilXml.childElementList(element, "sql-load-path");
        readDatas = UtilXml.childElementList(element, "read-data");
        datasourceElement = element;

        if (datasourceElement == null) {
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for schema-name (none)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-schemas (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-on-start (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for add-missing-on-start (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-pks-on-start (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-foreign-keys (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default use-foreign-key-indices (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-fks-on-start (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-fk-indices-on-start (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-pk-constraint-names (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for constraint-name-clip-length (30)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for fk-style (name_constraint)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-fk-initially-deferred (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-indices (true)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for check-indices-on-start (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for join-style (ansi)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for always-use-constraint-keyword (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for drop-fk-use-foreign-key-keyword (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for use-binary-type-for-blob (false)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for table-type (none)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for character-set (none)", module);
            Debug.logWarning("datasource def not found with name " + this.name + ", using default for collate (none)", module);
        } else {
            this.schemaName = datasourceElement.getAttribute("schema-name");
            // anything but false is true
            this.useSchemas = !"false".equals(datasourceElement.getAttribute("use-schemas"));
            // anything but false is true
            this.checkOnStart = !"false".equals(datasourceElement.getAttribute("check-on-start"));
            // anything but false is true
            this.checkPrimaryKeysOnStart = !"false".equals(datasourceElement.getAttribute("check-pks-on-start"));
            // anything but true is false
            this.addMissingOnStart = "true".equals(datasourceElement.getAttribute("add-missing-on-start"));
            // anything but false is true
            this.useFks = !"false".equals(datasourceElement.getAttribute("use-foreign-keys"));
            // anything but false is true
            this.useFkIndices = !"false".equals(datasourceElement.getAttribute("use-foreign-key-indices"));
            // anything but true is false
            this.checkForeignKeysOnStart = "true".equals(datasourceElement.getAttribute("check-fks-on-start"));
            // anything but true is false
            this.checkFkIndicesOnStart = "true".equals(datasourceElement.getAttribute("check-fk-indices-on-start"));
            // anything but false is true
            this.usePkConstraintNames = !"false".equals(datasourceElement.getAttribute("use-pk-constraint-names"));
            try {
                this.constraintNameClipLength = Integer.parseInt(datasourceElement.getAttribute("constraint-name-clip-length"));
            } catch (Exception e) {
                Debug.logError("Could not parse constraint-name-clip-length value for datasource with name " + this.name + ", using default value of 30", module);
            }
            this.useProxyCursor = "true".equalsIgnoreCase(datasourceElement.getAttribute("use-proxy-cursor"));
            this.cursorName = datasourceElement.getAttribute("proxy-cursor-name");
            try {
                this.resultFetchSize = Integer.parseInt(datasourceElement.getAttribute("result-fetch-size"));
            } catch (Exception e) {
                Debug.logWarning("Could not parse result-fetch-size value for datasource with name " + this.name + ", using JDBC driver default value", module);
            }
            this.fkStyle = datasourceElement.getAttribute("fk-style");
            // anything but true is false
            this.useFkInitiallyDeferred = "true".equals(datasourceElement.getAttribute("use-fk-initially-deferred"));
            // anything but false is true
            this.useIndices = !"false".equals(datasourceElement.getAttribute("use-indices"));
            // anything but true is false
            this.checkIndicesOnStart = "true".equals(datasourceElement.getAttribute("check-indices-on-start"));
            this.joinStyle = datasourceElement.getAttribute("join-style");
            this.aliasViews = !"false".equals(datasourceElement.getAttribute("alias-view-columns"));
            // anything but true is false
            this.alwaysUseConstraintKeyword = "true".equals(datasourceElement.getAttribute("always-use-constraint-keyword"));
            this.dropFkUseForeignKeyKeyword = "true".equals(datasourceElement.getAttribute("drop-fk-use-foreign-key-keyword"));
            this.useBinaryTypeForBlob = "true".equals(datasourceElement.getAttribute("use-binary-type-for-blob"));
            
            this.tableType = datasourceElement.getAttribute("table-type");
            this.characterSet = datasourceElement.getAttribute("character-set");
            this.collate = datasourceElement.getAttribute("collate");
        }
        if (this.fkStyle == null || this.fkStyle.length() == 0) this.fkStyle = "name_constraint";
        if (this.joinStyle == null || this.joinStyle.length() == 0) this.joinStyle = "ansi";

        this.jndiJdbcElement = UtilXml.firstChildElement(datasourceElement, "jndi-jdbc");
        this.tyrexDataSourceElement = UtilXml.firstChildElement(datasourceElement, "tyrex-dataSource");
        this.inlineJdbcElement = UtilXml.firstChildElement(datasourceElement, "inline-jdbc");
    }
}
