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

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.sql.rowset.serial.SerialClob;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericDataSourceException;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericModelException;
import org.apache.ofbiz.entity.GenericNotImplementedException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionParam;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.condition.OrderByList;
import org.apache.ofbiz.entity.config.model.Datasource;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;
import org.apache.ofbiz.entity.model.ModelKeyMap;
import org.apache.ofbiz.entity.model.ModelViewEntity;

/**
 * GenericDAO Utility methods for general tasks
 */
public final class SqlJdbcUtil {
    private static final String MODULE = SqlJdbcUtil.class.getName();

    private static final int CHAR_BUFFER_SIZE = 4096;
    private static Map<String, Integer> fieldTypeMap = new HashMap<>();

    static {
        fieldTypeMap.put("java.lang.String", 1);
        fieldTypeMap.put("String", 1);
        fieldTypeMap.put("java.sql.Timestamp", 2);
        fieldTypeMap.put("Timestamp", 2);
        fieldTypeMap.put("java.sql.Time", 3);
        fieldTypeMap.put("Time", 3);
        fieldTypeMap.put("java.sql.Date", 4);
        fieldTypeMap.put("Date", 4);
        fieldTypeMap.put("java.lang.Integer", 5);
        fieldTypeMap.put("Integer", 5);
        fieldTypeMap.put("java.lang.Long", 6);
        fieldTypeMap.put("Long", 6);
        fieldTypeMap.put("java.lang.Float", 7);
        fieldTypeMap.put("Float", 7);
        fieldTypeMap.put("java.lang.Double", 8);
        fieldTypeMap.put("Double", 8);
        fieldTypeMap.put("java.math.BigDecimal", 9);
        fieldTypeMap.put("BigDecimal", 9);
        fieldTypeMap.put("java.lang.Boolean", 10);
        fieldTypeMap.put("Boolean", 10);

        fieldTypeMap.put("java.lang.Object", 11);
        fieldTypeMap.put("Object", 11);

        fieldTypeMap.put("java.sql.Blob", 12);
        fieldTypeMap.put("Blob", 12);
        fieldTypeMap.put("byte[]", 12);
        fieldTypeMap.put("java.nio.ByteBuffer", 12);
        fieldTypeMap.put("java.nio.HeapByteBuffer", 12);

        fieldTypeMap.put("java.sql.Clob", 13);
        fieldTypeMap.put("Clob", 13);

        fieldTypeMap.put("java.util.Date", 14);

        // all of these treated as Collection
        fieldTypeMap.put("java.util.ArrayList", 15);
        fieldTypeMap.put("java.util.HashSet", 15);
        fieldTypeMap.put("java.util.LinkedHashSet", 15);
        fieldTypeMap.put("java.util.LinkedList", 15);
    }

    private SqlJdbcUtil() {
    }

