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

package org.ofbiz.entity.util;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ListIterator;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericResultSetClosedException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.jdbc.SQLProcessor;
import org.ofbiz.entity.jdbc.SqlJdbcUtil;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldTypeReader;


/**
 * Generic Entity Cursor List Iterator for Handling Cursored DB Results
 */
public class EntityListIterator implements ListIterator {

    /** Module Name Used for debugging */
    public static final String module = EntityListIterator.class.getName();
    
    protected SQLProcessor sqlp;
    protected ResultSet resultSet;
    protected ModelEntity modelEntity;
    protected List selectFields;
    protected ModelFieldTypeReader modelFieldTypeReader;
    protected boolean closed = false;
    protected boolean haveMadeValue = false;
    protected GenericDelegator delegator = null;

    private boolean haveShowHasNextWarning = false;
    
    public EntityListIterator(SQLProcessor sqlp, ModelEntity modelEntity, List selectFields, ModelFieldTypeReader modelFieldTypeReader) {
        this.sqlp = sqlp;
        this.resultSet = sqlp.getResultSet();
        this.modelEntity = modelEntity;
        this.selectFields = selectFields;
        this.modelFieldTypeReader = modelFieldTypeReader;
    }

    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /** Sets the cursor position to just after the last result so that previous() will return the last result */
    public void afterLast() throws GenericEntityException {
        try {
            resultSet.afterLast();
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error setting the cursor to afterLast", e);
        }
    }

    /** Sets the cursor position to just before the first result so that next() will return the first result */
    public void beforeFirst() throws GenericEntityException {
        try {
            resultSet.beforeFirst();
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error setting the cursor to beforeFirst", e);
        }
    }

    /** Sets the cursor position to last result; if result set is empty returns false */
    public boolean last() throws GenericEntityException {
        try {
            return resultSet.last();
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error setting the cursor to last", e);
        }
    }

    /** Sets the cursor position to last result; if result set is empty returns false */
    public boolean first() throws GenericEntityException {
        try {
            return resultSet.first();
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error setting the cursor to first", e);
        }
    }

    public void close() throws GenericEntityException {
        if (closed) {
            //maybe not the best way: throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");
            Debug.logWarning("This EntityListIterator for Entity [" + modelEntity==null?"":modelEntity.getEntityName() + "] has already been closed, not closing again.", module);
        } else {
            sqlp.close();
            closed = true;
        }
    }

    /** NOTE: Calling this method does return the current value, but so does calling next() or previous(), so calling one of those AND this method will cause the value to be created twice */
    public GenericValue currentGenericValue() throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        GenericValue value = GenericValue.create(modelEntity);

        for (int j = 0; j < selectFields.size(); j++) {
            ModelField curField = (ModelField) selectFields.get(j);

            SqlJdbcUtil.getValue(resultSet, j + 1, curField, value, modelFieldTypeReader);
        }

