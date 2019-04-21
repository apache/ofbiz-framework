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
package org.apache.ofbiz.entity.jdbc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.ofbiz.base.concurrent.ExecutionPool;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilTimer;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.config.model.EntityConfig;
import org.apache.ofbiz.entity.datasource.GenericHelperInfo;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;
import org.apache.ofbiz.entity.model.ModelIndex;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelRelation;
import org.apache.ofbiz.entity.model.ModelViewEntity;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;
import org.apache.ofbiz.entity.transaction.TransactionUtil;

/**
 * Utilities for Entity Database Maintenance
 *
 */
public class DatabaseUtil {

    public static final String module = DatabaseUtil.class.getName();

    // OFBiz Connections
    protected ModelFieldTypeReader modelFieldTypeReader = null;
    protected Datasource datasourceInfo = null;
    protected GenericHelperInfo helperInfo = null;

    // Legacy Connections
    protected String connectionUrl = null;
    protected String driverName = null;
    protected String userName = null;
    protected String password = null;

    boolean isLegacy = false;

    // OFBiz DatabaseUtil
    public DatabaseUtil(GenericHelperInfo helperInfo) {
        this.helperInfo = helperInfo;
        this.modelFieldTypeReader = ModelFieldTypeReader.getModelFieldTypeReader(helperInfo.getHelperBaseName());
        this.datasourceInfo = EntityConfig.getDatasource(helperInfo.getHelperBaseName());
    }

    protected Connection getConnection() throws SQLException, GenericEntityException {
        Connection connection = null;
        if (!isLegacy) {
            connection = TransactionFactoryLoader.getInstance().getConnection(helperInfo);
        } else {
            connection = getConnection(driverName, connectionUrl, null, userName, password);
        }

        if (connection == null) {
            if (!isLegacy) {
                throw new GenericEntityException("No connection available for helper named [" + helperInfo.getHelperFullName() + "]");
            } else {
                throw new GenericEntityException("No connection avaialble for URL [" + connectionUrl + "]");
            }
        }
        if (!TransactionUtil.isTransactionInPlace()) {
            connection.setAutoCommit(true);
        }
        return connection;
    }

    private Connection getConnection(String driverName, String connectionUrl, Properties props, String userName, String password) throws SQLException {
        // first register the JDBC driver with the DriverManager
        if (driverName != null) {
            if (DriverManager.getDriver(driverName) == null) {
                try {
                    Driver driver = (Driver) Class.forName(driverName, true, Thread.currentThread().getContextClassLoader()).getDeclaredConstructor().newInstance();
                    DriverManager.registerDriver(driver);
                } catch (ReflectiveOperationException e) {
                    Debug.logWarning(e, "Unable to load driver [" + driverName + "]", module);
                }
            }
        }

        try {
            if (UtilValidate.isNotEmpty(userName))
                return DriverManager.getConnection(connectionUrl, userName, password);
            else if (props != null)
                return DriverManager.getConnection(connectionUrl, props);
            else
                return DriverManager.getConnection(connectionUrl);
        } catch (SQLException e) {
            Debug.logError(e, "SQL Error obtaining JDBC connection", module);
            throw e;
        }
    }

    protected Connection getConnectionLogged(Collection<String> messages) {
        try {
            return getConnection();
        } catch (SQLException | GenericEntityException e) {
            String message = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            return null;
        }
    }