    /**
     * Makes the FROM clause and when necessary the JOIN clause(s) as well
     */
    public static String makeFromClause(ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader, Datasource datasourceInfo)
            throws GenericEntityException {
        StringBuilder sql = new StringBuilder(" FROM ");

        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(datasourceInfo.getJoinStyle()) || "ansi-no-parenthesis".equals(datasourceInfo.getJoinStyle())) {
                boolean useParenthesis = true;
                if ("ansi-no-parenthesis".equals(datasourceInfo.getJoinStyle())) {
                    useParenthesis = false;
                }

                // FROM clause: in this case will be a bunch of joins that correspond with the view-links

                // BIG NOTE on the JOIN clauses: the order of joins is determined by the order of the
                // view-links; for more flexible order we'll have to figure something else out and
                // extend the DTD for the nested view-link elements or something

                // At this point it is assumed that in each view-link the left hand alias will
                // either be the first alias in the series or will already be in a previous
                // view-link and already be in the big join; SO keep a set of all aliases
                // in the join so far and if the left entity alias isn't there yet, and this
                // isn't the first one, throw an exception
                Set<String> joinedAliasSet = new TreeSet<>();

                // TODO: at view-link read time make sure they are ordered properly so that each
                // left hand alias after the first view-link has already been linked before

                StringBuilder openParens = null;
                if (useParenthesis) {
                    openParens = new StringBuilder();
                }
                StringBuilder restOfStatement = new StringBuilder();

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    // don't put starting parenthesis
                    if (i > 0 && useParenthesis) {
                        openParens.append('(');
                    }

                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    if (i == 0) {
                        // this is the first referenced member alias, so keep track of it for future use...
                        restOfStatement.append(makeViewTable(linkEntity, modelFieldTypeReader, datasourceInfo));
                        // another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                        restOfStatement.append(" ");
                        restOfStatement.append(viewLink.getEntityAlias());

                        joinedAliasSet.add(viewLink.getEntityAlias());
                    } else {
                        // make sure the left entity alias is already in the join...
                        if (!joinedAliasSet.contains(viewLink.getEntityAlias())) {
                            throw new GenericModelException("Tried to link the " + viewLink.getEntityAlias() + " alias to the "
                                    + viewLink.getRelEntityAlias() + " alias of the " + modelViewEntity.getEntityName()
                                    + " view-entity, but it is not the first view-link and has not been included in a previous view-link."
                                    + "In other words, the left/main alias isn't connected to the rest of the member-entities yet.");
                        }
                    }
                    // now put the rel (right) entity alias into the set that is in the join
                    joinedAliasSet.add(viewLink.getRelEntityAlias());

                    if (viewLink.isRelOptional()) {
                        restOfStatement.append(" LEFT OUTER JOIN ");
                    } else {
                        restOfStatement.append(" INNER JOIN ");
                    }

                    restOfStatement.append(makeViewTable(relLinkEntity, modelFieldTypeReader, datasourceInfo));
                    // another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                    restOfStatement.append(" ");
                    restOfStatement.append(viewLink.getRelEntityAlias());
                    restOfStatement.append(" ON ");

                    StringBuilder condBuffer = new StringBuilder();

                    ModelViewEntity.ViewEntityCondition viewEntityCondition = viewLink.getViewEntityCondition();
                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        if (linkField == null) {
                            throw new GenericModelException("Invalid field name in view-link key-map for the " + viewLink.getEntityAlias()
                                    + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName()
                                    + " view-entity; the field [" + keyMap.getFieldName() + "] does not exist on the [" + linkEntity.getEntityName()
                                    + "] entity.");
                        }
                        ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());
                        if (relLinkField == null) {
                            throw new GenericModelException("Invalid related field name in view-link key-map for the " + viewLink.getEntityAlias()
                                    + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName()
                                    + " view-entity; the field [" + keyMap.getRelFieldName() + "] does not exist on the ["
                                    + relLinkEntity.getEntityName() + "] entity.");
                        }

                        if (condBuffer.length() > 0) {
                            condBuffer.append(" AND ");
                        }