        value.setDelegator(this.delegator);
        value.synchronizedWithDatasource();
        this.haveMadeValue = true;
        if (delegator != null) {
            delegator.decryptFields(value);
        }
        return value;
    }

    public int currentIndex() throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.getRow();
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error getting the current index", e);
        }
    }

    /** performs the same function as the ResultSet.absolute method;
     * if rowNum is positive, goes to that position relative to the beginning of the list;
     * if rowNum is negative, goes to that position relative to the end of the list;
     * a rowNum of 1 is the same as first(); a rowNum of -1 is the same as last()
     */
    public boolean absolute(int rowNum) throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.absolute(rowNum);
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error setting the absolute index to " + rowNum, e);
        }
    }

    /** performs the same function as the ResultSet.relative method;
     * if rows is positive, goes forward relative to the current position;
     * if rows is negative, goes backward relative to the current position;
     */
    public boolean relative(int rows) throws GenericEntityException {
        if (closed) throw new GenericResultSetClosedException("This EntityListIterator has been closed, this operation cannot be performed");

        try {
            return resultSet.relative(rows);
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error going to the relative index " + rows, e);
        }
    }

    /** 
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use next() until it returns null
     * For example, you could use the following to iterate through the results in an EntityListIterator:
     * 
     *      GenericValue nextValue = null;
     *      while ((nextValue = (GenericValue) this.next()) != null) { ... }
     * 
     */
    public boolean hasNext() {
        if (!haveShowHasNextWarning) {
            // DEJ20050207 To further discourage use of this, and to find existing use, always log a big warning showing where it is used:
            Exception whereAreWe = new Exception();
            Debug.logWarning(whereAreWe, "WARNING: For performance reasons do not use the EntityListIterator.hasNext() method, just call next() until it returns null; see JavaDoc comments in the EntityListIterator class for details and an example", module);
            
            haveShowHasNextWarning = true;
        }
        
        try {
            if (resultSet.isLast() || resultSet.isAfterLast()) {
                return false;
            } else {
                // do a quick game to see if the resultSet is empty:
                // if we are not in the first or beforeFirst positions and we haven't made any values yet, the result set is empty so return false
                if (!haveMadeValue && !resultSet.isBeforeFirst() && !resultSet.isFirst()) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (SQLException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error while checking to see if this is the last result", e);
        }
    }

    /** PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use previous() until it returns null */
    public boolean hasPrevious() {
        try {
            if (resultSet.isFirst() || resultSet.isBeforeFirst()) {
                return false;
            } else {
                // do a quick game to see if the resultSet is empty:
                // if we are not in the last or afterLast positions and we haven't made any values yet, the result set is empty so return false
                if (!haveMadeValue && !resultSet.isAfterLast() && !resultSet.isLast()) {
                    return false;
                } else {
                    return true;
                }
            }
        } catch (SQLException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error while checking to see if this is the first result", e);
        }
    }

    /** Moves the cursor to the next position and returns the GenericValue object for that position; if there is no next, returns null
     * For example, you could use the following to iterate through the results in an EntityListIterator:
     * 
     *      GenericValue nextValue = null;
     *      while ((nextValue = (GenericValue) this.next()) != null) { ... }
     * 
     */
    public Object next() {
        try {
            if (resultSet.next()) {
                return currentGenericValue();
            } else {
                return null;
            }
        } catch (SQLException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error getting the next result", e);
        } catch (GenericEntityException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /** Returns the index of the next result, but does not guarantee that there will be a next result */
    public int nextIndex() {
        try {
            return currentIndex() + 1;
        } catch (GenericEntityException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /** Moves the cursor to the previous position and returns the GenericValue object for that position; if there is no previous, returns null */
    public Object previous() {
        try {
            if (resultSet.previous()) {
                return currentGenericValue();
            } else {
                return null;
            }
        } catch (SQLException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error getting the previous result", e);
        } catch (GenericEntityException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error creating GenericValue", e);
        }
    }

    /** Returns the index of the previous result, but does not guarantee that there will be a previous result */
    public int previousIndex() {
        try {
            return currentIndex() - 1;
        } catch (GenericEntityException e) {
            if (!closed) {
                try {
                    this.close();
                } catch (GenericEntityException e1) {
                    Debug.logError(e1, "Error auto-closing EntityListIterator on error, so info below for more info on original error; close error: " + e1.toString(), module);
                }
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error getting the current index", e);
        }
    }

    public void setFetchSize(int rows) throws GenericEntityException {
        try {
            resultSet.setFetchSize(rows);
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException("Error getting the next result", e);
        }
    }

    public List getCompleteList() throws GenericEntityException {
        try {
            // if the resultSet has been moved forward at all, move back to the beginning
            if (haveMadeValue && !resultSet.isBeforeFirst()) {
                // do a quick check to see if the ResultSet is empty
                resultSet.beforeFirst();
            }
            List list = FastList.newInstance();
            Object nextValue = null;

            while ((nextValue = this.next()) != null) {
                list.add(nextValue);
            }
            return list;
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error getting results", e);
        } catch (GeneralRuntimeException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    /** Gets a partial list of results starting at start and containing at most number elements.
     * Start is a one based value, ie 1 is the first element.
     */
    public List getPartialList(int start, int number) throws GenericEntityException {
        try {
            if (number == 0) return FastList.newInstance();
            List list = FastList.newInstance();
            
            // just in case the caller missed the 1 based thingy
            if (start == 0) start = 1;

            // if starting on result 1 just call next() to avoid scrollable issues in some databases
            if (start == 1) {
                if (!resultSet.next()) {
                    return list;
                }
            } else {
                // if can't reposition to desired index, throw exception
                if (!resultSet.absolute(start)) {
                    // maybe better to just return an empty list here...
                    return list;
                    //throw new GenericEntityException("Could not move to the start position of " + start + ", there are probably not that many results for this find.");
                }
            }

            // get the first as the current one
            list.add(this.currentGenericValue());

            Object nextValue = null;
            // init numRetreived to one since we have already grabbed the initial one
            int numRetreived = 1;

            //number > numRetreived comparison goes first to avoid the unwanted call to next
            while (number > numRetreived && (nextValue = this.next()) != null) {
                list.add(nextValue);
                numRetreived++;
            }
            return list;
        } catch (SQLException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GeneralRuntimeException("Error getting results", e);
        } catch (GeneralRuntimeException e) {
            if (!closed) {
                this.close();
                Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), module);
            }
            throw new GenericEntityException(e.getNonNestedMessage(), e.getNested());
        }
    }

    public void add(Object obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    public void remove() {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    public void set(Object obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    protected void finalize() throws Throwable {
        try {
            if (!closed) {
                this.close();
                Debug.logError("\n====================================================================\n EntityListIterator Not Closed for Entity [" + (modelEntity==null ? "" : modelEntity.getEntityName()) + "], caught in Finalize\n ====================================================================\n", module);
            }
        } catch (Exception e) {
            Debug.logError(e, "Error closing the SQLProcessor in finalize EntityListIterator", module);
        }
        super.finalize();
    }
}