    public Datasource getDatasource() {
        return this.datasourceInfo;
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public void checkDb(Map<String, ModelEntity> modelEntities, List<String> messages, boolean addMissing) {
        checkDb(modelEntities, null, messages, datasourceInfo.getCheckPksOnStart(), (datasourceInfo.getUseForeignKeys() && datasourceInfo.getCheckFksOnStart()), (datasourceInfo.getUseForeignKeyIndices() && datasourceInfo.getCheckFkIndicesOnStart()), addMissing);
    }

    public void checkDb(Map<String, ModelEntity> modelEntities, List<String> colWrongSize, List<String> messages, boolean checkPks, boolean checkFks, boolean checkFkIdx, boolean addMissing) {
        if (isLegacy) {
            throw new RuntimeException("Cannot run checkDb on a legacy database connection; configure a database helper (entityengine.xml)");
        }

        ExecutorService executor = Executors.newFixedThreadPool(datasourceInfo.getMaxWorkerPoolSize());

        UtilTimer timer = new UtilTimer();
        timer.timerString("Start - Before Get Database Meta Data");

        // get ALL tables from this database
        TreeSet<String> tableNames = this.getTableNames(messages);
        TreeSet<String> fkTableNames = tableNames == null ? null : new TreeSet<String>(tableNames);
        TreeSet<String> indexTableNames = tableNames == null ? null : new TreeSet<String>(tableNames);

        if (tableNames == null) {
            String message = "Could not get table name information from the database, aborting.";
            if (messages != null) messages.add(message);
            Debug.logError(message, module);
            return;
        }
        timer.timerString("After Get All Table Names");

        // get ALL column info, put into hashmap by table name
        Map<String, Map<String, ColumnCheckInfo>> colInfo = getColumnInfo(tableNames, checkPks, messages, executor);
        if (colInfo == null) {
            String message = "Could not get column information from the database, aborting.";
            if (messages != null) messages.add(message);
            Debug.logError(message, module);
            return;
        }
        timer.timerString("After Get All Column Info");

        // -make sure all entities have a corresponding table
        // -list all tables that do not have a corresponding entity
        // -display message if number of table columns does not match number of entity fields
        // -list all columns that do not have a corresponding field
        // -make sure each corresponding column is of the correct type
        // -list all fields that do not have a corresponding column

        timer.timerString("Before Individual Table/Column Check");

        List<ModelEntity> modelEntityList = new ArrayList<ModelEntity>(modelEntities.values());
        // sort using compareTo method on ModelEntity
        Collections.sort(modelEntityList);
        int curEnt = 0;
        int totalEnt = modelEntityList.size();
        List<ModelEntity> entitiesAdded = new LinkedList<ModelEntity>();
        String schemaName;
        try {
            schemaName = getSchemaName(messages);
        } catch (SQLException e) {
            String message = "Could not get schema name the database, aborting.";
            if (messages != null) messages.add(message);
            Debug.logError(message, module);
            return;
        }
        List<Future<CreateTableCallable>> tableFutures = new LinkedList<Future<CreateTableCallable>>();
        for (ModelEntity entity: modelEntityList) {
            curEnt++;

            // if this is a view entity, do not check it...
            if (entity instanceof ModelViewEntity) {
                String entMessage = "(" + timer.timeSinceLast() + "ms) NOT Checking #" + curEnt + "/" + totalEnt + " View Entity " + entity.getEntityName();
                Debug.logVerbose(entMessage, module);
                if (messages != null) messages.add(entMessage);
                continue;                
            // if never-check is set then don't check it either
            } else if (entity.getNeverCheck()) {
                String entMessage = "(" + timer.timeSinceLast() + "ms) NOT Checking #" + curEnt + "/" + totalEnt + " Entity " + entity.getEntityName();
                Debug.logVerbose(entMessage, module);
                if (messages != null) messages.add(entMessage);
                continue;
            }

            String plainTableName = entity.getPlainTableName();
            String tableName;
            if (UtilValidate.isNotEmpty(schemaName)) {
                tableName = schemaName + "." + plainTableName;
            } else {
                tableName = plainTableName;
            }
            String entMessage = "(" + timer.timeSinceLast() + "ms) Checking #" + curEnt + "/" + totalEnt +
                " Entity " + entity.getEntityName() + " with table " + tableName;

            Debug.logVerbose(entMessage, module);
            if (messages != null) messages.add(entMessage);

            // -make sure all entities have a corresponding table
            if (tableNames.contains(tableName)) {
                tableNames.remove(tableName);

                Map<String, ModelField> fieldColNames = new HashMap<String, ModelField>();
                Iterator<ModelField> fieldIter = entity.getFieldsIterator();
                while (fieldIter.hasNext()) {
                    ModelField field = fieldIter.next();
                    fieldColNames.put(field.getColName(), field);
                }

                Map<String, ColumnCheckInfo> colMap = colInfo.get(tableName);
                if (colMap != null) {
                    for (ColumnCheckInfo ccInfo: colMap.values()) {
                        // -list all columns that do not have a corresponding field
                        if (fieldColNames.containsKey(ccInfo.columnName)) {
                            ModelField field = null;

                            field = fieldColNames.remove(ccInfo.columnName);
                            ModelFieldType modelFieldType = modelFieldTypeReader.getModelFieldType(field.getType());

                            if (modelFieldType != null) {
                                // make sure each corresponding column is of the correct type
                                String fullTypeStr = modelFieldType.getSqlType();
                                String typeName;
                                int columnSize = -1;
                                int decimalDigits = -1;

                                int openParen = fullTypeStr.indexOf('(');
                                int closeParen = fullTypeStr.indexOf(')');
                                int comma = fullTypeStr.indexOf(',');

                                if (openParen > 0 && closeParen > 0 && closeParen > openParen) {
                                    typeName = fullTypeStr.substring(0, openParen);
                                    if (!("DATETIME".equals(typeName) || "TIME".equals(typeName))) { // for DATETIME and TIME fields the number within the parenthesis doesn't represent the column size
                                        if (comma > 0 && comma > openParen && comma < closeParen) {
                                            String csStr = fullTypeStr.substring(openParen + 1, comma);
                                            try {
                                                columnSize = Integer.parseInt(csStr);
                                            } catch (NumberFormatException e) {
                                                Debug.logError(e, module);
                                            }

                                            String ddStr = fullTypeStr.substring(comma + 1, closeParen);
                                            try {
                                                decimalDigits = Integer.parseInt(ddStr);
                                            } catch (NumberFormatException e) {
                                                Debug.logError(e, module);
                                            }
                                        } else if (openParen + 1 < closeParen) {
                                            String csStr = fullTypeStr.substring(openParen + 1, closeParen);
                                            try {
                                                columnSize = Integer.parseInt(csStr);
                                            } catch (NumberFormatException e) {
                                                Debug.logError(e, module);
                                            }
                                        }
                                    }
                                } else {
                                    typeName = fullTypeStr;
                                }

                                // override the default typeName with the sqlTypeAlias if it is specified
                                if (UtilValidate.isNotEmpty(modelFieldType.getSqlTypeAlias())) {
                                    typeName = modelFieldType.getSqlTypeAlias();
                                }

                                // NOTE: this may need a toUpperCase in some cases, keep an eye on it, okay just compare with ignore case
                                if (!ccInfo.typeName.equalsIgnoreCase(typeName)) {
                                    String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" +
                                            entity.getEntityName() + "] is of type [" + ccInfo.typeName + "] in the database, but is defined as type [" +
                                            typeName + "] in the entity definition.";
                                    Debug.logError(message, module);
                                    if (messages != null) messages.add(message);
                                }
                                if (columnSize != -1 && ccInfo.columnSize != -1 && columnSize != ccInfo.columnSize && (columnSize * 3) != ccInfo.columnSize) {
                                    String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" +
                                            entity.getEntityName() + "] has a column size of [" + ccInfo.columnSize +
                                            "] in the database, but is defined to have a column size of [" + columnSize + "] in the entity definition.";
                                    Debug.logWarning(message, module);
                                    if (messages != null) messages.add(message);
                                    if (columnSize > ccInfo.columnSize && colWrongSize != null) {
                                        // add item to list of wrong sized columns; only if the entity is larger
                                        colWrongSize.add(entity.getEntityName() + "." + field.getName());
                                    }
                                }
                                if (decimalDigits != -1 && decimalDigits != ccInfo.decimalDigits) {
                                    String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" +
                                            entity.getEntityName() + "] has a decimalDigits of [" + ccInfo.decimalDigits +
                                            "] in the database, but is defined to have a decimalDigits of [" + decimalDigits + "] in the entity definition.";
                                    Debug.logWarning(message, module);
                                    if (messages != null) messages.add(message);
                                }

                                // do primary key matching check
                                if (checkPks && ccInfo.isPk && !field.getIsPk()) {
                                    String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" +
                                            entity.getEntityName() + "] IS a primary key in the database, but IS NOT a primary key in the entity definition. The primary key for this table needs to be re-created or modified so that this column is NOT part of the primary key.";
                                    Debug.logError(message, module);
                                    if (messages != null) messages.add(message);
                                }
                                if (checkPks && !ccInfo.isPk && field.getIsPk()) {
                                    String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" +
                                            entity.getEntityName() + "] IS NOT a primary key in the database, but IS a primary key in the entity definition. The primary key for this table needs to be re-created or modified to add this column to the primary key. Note that data may need to be added first as a primary key column cannot have an null values.";
                                    Debug.logError(message, module);
                                    if (messages != null) messages.add(message);
                                }
                            } else {
                                String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" + entity.getEntityName() +
                                        "] has a field type name of [" + field.getType() + "] which is not found in the field type definitions";
                                Debug.logError(message, module);
                                if (messages != null) messages.add(message);
                            }
                        } else {
                            String message = "Column [" + ccInfo.columnName + "] of table [" + tableName + "] of entity [" + entity.getEntityName() + "] exists in the database but has no corresponding field" + ((checkPks && ccInfo.isPk) ? " (and it is a PRIMARY KEY COLUMN)" : "");
                            Debug.logWarning(message, module);
                            if (messages != null) messages.add(message);
                        }
                    }

                    // -display message if number of table columns does not match number of entity fields
                    if (colMap.size() != entity.getFieldsSize()) {
                        String message = "Entity [" + entity.getEntityName() + "] has " + entity.getFieldsSize() + " fields but table [" + tableName + "] has " + colMap.size() + " columns.";
                        Debug.logWarning(message, module);
                        if (messages != null) messages.add(message);
                    }
                }

                // -list all fields that do not have a corresponding column
                for (ModelField field : fieldColNames.values()) {
                    String message = "Field [" + field.getName() + "] of entity [" + entity.getEntityName() + "] is missing its corresponding column [" + field.getColName() + "]" + (field.getIsPk() ? " (and it is a PRIMARY KEY FIELD)" : "");

                    Debug.logWarning(message, module);
                    if (messages != null) messages.add(message);

                    if (addMissing) {
                        // add the column
                        String errMsg = addColumn(entity, field);

                        if (UtilValidate.isNotEmpty(errMsg)) {
                            message = "Could not add column [" + field.getColName() + "] to table [" + tableName + "]: " + errMsg;
                            Debug.logError(message, module);
                            if (messages != null) messages.add(message);
                        } else {
                            message = "Added column [" + field.getColName() + "] to table [" + tableName + "]" + (field.getIsPk() ? " (NOTE: this is a PRIMARY KEY FIELD, but the primary key was not updated automatically (not considered a safe operation), be sure to fill in any needed data and re-create the primary key)" : "");
                            Debug.logImportant(message, module);
                            if (messages != null) messages.add(message);
                        }
                    }
                }
            } else {
                String message = "Entity [" + entity.getEntityName() + "] has no table in the database";
                Debug.logWarning(message, module);
                if (messages != null) messages.add(message);

                if (addMissing) {
                    // create the table
                    tableFutures.add(executor.submit(new CreateTableCallable(entity, modelEntities, tableName)));
                }
            }
        }
        for (CreateTableCallable tableCallable: ExecutionPool.getAllFutures(tableFutures)) {
            tableCallable.updateData(messages, entitiesAdded);
        }

        timer.timerString("After Individual Table/Column Check");

        // -list all tables that do not have a corresponding entity
        for (String tableName: tableNames) {
            String message = "Table named [" + tableName + "] exists in the database but has no corresponding entity";
            Debug.logWarning(message, module);
            if (messages != null) messages.add(message);
        }

        // for each newly added table, add fk indices
        if (datasourceInfo.getUseForeignKeyIndices()) {
            int totalFkIndices = 0;
            List<Future<AbstractCountingCallable>> fkIndicesFutures = new LinkedList<Future<AbstractCountingCallable>>();
            for (ModelEntity curEntity: entitiesAdded) {
                if (curEntity.getRelationsOneSize() > 0) {
                    fkIndicesFutures.add(executor.submit(new AbstractCountingCallable(curEntity, modelEntities) {
                        public AbstractCountingCallable call() throws Exception {
                            count = createForeignKeyIndices(entity, datasourceInfo.getConstraintNameClipLength(), messages);
                            return this;
                        }
                    }));
                }
            }
            for (AbstractCountingCallable fkIndicesCallable: ExecutionPool.getAllFutures(fkIndicesFutures)) {
                totalFkIndices += fkIndicesCallable.updateData(messages);
            }
            if (totalFkIndices > 0) Debug.logImportant("==== TOTAL Foreign Key Indices Created: " + totalFkIndices, module);
        }

        // for each newly added table, add fks
        if (datasourceInfo.getUseForeignKeys()) {
            int totalFks = 0;
            for (ModelEntity curEntity: entitiesAdded) {
                totalFks += this.createForeignKeys(curEntity, modelEntities, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.getUseFkInitiallyDeferred(), messages);
            }
            if (totalFks > 0) Debug.logImportant("==== TOTAL Foreign Keys Created: " + totalFks, module);
        }

        // for each newly added table, add declared indexes
        if (datasourceInfo.getUseIndices()) {
            int totalDis = 0;
            List<Future<AbstractCountingCallable>> disFutures = new LinkedList<Future<AbstractCountingCallable>>();
            for (ModelEntity curEntity: entitiesAdded) {
                if (curEntity.getIndexesSize() > 0) {
                    disFutures.add(executor.submit(new AbstractCountingCallable(curEntity,  modelEntities) {
                    public AbstractCountingCallable call() throws Exception {
                        count = createDeclaredIndices(entity, messages);
                        return this;
                    }
                }));

                }
            }
            for (AbstractCountingCallable disCallable: ExecutionPool.getAllFutures(disFutures)) {
                totalDis += disCallable.updateData(messages);
            }
            if (totalDis > 0) Debug.logImportant("==== TOTAL Declared Indices Created: " + totalDis, module);
        }

        // make sure each one-relation has an FK
        if (checkFks) {
        //if (!justColumns && datasourceInfo.getUseForeignKeys() && datasourceInfo.checkForeignKeysOnStart) {
            // NOTE: This ISN'T working for Postgres or MySQL, who knows about others, may be from JDBC driver bugs...
            int numFksCreated = 0;
            // TODO: check each key-map to make sure it exists in the FK, if any differences warn and then remove FK and recreate it

            // get ALL column info, put into hashmap by table name
            Map<String, Map<String, ReferenceCheckInfo>> refTableInfoMap = this.getReferenceInfo(fkTableNames, messages);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap, module);

            if (refTableInfoMap == null) {
                // uh oh, something happened while getting info...
                if (Debug.verboseOn()) Debug.logVerbose("Ref Table Info Map is null", module);
            } else {
                for (ModelEntity entity: modelEntityList) {
                    String entityName = entity.getEntityName();
                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {
                        String entMessage = "NOT Checking View Entity " + entity.getEntityName();
                        Debug.logVerbose(entMessage, module);
                        if (messages != null) {
                            messages.add(entMessage);
                        }
                        continue;
                    }

                    // get existing FK map for this table
                    Map<String, ReferenceCheckInfo> rcInfoMap = refTableInfoMap.get(entity.getTableName(datasourceInfo));
                    // Debug.logVerbose("Got ref info for table " + entity.getTableName(datasourceInfo) + ": " + rcInfoMap, module);

                    // go through each relation to see if an FK already exists
                    Iterator<ModelRelation> relations = entity.getRelationsIterator();
                    boolean createdConstraints = false;
                    while (relations.hasNext()) {
                        ModelRelation modelRelation = relations.next();
                        if (!"one".equals(modelRelation.getType())) {
                            continue;
                        }

                        ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());
                        if (relModelEntity == null) {
                            Debug.logError("No such relation: " + entity.getEntityName() + " -> " + modelRelation.getRelEntityName(), module);
                            continue;
                        }
                        String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());
                        ReferenceCheckInfo rcInfo = null;

                        if (rcInfoMap != null) {
                            rcInfo = rcInfoMap.get(relConstraintName);
                        }

                        if (rcInfo != null) {
                            rcInfoMap.remove(relConstraintName);
                        } else {
                            // if not, create one
                            String noFkMessage = "No Foreign Key Constraint [" + relConstraintName + "] found for entity [" + entityName + "]";
                            if (messages != null) messages.add(noFkMessage);
                            if (Debug.infoOn()) Debug.logInfo(noFkMessage, module);

                            if (addMissing) {
                                String errMsg = createForeignKey(entity, modelRelation, relModelEntity, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.getUseFkInitiallyDeferred());
                                if (UtilValidate.isNotEmpty(errMsg)) {
                                    String message = "Could not create foreign key " + relConstraintName + " for entity [" + entity.getEntityName() + "]: " + errMsg;
                                    Debug.logError(message, module);
                                    if (messages != null) messages.add(message);
                                } else {
                                    String message = "Created foreign key " + relConstraintName + " for entity [" + entity.getEntityName() + "]";
                                    Debug.logVerbose(message, module);
                                    if (messages != null) messages.add(message);
                                    createdConstraints = true;
                                    numFksCreated++;
                                }
                            }
                        }
                    }
                    if (createdConstraints) {
                        String message = "Created foreign key(s) for entity [" + entity.getEntityName() + "]";
                        Debug.logImportant(message, module);
                        if (messages != null) messages.add(message);
                    }

                    // show foreign key references that exist but are unknown
                    if (rcInfoMap != null) {
                        for (String rcKeyLeft: rcInfoMap.keySet()) {
                            String message = "Unknown Foreign Key Constraint " + rcKeyLeft + " found in table " + entity.getTableName(datasourceInfo);
                            Debug.logImportant(message, module);
                            if (messages != null) messages.add(message);
                        }
                    }
                }
            }
            if (Debug.infoOn()) Debug.logInfo("Created " + numFksCreated + " fk refs", module);
        }

        // make sure each one-relation has an index
        if (checkFkIdx || datasourceInfo.getCheckIndicesOnStart()) {
        //if (!justColumns && datasourceInfo.getUseForeignKeyIndices() && datasourceInfo.checkFkIndicesOnStart) {
            int numIndicesCreated = 0;
            // TODO: check each key-map to make sure it exists in the index, if any differences warn and then remove the index and recreate it

            // get ALL column info, put into hashmap by table name
            boolean needsUpperCase[] = new boolean[1];
            Map<String, Set<String>> tableIndexListMap = this.getIndexInfo(indexTableNames, messages, needsUpperCase);

            // Debug.logVerbose("Ref Info Map: " + refTableInfoMap, module);

            if (tableIndexListMap == null) {
                // uh oh, something happened while getting info...
                if (Debug.verboseOn()) Debug.logVerbose("Ref Table Info Map is null", module);
            } else {
                for (ModelEntity entity: modelEntityList) {
                    String entityName = entity.getEntityName();
                    // if this is a view entity, do not check it...
                    if (entity instanceof ModelViewEntity) {
                        String entMessage = "NOT Checking View Entity " + entity.getEntityName();
                        Debug.logVerbose(entMessage, module);
                        if (messages != null) messages.add(entMessage);
                        continue;
                    }

                    // get existing index list for this table
                    Set<String> tableIndexList = tableIndexListMap.get(entity.getTableName(datasourceInfo));

                    // Debug.logVerbose("Got ind info for table " + entity.getTableName(datasourceInfo) + ": " + tableIndexList, module);

                    if (tableIndexList == null) {
                        // evidently no indexes in the database for this table, do the create all
                        if (checkFkIdx) {
                            this.createForeignKeyIndices(entity, datasourceInfo.getConstraintNameClipLength(), messages);
                        }
                        if (datasourceInfo.getCheckIndicesOnStart()) {
                            this.createDeclaredIndices(entity, messages);
                        }
                        continue;
                    }
                    // go through each relation to see if an FK already exists
                    boolean createdConstraints = false;
                    Iterator<ModelRelation> relations = entity.getRelationsIterator();
                    while (relations.hasNext()) {
                        ModelRelation modelRelation = relations.next();
                        if (!"one".equals(modelRelation.getType())) {
                            continue;
                        }

                        String relConstraintName = makeFkConstraintName(modelRelation, datasourceInfo.getConstraintNameClipLength());
                        if (tableIndexList.contains(relConstraintName)) {
                            tableIndexList.remove(relConstraintName);
                        } else {
                            if (checkFkIdx) {
                                // if not, create one
                                String noIdxMessage = "No Index [" + relConstraintName + "] found for entity [" + entityName + "]";
                                if (messages != null) messages.add(noIdxMessage);
                                if (Debug.infoOn()) Debug.logInfo(noIdxMessage, module);

                                if (addMissing) {
                                    String errMsg = createForeignKeyIndex(entity, modelRelation, datasourceInfo.getConstraintNameClipLength());
                                    if (UtilValidate.isNotEmpty(errMsg)) {
                                        String message = "Could not create foreign key index " + relConstraintName + " for entity [" + entity.getEntityName() + "]: " + errMsg;
                                        Debug.logError(message, module);
                                        if (messages != null) messages.add(message);
                                    } else {
                                        String message = "Created foreign key index " + relConstraintName + " for entity [" + entity.getEntityName() + "]";
                                        Debug.logVerbose(message, module);
                                        if (messages != null) messages.add(message);
                                        createdConstraints = true;
                                        numIndicesCreated++;
                                    }
                                }
                            }
                        }
                    }

                    if (createdConstraints) {
                        String message = "Created foreign key index/indices for entity [" + entity.getEntityName() + "]";
                        Debug.logImportant(message, module);
                        if (messages != null) messages.add(message);
                    }
                    // go through each indice to see if an indice already exists
                    boolean createdIndexes = false;
                    Iterator<ModelIndex> indexes = entity.getIndexesIterator();
                    while (indexes.hasNext()) {
                        ModelIndex modelIndex = indexes.next();

                        String relIndexName = makeIndexName(modelIndex, datasourceInfo.getConstraintNameClipLength());
                        String checkIndexName = needsUpperCase[0] ? relIndexName.toUpperCase() : relIndexName;
                        if (tableIndexList.contains(checkIndexName)) {
                            tableIndexList.remove(checkIndexName);
                        } else {
                            if (datasourceInfo.getCheckIndicesOnStart()) {
                                // if not, create one
                                String noIdxMessage = "No Index [" + relIndexName + "] found for entity [" + entityName + "]";
                                if (messages != null) messages.add(noIdxMessage);
                                if (Debug.infoOn()) Debug.logInfo(noIdxMessage, module);

                                if (addMissing) {
                                    String errMsg = createDeclaredIndex(entity, modelIndex);
                                    if (UtilValidate.isNotEmpty(errMsg)) {
                                        String message = "Could not create index " + relIndexName + " for entity [" + entity.getEntityName() + "]: " + errMsg;
                                        Debug.logError(message, module);
                                        if (messages != null) messages.add(message);
                                    } else {
                                        String message = "Created index " + relIndexName + " for entity [" + entity.getEntityName() + "]";
                                        Debug.logVerbose(message, module);
                                        if (messages != null) messages.add(message);
                                        createdIndexes = true;
                                        numIndicesCreated++;
                                    }
                                }
                            }
                        }
                    }
                    if (createdIndexes) {
                        String message = "Created declared index/indices for entity [" + entity.getEntityName() + "]";
                        Debug.logImportant(message, module);
                        if (messages != null) messages.add(message);
                    }

                    // show index key references that exist but are unknown
                    for (String indexLeft: tableIndexList) {
                        String message = "Unknown Index " + indexLeft + " found in table " + entity.getTableName(datasourceInfo);
                        Debug.logImportant(message, module);
                        if (messages != null) messages.add(message);
                    }
                }
            }
            if (numIndicesCreated > 0 && Debug.infoOn()) Debug.logInfo("Created " + numIndicesCreated + " indices", module);

        }

        executor.shutdown();
        timer.timerString("Finished Checking Entity Database");
    }

    /** Creates a list of ModelEntity objects based on meta data from the database */
    public List<ModelEntity> induceModelFromDb(Collection<String> messages) {
        ExecutorService executor = Executors.newFixedThreadPool(datasourceInfo.getMaxWorkerPoolSize());

        // get ALL tables from this database
        TreeSet<String> tableNames = this.getTableNames(messages);

        // get ALL column info, put into hashmap by table name
        Map<String, Map<String, ColumnCheckInfo>> colInfo = getColumnInfo(tableNames, true, messages, executor);

        // go through each table and make a ModelEntity object, add to list
        // for each entity make corresponding ModelField objects
        // then print out XML for the entities/fields
        List<ModelEntity> newEntList = new LinkedList<ModelEntity>();

        boolean isCaseSensitive = getIsCaseSensitive(messages);

        // iterate over the table names is alphabetical order
        for (String tableName: new TreeSet<String>(colInfo.keySet())) {
            Map<String, ColumnCheckInfo> colMap = colInfo.get(tableName);
            ModelEntity newEntity = new ModelEntity(tableName, colMap, modelFieldTypeReader, isCaseSensitive);
            newEntList.add(newEntity);
        }

        executor.shutdown();
        return newEntList;
    }

    private String getSchemaName(Collection<String> messages) throws SQLException {
        String schemaName;
        try (Connection connection = getConnectionLogged(messages)) {
            DatabaseMetaData dbData = this.getDatabaseMetaData(connection, messages);
            schemaName = getSchemaName(dbData);
            return schemaName;
        }
    }

    private boolean getIsCaseSensitive(Collection<String> messages) {
        boolean isCaseSensitive = false;
        try (Connection connection = getConnectionLogged(messages)) {
            DatabaseMetaData dbData = this.getDatabaseMetaData(connection, messages);
            if (dbData != null) {
                isCaseSensitive = dbData.supportsMixedCaseIdentifiers();
            }
        } catch (SQLException e) {
            Debug.logError(e, "Error getting db meta data about case sensitive", module);
        }
        return isCaseSensitive;
    }

    public DatabaseMetaData getDatabaseMetaData(Connection connection, Collection<String> messages) {
        DatabaseMetaData dbData = null;
        try {
            dbData = connection.getMetaData();
        } catch (SQLException e) {
            String message = "Unable to get database meta data... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) {
                messages.add(message);
            }
            return null;
        }

        if (dbData == null) {
            Debug.logWarning("Unable to get database meta data; method returned null", module);
        }

        return dbData;
    }

    private static final List<Detection> detections = new ArrayList<Detection>();
    private static final String goodFormatStr;
    private static final String badFormatStr;

    private static class Detection {
        protected final String name;
        protected final boolean required;
        protected final Method method;
        protected final Object[] params;

        protected Detection(String name, boolean required, String methodName, Object... params) throws NoSuchMethodException {
            this.name = name;
            this.required = required;
            Class<?>[] paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                Class<?> paramType = params[i].getClass();
                if (paramType == Integer.class) {
                    paramType = Integer.TYPE;
                }
                paramTypes[i] = paramType;
            }
            method = DatabaseMetaData.class.getMethod(methodName, paramTypes);
            this.params = params;
        }
    }

    static {
        try {
            detections.add(new Detection("supports transactions", true, "supportsTransactions"));
            detections.add(new Detection("isolation None", false, "supportsTransactionIsolationLevel", Connection.TRANSACTION_NONE));
            detections.add(new Detection("isolation ReadCommitted", false, "supportsTransactionIsolationLevel", Connection.TRANSACTION_READ_COMMITTED));
            detections.add(new Detection("isolation ReadUncommitted", false, "supportsTransactionIsolationLevel", Connection.TRANSACTION_READ_UNCOMMITTED));
            detections.add(new Detection("isolation RepeatableRead", false, "supportsTransactionIsolationLevel", Connection.TRANSACTION_REPEATABLE_READ));
            detections.add(new Detection("isolation Serializable", false, "supportsTransactionIsolationLevel", Connection.TRANSACTION_SERIALIZABLE));
            detections.add(new Detection("forward only type", false, "supportsResultSetType", ResultSet.TYPE_FORWARD_ONLY));
            detections.add(new Detection("scroll sensitive type", false, "supportsResultSetType", ResultSet.TYPE_SCROLL_SENSITIVE));
            detections.add(new Detection("scroll insensitive type", false, "supportsResultSetType", ResultSet.TYPE_SCROLL_INSENSITIVE));
            detections.add(new Detection("is case sensitive", false, "supportsMixedCaseIdentifiers"));
            detections.add(new Detection("stores LowerCase", false, "storesLowerCaseIdentifiers"));
            detections.add(new Detection("stores MixedCase", false, "storesMixedCaseIdentifiers"));
            detections.add(new Detection("stores UpperCase", false, "storesUpperCaseIdentifiers"));
            detections.add(new Detection("max table name length", false, "getMaxTableNameLength"));
            detections.add(new Detection("max column name length", false, "getMaxColumnNameLength"));
            detections.add(new Detection("concurrent connections", false, "getMaxConnections"));
            detections.add(new Detection("concurrent statements", false, "getMaxStatements"));
            detections.add(new Detection("ANSI SQL92 Entry", false, "supportsANSI92EntryLevelSQL"));
            detections.add(new Detection("ANSI SQL92 Intermediate", false, "supportsANSI92IntermediateSQL"));
            detections.add(new Detection("ANSI SQL92 Full", false, "supportsANSI92FullSQL"));
            detections.add(new Detection("ODBC SQL Grammar Core", false, "supportsCoreSQLGrammar"));
            detections.add(new Detection("ODBC SQL Grammar Extended", false, "supportsExtendedSQLGrammar"));
            detections.add(new Detection("ODBC SQL Grammar Minimum", false, "supportsMinimumSQLGrammar"));
            detections.add(new Detection("outer joins", true, "supportsOuterJoins"));
            detections.add(new Detection("limited outer joins", false, "supportsLimitedOuterJoins"));
            detections.add(new Detection("full outer joins", false, "supportsFullOuterJoins"));
            detections.add(new Detection("group by", true, "supportsGroupBy"));
            detections.add(new Detection("group by not in select", false, "supportsGroupByUnrelated"));
            detections.add(new Detection("column aliasing", false, "supportsColumnAliasing"));
            detections.add(new Detection("order by not in select", false, "supportsOrderByUnrelated"));
            detections.add(new Detection("alter table add column", true, "supportsAlterTableWithAddColumn"));
            detections.add(new Detection("non-nullable column", true, "supportsNonNullableColumns"));
            //detections.add(new Detection("", false, "", ));
        } catch (NoSuchMethodException e) {
            throw (InternalError) new InternalError(e.getMessage()).initCause(e);
        }
        int maxWidth = 0;
        for (Detection detection: detections) {
            if (detection.name.length() > maxWidth) {
                maxWidth = detection.name.length();
            }
        }
        goodFormatStr = "- %-" + maxWidth + "s [%s]%s";
        badFormatStr = "- %-" + maxWidth + "s [ DETECTION FAILED ]%s";
    }

    public void printDbMiscData(DatabaseMetaData dbData, Connection con) {
        if (dbData == null) {
            return;
        }
        if (Debug.infoOn()) {
            // Database Info
            try {
                Debug.logInfo("Database Product Name is " + dbData.getDatabaseProductName(), module);
                Debug.logInfo("Database Product Version is " + dbData.getDatabaseProductVersion(), module);
            } catch (SQLException e) {
                Debug.logWarning("Unable to get Database name & version information", module);
            }
            // JDBC Driver Info
            try {
                Debug.logInfo("Database Driver Name is " + dbData.getDriverName(), module);
                Debug.logInfo("Database Driver Version is " + dbData.getDriverVersion(), module);
                Debug.logInfo("Database Driver JDBC Version is " + dbData.getJDBCMajorVersion() + "." + dbData.getJDBCMinorVersion(), module);
            } catch (SQLException e) {
                Debug.logWarning("Unable to get Driver name & version information", module);
            } catch (AbstractMethodError ame) {
                Debug.logWarning("Unable to get Driver JDBC Version", module);
            }
            // Db/Driver support settings
            Debug.logInfo("Database Setting/Support Information (those with a * should be true):", module);
            for (Detection detection: detections) {
                String requiredFlag = detection.required ? "*" : "";
                try {
                    Object result = detection.method.invoke(dbData, detection.params);
                    Debug.logInfo(String.format(goodFormatStr, detection.name, result, requiredFlag), module);
                } catch (Exception e) {
                    Debug.logVerbose(e, module);
                    Debug.logWarning(String.format(badFormatStr, detection.name, requiredFlag), module);
                }
            }
            try {
                Debug.logInfo("- default fetchsize        [" + con.createStatement().getFetchSize() + "]", module);
            } catch (Exception e) {
                Debug.logVerbose(e, module);
                Debug.logWarning("- default fetchsize        [ DETECTION FAILED ]", module);
            }
            try {
                //this doesn't work in HSQLDB, other databases?
                //crashed (vm-death) with MS SQL Server 2000, runs properly with MS SQL Server 2005
                //Debug.logInfo("- named parameters         [" + dbData.supportsNamedParameters() + "]", module);
                Debug.logInfo("- named parameters         [ SKIPPED ]", module);
            } catch (Exception e) {
                Debug.logVerbose(e, module);
                Debug.logWarning("- named parameters JDBC-3  [ DETECTION FAILED ]", module);
            }
        }
    }

    public TreeSet<String> getTableNames(Collection<String> messages) {
        Connection connection = getConnectionLogged(messages);

        if (connection == null) {
            String message = "Unable to establish a connection with the database, no additional information available.";
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            return null;
        }

        DatabaseMetaData dbData = this.getDatabaseMetaData(connection, messages);
        if (dbData == null) {
            return null;
        }

        printDbMiscData(dbData, connection);
        if (Debug.infoOn()) Debug.logInfo("Getting Table Info From Database", module);

        // get ALL tables from this database
        TreeSet<String> tableNames = new TreeSet<String>();
        ResultSet tableSet = null;

        String lookupSchemaName = null;
        try {
            String[] types = {"TABLE", "BASE TABLE", "VIEW", "ALIAS", "SYNONYM"};
            lookupSchemaName = getSchemaName(dbData);
            tableSet = dbData.getTables(null, lookupSchemaName, null, types);
            if (tableSet == null) {
                Debug.logWarning("getTables returned null set", module);
            }
        } catch (SQLException e) {
            String message = "Unable to get list of table information, let's try the create anyway... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);

            try {
                connection.close();
            } catch (SQLException e2) {
                String message2 = "Unable to close database connection, continuing anyway... Error was:" + e2.toString();
                Debug.logError(message2, module);
                if (messages != null) messages.add(message2);
            }
            // we are returning an empty set here because databases like SapDB throw an exception when there are no tables in the database
            return tableNames;
        }

        try {
            boolean needsUpperCase = false;
            try {
                needsUpperCase = dbData.storesLowerCaseIdentifiers() || dbData.storesMixedCaseIdentifiers();
            } catch (SQLException e) {
                String message = "Error getting identifier case information... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }
            while (tableSet.next()) {
                try {
                    String tableName = tableSet.getString("TABLE_NAME");
                    // for those databases which do not return the schema name with the table name (pgsql 7.3)
                    boolean appendSchemaName = false;
                    if (tableName != null && lookupSchemaName != null && !tableName.startsWith(lookupSchemaName + "\\.")) {
                        appendSchemaName = true;
                    }
                    if (needsUpperCase && tableName != null) {
                        tableName = tableName.toUpperCase();
                    }
                    if (appendSchemaName) {
                        tableName = lookupSchemaName + "." + tableName;
                    }

                    // NOTE: this may need a toUpperCase in some cases, keep an eye on it, okay for now just do a compare with equalsIgnoreCase
                    String tableType = tableSet.getString("TABLE_TYPE");
                    // only allow certain table types
                    if (tableType != null && !"TABLE".equalsIgnoreCase(tableType) && !"VIEW".equalsIgnoreCase(tableType)
                            && !"ALIAS".equalsIgnoreCase(tableType) && !"SYNONYM".equalsIgnoreCase(tableType) && !"BASE TABLE".equalsIgnoreCase(tableType)) {
                        continue;
                    }

                    // String remarks = tableSet.getString("REMARKS");
                    tableNames.add(tableName);
                    // if (Debug.infoOn()) Debug.logInfo("Found table named [" + tableName + "] of type [" + tableType + "] with remarks: " + remarks, module);
                } catch (SQLException e) {
                    String message = "Error getting table information... Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                    continue;
                }
            }
        } catch (SQLException e) {
            String message = "Error getting next table information... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
        } finally {
            try {
                tableSet.close();
            } catch (SQLException e) {
                String message = "Unable to close ResultSet for table list, continuing anyway... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }

            try {
                connection.close();
            } catch (SQLException e) {
                String message = "Unable to close database connection, continuing anyway... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }
        }
        return tableNames;
    }

    private AbstractCountingCallable createPrimaryKeyFetcher(final DatabaseMetaData dbData, final String lookupSchemaName, final boolean needsUpperCase, final Map<String, Map<String, ColumnCheckInfo>> colInfo, final Collection<String> messages, final String curTable) {
        return new AbstractCountingCallable(null, null) {
            public AbstractCountingCallable call() throws Exception {
                if (Debug.verboseOn()) Debug.logVerbose("Fetching primary keys for " + curTable, module);
                ResultSet rsPks = dbData.getPrimaryKeys(null, lookupSchemaName, curTable);
                count = checkPrimaryKeyInfo(rsPks, lookupSchemaName, needsUpperCase, colInfo, messages);
                return this;
            }
        };
    }

    private Map<String, Map<String, ColumnCheckInfo>> getColumnInfo(Set<String> tableNames, boolean getPks, Collection<String> messages, ExecutorService executor) {
        // if there are no tableNames, don't even try to get the columns
        if (tableNames.size() == 0) {
            return new HashMap<String, Map<String, ColumnCheckInfo>>();
        }

        Connection connection = null;
        try {
            connection = getConnectionLogged(messages);
            if (connection == null) {
                return null;
            }

            DatabaseMetaData dbData = null;
            try {
                dbData = connection.getMetaData();
            } catch (SQLException e) {
                String message = "Unable to get database meta data... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);

                try {
                    connection.close();
                } catch (SQLException e2) {
                    String message2 = "Unable to close database connection, continuing anyway... Error was:" + e2.toString();
                    Debug.logError(message2, module);
                    if (messages != null) messages.add(message2);
                }
                return null;
            }

            if (Debug.infoOn()) Debug.logInfo("Getting Column Info From Database", module);

            Map<String, Map<String, ColumnCheckInfo>> colInfo = new HashMap<String, Map<String, ColumnCheckInfo>>();
            try {
                String lookupSchemaName = getSchemaName(dbData);
                boolean needsUpperCase = false;
                try {
                    needsUpperCase = dbData.storesLowerCaseIdentifiers() || dbData.storesMixedCaseIdentifiers();
                } catch (SQLException e) {
                    String message = "Error getting identifier case information... Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                }

                boolean foundCols = false;
                ResultSet rsCols = dbData.getColumns(null, lookupSchemaName, null, null);
                if (!rsCols.next()) {
                    try {
                        rsCols.close();
                    } catch (SQLException e) {
                        String message = "Unable to close ResultSet for column list, continuing anyway... Error was:" + e.toString();
                        Debug.logError(message, module);
                        if (messages != null) messages.add(message);
                    }
                    rsCols = dbData.getColumns(null, lookupSchemaName, "%", "%");
                    if (!rsCols.next()) {
                        // TODO: now what to do? I guess try one table name at a time...
                    } else {
                        foundCols = true;
                    }
                } else {
                    foundCols = true;
                }
                if (foundCols) {
                    do {
                        try {
                            ColumnCheckInfo ccInfo = new ColumnCheckInfo();

                            ccInfo.tableName = ColumnCheckInfo.fixupTableName(rsCols.getString("TABLE_NAME"), lookupSchemaName, needsUpperCase);
                            // ignore the column info if the table name is not in the list we are concerned with
                            if (!tableNames.contains(ccInfo.tableName)) {
                                continue;
                            }

                            ccInfo.columnName = rsCols.getString("COLUMN_NAME");
                            if (needsUpperCase && ccInfo.columnName != null) {
                                ccInfo.columnName = ccInfo.columnName.toUpperCase();
                            }
                            // NOTE: this may need a toUpperCase in some cases, keep an eye on it
                            ccInfo.typeName = rsCols.getString("TYPE_NAME");
                            ccInfo.columnSize = rsCols.getInt("COLUMN_SIZE");
                            ccInfo.decimalDigits = rsCols.getInt("DECIMAL_DIGITS");
                            // NOTE: this may need a toUpperCase in some cases, keep an eye on it
                            ccInfo.isNullable = rsCols.getString("IS_NULLABLE");

                            Map<String, ColumnCheckInfo> tableColInfo = colInfo.get(ccInfo.tableName);
                            if (tableColInfo == null) {
                                tableColInfo = new HashMap<String, ColumnCheckInfo>();
                                colInfo.put(ccInfo.tableName, tableColInfo);
                            }
                            tableColInfo.put(ccInfo.columnName, ccInfo);
                        } catch (SQLException e) {
                            String message = "Error getting column info for column. Error was:" + e.toString();
                            Debug.logError(message, module);
                            if (messages != null) messages.add(message);
                            continue;
                        }
                    } while (rsCols.next());
                }

                try {
                    rsCols.close();
                } catch (SQLException e) {
                    String message = "Unable to close ResultSet for column list, continuing anyway... Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                }

                if (getPks) {
                    int pkCount = 0;

                    // first try getting all at once for databases that support that and can generally perform WAY better, if that fails get one at a time so it will at least work
                    try (ResultSet rsPks = dbData.getPrimaryKeys(null, lookupSchemaName, null)) {
                        pkCount += checkPrimaryKeyInfo(rsPks, lookupSchemaName, needsUpperCase, colInfo, messages);
                    } catch (Exception e1) {
                        Debug.logInfo("Error getting primary key info from database with null tableName, will try other means: " + e1.toString(), module);
                    }
                    if (pkCount == 0) {
                        try (ResultSet rsPks = dbData.getPrimaryKeys(null, lookupSchemaName, "%")) {
                            pkCount += checkPrimaryKeyInfo(rsPks, lookupSchemaName, needsUpperCase, colInfo, messages);
                        } catch (Exception e1) {
                            Debug.logInfo("Error getting primary key info from database with % tableName, will try other means: " + e1.toString(), module);
                        }
                    }
                    if (pkCount == 0) {
                        Debug.logInfo("Searching in " + tableNames.size() + " tables for primary key fields ...", module);
                        List<Future<AbstractCountingCallable>> pkFetcherFutures = new LinkedList<Future<AbstractCountingCallable>>();
                        for (String curTable: tableNames) {
                            curTable = curTable.substring(curTable.indexOf('.') + 1); //cut off schema name
                            pkFetcherFutures.add(executor.submit(createPrimaryKeyFetcher(dbData, lookupSchemaName, needsUpperCase, colInfo, messages, curTable)));
                        }
                        for (AbstractCountingCallable pkFetcherCallable: ExecutionPool.getAllFutures(pkFetcherFutures)) {
                            pkCount += pkFetcherCallable.updateData(messages);
                        }
                    }

                    Debug.logInfo("Reviewed " + pkCount + " primary key fields from database.", module);
                }
            } catch (SQLException e) {
                String message = "Error getting column meta data for Error was: [" + e.toString() + "]. Not checking columns.";
                Debug.logError(e, message, module);
                if (messages != null) messages.add(message);
                // we are returning an empty set in this case because databases like SapDB throw an exception when there are no tables in the database
                // colInfo = null;
            }
            return colInfo;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    String message = "Unable to close database connection, continuing anyway... Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                }
            }
        }
    }

    public int checkPrimaryKeyInfo(ResultSet rsPks, String lookupSchemaName, boolean needsUpperCase, Map<String, Map<String, ColumnCheckInfo>> colInfo, Collection<String> messages) throws SQLException {
        int pkCount = 0;
            while (rsPks.next()) {
                pkCount++;
                try {
                    String tableName = ColumnCheckInfo.fixupTableName(rsPks.getString("TABLE_NAME"), lookupSchemaName, needsUpperCase);
                    String columnName = rsPks.getString("COLUMN_NAME");
                    if (needsUpperCase && columnName != null) {
                        columnName = columnName.toUpperCase();
                    }
                    Map<String, ColumnCheckInfo> tableColInfo = colInfo.get(tableName);
                    if (tableColInfo == null) {
                        // not looking for info on this table
                        continue;
                    }
                    ColumnCheckInfo ccInfo = tableColInfo.get(columnName);
                    if (ccInfo == null) {
                        // this isn't good, what to do?
                        Debug.logWarning("Got primary key information for a column that we didn't get column information for: tableName=[" + tableName + "], columnName=[" + columnName + "]", module);
                        continue;
                    }


                    // KEY_SEQ short => sequence number within primary key
                    // PK_NAME String => primary key name (may be null)

                    ccInfo.isPk = true;
                    ccInfo.pkSeq = rsPks.getShort("KEY_SEQ");
                    ccInfo.pkName = rsPks.getString("PK_NAME");
                } catch (SQLException e) {
                    String message = "Error getting primary key info for column. Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                    continue;
                }
            }
        return pkCount;
    }

    public Map<String, Map<String, ReferenceCheckInfo>> getReferenceInfo(Set<String> tableNames, Collection<String> messages) {
        Connection connection = getConnectionLogged(messages);
        if (connection == null) {
            return null;
        }

        DatabaseMetaData dbData = null;
        try {
            dbData = connection.getMetaData();
        } catch (SQLException e) {
            String message = "Unable to get database meta data... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);

            try {
                connection.close();
            } catch (SQLException e2) {
                String message2 = "Unable to close database connection, continuing anyway... Error was:" + e2.toString();
                Debug.logError(message2, module);
                if (messages != null) messages.add(message2);
            }
            return null;
        }

        /*
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Product Name is " + dbData.getDatabaseProductName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Product Version is " + dbData.getDatabaseProductVersion(), module);
         } catch (SQLException e) {
         Debug.logWarning("Unable to get Database name & version information", module);
         }
         try {
         if (Debug.infoOn()) Debug.logInfo("Database Driver Name is " + dbData.getDriverName(), module);
         if (Debug.infoOn()) Debug.logInfo("Database Driver Version is " + dbData.getDriverVersion(), module);
         } catch (SQLException e) {
         Debug.logWarning("Unable to get Driver name & version information", module);
         }
         */

        if (Debug.infoOn()) Debug.logInfo("Getting Foreign Key (Reference) Info From Database", module);

        Map<String, Map<String, ReferenceCheckInfo>> refInfo = new HashMap<String, Map<String, ReferenceCheckInfo>>();

        try {
            // ResultSet rsCols = dbData.getCrossReference(null, null, null, null, null, null);
            String lookupSchemaName = getSchemaName(dbData);
            boolean needsUpperCase = false;
            try {
                needsUpperCase = dbData.storesLowerCaseIdentifiers() || dbData.storesMixedCaseIdentifiers();
            } catch (SQLException e) {
                String message = "Error getting identifier case information... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }

            ResultSet rsCols = dbData.getImportedKeys(null, lookupSchemaName, null);
            int totalFkRefs = 0;

            // Iterator tableNamesIter = tableNames.iterator();
            // while (tableNamesIter.hasNext()) {
            // String tableName = (String) tableNamesIter.next();
            // ResultSet rsCols = dbData.getImportedKeys(null, null, tableName);
            // Debug.logVerbose("Getting imported keys for table " + tableName, module);

            while (rsCols.next()) {
                try {
                    ReferenceCheckInfo rcInfo = new ReferenceCheckInfo();

                    rcInfo.pkTableName = rsCols.getString("PKTABLE_NAME");
                    if (needsUpperCase && rcInfo.pkTableName != null) {
                        rcInfo.pkTableName = rcInfo.pkTableName.toUpperCase();
                    }
                    rcInfo.pkColumnName = rsCols.getString("PKCOLUMN_NAME");
                    if (needsUpperCase && rcInfo.pkColumnName != null) {
                        rcInfo.pkColumnName = rcInfo.pkColumnName.toUpperCase();
                    }

                    rcInfo.fkTableName = rsCols.getString("FKTABLE_NAME");
                    if (needsUpperCase && rcInfo.fkTableName != null) {
                        rcInfo.fkTableName = rcInfo.fkTableName.toUpperCase();
                    }
                    // ignore the column info if the FK table name is not in the list we are concerned with
                    if (!tableNames.contains(rcInfo.fkTableName)) {
                        continue;
                    }
                    rcInfo.fkColumnName = rsCols.getString("FKCOLUMN_NAME");
                    if (needsUpperCase && rcInfo.fkColumnName != null) {
                        rcInfo.fkColumnName = rcInfo.fkColumnName.toUpperCase();
                    }
                    rcInfo.fkName = rsCols.getString("FK_NAME");
                    if (needsUpperCase && rcInfo.fkName != null) {
                        rcInfo.fkName = rcInfo.fkName.toUpperCase();
                    }

                    if (Debug.verboseOn()) Debug.logVerbose("Got: " + rcInfo.toString(), module);

                    Map<String, ReferenceCheckInfo> tableRefInfo = refInfo.get(rcInfo.fkTableName);
                    if (tableRefInfo == null) {
                        tableRefInfo = new HashMap<String, ReferenceCheckInfo>();
                        refInfo.put(rcInfo.fkTableName, tableRefInfo);
                        if (Debug.verboseOn()) Debug.logVerbose("Adding new Map for table: " + rcInfo.fkTableName, module);
                    }
                    if (!tableRefInfo.containsKey(rcInfo.fkName)) totalFkRefs++;
                    tableRefInfo.put(rcInfo.fkName, rcInfo);
                } catch (SQLException e) {
                    String message = "Error getting fk reference info for table. Error was:" + e.toString();
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                    continue;
                }
            }

            // if (Debug.infoOn()) Debug.logInfo("There are " + totalFkRefs + " in the database", module);
            try {
                rsCols.close();
            } catch (SQLException e) {
                String message = "Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }
            // }
            if (Debug.infoOn()) Debug.logInfo("There are " + totalFkRefs + " foreign key refs in the database", module);

        } catch (SQLException e) {
            String message = "Error getting fk reference meta data Error was:" + e.toString() + ". Not checking fk refs.";
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            refInfo = null;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                String message = "Unable to close database connection, continuing anyway... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }
        }
        return refInfo;
    }

    public Map<String, Set<String>> getIndexInfo(Set<String> tableNames, Collection<String> messages, boolean[] needsUpperCase) {
        Connection connection = getConnectionLogged(messages);
        if (connection == null) {
            return null;
        }

        DatabaseMetaData dbData = null;
        try {
            dbData = connection.getMetaData();
        } catch (SQLException e) {
            String message = "Unable to get database meta data... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);

            try {
                connection.close();
            } catch (SQLException e2) {
                String message2 = "Unable to close database connection, continuing anyway... Error was:" + e2.toString();
                Debug.logError(message2, module);
                if (messages != null) messages.add(message2);
            }
            return null;
        }

        needsUpperCase[0] = false;
        try {
            needsUpperCase[0] = dbData.storesLowerCaseIdentifiers() || dbData.storesMixedCaseIdentifiers();
        } catch (SQLException e) {
            String message = "Error getting identifier case information... Error was:" + e.toString();
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
        }

        if (Debug.infoOn()) Debug.logInfo("Getting Index Info From Database", module);

        Map<String, Set<String>> indexInfo = new HashMap<String, Set<String>>();
        try {
            int totalIndices = 0;
            String lookupSchemaName = getSchemaName(dbData);
            for (String curTableName: tableNames) {
                if (lookupSchemaName != null) {
                    curTableName = curTableName.substring(lookupSchemaName.length() + 1);
                }

                ResultSet rsCols = null;
                try {
                    // false for unique, we don't really use unique indexes
                    // true for approximate, don't really care if stats are up-to-date
                    rsCols = dbData.getIndexInfo(null, lookupSchemaName, needsUpperCase[0] ? curTableName.toLowerCase() : curTableName, false, true);
                } catch (Exception e) {
                    Debug.logWarning(e, "Error getting index info for table: " + curTableName + " using lookupSchemaName " + lookupSchemaName, module);
                }

                while (rsCols != null && rsCols.next()) {
                    // NOTE: The code in this block may look funny, but it is designed so that the wrapping loop can be removed
                    try {
                        // skip all index info for statistics
                        if (rsCols.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) continue;

                        // HACK: for now skip all "unique" indexes since our foreign key indices are not unique, but the primary key ones are
                        // not correct, declared indices can also be unique
                        // if (!rsCols.getBoolean("NON_UNIQUE")) continue;

                        String tableName = rsCols.getString("TABLE_NAME");
                        if (needsUpperCase[0] && tableName != null) {
                            tableName = tableName.toUpperCase();
                        }
                        if (lookupSchemaName != null) {
                            tableName = lookupSchemaName + '.' + tableName;
                        }
                        if (!tableNames.contains(tableName)) continue;

                        String indexName = rsCols.getString("INDEX_NAME");
                        if (needsUpperCase[0] && indexName != null) {
                            indexName = indexName.toUpperCase();
                        }
                        if (indexName.startsWith("PK_") || indexName.startsWith("pk_")) continue;

                        Set<String> tableIndexList = indexInfo.get(tableName);
                        if (tableIndexList == null) {
                            tableIndexList = new TreeSet<String>();
                            indexInfo.put(tableName, tableIndexList);
                            if (Debug.verboseOn()) Debug.logVerbose("Adding new Map for table: " + tableName, module);
                        }
                        // Need the check here, because an index can contain multiple columns,
                        // and this is represented by having multiple rows in rsCols.
                        if (!tableIndexList.contains(indexName)) totalIndices++;
                        tableIndexList.add(indexName);
                    } catch (SQLException e) {
                        String message = "Error getting fk reference info for table. Error was:" + e.toString();
                        Debug.logError(message, module);
                        if (messages != null) messages.add(message);
                        continue;
                    }
                }

                // if (Debug.infoOn()) Debug.logInfo("There are " + totalIndices + " indices in the database", module);
                if (rsCols != null) {
                    try {
                        rsCols.close();
                    } catch (SQLException e) {
                        String message = "Unable to close ResultSet for fk reference list, continuing anyway... Error was:" + e.toString();
                        Debug.logError(message, module);
                        if (messages != null) messages.add(message);
                    }
                }
            }
            if (Debug.infoOn()) Debug.logInfo("There are " + totalIndices + " indices in the database", module);

        } catch (SQLException e) {
            String message = "Error getting fk reference meta data Error was:" + e.toString() + ". Not checking fk refs.";
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            indexInfo = null;
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                String message = "Unable to close database connection, continuing anyway... Error was:" + e.toString();
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
            }
        }
        return indexInfo;
    }

    private class CreateTableCallable implements Callable<CreateTableCallable> {
        private final ModelEntity entity;
        private final Map<String, ModelEntity> modelEntities;
        private final String tableName;
        private String message;
        private boolean success;

        protected CreateTableCallable(ModelEntity entity, Map<String, ModelEntity> modelEntities, String tableName) {
            this.entity = entity;
            this.modelEntities = modelEntities;
            this.tableName = tableName;
        }

        public CreateTableCallable call() throws Exception {
            String errMsg = createTable(entity, modelEntities, false);
            if (UtilValidate.isNotEmpty(errMsg)) {
                this.success = false;
                this.message = "Could not create table [" + tableName + "]: " + errMsg;
                Debug.logError(this.message, module);
            } else {
                this.success = true;
                this.message = "Created table [" + tableName + "]";
                Debug.logImportant(this.message, module);
            }
            return this;
        }

        protected void updateData(Collection<String> messages, List<ModelEntity> entitiesAdded) {
            if (this.success) {
                entitiesAdded.add(entity);
                if (messages != null) {
                    messages.add(this.message);
                }
            } else {
                if (messages != null) {
                    messages.add(this.message);
                }
            }
        }
    }

    private abstract class AbstractCountingCallable implements Callable<AbstractCountingCallable> {
        protected final ModelEntity entity;
        protected final Map<String, ModelEntity> modelEntities;
        protected final List<String> messages = new LinkedList<String>();
        protected int count;

        protected AbstractCountingCallable(ModelEntity entity, Map<String, ModelEntity> modelEntities) {
            this.entity = entity;
            this.modelEntities = modelEntities;
        }

        protected int updateData(Collection<String> messages) {
            if (messages != null && UtilValidate.isNotEmpty(this.messages)) {
                this.messages.addAll(messages);
            }
            return count;
        }
    }

    /* ====================================================================== */

    /* ====================================================================== */

    public String createTable(ModelEntity entity, Map<String, ModelEntity> modelEntities, boolean addFks) {
        if (entity == null) {
            return "ModelEntity was null and is required to create a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot create table for a view entity";
        }

        StringBuilder sqlBuf = new StringBuilder("CREATE TABLE ");
        sqlBuf.append(entity.getTableName(this.datasourceInfo));
        sqlBuf.append(" (");
        Iterator<ModelField> fieldIter = entity.getFieldsIterator();
        while (fieldIter.hasNext()) {
            ModelField field = fieldIter.next();
            ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());
            if (type == null) {
                return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not creating table.";
            }

            sqlBuf.append(field.getColName());
            sqlBuf.append(" ");
            sqlBuf.append(type.getSqlType());

            if ("String".equals(type.getJavaType()) || "java.lang.String".equals(type.getJavaType())) {
                // if there is a characterSet, add the CHARACTER SET arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
                    sqlBuf.append(" CHARACTER SET ");
                    sqlBuf.append(this.datasourceInfo.getCharacterSet());
                }
                // if there is a collate, add the COLLATE arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
                    sqlBuf.append(" COLLATE ");
                    sqlBuf.append(this.datasourceInfo.getCollate());
                }
            }

            if (field.getIsNotNull() || field.getIsPk()) {
                if (this.datasourceInfo.getAlwaysUseConstraintKeyword()) {
                    sqlBuf.append(" CONSTRAINT NOT NULL, ");
                } else {
                    sqlBuf.append(" NOT NULL, ");
                }
            } else {
                sqlBuf.append(", ");
            }
        }

        String pkName = makePkConstraintName(entity, this.datasourceInfo.getConstraintNameClipLength());
        if (this.datasourceInfo.getUsePkConstraintNames()) {
            sqlBuf.append("CONSTRAINT ");
            sqlBuf.append(pkName);
        }
        sqlBuf.append(" PRIMARY KEY (");
        entity.colNameString(entity.getPkFieldsUnmodifiable(), sqlBuf, "");
        sqlBuf.append(")");

        if (addFks) {
            // NOTE: This is kind of a bad idea anyway since ordering table creations is crazy, if not impossible

            // go through the relationships to see if any foreign keys need to be added
            Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();
            while (relationsIter.hasNext()) {
                ModelRelation modelRelation = relationsIter.next();
                if ("one".equals(modelRelation.getType())) {
                    ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());
                    if (relModelEntity == null) {
                        Debug.logError("Error adding foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName(), module);
                        continue;
                    }
                    if (relModelEntity instanceof ModelViewEntity) {
                        Debug.logError("Error adding foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName(), module);
                        continue;
                    }

                    String fkConstraintClause = makeFkConstraintClause(entity, modelRelation, relModelEntity, this.datasourceInfo.getConstraintNameClipLength(), this.datasourceInfo.getFkStyle(), this.datasourceInfo.getUseFkInitiallyDeferred());
                    if (UtilValidate.isNotEmpty(fkConstraintClause)) {
                        sqlBuf.append(", ");
                        sqlBuf.append(fkConstraintClause);
                    } else {
                        continue;
                    }
                }
            }
        }

        sqlBuf.append(")");

        // if there is a tableType, add the TYPE arg here
        if (UtilValidate.isNotEmpty(this.datasourceInfo.getTableType())) {
         // jaz:20101229 - This appears to be only used by mysql and now mysql has
            // deprecated (and in 5.5.x removed) the use of the TYPE keyword. This is
            // changed to ENGINE which is supported starting at 4.1
            sqlBuf.append(" ENGINE ");
            //sqlBuf.append(" TYPE ");
            sqlBuf.append(this.datasourceInfo.getTableType());
        }

        // if there is a characterSet, add the CHARACTER SET arg here
        if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
            sqlBuf.append(" CHARACTER SET ");
            sqlBuf.append(this.datasourceInfo.getCharacterSet());
        }

        // if there is a collate, add the COLLATE arg here
        if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
            sqlBuf.append(" COLLATE ");
            sqlBuf.append(this.datasourceInfo.getCollate());
        }

        if (Debug.verboseOn()) Debug.logVerbose("[createTable] sql=" + sqlBuf.toString(), module);
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public void deleteTable(ModelEntity entity, List<String> messages) {
        if (entity == null) {
            String errMsg = "ModelEntity was null and is required to delete a table";
            Debug.logError(errMsg, module);
            if (messages != null) messages.add(errMsg);
            return;
        }
        if (entity instanceof ModelViewEntity) {
            //String errMsg = "ERROR: Cannot delete table for a view entity";
            //Debug.logError(errMsg, module);
            //if (messages != null) messages.add(errMsg);
            return;
        }

        String message = "Deleting table for entity [" + entity.getEntityName() + "]";
        Debug.logImportant(message, module);
        if (messages != null) messages.add(message);

        StringBuilder sqlBuf = new StringBuilder("DROP TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        if (Debug.verboseOn()) Debug.logVerbose("[deleteTable] sql=" + sqlBuf.toString(), module);
        try (Connection connection = getConnectionLogged(messages); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
            Debug.logError(errMsg, module);
            if (messages != null) messages.add(errMsg);
        }
    }

    public String addColumn(ModelEntity entity, ModelField field) {
        if (entity == null || field == null)
            return "ModelEntity or ModelField where null, cannot add column";
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot add column for a view entity";
        }

        ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

        if (type == null) {
            return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not adding column.";
        }

        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        sqlBuf.append(field.getColName());
        sqlBuf.append(" ");
        sqlBuf.append(type.getSqlType());

        if ("String".equals(type.getJavaType()) || "java.lang.String".equals(type.getJavaType())) {
            // if there is a characterSet, add the CHARACTER SET arg here
            if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
                sqlBuf.append(" CHARACTER SET ");
                sqlBuf.append(this.datasourceInfo.getCharacterSet());
            }

            // if there is a collate, add the COLLATE arg here
            if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
                sqlBuf.append(" COLLATE ");
                sqlBuf.append(this.datasourceInfo.getCollate());
            }
        }

        String sql = sqlBuf.toString();
        if (Debug.infoOn()) Debug.logInfo("[addColumn] sql=" + sql, module);
        try (Connection connection = getConnection(); Statement  stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // if that failed try the alternate syntax real quick
            StringBuilder sql2Buf = new StringBuilder("ALTER TABLE ");
            sql2Buf.append(entity.getTableName(datasourceInfo));
            sql2Buf.append(" ADD COLUMN ");
            sql2Buf.append(field.getColName());
            sql2Buf.append(" ");
            sql2Buf.append(type.getSqlType());

            if ("String".equals(type.getJavaType()) || "java.lang.String".equals(type.getJavaType())) {
                // if there is a characterSet, add the CHARACTER SET arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
                    sql2Buf.append(" CHARACTER SET ");
                    sql2Buf.append(this.datasourceInfo.getCharacterSet());
                }

                // if there is a collate, add the COLLATE arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
                    sql2Buf.append(" COLLATE ");
                    sql2Buf.append(this.datasourceInfo.getCollate());
                }
            }

            String sql2 = sql2Buf.toString();
            if (Debug.infoOn()) Debug.logInfo("[addColumn] sql failed, trying sql2=" + sql2, module);
            try (Connection connection = getConnection(); Statement  stmt = connection.createStatement()) {
                stmt.executeUpdate(sql2);
            } catch (SQLException e2) {
                // if this also fails report original error, not this error...
                return "SQL Exception while executing the following:\n" + sql + "\nError was: " + e2.toString();
            } catch (GenericEntityException e2) {
                String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e2.toString();
                Debug.logError(e2, errMsg, module);
                return errMsg;
            }
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public String renameColumn(ModelEntity entity, ModelField field, String newName) {
        if (entity == null || field == null)
            return "ModelEntity or ModelField where null, cannot rename column";
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot rename column for a view entity";
        }

        ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());

        if (type == null) {
            return "Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not renaming column.";
        }

        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" RENAME ");
        sqlBuf.append(field.getColName());
        sqlBuf.append(" TO ");
        sqlBuf.append(newName);

        String sql = sqlBuf.toString();
        if (Debug.infoOn()) Debug.logInfo("[renameColumn] sql=" + sql, module);
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + sql + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public void repairColumnSize(ModelEntity entity, ModelField field, List<String> messages) {
        // first rename the column
        String tempName = makeTempFieldName(field);
        String renamedErr = renameColumn(entity, field, tempName);
        if (UtilValidate.isNotEmpty(renamedErr)) {
            if (messages != null) messages.add(renamedErr);
            Debug.logError(renamedErr, module);
            return;
        }

        // next add back in the column
        String addedErr = addColumn(entity, field);
        if (UtilValidate.isNotEmpty(addedErr)) {
            if (messages != null) messages.add(addedErr);
            Debug.logError(addedErr, module);
            return;
        }

        // copy the data from old to new
        StringBuilder sqlBuf1 = new StringBuilder("UPDATE ");
        sqlBuf1.append(entity.getTableName(datasourceInfo));
        sqlBuf1.append(" SET ");
        sqlBuf1.append(field.getColName());
        sqlBuf1.append(" = ");
        sqlBuf1.append(tempName);

        String sql1 = sqlBuf1.toString();
        if (Debug.infoOn()) Debug.logInfo("[moveData] sql=" + sql1, module);
        try (Connection connection = getConnectionLogged(messages); Statement stmt = connection.createStatement()) {
            int changed = stmt.executeUpdate(sql1);
            if (Debug.infoOn()) Debug.logInfo("[moveData] " + changed + " records updated", module);
        } catch (SQLException e) {
            String thisMsg = "SQL Exception while executing the following:\n" + sql1 + "\nError was: " + e.toString();
            if (messages != null)
                messages.add(thisMsg);
            Debug.logError(thisMsg, module);
            return;
        }

        // remove the old column
        StringBuilder sqlBuf2 = new StringBuilder("ALTER TABLE ");
        sqlBuf2.append(entity.getTableName(datasourceInfo));
        sqlBuf2.append(" DROP COLUMN ");
        sqlBuf2.append(tempName);

        String sql2 = sqlBuf2.toString();
        if (Debug.infoOn()) Debug.logInfo("[dropColumn] sql=" + sql2, module);
        try (Connection connection = getConnectionLogged(messages); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql2);
        } catch (SQLException e) {
            String thisMsg = "SQL Exception while executing the following:\n" + sql2 + "\nError was: " + e.toString();
            if (messages != null)
                messages.add(thisMsg);
            Debug.logError(thisMsg, module);
            return;
        }
    }

    public void repairColumnSizeChanges(Map<String, ModelEntity> modelEntities, List<String> fieldsWrongSize, List<String> messages) {
        if (modelEntities == null || UtilValidate.isEmpty(fieldsWrongSize)) {
            messages.add("No fields to repair");
            return;
        }

        if (messages == null) messages = new ArrayList<String>();

        for (String fieldInfo: fieldsWrongSize) {
            String entityName = fieldInfo.substring(0, fieldInfo.indexOf('.'));
            String fieldName = fieldInfo.substring(fieldInfo.indexOf('.') + 1);

            ModelEntity modelEntity = modelEntities.get(entityName);
            ModelField modelField = modelEntity.getField(fieldName);
            repairColumnSize(modelEntity, modelField, messages);
        }
    }

    private String makeTempFieldName(ModelField field) {
        String tempName = "tmp_" + field.getName();
        if (tempName.length() > 30) {
            tempName = tempName.substring(0, 30);
        }
        return tempName.toUpperCase();
    }

    /* ====================================================================== */

    /* ====================================================================== */
    public String makePkConstraintName(ModelEntity entity, int constraintNameClipLength) {
        String pkName = "PK_" + entity.getPlainTableName();

        if (pkName.length() > constraintNameClipLength) {
            pkName = pkName.substring(0, constraintNameClipLength);
        }

        return pkName;
    }

    public String makeFkConstraintName(ModelRelation modelRelation, int constraintNameClipLength) {
        String relConstraintName = modelRelation.getFkName();

        if (UtilValidate.isEmpty(relConstraintName)) {
            relConstraintName = modelRelation.getTitle() + modelRelation.getRelEntityName();
            relConstraintName = relConstraintName.toUpperCase();
        }

        if (relConstraintName.length() > constraintNameClipLength) {
            relConstraintName = relConstraintName.substring(0, constraintNameClipLength);
        }

        return relConstraintName;
    }

    /* ====================================================================== */
    public String makeIndexName(ModelIndex modelIndex, int constraintNameClipLength) {
        String indexName = modelIndex.getName();

        if (indexName.length() > constraintNameClipLength) {
            indexName = indexName.substring(0, constraintNameClipLength);
        }

        return indexName;
    }

    /* ====================================================================== */
    public int createForeignKeys(ModelEntity entity, Map<String, ModelEntity> modelEntities, List<String> messages) {
        return this.createForeignKeys(entity, modelEntities, datasourceInfo.getConstraintNameClipLength(), datasourceInfo.getFkStyle(), datasourceInfo.getUseFkInitiallyDeferred(), messages);
    }
    public int createForeignKeys(ModelEntity entity, Map<String, ModelEntity> modelEntities, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred, List<String> messages) {
        if (entity == null) {
            String errMsg = "ModelEntity was null and is required to create foreign keys for a table";
            Debug.logError(errMsg, module);
            if (messages != null) messages.add(errMsg);
            return 0;
        }
        if (entity instanceof ModelViewEntity) {
            //String errMsg = "ERROR: Cannot create foreign keys for a view entity";
            //Debug.logError(errMsg, module);
            //if (messages != null) messages.add(errMsg);
            return 0;
        }

        int fksCreated = 0;

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();
        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();
            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

                if (relModelEntity == null) {
                    String errMsg = "Error adding foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName();
                    Debug.logError(errMsg, module);
                    if (messages != null) messages.add(errMsg);
                    continue;
                }
                if (relModelEntity instanceof ModelViewEntity) {
                    String errMsg = "Error adding foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName();
                    Debug.logError(errMsg, module);
                    if (messages != null) messages.add(errMsg);
                    continue;
                }

                String retMsg = createForeignKey(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred);
                if (UtilValidate.isNotEmpty(retMsg)) {
                    Debug.logError(retMsg, module);
                    if (messages != null) messages.add(retMsg);
                    continue;
                }

                fksCreated++;
            }
        }

        if (fksCreated > 0) {
            String message = "Created " + fksCreated + " foreign keys for entity [" + entity.getEntityName() + "]";
            Debug.logImportant(message, module);
            if (messages != null) messages.add(message);
        }

        return fksCreated;
    }

    public String createForeignKey(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        // now add constraint clause
        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        sqlBuf.append(" ADD ");
        String fkConstraintClause = makeFkConstraintClause(entity, modelRelation, relModelEntity, constraintNameClipLength, fkStyle, useFkInitiallyDeferred);
        if (UtilValidate.isEmpty(fkConstraintClause)) {
            return "Error creating foreign key constraint clause, see log for details";
        }
        sqlBuf.append(fkConstraintClause);

        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKey] sql=" + sqlBuf.toString(), module);
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public String makeFkConstraintClause(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength, String fkStyle, boolean useFkInitiallyDeferred) {
        // make the two column lists
        StringBuilder mainCols = new StringBuilder();
        StringBuilder relCols = new StringBuilder();

        for (ModelKeyMap keyMap : modelRelation.getKeyMaps()) {

            ModelField mainField = entity.getField(keyMap.getFieldName());
            if (mainField == null) {
                Debug.logError("Bad key-map in entity [" + entity.getEntityName() + "] relation to [" + modelRelation.getTitle() + modelRelation.getRelEntityName() + "] for field [" + keyMap.getFieldName() + "]", module);
                return null;
            }

            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());

            ModelField relField = relModelEntity.getField(keyMap.getRelFieldName());
            if (relField == null) {
                Debug.logError("The field '" + keyMap.getRelFieldName() + "' was not found at related entity - check relations at entity '" + entity.getEntityName() + "'!", module);
            }

            if (relCols.length() > 0) {
                relCols.append(", ");
            }
            relCols.append(relField.getColName());
        }

        StringBuilder sqlBuf = new StringBuilder("");

        if ("name_constraint".equals(fkStyle)) {
            sqlBuf.append("CONSTRAINT ");
            String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

            sqlBuf.append(relConstraintName);

            sqlBuf.append(" FOREIGN KEY (");
            sqlBuf.append(mainCols.toString());
            sqlBuf.append(") REFERENCES ");
            sqlBuf.append(relModelEntity.getTableName(datasourceInfo));
            sqlBuf.append(" (");
            sqlBuf.append(relCols.toString());
            sqlBuf.append(")");
            if (useFkInitiallyDeferred) {
                sqlBuf.append(" INITIALLY DEFERRED");
            }
        } else if ("name_fk".equals(fkStyle)) {
            sqlBuf.append(" FOREIGN KEY ");
            String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

            sqlBuf.append(relConstraintName);
            sqlBuf.append(" (");
            sqlBuf.append(mainCols.toString());
            sqlBuf.append(") REFERENCES ");
            sqlBuf.append(relModelEntity.getTableName(datasourceInfo));
            sqlBuf.append(" (");
            sqlBuf.append(relCols.toString());
            sqlBuf.append(")");
            if (useFkInitiallyDeferred) {
                sqlBuf.append(" INITIALLY DEFERRED");
            }
        } else {
            String emsg = "ERROR: fk-style specified for this data-source is not valid: " + fkStyle;

            Debug.logError(emsg, module);
            throw new IllegalArgumentException(emsg);
        }

        return sqlBuf.toString();
    }

    public void deleteForeignKeys(ModelEntity entity, Map<String, ModelEntity> modelEntities, List<String> messages) {
        this.deleteForeignKeys(entity, modelEntities, datasourceInfo.getConstraintNameClipLength(), messages);
    }

    public void deleteForeignKeys(ModelEntity entity, Map<String, ModelEntity> modelEntities, int constraintNameClipLength, List<String> messages) {
        if (entity == null) {
            String errMsg = "ModelEntity was null and is required to delete foreign keys for a table";
            if (messages != null) messages.add(errMsg);
            Debug.logError(errMsg, module);
            return;
        }
        if (entity instanceof ModelViewEntity) {
            //String errMsg = "ERROR: Cannot delete foreign keys for a view entity";
            //if (messages != null) messages.add(errMsg);
            //Debug.logError(errMsg, module);
            return;
        }

        String message = "Deleting foreign keys for entity [" + entity.getEntityName() + "]";
        Debug.logImportant(message, module);
        if (messages != null) messages.add(message);

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();
        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                ModelEntity relModelEntity = modelEntities.get(modelRelation.getRelEntityName());

                if (relModelEntity == null) {
                    String errMsg = "Error removing foreign key: ModelEntity was null for related entity name " + modelRelation.getRelEntityName();
                    if (messages != null) messages.add(errMsg);
                    Debug.logError(errMsg, module);
                    continue;
                }
                if (relModelEntity instanceof ModelViewEntity) {
                    String errMsg = "Error removing foreign key: related entity is a view entity for related entity name " + modelRelation.getRelEntityName();
                    if (messages != null) messages.add(errMsg);
                    Debug.logError(errMsg, module);
                    continue;
                }

                String retMsg = deleteForeignKey(entity, modelRelation, relModelEntity, constraintNameClipLength);
                if (UtilValidate.isNotEmpty(retMsg)) {
                    if (messages != null) messages.add(retMsg);
                    Debug.logError(retMsg, module);
                }
            }
        }
    }

    public String deleteForeignKey(ModelEntity entity, ModelRelation modelRelation, ModelEntity relModelEntity, int constraintNameClipLength) {
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        // now add constraint clause
        StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
        sqlBuf.append(entity.getTableName(datasourceInfo));
        if (datasourceInfo.getDropFkUseForeignKeyKeyword()) {
            sqlBuf.append(" DROP FOREIGN KEY ");
        } else {
            sqlBuf.append(" DROP CONSTRAINT ");
        }
        sqlBuf.append(relConstraintName);

        if (Debug.verboseOn()) Debug.logVerbose("[deleteForeignKey] sql=" + sqlBuf.toString(), module);
        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sqlBuf.toString());
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    /* ====================================================================== */
    /* ====================================================================== */
    public void createPrimaryKey(ModelEntity entity, boolean usePkConstraintNames, int constraintNameClipLength, List<String> messages) {
        if (messages == null) messages = new ArrayList<String>();
        String err = createPrimaryKey(entity, usePkConstraintNames, constraintNameClipLength);
        if (UtilValidate.isNotEmpty(err)) {
            messages.add(err);
        }
    }

    public void createPrimaryKey(ModelEntity entity, boolean usePkConstraintNames, List<String> messages) {
        createPrimaryKey(entity, usePkConstraintNames, datasourceInfo.getConstraintNameClipLength(), messages);
    }

    public void createPrimaryKey(ModelEntity entity, List<String> messages) {
        createPrimaryKey(entity, datasourceInfo.getUsePkConstraintNames(), messages);
    }

    public String createPrimaryKey(ModelEntity entity, boolean usePkConstraintNames, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to create the primary key for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "Ignoring view entity [" + entity.getEntityName() + "]";
        }

        String message;
        if (entity.getPksSize() > 0) {
            message = "Creating primary key for entity [" + entity.getEntityName() + "]";

            // now add constraint clause
            StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
            sqlBuf.append(entity.getTableName(datasourceInfo));
            sqlBuf.append(" ADD ");

            String pkName = makePkConstraintName(entity, constraintNameClipLength);

            if (usePkConstraintNames) {
                sqlBuf.append("CONSTRAINT ");
                sqlBuf.append(pkName);
            }
            sqlBuf.append(" PRIMARY KEY (");
            entity.colNameString(entity.getPkFieldsUnmodifiable(), sqlBuf, "");
            sqlBuf.append(")");

            if (Debug.verboseOn()) Debug.logVerbose("[createPrimaryKey] sql=" + sqlBuf.toString(), module);
            try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sqlBuf.toString());
            } catch (SQLException e) {
                return "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
            } catch (GenericEntityException e) {
                return "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            }
        } else {
            message = "No primary-key defined for table [" + entity.getEntityName() + "]";
        }

        Debug.logImportant(message, module);
        return message;
    }

    public void deletePrimaryKey(ModelEntity entity, boolean usePkConstraintNames, int constraintNameClipLength, List<String> messages) {
        if (messages == null) messages = new ArrayList<String>();
        String err = deletePrimaryKey(entity, usePkConstraintNames, constraintNameClipLength);
        if (UtilValidate.isNotEmpty(err)) {
            messages.add(err);
        }
    }

    public void deletePrimaryKey(ModelEntity entity, boolean usePkConstraintNames,  List<String> messages) {
        deletePrimaryKey(entity, usePkConstraintNames, datasourceInfo.getConstraintNameClipLength(), messages);
    }

    public void deletePrimaryKey(ModelEntity entity, List<String> messages) {
        deletePrimaryKey(entity, datasourceInfo.getUsePkConstraintNames(), messages);
    }

    public String deletePrimaryKey(ModelEntity entity, boolean usePkConstraintNames, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete the primary key for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "Ignoring view entity [" + entity.getEntityName() + "]";
        }

        String message;
        if (entity.getPksSize() > 0) {
            message = "Deleting primary key for entity [" + entity.getEntityName() + "]";
            // now add constraint clause
            StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
            sqlBuf.append(entity.getTableName(datasourceInfo));
            sqlBuf.append(" DROP ");

            String pkName = makePkConstraintName(entity, constraintNameClipLength);

            if (usePkConstraintNames) {
                sqlBuf.append("CONSTRAINT ");
                sqlBuf.append(pkName);
                sqlBuf.append(" CASCADE");
            } else {
                sqlBuf.append(" PRIMARY KEY");
                // DEJ20050502 not sure why this is here, shouldn't be needed and some dbs don't support like this, ie when used with PRIMARY KEY: sqlBuf.append(" CASCADE");
            }

            if (Debug.verboseOn()) Debug.logVerbose("[deletePrimaryKey] sql=" + sqlBuf.toString(), module);
            try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sqlBuf.toString());
            } catch (SQLException e) {
                String errMsg = "SQL Exception while executing the following:\n" + sqlBuf.toString() + "\nError was: " + e.toString();
                Debug.logError(e, errMsg, module);
                return errMsg;
            } catch (GenericEntityException e) {
                String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
                Debug.logError(e, errMsg, module);
                return errMsg;
            }
        } else {
            message = "No primary-key defined for table [" + entity.getEntityName() + "]";
        }

        Debug.logImportant(message, module);
        return message;
    }

    /* ====================================================================== */
    /* ====================================================================== */
    public int createDeclaredIndices(ModelEntity entity, List<String> messages) {
        if (entity == null) {
            String message = "ModelEntity was null and is required to create declared indices for a table";
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            return 0;
        }
        if (entity instanceof ModelViewEntity) {
            String message = "Cannot create declared indices for a view entity";
            Debug.logWarning(message, module);
            if (messages != null) messages.add(message);
            return 0;
        }

        int dinsCreated = 0;

        // go through the indexes to see if any need to be added
        Iterator<ModelIndex> indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = indexesIter.next();

            String retMsg = createDeclaredIndex(entity, modelIndex);
            if (UtilValidate.isNotEmpty(retMsg)) {
                String message = "Could not create declared indices for entity [" + entity.getEntityName() + "]: " + retMsg;
                Debug.logError(message, module);
                if (messages != null) messages.add(message);
                continue;
            }
            dinsCreated++;
        }

        if (dinsCreated > 0) {
            String message = "Created " + dinsCreated + " declared indices for entity [" + entity.getEntityName() + "]";
            Debug.logImportant(message, module);
            if (messages != null) messages.add(message);
        }
        return dinsCreated;
    }

    public String createDeclaredIndex(ModelEntity entity, ModelIndex modelIndex) {
        String createIndexSql = makeIndexClause(entity, modelIndex);
        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql, module);

        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createIndexSql);
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + createIndexSql + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public String makeIndexClause(ModelEntity entity, ModelIndex modelIndex) {
        StringBuilder mainCols = new StringBuilder();

        for (ModelIndex.Field field : modelIndex.getFields()) {
            ModelIndex.Function function = field.getFunction();
            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            if (function != null) {
                mainCols.append(function.toString()).append('(');
            }
            ModelField mainField = entity.getField(field.getFieldName());
            mainCols.append(mainField.getColName());
            if (function != null) {
                mainCols.append(')');
            }
        }

        StringBuilder indexSqlBuf = new StringBuilder("CREATE ");
        if (datasourceInfo.getUseIndicesUnique() && modelIndex.getUnique()) {
            indexSqlBuf.append("UNIQUE ");
        }
        indexSqlBuf.append("INDEX ");
        indexSqlBuf.append(makeIndexName(modelIndex, datasourceInfo.getConstraintNameClipLength()));
        indexSqlBuf.append(" ON ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));

        indexSqlBuf.append(" (");
        indexSqlBuf.append(mainCols.toString());
        indexSqlBuf.append(")");

        return indexSqlBuf.toString();
    }

    public void deleteDeclaredIndices(ModelEntity entity, List<String> messages) {
        if (messages == null) messages = new ArrayList<String>();
        String err = deleteDeclaredIndices(entity);
        if (UtilValidate.isNotEmpty(err)) {
            messages.add(err);
        }
    }

    public String deleteDeclaredIndices(ModelEntity entity) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete declared indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete declared indices for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelIndex> indexesIter = entity.getIndexesIterator();
        while (indexesIter.hasNext()) {
            ModelIndex modelIndex = indexesIter.next();
            String retMsg = deleteDeclaredIndex(entity, modelIndex);
            if (UtilValidate.isNotEmpty(retMsg)) {
                if (retMsgsBuffer.length() > 0) {
                    retMsgsBuffer.append("\n");
                }
                retMsgsBuffer.append(retMsg);
                if (Debug.infoOn()) Debug.logInfo(retMsg, module);
            }
        }

        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String deleteDeclaredIndex(ModelEntity entity, ModelIndex modelIndex) {
        // TODO: also remove the constraing if this was a unique index, in most databases dropping the index does not drop the constraint

        StringBuilder indexSqlBuf = new StringBuilder("DROP INDEX ");
        String tableName = entity.getTableName(datasourceInfo);
        String schemaName = (UtilValidate.isEmpty(tableName) || tableName.indexOf('.') == -1) ? "" :
                tableName.substring(0, tableName.indexOf('.'));

        indexSqlBuf.append(schemaName);
        indexSqlBuf.append(".");
        indexSqlBuf.append(modelIndex.getName());

        String deleteIndexSql = indexSqlBuf.toString();
        if (Debug.verboseOn()) Debug.logVerbose("[deleteDeclaredIndex] index sql=" + deleteIndexSql, module);

        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteIndexSql);
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + deleteIndexSql + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    /* ====================================================================== */
    /* ====================================================================== */
    public int createForeignKeyIndices(ModelEntity entity, List<String> messages) {
        return createForeignKeyIndices(entity, datasourceInfo.getConstraintNameClipLength(), messages);
    }

    public int createForeignKeyIndices(ModelEntity entity, int constraintNameClipLength, List<String> messages) {
        if (entity == null) {
            String message = "ModelEntity was null and is required to create foreign keys indices for a table";
            Debug.logError(message, module);
            if (messages != null) messages.add(message);
            return 0;
        }
        if (entity instanceof ModelViewEntity) {
            String message = "Cannot create foreign keys indices for a view entity";
            Debug.logWarning(message, module);
            if (messages != null) messages.add(message);
            return 0;
        }

        int fkisCreated = 0;

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();
        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();
            if ("one".equals(modelRelation.getType())) {
                String retMsg = createForeignKeyIndex(entity, modelRelation, constraintNameClipLength);
                if (UtilValidate.isNotEmpty(retMsg)) {
                    String message = "Could not create foreign key indices for entity [" + entity.getEntityName() + "]: " + retMsg;
                    Debug.logError(message, module);
                    if (messages != null) messages.add(message);
                    continue;
                }
                fkisCreated++;
            }
        }

        if (fkisCreated > 0) {
            String message = "Created " + fkisCreated + " foreign key indices for entity [" + entity.getEntityName() + "]";
            Debug.logImportant(message, module);
            if (messages != null) messages.add(message);
        }
        return fkisCreated;
    }

    public String createForeignKeyIndex(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        String createIndexSql = makeFkIndexClause(entity, modelRelation, constraintNameClipLength);
        if (UtilValidate.isEmpty(createIndexSql)) {
            return "Error creating foreign key index clause, see log for details";
        }

        if (Debug.verboseOn()) Debug.logVerbose("[createForeignKeyIndex] index sql=" + createIndexSql, module);

        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createIndexSql);
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + createIndexSql + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public String makeFkIndexClause(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        StringBuilder mainCols = new StringBuilder();

        for (ModelKeyMap keyMap : modelRelation.getKeyMaps()) {
            ModelField mainField = entity.getField(keyMap.getFieldName());

            if (mainField == null) {
                Debug.logError("Bad key-map in entity [" + entity.getEntityName() + "] relation to [" + modelRelation.getTitle() + modelRelation.getRelEntityName() + "] for field [" + keyMap.getFieldName() + "]", module);
                return null;
            }

            if (mainCols.length() > 0) {
                mainCols.append(", ");
            }
            mainCols.append(mainField.getColName());
        }

        StringBuilder indexSqlBuf = new StringBuilder("CREATE INDEX ");
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        indexSqlBuf.append(relConstraintName);
        indexSqlBuf.append(" ON ");
        indexSqlBuf.append(entity.getTableName(datasourceInfo));

        indexSqlBuf.append(" (");
        indexSqlBuf.append(mainCols.toString());
        indexSqlBuf.append(")");

        return indexSqlBuf.toString();
    }

    public void deleteForeignKeyIndices(ModelEntity entity, List<String> messages) {
        if (messages == null) messages = new ArrayList<String>();
        String err = deleteForeignKeyIndices(entity, datasourceInfo.getConstraintNameClipLength());
        if (UtilValidate.isNotEmpty(err)) {
            messages.add(err);
        }
    }

    public String deleteForeignKeyIndices(ModelEntity entity, int constraintNameClipLength) {
        if (entity == null) {
            return "ModelEntity was null and is required to delete foreign keys indices for a table";
        }
        if (entity instanceof ModelViewEntity) {
            return "ERROR: Cannot delete foreign keys indices for a view entity";
        }

        StringBuilder retMsgsBuffer = new StringBuilder();

        // go through the relationships to see if any foreign keys need to be added
        Iterator<ModelRelation> relationsIter = entity.getRelationsIterator();

        while (relationsIter.hasNext()) {
            ModelRelation modelRelation = relationsIter.next();

            if ("one".equals(modelRelation.getType())) {
                String retMsg = deleteForeignKeyIndex(entity, modelRelation, constraintNameClipLength);

                if (UtilValidate.isNotEmpty(retMsg)) {
                    if (retMsgsBuffer.length() > 0) {
                        retMsgsBuffer.append("\n");
                    }
                    retMsgsBuffer.append(retMsg);
                }
            }
        }
        if (retMsgsBuffer.length() > 0) {
            return retMsgsBuffer.toString();
        } else {
            return null;
        }
    }

    public String deleteForeignKeyIndex(ModelEntity entity, ModelRelation modelRelation, int constraintNameClipLength) {
        StringBuilder indexSqlBuf = new StringBuilder("DROP INDEX ");
        String relConstraintName = makeFkConstraintName(modelRelation, constraintNameClipLength);

        String tableName = entity.getTableName(datasourceInfo);
        String schemaName = (UtilValidate.isEmpty(tableName) || tableName.indexOf('.') == -1) ? "" :
                tableName.substring(0, tableName.indexOf('.'));

        if (UtilValidate.isNotEmpty(schemaName)) {
            indexSqlBuf.append(schemaName);
            indexSqlBuf.append(".");
        }
        indexSqlBuf.append(relConstraintName);

        String deleteIndexSql = indexSqlBuf.toString();

        if (Debug.verboseOn()) Debug.logVerbose("[deleteForeignKeyIndex] index sql=" + deleteIndexSql, module);

        try (Connection connection = getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(deleteIndexSql);
        } catch (SQLException e) {
            String errMsg = "SQL Exception while executing the following:\n" + deleteIndexSql + "\nError was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        } catch (GenericEntityException e) {
            String errMsg = "Unable to establish a connection with the database for helperName [" + this.helperInfo.getHelperFullName() + "]... Error was: " + e.toString();
            Debug.logError(e, errMsg, module);
            return errMsg;
        }
        return null;
    }

    public String getSchemaName(DatabaseMetaData dbData) throws SQLException {
        if (!isLegacy && this.datasourceInfo.getUseSchemas() && dbData.supportsSchemasInTableDefinitions()) {
            if (UtilValidate.isNotEmpty(this.datasourceInfo.getSchemaName())) {
                if (dbData.storesLowerCaseIdentifiers()) {
                    return this.datasourceInfo.getSchemaName().toLowerCase();
                } else if (dbData.storesUpperCaseIdentifiers()) {
                    return this.datasourceInfo.getSchemaName().toUpperCase();
                } else {
                    return this.datasourceInfo.getSchemaName();
                }
            } else {
                return dbData.getUserName();
            }
        }
        return null;
    }

    /* ====================================================================== */
    /* ====================================================================== */
    public void updateCharacterSetAndCollation(ModelEntity entity, List<String> messages) {
        if (entity instanceof ModelViewEntity) {
            return;
        }
        if (UtilValidate.isEmpty(this.datasourceInfo.getCharacterSet()) && UtilValidate.isEmpty(this.datasourceInfo.getCollate())) {
            messages.add("Not setting character-set and collate for entity [" + entity.getEntityName() + "], options not specified in the datasource definition in the entityengine.xml file.");
            return;
        }

        try (Connection connection = getConnectionLogged(messages)) {
            if (connection == null) {
                return;
            }

            StringBuilder sqlTableBuf = new StringBuilder("ALTER TABLE ");
            sqlTableBuf.append(entity.getTableName(this.datasourceInfo));
            //sqlTableBuf.append("");

            // if there is a characterSet, add the CHARACTER SET arg here
            if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
                sqlTableBuf.append(" DEFAULT CHARACTER SET ");
                sqlTableBuf.append(this.datasourceInfo.getCharacterSet());
            }
            // if there is a collate, add the COLLATE arg here
            if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
                sqlTableBuf.append(" COLLATE ");
                sqlTableBuf.append(this.datasourceInfo.getCollate());
            }

            if (Debug.verboseOn()) Debug.logVerbose("[updateCharacterSetAndCollation] character-set and collate sql=" + sqlTableBuf, module);

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sqlTableBuf.toString());
            } catch (SQLException e) {
                String errMsg = "SQL Exception while executing the following:\n" + sqlTableBuf + "\nError was: " + e.toString();
                messages.add(errMsg);
                Debug.logError(errMsg, module);
            }

            Iterator<ModelField> fieldIter = entity.getFieldsIterator();
            while (fieldIter.hasNext()) {
                ModelField field = fieldIter.next();
                ModelFieldType type = modelFieldTypeReader.getModelFieldType(field.getType());
                if (type == null) {
                    messages.add("Field type [" + type + "] not found for field [" + field.getName() + "] of entity [" + entity.getEntityName() + "], not creating table.");
                    continue;
                }
                if (!"String".equals(type.getJavaType()) && !"java.lang.String".equals(type.getJavaType())) {
                    continue;
                }

                StringBuilder sqlBuf = new StringBuilder("ALTER TABLE ");
                sqlBuf.append(entity.getTableName(this.datasourceInfo));
                sqlBuf.append(" MODIFY COLUMN ");
                sqlBuf.append(field.getColName());
                sqlBuf.append(" ");
                sqlBuf.append(type.getSqlType());

                // if there is a characterSet, add the CHARACTER SET arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCharacterSet())) {
                    sqlBuf.append(" CHARACTER SET ");
                    sqlBuf.append(this.datasourceInfo.getCharacterSet());
                }
                // if there is a collate, add the COLLATE arg here
                if (UtilValidate.isNotEmpty(this.datasourceInfo.getCollate())) {
                    sqlBuf.append(" COLLATE ");
                    sqlBuf.append(this.datasourceInfo.getCollate());
                }

                if (field.getIsPk()  || field.getIsNotNull()) {
                    if (this.datasourceInfo.getAlwaysUseConstraintKeyword()) {
                        sqlBuf.append(" CONSTRAINT NOT NULL");
                    } else {
                        sqlBuf.append(" NOT NULL");
                    }
                }

                if (Debug.verboseOn()) Debug.logVerbose("[updateCharacterSetAndCollation] character-set and collate sql=" + sqlBuf, module);
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(sqlBuf.toString());
                } catch (SQLException e) {
                    String errMsg = "SQL Exception while executing the following:\n" + sqlBuf + "\nError was: " + e.toString();
                    messages.add(errMsg);
                    Debug.logError(errMsg, module);
                }
            }
        } catch (SQLException e) {
            Debug.logError(e, module);
        }
    }

    /* ====================================================================== */
    /* ====================================================================== */
    @SuppressWarnings("serial")
    public static class ColumnCheckInfo implements Serializable {
        public String tableName;
        public String columnName;
        public String typeName;
        public int columnSize;
        public int decimalDigits;
        public String isNullable; // YES/NO or "" = ie nobody knows
        public boolean isPk = false;
        public int pkSeq;
        public String pkName;

        public static String fixupTableName(String rawTableName, String lookupSchemaName, boolean needsUpperCase) {
            String tableName = rawTableName;
            // for those databases which do not return the schema name with the table name (pgsql 7.3)
            boolean appendSchemaName = false;
            if (tableName != null && lookupSchemaName != null && !tableName.startsWith(lookupSchemaName + "\\.")) {
                appendSchemaName = true;
            }
            if (needsUpperCase && tableName != null) {
                tableName = tableName.toUpperCase();
            }
            if (appendSchemaName) {
                tableName = lookupSchemaName + "." + tableName;
            }
            return tableName;
        }
    }

    @SuppressWarnings("serial")
    public static class ReferenceCheckInfo implements Serializable {
        public String pkTableName;

        /** Comma separated list of column names in the related tables primary key */
        public String pkColumnName;
        public String fkName;
        public String fkTableName;

        /** Comma separated list of column names in the primary tables foreign keys */
        public String fkColumnName;

        @Override
        public String toString() {
            return "FK Reference from table " + fkTableName + " called " + fkName + " to PK in table " + pkTableName;
        }
    }
}
