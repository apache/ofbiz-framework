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

package org.apache.ofbiz.entity.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericResultSetClosedException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.datasource.GenericDAO;
import org.apache.ofbiz.entity.jdbc.SQLProcessor;
import org.apache.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldTypeReader;

/**
 * Generic Entity Cursor List Iterator for Handling Cursored DB Results
 */
public class EntityListIterator implements AutoCloseable, ListIterator<GenericValue> {

    /** Module Name Used for debugging */
    private static final String MODULE = EntityListIterator.class.getName();

    private SQLProcessor sqlp;
    private ResultSet resultSet;
    private ModelEntity modelEntity;
    private List<ModelField> selectFields;
    private ModelFieldTypeReader modelFieldTypeReader;
    private boolean closed = false;
    private boolean haveMadeValue = false;
    private Delegator delegator = null;
    private GenericDAO genericDAO = null;
    private EntityCondition whereCondition = null;
    private EntityCondition havingCondition = null;
    private boolean distinctQuery = false;

    private boolean haveShowHasNextWarning = false;
    private Integer resultSize = null;

    public EntityListIterator(SQLProcessor sqlp, ModelEntity modelEntity, List<ModelField> selectFields, ModelFieldTypeReader modelFieldTypeReader) {
        this(sqlp, modelEntity, selectFields, modelFieldTypeReader, null, null, null, false);
    }

    public EntityListIterator(SQLProcessor sqlp, ModelEntity modelEntity, List<ModelField> selectFields, ModelFieldTypeReader modelFieldTypeReader,
                              GenericDAO genericDAO, EntityCondition whereCondition, EntityCondition havingCondition, boolean distinctQuery) {
        this(sqlp.getResultSet(), modelEntity, selectFields, modelFieldTypeReader);
        this.sqlp = sqlp;
        this.genericDAO = genericDAO;
        this.whereCondition = whereCondition;
        this.havingCondition = havingCondition;
        this.distinctQuery = distinctQuery;
    }

    public EntityListIterator(ResultSet resultSet, ModelEntity modelEntity, List<ModelField> selectFields, ModelFieldTypeReader
            modelFieldTypeReader) {
        this.sqlp = null;
        this.resultSet = resultSet;
        this.modelEntity = modelEntity;
        this.selectFields = selectFields;
        this.modelFieldTypeReader = modelFieldTypeReader;
    }

    /**
     * Sets delegator.
     * @param delegator the delegator
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Sets the cursor position to just after the last result so that previous() will return the last result.
     * @throws GenericEntityException
     *             if a database error occurs.
     */
    public void afterLast() throws GenericEntityException {
        try {
            resultSet.afterLast();
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error setting the cursor to afterLast", e);
        }
    }

    /**
     * Sets the cursor position to just before the first result so that next() will return the first result.
     * @throws GenericEntityException
     *             if a database error occurs.
     */
    public void beforeFirst() throws GenericEntityException {
        try {
            resultSet.beforeFirst();
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error setting the cursor to beforeFirst", e);
        }
    }

    /**
     * Sets the cursor position to last result.
     * @return true if the result set is not empty.
     * @throws GenericEntityException
     *             if a database error occurs.
     */
    public boolean last() throws GenericEntityException {
        try {
            return resultSet.last();
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error setting the cursor to last", e);
        }
    }

    /**
     * Sets the cursor position to first result.
     * @return true if the result set is not empty.
     * @throws GenericEntityException
     *             in case of a database error.
     */
    public boolean first() throws GenericEntityException {
        try {
            return resultSet.first();
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error setting the cursor to first", e);
        }
    }

    /**
     * Closes the iterator.
     * @throws GenericEntityException
     *             if the {@link EntityListIterator} cannot be closed.
     */
    @Override
    public void close() throws GenericEntityException {
        if (closed) {
            String modelEntityName = modelEntity != null ? modelEntity.getEntityName() : "";
            Debug.logWarning("This EntityListIterator for Entity [" + modelEntityName + "] has already been closed, not closing again.", MODULE);
            return;
        }
        if (sqlp != null) {
            sqlp.close();
            closed = true;
            return;
        }
        if (resultSet == null) {
            throw new GenericEntityException(
                    "Cannot close an EntityListIterator without a SQLProcessor or a ResultSet");
        }
        try {
            resultSet.close();
        } catch (SQLException e) {
            throw new GenericEntityException("Cannot close EntityListIterator with ResultSet", e);
        }
        closed = true;
    }

    /**
     * NOTE: Calling this method does return the current value, but so does calling next() or previous()
     *  So calling one of those AND this method will cause the value to be created twice.
     */
    public GenericValue currentGenericValue() throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        GenericValue value = GenericValue.create(modelEntity);
        value.setDelegator(this.delegator);

