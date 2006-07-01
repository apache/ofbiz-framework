/*
 * $Id: EntityFindOptions.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.util;

import java.sql.ResultSet;

/**
 * Contains a number of variables used to select certain advanced finding options.
 *
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 *@version    $Rev$
 *@since      2.0
 */
public class EntityFindOptions implements java.io.Serializable {

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_FORWARD_ONLY = ResultSet.TYPE_FORWARD_ONLY;

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_SCROLL_INSENSITIVE = ResultSet.TYPE_SCROLL_INSENSITIVE;

    /** Type constant from the java.sql.ResultSet object for convenience */
    public static final int TYPE_SCROLL_SENSITIVE = ResultSet.TYPE_SCROLL_SENSITIVE;

    /** Concurrency constant from the java.sql.ResultSet object for convenience */
    public static final int CONCUR_READ_ONLY = ResultSet.CONCUR_READ_ONLY;

    /** Concurrency constant from the java.sql.ResultSet object for convenience */
    public static final int CONCUR_UPDATABLE = ResultSet.CONCUR_UPDATABLE;

    protected boolean specifyTypeAndConcur = true;
    protected int resultSetType = TYPE_FORWARD_ONLY;
    protected int resultSetConcurrency = CONCUR_READ_ONLY;
    protected int fetchSize = -1;
    protected int maxRows = -1;
    protected boolean distinct = false;

    /** Default constructor. Defaults are as follows:
     *      specifyTypeAndConcur = true
     *      resultSetType = TYPE_FORWARD_ONLY
     *      resultSetConcurrency = CONCUR_READ_ONLY
     *      distinct = false
     *      maxRows = 0 (all rows)
     */
    public EntityFindOptions() {}

    public EntityFindOptions(boolean specifyTypeAndConcur, int resultSetType, int resultSetConcurrency, int fetchSize, int maxRows, boolean distinct) {
        this.specifyTypeAndConcur = specifyTypeAndConcur;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.fetchSize = fetchSize;
        this.maxRows = maxRows;
        this.distinct = distinct;
    }

    public EntityFindOptions(boolean specifyTypeAndConcur, int resultSetType, int resultSetConcurrency, boolean distinct) {
        this(specifyTypeAndConcur, resultSetType, resultSetConcurrency, -1, -1, distinct);
    }

    /** If true the following two parameters (resultSetType and resultSetConcurrency) will be used to specify
     *      how the results will be used; if false the default values for the JDBC driver will be used
     */
    public boolean getSpecifyTypeAndConcur() {
        return specifyTypeAndConcur;
    }

    /** If true the following two parameters (resultSetType and resultSetConcurrency) will be used to specify
     *      how the results will be used; if false the default values for the JDBC driver will be used
     */
    public void setSpecifyTypeAndConcur(boolean specifyTypeAndConcur) {
        this.specifyTypeAndConcur = specifyTypeAndConcur;
    }

    /** Specifies how the ResultSet will be traversed. Available values: ResultSet.TYPE_FORWARD_ONLY,
     *      ResultSet.TYPE_SCROLL_INSENSITIVE or ResultSet.TYPE_SCROLL_SENSITIVE. See the java.sql.ResultSet JavaDoc for
     *      more information. If you want it to be fast, use the common default: ResultSet.TYPE_FORWARD_ONLY.
     */
    public int getResultSetType() {
        return resultSetType;
    }

    /** Specifies how the ResultSet will be traversed. Available values: ResultSet.TYPE_FORWARD_ONLY,
     *      ResultSet.TYPE_SCROLL_INSENSITIVE or ResultSet.TYPE_SCROLL_SENSITIVE. See the java.sql.ResultSet JavaDoc for
     *      more information. If you want it to be fast, use the common default: ResultSet.TYPE_FORWARD_ONLY.
     */
    public void setResultSetType(int resultSetType) {
        this.resultSetType = resultSetType;
    }

    /** Specifies whether or not the ResultSet can be updated. Available values:
     *      ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty much always be
     *      ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     */
    public int getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /** Specifies whether or not the ResultSet can be updated. Available values:
     *      ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE. Should pretty much always be
     *      ResultSet.CONCUR_READ_ONLY with the Entity Engine.
     */
    public void setResultSetConcurrency(int resultSetConcurrency) {
        this.resultSetConcurrency = resultSetConcurrency;
    }

    /** Specifies the fetch size for this query. -1 will fall back to datasource settings. */
    public int getFetchSize() {
        return fetchSize;
    }

    /** Specifies the fetch size for this query. -1 will fall back to datasource settings. */
    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /** Specifies the max number of rows to return, 0 means all rows. */
    public int getMaxRows() {
        return maxRows;
    }

    /** Specifies the max number of rows to return, 0 means all rows. */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /** Specifies whether the values returned should be filtered to remove duplicate values. */
    public boolean getDistinct() {
        return distinct;
    }

    /** Specifies whether the values returned should be filtered to remove duplicate values. */
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }
}