                        condBuffer.append(viewLink.getEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(linkField.getColName());

                        condBuffer.append(" = ");

                        condBuffer.append(viewLink.getRelEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(relLinkField.getColName());
                    }
                    if (condBuffer.length() == 0 && viewEntityCondition == null) {
                        throw new GenericModelException("No view-link/join key-maps found for the " + viewLink.getEntityAlias() + " and the "
                                + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity.");
                    }

                    if (viewEntityCondition != null) {
                        EntityCondition whereCondition = viewEntityCondition.getWhereCondition(modelFieldTypeReader, null);
                        if (condBuffer.length() > 0) {
                            condBuffer.append(" AND ");
                        }
                        condBuffer.append(whereCondition.makeWhereString(modelEntity, null, datasourceInfo));
                    }

                    restOfStatement.append(condBuffer.toString());

                    // don't put ending parenthesis
                    if (i < (modelViewEntity.getViewLinksSize() - 1) && useParenthesis) {
                        restOfStatement.append(')');
                    }
                }

                if (useParenthesis) {
                    sql.append(openParens.toString());
                }
                sql.append(restOfStatement.toString());

                // handle tables not included in view-link
                boolean fromEmpty = restOfStatement.length() == 0;
                for (String aliasName : modelViewEntity.getMemberModelMemberEntities().keySet()) {
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity(aliasName);

                    if (!joinedAliasSet.contains(aliasName)) {
                        if (!fromEmpty) {
                            sql.append(", ");
                        }
                        fromEmpty = false;

                        sql.append(makeViewTable(fromEntity, modelFieldTypeReader, datasourceInfo));
                        sql.append(" ");
                        sql.append(aliasName);
                    }
                }

            } else if ("theta-oracle".equals(datasourceInfo.getJoinStyle()) || "theta-mssql".equals(datasourceInfo.getJoinStyle())) {
                // FROM clause
                Iterator<String> meIter = modelViewEntity.getMemberModelMemberEntities().keySet().iterator();

                while (meIter.hasNext()) {
                    String aliasName = meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity(aliasName);

                    sql.append(makeViewTable(fromEntity, modelFieldTypeReader, datasourceInfo));
                    sql.append(" ");
                    sql.append(aliasName);
                    if (meIter.hasNext()) {
                        sql.append(", ");
                    }
                }

                // JOIN clause(s): none needed, all the work done in the where clause for theta-oracle
            } else {
                throw new GenericModelException("The join-style " + datasourceInfo.getJoinStyle() + " is not yet supported");
            }
        } else {
            sql.append(modelEntity.getTableName(datasourceInfo));
        }
        return sql.toString();
    }

    /**
     * Makes a WHERE clause String with "&lt;col name&gt;=?" if not null or "&lt;col name&gt; IS null" if null, all AND separated
     */
    @Deprecated
    public static String makeWhereStringFromFields(List<ModelField> modelFields, Map<String, Object> fields, String operator) {
        return makeWhereStringFromFields(new StringBuilder(), modelFields, fields, operator, null).toString();
    }

    public static StringBuilder makeWhereStringFromFields(StringBuilder sb, List<ModelField> modelFields, Map<String, Object> fields,
            String operator) {
        return makeWhereStringFromFields(sb, modelFields, fields, operator, null);
    }

    /**
     * Makes a WHERE clause String with "&lt;col name&gt;=?" if not null or "&lt;col name&gt; IS null" if null, all AND separated
     */
    @Deprecated
    public static String makeWhereStringFromFields(List<ModelField> modelFields, Map<String, Object> fields, String operator,
            List<EntityConditionParam> entityConditionParams) {
        return makeWhereStringFromFields(new StringBuilder(), modelFields, fields, operator, entityConditionParams).toString();
    }

    /**
     * Makes a WHERE clause String with "&lt;col name&gt;=?" if not null or "&lt;col name&gt; IS null" if null, all AND separated
     */
    public static StringBuilder makeWhereStringFromFields(StringBuilder sb, List<ModelField> modelFields, Map<String, Object> fields,
            String operator, List<EntityConditionParam> entityConditionParams) {
        if (modelFields.size() < 1) {
            return sb;
        }

        Iterator<ModelField> iter = modelFields.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            Object name = null;
            ModelField modelField = null;
            if (item instanceof ModelField) {
                modelField = (ModelField) item;
                sb.append(modelField.getColValue());
                name = modelField.getName();
            } else {
                sb.append(item);
                name = item;
            }

            Object fieldValue = fields.get(name);
            if (fieldValue != null && fieldValue != GenericEntity.NULL_FIELD) {
                sb.append('=');
                addValue(sb, modelField, fieldValue, entityConditionParams);
            } else {
                sb.append(" IS NULL");
            }

            if (iter.hasNext()) {
                sb.append(' ');
                sb.append(operator);
                sb.append(' ');
            }
        }

        return sb;
    }

    public static String makeWhereClause(ModelEntity modelEntity, List<ModelField> modelFields, Map<String, Object> fields, String operator,
            String joinStyle) throws GenericEntityException {
        StringBuilder whereString = new StringBuilder("");

        if (UtilValidate.isNotEmpty(modelFields)) {
            whereString.append(makeWhereStringFromFields(modelFields, fields, "AND"));
        }

        String viewClause = makeViewWhereClause(modelEntity, joinStyle);

        if (!viewClause.isEmpty()) {
            if (whereString.length() > 0) {
                whereString.append(' ');
                whereString.append(operator);
                whereString.append(' ');
            }

            whereString.append(viewClause);
        }

        if (whereString.length() > 0) {
            return " WHERE " + whereString.toString();
        }

        return "";
    }

    public static String makeViewWhereClause(ModelEntity modelEntity, String joinStyle) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            StringBuilder whereString = new StringBuilder();
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(joinStyle) || "ansi-no-parenthesis".equals(joinStyle)) {
                Debug.logVerbose("Nothing to do here, all done in the JOIN clauses...", MODULE);
            } else if ("theta-oracle".equals(joinStyle) || "theta-mssql".equals(joinStyle)) {
                boolean isOracleStyle = "theta-oracle".equals(joinStyle);
                boolean isMssqlStyle = "theta-mssql".equals(joinStyle);

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    if (linkEntity == null) {
                        throw new GenericEntityException("Link entity not found with alias: " + viewLink.getEntityAlias() + " for entity: "
                                + modelViewEntity.getEntityName());
                    }

                    if (relLinkEntity == null) {
                        throw new GenericEntityException("Rel-Link entity not found with alias: " + viewLink.getRelEntityAlias() + " for entity: "
                                + modelViewEntity.getEntityName());
                    }

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());

                        if (whereString.length() > 0) {
                            whereString.append(" AND ");
                        }
                        whereString.append(viewLink.getEntityAlias());
                        whereString.append(".");
                        whereString.append(linkField.getColName());

                        // check to see whether the left or right members are optional, if so:
                        // oracle: use the (+) on the optional side
                        // mssql: use the * on the required side

                        // NOTE: not testing if original table is optional, ONLY if related table is optional; otherwise things get really ugly...
                        // if (isOracleStyle && linkMemberEntity.getOptional()) whereString.append(" (+) ");
                        if (isMssqlStyle && viewLink.isRelOptional()) {
                            whereString.append("*");
                        }
                        whereString.append("=");
                        // if (isMssqlStyle && linkMemberEntity.getOptional()) whereString.append("*");
                        if (isOracleStyle && viewLink.isRelOptional()) {
                            whereString.append(" (+) ");
                        }

                        whereString.append(viewLink.getRelEntityAlias());
                        whereString.append(".");
                        whereString.append(relLinkField.getColName());
                    }
                }
            } else {
                throw new GenericModelException("The join-style " + joinStyle + " is not supported");
            }

            if (whereString.length() > 0) {
                return "(" + whereString.toString() + ")";
            }
        }
        return "";
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, Datasource datasourceInfo) throws GenericModelException {
        return makeOrderByClause(modelEntity, orderBy, false, datasourceInfo);
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List<String> orderBy, boolean includeTablenamePrefix, Datasource datasourceInfo)
            throws GenericModelException {
        StringBuilder sql = new StringBuilder("");

        if (UtilValidate.isNotEmpty(orderBy)) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Order by list contains: " + orderBy.size() + " entries.", MODULE);
            }
            OrderByList orderByList = new OrderByList(orderBy);
            orderByList.checkOrderBy(modelEntity);
            orderByList.makeOrderByString(sql, modelEntity, includeTablenamePrefix, datasourceInfo);
        }
        if (Debug.verboseOn()) {
            Debug.logVerbose("makeOrderByClause: " + sql.toString(), MODULE);
        }
        return sql.toString();
    }

    public static String makeViewTable(ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader, Datasource datasourceInfo)
            throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            StringBuilder sql = new StringBuilder("(SELECT ");
            Iterator<ModelField> fieldsIter = modelEntity.getFieldsIterator();
            if (fieldsIter.hasNext()) {
                ModelField curField = fieldsIter.next();
                sql.append(curField.getColValue());
                sql.append(" AS ");
                sql.append(curField.getColName());
                while (fieldsIter.hasNext()) {
                    curField = fieldsIter.next();
                    sql.append(", ");
                    sql.append(curField.getColValue());
                    sql.append(" AS ");
                    sql.append(curField.getColName());
                }
            }
            sql.append(makeFromClause(modelEntity, modelFieldTypeReader, datasourceInfo));
            String viewWhereClause = makeViewWhereClause(modelEntity, datasourceInfo.getJoinStyle());
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            List<EntityCondition> whereConditions = new LinkedList<>();
            List<EntityCondition> havingConditions = new LinkedList<>();
            List<String> orderByList = new LinkedList<>();

            modelViewEntity.populateViewEntityConditionInformation(modelFieldTypeReader, whereConditions, havingConditions, orderByList, null);
            String viewConditionClause;
            if (!whereConditions.isEmpty()) {
                viewConditionClause = EntityCondition.makeCondition(whereConditions, EntityOperator.AND).makeWhereString(modelViewEntity, null,
                        datasourceInfo);
            } else {
                viewConditionClause = null;
            }
            if (UtilValidate.isNotEmpty(viewWhereClause) || UtilValidate.isNotEmpty(viewConditionClause)) {
                sql.append(" WHERE ");
                if (UtilValidate.isNotEmpty(viewWhereClause)) {
                    sql.append("(").append(viewWhereClause).append(")");
                    if (UtilValidate.isNotEmpty(viewConditionClause)) {
                        sql.append(" AND ");
                    }
                }
                if (UtilValidate.isNotEmpty(viewConditionClause)) {
                    sql.append("(").append(viewConditionClause).append(")");
                }
            }
            // FIXME: handling HAVING, don't need ORDER BY for nested views
            modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), sql, " GROUP BY ", ", ", "", false);

            sql.append(")");
            return sql.toString();
        } else {
            return modelEntity.getTableName(datasourceInfo);
        }
    }

    public static String filterColName(String colName) {
        return colName.replace('.', '_').replace('(', '_').replace(')', '_');
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     * The elements (ModelFields) of the list are bound to an SQL statement (SQL-Processor)
     * @param sqlP
     * @param list
     * @param entity
     * @throws GenericEntityException
     */
    public static void setValues(SQLProcessor sqlP, List<ModelField> list, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        for (ModelField curField : list) {
            setValue(sqlP, curField, entity, modelFieldTypeReader);
        }
    }

    /**
     * The elements (ModelFields) of the list are bound to an SQL statement (SQL-Processor), but values must not be null.
     * @param sqlP
     * @param list
     * @param dummyValue
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValuesWhereClause(SQLProcessor sqlP, List<ModelField> list, GenericValue dummyValue,
            ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        for (ModelField curField : list) {
            // for where clause variables only setValue if not null...
            if (dummyValue.get(curField.getName()) != null) {
                setValue(sqlP, curField, dummyValue, modelFieldTypeReader);
            }
        }
    }

    /**
     * Get all primary keys from the model entity and bind their values to the an SQL statement (SQL-Processor)
     * @param sqlP
     * @param modelEntity
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setPkValues(SQLProcessor sqlP, ModelEntity modelEntity, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        Iterator<ModelField> pksIter = modelEntity.getPksIterator();
        while (pksIter.hasNext()) {
            ModelField curField = pksIter.next();

            // for where clause variables only setValue if not null...
            if (entity.dangerousGetNoCheckButFast(curField) != null) {
                setValue(sqlP, curField, entity, modelFieldTypeReader);
            }
        }
    }

    public static void getValue(ResultSet rs, int ind, ModelField curField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(curField.getType());

        if (mft == null) {
            throw new GenericModelException("definition fieldType " + curField.getType() + " not found, cannot getValue for field "
                    + entity.getEntityName() + "." + curField.getName() + ".");
        }

        ModelEntity model = entity.getModelEntity();
        while (curField.getEncryptMethod().isEncrypted() && model instanceof ModelViewEntity) {
            ModelViewEntity modelView = (ModelViewEntity) model;
            String entityName = modelView.getAliasedEntity(
                    modelView.getAlias(curField.getName()).getEntityAlias(), entity.getDelegator().getModelReader()).getEntityName();
            model = entity.getDelegator().getModelEntity(entityName);
        }
        String encryptionKeyName = model.getEntityName();

        // ----- Try out the new handler code -----

        JdbcValueHandler<?> handler = mft.getJdbcValueHandler();
        if (handler != null) {
            try {
                Object jdbcValue = handler.getValue(rs, ind);
                if (jdbcValue instanceof String && curField.getEncryptMethod().isEncrypted()) {
                    jdbcValue = entity.getDelegator().decryptFieldValue(encryptionKeyName, curField.getEncryptMethod(), (String) jdbcValue);
                }
                entity.dangerousSetNoCheckButFast(curField, jdbcValue);
                return;
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        } else {
            Debug.logWarning("JdbcValueHandler not found for java-type " + mft.getJavaType()
                    + ", falling back on switch statement. Entity = "
                    + curField.getModelEntity().getEntityName()
                    + ", field = " + curField.getName() + ".", MODULE);
        }

        // ------------------------------------------

        String fieldType = mft.getJavaType();

        try {
            // checking to see if the object is null is really only necessary for the numbers
            int typeValue = getType(fieldType);
            ResultSetMetaData rsmd = rs.getMetaData();
            int colType = rsmd.getColumnType(ind);

            if (typeValue <= 4 || typeValue >= 11) {
                switch (typeValue) {
                case 1:
                    if (java.sql.Types.CLOB == colType) {
                        // Debug.logInfo("For field " + curField.getName() + " of entity " + entity.getEntityName() + " getString is a CLOB,
                        // trying getCharacterStream", MODULE);
                        // if the String is empty, try to get a text input stream, this is required for some databases for larger fields, like CLOBs

                        Clob valueClob = rs.getClob(ind);
                        Reader valueReader = null;
                        if (valueClob != null) {
                            valueReader = valueClob.getCharacterStream();
                        }
                        if (valueReader != null) {
                            char[] inCharBuffer = new char[CHAR_BUFFER_SIZE];
                            StringBuilder strBuf = new StringBuilder();
                            int charsRead = 0;
                            try {
                                while ((charsRead = valueReader.read(inCharBuffer, 0, CHAR_BUFFER_SIZE)) > 0) {
                                    strBuf.append(inCharBuffer, 0, charsRead);
                                }
                                valueReader.close();
                            } catch (IOException e) {
                                throw new GenericEntityException("Error reading long character stream for field " + curField.getName()
                                        + " of entity " + entity.getEntityName(), e);
                            }
                            entity.dangerousSetNoCheckButFast(curField, strBuf.toString());
                        } else {
                            entity.dangerousSetNoCheckButFast(curField, null);
                        }
                    } else {
                        String value = rs.getString(ind);
                        if (curField.getEncryptMethod().isEncrypted()) {
                            value = (String) entity.getDelegator().decryptFieldValue(encryptionKeyName, curField.getEncryptMethod(), value);
                        }
                        entity.dangerousSetNoCheckButFast(curField, value);
                    }
                    break;

                case 2:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTimestamp(ind));
                    break;

                case 3:
                    entity.dangerousSetNoCheckButFast(curField, rs.getTime(ind));
                    break;

                case 4:
                    entity.dangerousSetNoCheckButFast(curField, rs.getDate(ind));
                    break;

                case 11:
                    entity.dangerousSetNoCheckButFast(curField, rs.getBytes(ind));
                    break;

                case 12:
                    entity.dangerousSetNoCheckButFast(curField, rs.getBlob(ind));
                    break;

                case 13:
                    entity.dangerousSetNoCheckButFast(curField, new SerialClob(rs.getClob(ind)));
                    break;
                case 14:
                case 15:
                    entity.dangerousSetNoCheckButFast(curField, rs.getObject(ind));
                    break;
                }
            } else {
                switch (typeValue) {
                case 5:
                    int intValue = rs.getInt(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, intValue);
                    }
                    break;

                case 6:
                    long longValue = rs.getLong(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, longValue);
                    }
                    break;

                case 7:
                    float floatValue = rs.getFloat(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, floatValue);
                    }
                    break;

                case 8:
                    double doubleValue = rs.getDouble(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, doubleValue);
                    }
                    break;

                case 9:
                    BigDecimal bigDecimalValue = rs.getBigDecimal(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, bigDecimalValue);
                    }
                    break;

                case 10:
                    boolean booleanValue = rs.getBoolean(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, booleanValue);
                    }
                    break;
                }
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while getting value : " + curField.getName() + " [" + curField.getColName()
                    + "] (" + ind + ")", sqle);
        }
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader)
            throws GenericEntityException {
        Object fieldValue = entity.dangerousGetNoCheckButFast(modelField);

        setValue(sqlP, modelField, entity.getEntityName(), fieldValue, modelFieldTypeReader);
    }

    public static <T> void setValue(SQLProcessor sqlP, ModelField modelField, String entityName, Object fieldValue,
            ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(modelField.getType());

        if (mft == null) {
            throw new GenericModelException("GenericDAO.getValue: definition fieldType " + modelField.getType()
                    + " not found, cannot setValue for field "
                    + entityName + "." + modelField.getName() + ".");
        }

        // if the value is the GenericEntity.NullField, treat as null
        if (fieldValue == GenericEntity.NULL_FIELD) {
            fieldValue = null;
        }

        // ----- Try out the new handler code -----

        ModelField.EncryptMethod encryptMethod = modelField.getEncryptMethod();
        if (encryptMethod.isEncrypted()) {
            fieldValue = sqlP.getDelegator().encryptFieldValue(entityName, encryptMethod, fieldValue);
        }
        JdbcValueHandler<T> handler = UtilGenerics.cast(mft.getJdbcValueHandler());
        if (handler != null) {
            try {
                sqlP.setValue(handler, handler.getJavaClass().cast(fieldValue));
                return;
            } catch (SQLException e) {
                throw new GenericDataSourceException("SQL Exception while setting value on field [" + modelField.getName() + "] of entity "
                        + entityName + ": ", e);
            }
        } else {
            Debug.logWarning("JdbcValueHandler not found for java-type " + mft.getJavaType()
                    + ", falling back on switch statement. Entity = "
                    + modelField.getModelEntity().getEntityName()
                    + ", field = " + modelField.getName() + ".", MODULE);
        }

        // ------------------------------------------

        String fieldType = mft.getJavaType();
        if (fieldValue != null) {
            if (!ObjectType.instanceOf(fieldValue, fieldType)) {
                // this is only an info level message because under normal operation for most JDBC
                // drivers this will be okay, but if not then the JDBC driver will throw an exception
                // and when lower debug levels are on this should help give more info on what happened
                String fieldClassName = fieldValue.getClass().getName();
                if (fieldValue instanceof byte[]) {
                    fieldClassName = "byte[]";
                }

                if (Debug.verboseOn()) {
                    Debug.logVerbose("type of field " + entityName + "." + modelField.getName()
                            + " is " + fieldClassName + ", was expecting " + mft.getJavaType() + "; this may "
                            + "indicate an error in the configuration or in the class, and may result "
                            + "in an SQL-Java data conversion error. Will use the real field type: "
                            + fieldClassName + ", not the definition.", MODULE);
                }
                fieldType = fieldClassName;
            }
        }

        try {
            int typeValue = getType(fieldType);

            switch (typeValue) {
            case 1:
                sqlP.setValue((String) fieldValue);
                break;

            case 2:
                sqlP.setValue((java.sql.Timestamp) fieldValue);
                break;

            case 3:
                sqlP.setValue((java.sql.Time) fieldValue);
                break;

            case 4:
                sqlP.setValue((java.sql.Date) fieldValue);
                break;

            case 5:
                sqlP.setValue((java.lang.Integer) fieldValue);
                break;

            case 6:
                sqlP.setValue((java.lang.Long) fieldValue);
                break;

            case 7:
                sqlP.setValue((java.lang.Float) fieldValue);
                break;

            case 8:
                sqlP.setValue((java.lang.Double) fieldValue);
                break;

            case 9:
                sqlP.setValue((java.math.BigDecimal) fieldValue);
                break;

            case 10:
                sqlP.setValue((java.lang.Boolean) fieldValue);
                break;

            case 11:
                sqlP.setBinaryStream(fieldValue);
                break;

            case 12:
                if (fieldValue instanceof byte[]) {
                    sqlP.setBytes((byte[]) fieldValue);
                } else if (fieldValue instanceof ByteBuffer) {
                    sqlP.setBytes(((ByteBuffer) fieldValue).array());
                } else {
                    sqlP.setValue((java.sql.Blob) fieldValue);
                }
                break;

            case 13:
                sqlP.setValue((java.sql.Clob) fieldValue);
                break;

            case 14:
                if (fieldValue != null) {
                    sqlP.setValue(new java.sql.Date(((java.util.Date) fieldValue).getTime()));
                } else {
                    sqlP.setValue((java.sql.Date) null);
                }
                break;

            case 15:
                sqlP.setValue(UtilGenerics.<Collection<?>>cast(fieldValue));
                break;
            }
        } catch (GenericNotImplementedException e) {
            throw new GenericNotImplementedException("Not Implemented Exception while setting value on field [" + modelField.getName()
                    + "] of entity " + entityName + ": " + e.toString(), e);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while setting value on field [" + modelField.getName() + "] of entity "
                    + entityName + ": ", sqle);
        }
    }

    public static int getType(String fieldType) throws GenericNotImplementedException {
        Integer val = fieldTypeMap.get(fieldType);

        if (val == null) {
            throw new GenericNotImplementedException("Java type " + fieldType + " not currently supported. Sorry.");
        }
        return val;
    }

    public static void addValueSingle(StringBuffer buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        StringBuilder sb = new StringBuilder();
        addValueSingle(sb, field, value, params);
        buffer.append(sb);
    }

    public static void addValueSingle(StringBuilder buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        if (field != null) {
            buffer.append('?');
        } else if (value instanceof Number) {
            buffer.append(value);
        } else {
            buffer.append('\'');
            if (value instanceof String) {
                buffer.append(((String) value).replaceAll("'", "''"));
            } else {
                buffer.append(value);
            }
            buffer.append('\'');
        }
        if (field != null && params != null) {
            params.add(new EntityConditionParam(field, value));
        }
    }

    public static void addValue(StringBuffer buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        StringBuilder sb = new StringBuilder();
        addValue(sb, field, value, params);
        buffer.append(sb);
    }

    public static void addValue(StringBuilder buffer, ModelField field, Object value, List<EntityConditionParam> params) {
        if (value instanceof Collection<?>) {
            buffer.append("(");
            Collection<Object> coll = UtilGenerics.cast(value);
            Iterator<Object> it = coll.iterator();
            while (it.hasNext()) {
                Object thisValue = it.next();
                addValueSingle(buffer, field, thisValue, params);
                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append(")");
        } else {
            addValueSingle(buffer, field, value, params);
        }
    }
}