        for (int j = 0; j < selectFields.size(); j++) {
            ModelField curField = selectFields.get(j);

            SqlJdbcUtil.getValue(resultSet, j + 1, curField, value, modelFieldTypeReader);
        }

        value.synchronizedWithDatasource();
        this.haveMadeValue = true;
        return value;
    }

    /**
     * Determines the current index of the cursor position.
     * @return the current index, 0 if there is none.
     * @throws GenericEntityException
     *             if an error with the database access occurs.
     */
    public int currentIndex() throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.getRow();
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error getting the current index", e);
        }
    }

    /**
     * performs the same function as the {@link ResultSet#absolute(int)} method.
     * if rowNum is positive, goes to that position relative to the beginning of the list;
     * if rowNum is negative, goes to that position relative to the end of the list;
     * a rowNum of 1 is the same as first();
     * a rowNum of -1 is the same as last()
     * @param rowNum
     *            the index of the row to set the cursor to.
     * @return true if the cursor moved to a row within the result set, false if it
     *         is on the row before or after.
     * @throws GenericEntityException
     *             if an error with the database access occurs.
     */
    public boolean absolute(int rowNum) throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            if (rowNum == 0) {
                resultSet.beforeFirst();
                return true;
            }
            return resultSet.absolute(rowNum);
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error setting the absolute index to " + rowNum, e);
        }
    }

    /**
     * performs the same function as the {@link ResultSet#relative(int)} method.
     * if rows is positive, goes forward relative to the current position;
     * if rows is negative, goes backward relative to the current position;
     * @param rows
     *            the amount of rows to move.
     * @return true if the cursor is on a row.
     * @throws GenericEntityException
     *             in case of an error related to the database.
     */
    public boolean relative(int rows) throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.relative(rows);
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error going to the relative index " + rows, e);
        }
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient.
     * It is much better to just use next() until it returns null
     * For example, you could use the following to iterate through the results in an EntityListIterator:
     * GenericValue nextValue = null;
     * while ((nextValue = (GenericValue)
     * this.next()) != null) { ... }
     */
    @Override
    public boolean hasNext() {
        if (!haveShowHasNextWarning) {
            // DEJ20050207 To further discourage use of this, and to find existing use, always log a big warning showing where it is used:
            Exception whereAreWe = new Exception();
            Debug.logWarning(whereAreWe,
                    "For performance reasons do not use the EntityListIterator.hasNext() method, just call next() until it returns null; "
                            + "see JavaDoc comments in the EntityListIterator class for details and an example", MODULE);

            haveShowHasNextWarning = true;
        }

        try {
            // do a quick game to see if the resultSet is empty:
            // if we are not in the first or beforeFirst positions and we haven't made any values yet, the result set is empty so return false
            return !(resultSet.isLast() || resultSet.isAfterLast())
                    && (haveMadeValue || resultSet.isBeforeFirst() || resultSet.isFirst());
        } catch (SQLException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error while checking to see if this is the last result", e);
        }
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient.
     * It is much better to just use previous() until it returns null.
     */
    @Override
    public boolean hasPrevious() {
        try {
            // do a quick game to see if the resultSet is empty:
            // if we are not in the first or beforeFirst positions and we haven't made any values yet, the result set is
            // empty so return false
            return !(resultSet.isFirst() || resultSet.isBeforeFirst()) && (haveMadeValue || resultSet.isAfterLast() || resultSet.isLast());
        } catch (SQLException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error while checking to see if this is the first result", e);
        }
    }

    /**
     * Moves the cursor to the next position and returns the value for that position; For example, you could use the following
     * to iterate through the results in an
     * EntityListIterator:
     * GenericValue nextValue = null; while ((nextValue = (GenericValue) this.next()) != null) { ... }
     * @return the next element or null, if there is no next element.
     */
    @Override
    public GenericValue next() {
        try {
            return resultSet.next() ? currentGenericValue() : null;
        } catch (SQLException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error getting the next result", e);
        } catch (GenericEntityException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /**
     * Returns the index of the next result, but does not guarantee that there will be a next result.
     */
    @Override
    public int nextIndex() {
        try {
            return currentIndex() + 1;
        } catch (GenericEntityException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /**
     * Moves the cursor to the previous position and returns the GenericValue object for that position;
     * if there is no previous, returns null.
     */
    @Override
    public GenericValue previous() {
        try {
            return resultSet.previous() ? currentGenericValue() : null;
        } catch (SQLException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error getting the previous result", e);
        } catch (GenericEntityException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /**
     * Returns the index of the previous result, but does not guarantee that there will be a previous result.
     */
    @Override
    public int previousIndex() {
        try {
            return currentIndex() - 1;
        } catch (GenericEntityException e) {
            tryCloseWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error getting the current index", e);
        }
    }

    /**
     * Sets the fetch size of the result set to the given value.
     * @param rows
     *            the fetch size
     * @throws GenericEntityException
     *             if a database error occurs.
     */
    public void setFetchSize(int rows) throws GenericEntityException {
        try {
            resultSet.setFetchSize(rows);
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException("Error getting the next result", e);
        }
    }

    /**
     * Gets all the elements of the {@link EntityListIterator} as a list.
     * @return a list of the elements.
     * @throws GenericEntityException
     *             if there is a problem with the database.
     */
    public List<GenericValue> getCompleteList() throws GenericEntityException {
        try {
            // if the resultSet has been moved forward at all, move back to the beginning
            if (haveMadeValue && !resultSet.isBeforeFirst()) {
                // do a quick check to see if the ResultSet is empty
                resultSet.beforeFirst();
            }
            List<GenericValue> list = new ArrayList<>();
            GenericValue nextValue;

            while ((nextValue = this.next()) != null) {
                list.add(nextValue);
            }
            return list;
        } catch (SQLException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GeneralRuntimeException("Error getting results", e);
        } catch (GeneralRuntimeException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /**
     * Gets a partial list of results starting at start and containing at most number elements.
     * Start is a one based value, i.e. 1 is the first element.
     * @param start
     *            the index from which on the elements should be retrieved. Is one based.
     * @param number
     *            the maximum number of elements to get after the start index.
     * @return A list with the retrieved elements, with the size of number or less if the result set does not contain enough values.
     *            Empty list in case of no values or an invalid start index.
     * @throws GenericEntityException
     *            if there is an issue with the database.
     */
    public List<GenericValue> getPartialList(int start, int number) throws GenericEntityException {
        try {
            if (number == 0) {
                return new ArrayList<>(0);
            }

            // just in case the caller missed the 1 based thingy
            if (start == 0) start = 1;

            // if can't reposition to desired index, throw exception
            if (!this.absolute(start - 1)) {
                // maybe better to just return an empty list here...
                return new ArrayList<>(0);
            }

            List<GenericValue> list = new ArrayList<>();
            GenericValue nextValue = null;
            // number > 0 comparison goes first to avoid the unwanted call to next
            while (number > 0 && (nextValue = this.next()) != null) {
                list.add(nextValue);
                number--;
            }
            return list;
        } catch (GeneralRuntimeException e) {
            closeWithWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString());
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /**
     * Determines the possible result size.
     * If a {@link GenericDAO} is known, the result size will be counted by the
     * database. Otherwise the result size is the last index of the
     * {@link EntityListIterator}.
     * @return the result size or 0 if the result set is empty.
     * @throws GenericEntityException
     *             if there is an issue with the call to the database.
     */
    public int getResultsSizeAfterPartialList() throws GenericEntityException {
        if (genericDAO != null) {
            if (resultSize == null) {
                resultSize = (int) getResultSize();
            }
            return resultSize;
        }
        return this.last() ? this.currentIndex() : 0;
    }

    /**
     * Finds the size of the result.
     * @return count of elements returned by a query.
     * @throws GenericEntityException
     *             if there is an issue with the call to the database.
     */
    private long getResultSize() throws GenericEntityException {
        EntityFindOptions efo = null;
        if (distinctQuery) {
            efo = new EntityFindOptions();
            efo.setDistinct(distinctQuery);
        }
        return genericDAO.selectCountByCondition(sqlp.getDelegator(), modelEntity, whereCondition,
                havingCondition, selectFields, efo);
    }

    /**
     * Unsupported {@link ListIterator#add(Object)} method.
     */
    @Override
    public void add(GenericValue obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    /**
     * Unsupported {@link ListIterator#remove()} method.
     */
    @Override
    public void remove() {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    /**
     * Unsupported {@link ListIterator#set(Object)} method.
     */
    @Override
    public void set(GenericValue obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    /**
     * Closes the {@link EntityListIterator} and logs a warning if it isn't already closed.
     * If you don't want to handle the {@link GenericEntityException} thrown by {@link #close()}, use {@link #tryCloseWithWarning()}.
     * @param warningMessage
     *            the warning to be logged.
     * @throws GenericEntityException
     *             when the closing of the {@link EntityListIterator} fails. Thrown by {@link #close()}.
     */
    private void closeWithWarning(String warningMessage) throws GenericEntityException {
        if (!closed) {
            this.close();
            Debug.logWarning(warningMessage, MODULE);
        }
    }

    /**
     * Tries to close the {@link EntityListIterator} and logs a warning if it isn't already closed.
     * Catches the {@link GenericEntityException} thrown by {@link #close()} and logs it. If you want to handle the exception
     * yourself, use {@link #closeWithWarning()}.
     * @param warningMessage
     *            the warning to be logged.
     */
    private void tryCloseWithWarning(String warningMessage) {
        if (!closed) {
            try {
                this.close();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: %s",
                        MODULE, e.toString());
            }
            Debug.logWarning(warningMessage, MODULE);
        }
    }
}
