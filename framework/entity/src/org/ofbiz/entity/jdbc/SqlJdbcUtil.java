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
package org.ofbiz.entity.jdbc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Clob;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.GenericNotImplementedException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionParam;
import org.ofbiz.entity.condition.OrderByList;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.datasource.GenericDAO;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;
import org.ofbiz.entity.model.ModelFieldTypeReader;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.model.ModelViewEntity;

/**
 * GenericDAO Utility methods for general tasks
 *
 */
public class SqlJdbcUtil {
    public static final String module = GenericDAO.class.getName();
    
    public static final int CHAR_BUFFER_SIZE = 4096;

    /** Makes the FROM clause and when necessary the JOIN clause(s) as well */
    public static String makeFromClause(ModelEntity modelEntity, DatasourceInfo datasourceInfo) throws GenericEntityException {
        StringBuffer sql = new StringBuffer(" FROM ");

        if (modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(datasourceInfo.joinStyle) || "ansi-no-parenthesis".equals(datasourceInfo.joinStyle)) {
                boolean useParenthesis = true;
                if ("ansi-no-parenthesis".equals(datasourceInfo.joinStyle)) {
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
                Set joinedAliasSet = new TreeSet();

                // TODO: at view-link read time make sure they are ordered properly so that each
                // left hand alias after the first view-link has already been linked before

                StringBuffer openParens = null;
                if (useParenthesis) openParens = new StringBuffer();
                StringBuffer restOfStatement = new StringBuffer();

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    // don't put starting parenthesis
                    if (i > 0 && useParenthesis) openParens.append('(');

                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    // ModelViewEntity.ModelMemberEntity linkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getEntityAlias());
                    // ModelViewEntity.ModelMemberEntity relLinkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getRelEntityAlias());

                    if (i == 0) {
                        // this is the first referenced member alias, so keep track of it for future use...
                        restOfStatement.append(makeViewTable(linkEntity, datasourceInfo));
                        //another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                        restOfStatement.append(" ");
                        restOfStatement.append(viewLink.getEntityAlias());

                        joinedAliasSet.add(viewLink.getEntityAlias());
                    } else {
                        // make sure the left entity alias is already in the join...
                        if (!joinedAliasSet.contains(viewLink.getEntityAlias())) {
                            throw new GenericModelException("Tried to link the " + viewLink.getEntityAlias() + " alias to the " + viewLink.getRelEntityAlias() + " alias of the " + modelViewEntity.getEntityName() + " view-entity, but it is not the first view-link and has not been included in a previous view-link. In other words, the left/main alias isn't connected to the rest of the member-entities yet.");
                        }
                    }
                    // now put the rel (right) entity alias into the set that is in the join
                    joinedAliasSet.add(viewLink.getRelEntityAlias());

                    if (viewLink.isRelOptional()) {
                        restOfStatement.append(" LEFT OUTER JOIN ");
                    } else {
                        restOfStatement.append(" INNER JOIN ");
                    }

                    restOfStatement.append(makeViewTable(relLinkEntity, datasourceInfo));
                    //another possible one that some dbs might need, but not sure of any yet: restOfStatement.append(" AS ");
                    restOfStatement.append(" ");
                    restOfStatement.append(viewLink.getRelEntityAlias());
                    restOfStatement.append(" ON ");

                    StringBuffer condBuffer = new StringBuffer();

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        if (linkField == null) {
                            throw new GenericModelException("Invalid field name in view-link key-map for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity; the field [" + keyMap.getFieldName() + "] does not exist on the [" + linkEntity.getEntityName() + "] entity.");
                        }
                        ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());
                        if (relLinkField == null) {
                            throw new GenericModelException("Invalid related field name in view-link key-map for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity; the field [" + keyMap.getRelFieldName() + "] does not exist on the [" + relLinkEntity.getEntityName() + "] entity.");
                        }

                        if (condBuffer.length() > 0) {
                            condBuffer.append(" AND ");
                        }
                        
                        condBuffer.append(viewLink.getEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(filterColName(linkField.getColName()));

                        condBuffer.append(" = ");

                        condBuffer.append(viewLink.getRelEntityAlias());
                        condBuffer.append(".");
                        condBuffer.append(filterColName(relLinkField.getColName()));
                    }
                    if (condBuffer.length() == 0) {
                        throw new GenericModelException("No view-link/join key-maps found for the " + viewLink.getEntityAlias() + " and the " + viewLink.getRelEntityAlias() + " member-entities of the " + modelViewEntity.getEntityName() + " view-entity.");
                    }
                    restOfStatement.append(condBuffer.toString());

                    // don't put ending parenthesis
                    if (i < (modelViewEntity.getViewLinksSize() - 1) && useParenthesis) restOfStatement.append(')');
                }

                if (useParenthesis) sql.append(openParens.toString());
                sql.append(restOfStatement.toString());

                // handle tables not included in view-link
                Iterator meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();
                boolean fromEmpty = restOfStatement.length() == 0;

                while (meIter.hasNext()) {
                    Map.Entry entry = (Map.Entry) meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity((String) entry.getKey());

                    if (!joinedAliasSet.contains((String) entry.getKey())) {
                        if (!fromEmpty) sql.append(", ");
                        fromEmpty = false;

                        sql.append(makeViewTable(fromEntity, datasourceInfo));
                        sql.append(" ");
                        sql.append((String) entry.getKey());
                    }
                }


            } else if ("theta-oracle".equals(datasourceInfo.joinStyle) || "theta-mssql".equals(datasourceInfo.joinStyle)) {
                // FROM clause
                Iterator meIter = modelViewEntity.getMemberModelMemberEntities().entrySet().iterator();

                while (meIter.hasNext()) {
                    Map.Entry entry = (Map.Entry) meIter.next();
                    ModelEntity fromEntity = modelViewEntity.getMemberModelEntity((String) entry.getKey());

                    sql.append(makeViewTable(fromEntity, datasourceInfo));
                    sql.append(" ");
                    sql.append((String) entry.getKey());
                    if (meIter.hasNext()) sql.append(", ");
                }

                // JOIN clause(s): none needed, all the work done in the where clause for theta-oracle
            } else {
                throw new GenericModelException("The join-style " + datasourceInfo.joinStyle + " is not yet supported");
            }
        } else {
            sql.append(modelEntity.getTableName(datasourceInfo));
        }
        return sql.toString();
    }

    /** Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS null" if null, all AND separated */
    public static String makeWhereStringFromFields(List modelFields, Map fields, String operator) {
        return makeWhereStringFromFields(modelFields, fields, operator, null);
    }

    /** Makes a WHERE clause String with "<col name>=?" if not null or "<col name> IS null" if null, all AND separated */
    public static String makeWhereStringFromFields(List modelFields, Map fields, String operator, List entityConditionParams) {
        if (modelFields.size() < 1) {
            return "";
        }

        StringBuffer returnString = new StringBuffer("");
        Iterator iter = modelFields.iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            Object name = null;
            ModelField modelField = null;
            if (item instanceof ModelField) {
                modelField = (ModelField) item;
                returnString.append(modelField.getColName());
                name = modelField.getName();
            } else {
                returnString.append(item);
                name = item;
            }

            Object fieldValue = fields.get(name);
            if (fieldValue != null && fieldValue != GenericEntity.NULL_FIELD) {
                returnString.append('=');
                addValue(returnString, modelField, fieldValue, entityConditionParams);
            } else {
                returnString.append(" IS NULL");
            }

            if (iter.hasNext()) {
                returnString.append(' ');
                returnString.append(operator);
                returnString.append(' ');
            }
        }

        return returnString.toString();
    }

    public static String makeWhereClause(ModelEntity modelEntity, List modelFields, Map fields, String operator, String joinStyle) throws GenericEntityException {
        StringBuffer whereString = new StringBuffer("");

        if (modelFields != null && modelFields.size() > 0) {
            whereString.append(makeWhereStringFromFields(modelFields, fields, "AND"));
        }

        String viewClause = makeViewWhereClause(modelEntity, joinStyle);

        if (viewClause.length() > 0) {
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
            StringBuffer whereString = new StringBuffer();
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;

            if ("ansi".equals(joinStyle) || "ansi-no-parenthesis".equals(joinStyle)) {
                // nothing to do here, all done in the JOIN clauses
            } else if ("theta-oracle".equals(joinStyle) || "theta-mssql".equals(joinStyle)) {
                boolean isOracleStyle = "theta-oracle".equals(joinStyle);
                boolean isMssqlStyle = "theta-mssql".equals(joinStyle);

                for (int i = 0; i < modelViewEntity.getViewLinksSize(); i++) {
                    ModelViewEntity.ModelViewLink viewLink = modelViewEntity.getViewLink(i);

                    ModelEntity linkEntity = modelViewEntity.getMemberModelEntity(viewLink.getEntityAlias());
                    ModelEntity relLinkEntity = modelViewEntity.getMemberModelEntity(viewLink.getRelEntityAlias());

                    if (linkEntity == null) {
                        throw new GenericEntityException("Link entity not found with alias: " + viewLink.getEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    if (relLinkEntity == null) {
                        throw new GenericEntityException("Rel-Link entity not found with alias: " + viewLink.getRelEntityAlias() + " for entity: " + modelViewEntity.getEntityName());
                    }

                    // ModelViewEntity.ModelMemberEntity linkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getEntityAlias());
                    // ModelViewEntity.ModelMemberEntity relLinkMemberEntity = modelViewEntity.getMemberModelMemberEntity(viewLink.getRelEntityAlias());

                    for (int j = 0; j < viewLink.getKeyMapsSize(); j++) {
                        ModelKeyMap keyMap = viewLink.getKeyMap(j);
                        ModelField linkField = linkEntity.getField(keyMap.getFieldName());
                        ModelField relLinkField = relLinkEntity.getField(keyMap.getRelFieldName());

                        if (whereString.length() > 0) {
                            whereString.append(" AND ");
                        }
                        whereString.append(viewLink.getEntityAlias());
                        whereString.append(".");
                        whereString.append(filterColName(linkField.getColName()));

                        // check to see whether the left or right members are optional, if so:
                        // oracle: use the (+) on the optional side
                        // mssql: use the * on the required side

                        // NOTE: not testing if original table is optional, ONLY if related table is optional; otherwise things get really ugly...
                        // if (isOracleStyle && linkMemberEntity.getOptional()) whereString.append(" (+) ");
                        if (isMssqlStyle && viewLink.isRelOptional()) whereString.append("*");
                        whereString.append("=");
                        // if (isMssqlStyle && linkMemberEntity.getOptional()) whereString.append("*");
                        if (isOracleStyle && viewLink.isRelOptional()) whereString.append(" (+) ");

                        whereString.append(viewLink.getRelEntityAlias());
                        whereString.append(".");
                        whereString.append(filterColName(relLinkField.getColName()));
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

    public static String makeOrderByClause(ModelEntity modelEntity, List orderBy, DatasourceInfo datasourceInfo) throws GenericModelException {
        return makeOrderByClause(modelEntity, orderBy, false, datasourceInfo);
    }

    public static String makeOrderByClause(ModelEntity modelEntity, List orderBy, boolean includeTablenamePrefix, DatasourceInfo datasourceInfo) throws GenericModelException {
        StringBuffer sql = new StringBuffer("");
        //String fieldPrefix = includeTablenamePrefix ? (modelEntity.getTableName(datasourceInfo) + ".") : "";

        if (orderBy != null && orderBy.size() > 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Order by list contains: " + orderBy.size() + " entries.", module);
            OrderByList orderByList = new OrderByList(orderBy);
            orderByList.checkOrderBy(modelEntity);
            orderByList.makeOrderByString(sql, modelEntity, includeTablenamePrefix, datasourceInfo);
        }
        if (Debug.verboseOn()) Debug.logVerbose("makeOrderByClause: " + sql.toString(), module);
        return sql.toString();
    }

    public static String makeViewTable(ModelEntity modelEntity, DatasourceInfo datasourceInfo) throws GenericEntityException {
        if (modelEntity instanceof ModelViewEntity) {
            StringBuffer sql = new StringBuffer("(SELECT ");
            Iterator fieldsIter = modelEntity.getFieldsIterator();
            if (fieldsIter.hasNext()) {
                ModelField curField = (ModelField) fieldsIter.next();
                String colname = curField.getColName();
                sql.append(colname);
                sql.append(" AS ");
                sql.append(filterColName(colname));
                while (fieldsIter.hasNext()) {
                    curField = (ModelField) fieldsIter.next();
                    colname = curField.getColName();
                    sql.append(", ");
                    sql.append(colname);
                    sql.append(" AS ");
                    sql.append(filterColName(colname));
                }
            }
            sql.append(makeFromClause(modelEntity, datasourceInfo));
            String viewWhereClause = makeViewWhereClause(modelEntity, datasourceInfo.joinStyle);
            if (UtilValidate.isNotEmpty(viewWhereClause)) {
                sql.append(" WHERE ");
                sql.append(viewWhereClause);
            }
            ModelViewEntity modelViewEntity = (ModelViewEntity)modelEntity;
            String groupByString = modelViewEntity.colNameString(modelViewEntity.getGroupBysCopy(), ", ", "", false);
            if (UtilValidate.isNotEmpty(groupByString)) {
                sql.append(" GROUP BY ");
                sql.append(groupByString);
            }

            sql.append(")");
            return sql.toString();
        } else {
            return modelEntity.getTableName(datasourceInfo);
        }
    }

    public static String filterColName(String colName) {
        return colName.replace('.', '_').replace('(','_').replace(')','_');
    }

    /* ====================================================================== */

    /* ====================================================================== */

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor)
     *
     * @param sqlP
     * @param list
     * @param entity
     * @throws GenericEntityException
     */
    public static void setValues(SQLProcessor sqlP, List list, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Iterator fieldIter = list.iterator();
        while (fieldIter.hasNext()) {
            ModelField curField = (ModelField) fieldIter.next();
            setValue(sqlP, curField, entity, modelFieldTypeReader);
        }
    }

    /**
     *  The elements (ModelFields) of the list are bound to an SQL statement
     *  (SQL-Processor), but values must not be null.
     *
     * @param sqlP
     * @param list
     * @param dummyValue
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setValuesWhereClause(SQLProcessor sqlP, List list, GenericValue dummyValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Iterator fieldIter = list.iterator();
        while (fieldIter.hasNext()) {
            ModelField curField = (ModelField) fieldIter.next();
            // for where clause variables only setValue if not null...
            if (dummyValue.get(curField.getName()) != null) {
                setValue(sqlP, curField, dummyValue, modelFieldTypeReader);
            }
        }
    }

    /**
     *  Get all primary keys from the model entity and bind their values
     *  to the an SQL statement (SQL-Processor)
     *
     * @param sqlP
     * @param modelEntity
     * @param entity
     * @param modelFieldTypeReader
     * @throws GenericEntityException
     */
    public static void setPkValues(SQLProcessor sqlP, ModelEntity modelEntity, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Iterator pksIter = modelEntity.getPksIterator();
        while (pksIter.hasNext()) {
            ModelField curField = (ModelField) pksIter.next();

            // for where clause variables only setValue if not null...
            if (entity.dangerousGetNoCheckButFast(curField) != null) {
                setValue(sqlP, curField, entity, modelFieldTypeReader);
            }
        }
    }

    public static void getValue(ResultSet rs, int ind, ModelField curField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(curField.getType());

        if (mft == null) {
            throw new GenericModelException("definition fieldType " + curField.getType() + " not found, cannot getValue for field " +
                    entity.getEntityName() + "." + curField.getName() + ".");
        }
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
                        // Debug.logInfo("For field " + curField.getName() + " of entity " + entity.getEntityName() + " getString is a CLOB, trying getCharacterStream", module);
                        // if the String is empty, try to get a text input stream, this is required for some databases for larger fields, like CLOBs
                        
                        Clob valueClob = rs.getClob(ind);
                        Reader valueReader = null;
                        if (valueClob != null) {
                            valueReader = valueClob.getCharacterStream();
                        }
                        
                        //Reader valueReader = rs.getCharacterStream(ind);
                        if (valueReader != null) {
                            char[] inCharBuffer = new char[CHAR_BUFFER_SIZE];
                            StringBuffer strBuf = new StringBuffer();
                            int charsRead = 0;
                            try {
                                while ((charsRead = valueReader.read(inCharBuffer, 0, CHAR_BUFFER_SIZE)) > 0) {
                                    strBuf.append(inCharBuffer, 0, charsRead);
                                }
                                valueReader.close();
                            } catch (IOException e) {
                                throw new GenericEntityException("Error reading long character stream for field " + curField.getName() + " of entity " + entity.getEntityName(), e);
                            }
                            entity.dangerousSetNoCheckButFast(curField, strBuf.toString());
                        } else {
                            entity.dangerousSetNoCheckButFast(curField, null);
                        }
                    } else {
                        String value = rs.getString(ind);
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
                    Object obj = null;
                    InputStream binaryInput = null;

                    byte[] fieldBytes = rs.getBytes(ind);
                    if (fieldBytes != null && fieldBytes.length > 0) {
                        binaryInput = new ByteArrayInputStream(fieldBytes);
                    }

                    if (fieldBytes != null && fieldBytes.length <= 0) {
                        Debug.logWarning("Got bytes back for Object field with length: " + fieldBytes.length + " while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): ", module);
                    }

                    //alt 1: binaryInput = rs.getBinaryStream(ind);
                    //alt 2: Blob blobLocator = rs.getBlob(ind);
                    //if (blobLocator != null) {
                    //    binaryInput = blobLocator.getBinaryStream();
                    //}

                    if (binaryInput != null) {
                        ObjectInputStream in = null;
                        try {
                            in = new ObjectInputStream(binaryInput);
                            obj = in.readObject();
                        } catch (IOException ex) {
                            throw new GenericDataSourceException("Unable to read BLOB data from input stream while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): " + ex.toString(), ex);
                        } catch (ClassNotFoundException ex) {
                            throw new GenericDataSourceException("Class not found: Unable to cast BLOB data to an Java object while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): " + ex.toString(), ex);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    throw new GenericDataSourceException("Unable to close binary input stream while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + "): " + e.toString(), e);
                                }
                            }
                        }
                    }

                    binaryInput = null;
                    entity.dangerousSetNoCheckButFast(curField, obj);
                    break;
                case 12:
                    entity.dangerousSetNoCheckButFast(curField, rs.getBlob(ind));
                    break;
                case 13:
                    entity.dangerousSetNoCheckButFast(curField, rs.getClob(ind));
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
                        entity.dangerousSetNoCheckButFast(curField, new Integer(intValue));
                    }
                    break;

                case 6:
                    long longValue = rs.getLong(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, new Long(longValue));
                    }
                    break;

                case 7:
                    float floatValue = rs.getFloat(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, new Float(floatValue));
                    }
                    break;

                case 8:
                    double doubleValue = rs.getDouble(ind);
                    if (rs.wasNull()) {
                        entity.dangerousSetNoCheckButFast(curField, null);
                    } else {
                        entity.dangerousSetNoCheckButFast(curField, new Double(doubleValue));
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
                        entity.dangerousSetNoCheckButFast(curField, new Boolean(booleanValue));
                    }
                    break;
                }
            }
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while getting value : " + curField.getName() + " [" + curField.getColName() + "] (" + ind + ")", sqle);
        }
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, GenericEntity entity, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        Object fieldValue = entity.dangerousGetNoCheckButFast(modelField);

        setValue(sqlP, modelField, entity.getEntityName(), fieldValue, modelFieldTypeReader);
    }

    public static void setValue(SQLProcessor sqlP, ModelField modelField, String entityName, Object fieldValue, ModelFieldTypeReader modelFieldTypeReader) throws GenericEntityException {
        ModelFieldType mft = modelFieldTypeReader.getModelFieldType(modelField.getType());

        if (mft == null) {
            throw new GenericModelException("GenericDAO.getValue: definition fieldType " + modelField.getType() + " not found, cannot setValue for field " +
                    entityName + "." + modelField.getName() + ".");
        }
        
        // if the value is the GenericEntity.NullField, treat as null
        if (fieldValue == GenericEntity.NULL_FIELD) {
            fieldValue = null;
        }

        String fieldType = mft.getJavaType();
        if (fieldValue != null) {
            if (!ObjectType.instanceOf(fieldValue, fieldType)) {
                // this is only an info level message because under normal operation for most JDBC
                // drivers this will be okay, but if not then the JDBC driver will throw an exception
                // and when lower debug levels are on this should help give more info on what happened
                Class fieldClass = fieldValue.getClass();
                String fieldClassName = fieldClass.getName();

                if (Debug.verboseOn()) Debug.logVerbose("type of field " + entityName + "." + modelField.getName() +
                        " is " + fieldClassName + ", was expecting " + mft.getJavaType() + "; this may " +
                        "indicate an error in the configuration or in the class, and may result " +
                        "in an SQL-Java data conversion error. Will use the real field type: " +
                        fieldClassName + ", not the definition.", module);
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
                sqlP.setValue((java.sql.Blob) fieldValue);
                break;

            case 13:
                sqlP.setValue((java.sql.Clob) fieldValue);
                break;

            case 14:
                sqlP.setValue(new java.sql.Date(((java.util.Date) fieldValue).getTime()));
                break;

            case 15:
                sqlP.setValue((java.util.Collection) fieldValue);
                break;
            }
        } catch (GenericNotImplementedException e) {
            throw new GenericNotImplementedException("Not Implemented Exception while setting value on field [" + modelField.getName() + "] of entity " + entityName + ": " + e.toString(), e);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while setting value on field [" + modelField.getName() + "] of entity " + entityName + ": ", sqle);
        }
    }

    protected static Map fieldTypeMap = FastMap.newInstance();
    static {
        fieldTypeMap.put("java.lang.String", new Integer(1));
        fieldTypeMap.put("String", new Integer(1));
        fieldTypeMap.put("java.sql.Timestamp", new Integer(2));
        fieldTypeMap.put("Timestamp", new Integer(2));
        fieldTypeMap.put("java.sql.Time", new Integer(3));
        fieldTypeMap.put("Time", new Integer(3));
        fieldTypeMap.put("java.sql.Date", new Integer(4));
        fieldTypeMap.put("Date", new Integer(4));
        fieldTypeMap.put("java.lang.Integer", new Integer(5));
        fieldTypeMap.put("Integer", new Integer(5));
        fieldTypeMap.put("java.lang.Long", new Integer(6));
        fieldTypeMap.put("Long", new Integer(6));
        fieldTypeMap.put("java.lang.Float", new Integer(7));
        fieldTypeMap.put("Float", new Integer(7));
        fieldTypeMap.put("java.lang.Double", new Integer(8));
        fieldTypeMap.put("Double", new Integer(8));
        fieldTypeMap.put("java.math.BigDecimal", new Integer(9));
        fieldTypeMap.put("BigDecimal", new Integer(9));
        fieldTypeMap.put("java.lang.Boolean", new Integer(10));
        fieldTypeMap.put("Boolean", new Integer(10));
        
        fieldTypeMap.put("java.lang.Object", new Integer(11));
        fieldTypeMap.put("Object", new Integer(11));
        fieldTypeMap.put("java.sql.Blob", new Integer(12));
        fieldTypeMap.put("Blob", new Integer(12));
        fieldTypeMap.put("java.sql.Clob", new Integer(13));
        fieldTypeMap.put("Clob", new Integer(13));

        fieldTypeMap.put("java.util.Date", new Integer(14));

        // all of these treated as Collection
        fieldTypeMap.put("java.util.ArrayList", new Integer(15));
        fieldTypeMap.put("java.util.HashSet", new Integer(15));
        fieldTypeMap.put("java.util.LinkedHashSet", new Integer(15));
        fieldTypeMap.put("java.util.LinkedList", new Integer(15));
    }

    public static int getType(String fieldType) throws GenericNotImplementedException {
        Integer val = (Integer) fieldTypeMap.get(fieldType);

        if (val == null) {
            throw new GenericNotImplementedException("Java type " + fieldType + " not currently supported. Sorry.");
        }
        return val.intValue();
    }

    public static void addValueSingle(StringBuffer buffer, ModelField field, Object value, List params) {
        if (field != null) {
            buffer.append('?');
        } else {
            buffer.append('\'').append(value).append('\'');
        }
        if (field != null && params != null) params.add(new EntityConditionParam(field, value));
    }

    public static void addValue(StringBuffer buffer, ModelField field, Object value, List params) {
        if (value instanceof Collection) {
            buffer.append("( ");
            Iterator it = ((Collection) value).iterator();
            while (it.hasNext()) {
                Object thisValue = it.next();
                addValueSingle(buffer, field, thisValue, params);
                if (it.hasNext()) buffer.append(", ");
            }
            buffer.append(" )");
        } else {
            addValueSingle(buffer, field, value, params);
        }
    }
}
