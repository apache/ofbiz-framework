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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDataSourceException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.config.DatasourceInfo;
import org.ofbiz.entity.config.EntityConfigUtil;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;

/**
 * SQLProcessor - provides utitlity functions to ease database access
 * 
 */
public class SQLProcessor {

    /** Module Name Used for debugging */
    public static final String module = SQLProcessor.class.getName();
    
    /** Used for testing connections when test is enabled */
    public static List CONNECTION_TEST_LIST = new ArrayList();
    public static int MAX_CONNECTIONS = 1000;
    public static boolean ENABLE_TEST = false;

    /** The datasource helper (see entityengine.xml <datasource name="..">) */
    private String helperName;

    // / The database resources to be used
    private Connection _connection = null;

    // / The database resources to be used
    private PreparedStatement _ps = null;

    // / The database resources to be used
    private Statement _stmt = null;

    // / The database resources to be used
    private ResultSet _rs = null;
    
    private ResultSetMetaData _rsmd = null;

    // / The SQL String used. Use for debugging only
    private String _sql;

    // / Index to be used with preparedStatement.setValue(_ind, ...)
    private int _ind;

    // / true in case of manual transactions
    private boolean _manualTX;

    // / true in case the connection shall be closed.
    private boolean _bDeleteConnection = false;
    
    private Map _needClobWorkAroundWrite = null;
    private Map _needBlobWorkAroundWrite = null;
    
    /**
     * Construct an object based on the helper/datasource
     *
     * @param helperName  The datasource helper (see entityengine.xml &lt;datasource name=".."&gt;)
     */
    public SQLProcessor(String helperName) {
        this.helperName = helperName;
        this._manualTX = true;
    }

    /**
     * Construct an object with an connection given. The connection will not
     * be closed by this SQLProcessor, but may be by some other.
     *
     * @param helperName  The datasource helper (see entityengine.xml &lt;datasource name=".."&gt;)
     * @param connection  The connection to be used
     */
    public SQLProcessor(String helperName, Connection connection) {
        this.helperName = helperName;
        this._connection = connection;

        // Do not commit while closing
        if (_connection != null) {
            _manualTX = false;
        }
    }
    
    ResultSetMetaData getResultSetMetaData() {
        if (_rsmd == null) {
            // try the ResultSet, if not null, or try the PreparedStatement, also if not null
            try {
                if (_rs != null) {
                    _rsmd = _rs.getMetaData();
                } else if (_ps != null) {
                    _rsmd = _ps.getMetaData();
                }
            } catch (SQLException sqle2) {
                Debug.logWarning("[SQLProcessor.rollback]: SQL Exception while rolling back insert. Error was:" + sqle2, module);
                Debug.logWarning(sqle2, module);
            }
        }
        return _rsmd;
    }

    /**
     * Commit all modifications
     *
     * @throws GenericDataSourceException
     */
    public void commit() throws GenericDataSourceException {
        if (_connection == null) {
            return;
        }
        
        if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:commit() _manualTX=" + _manualTX, module);

        if (_manualTX) {
            try {
                _connection.commit();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:commit() : called commit on connection", module);
            } catch (SQLException sqle) {
                Debug.logError(sqle, "Error committing transaction: " + sqle.toString());
                try {
                    rollback();
                } catch (GenericDataSourceException rbsqle) {
                    Debug.logError(rbsqle, "Got another error when trying to rollback after error committing transaction: " + sqle.toString());
                } 
                throw new GenericDataSourceException("SQL Exception occurred on commit", sqle);
            }
        }
    }

    /**
     * Rollback all modifications
     */
    public void rollback() throws GenericDataSourceException {
        if (_connection == null) {
            return;
        }
        
        if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:rollback() _manualTX=" + _manualTX, module);

        try {
            if (_manualTX) {
                _connection.rollback();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:rollback() : _manualTX=" + _manualTX, module);
            } else {
                try {
                    TransactionUtil.setRollbackOnly("rollback called in Entity Engine SQLProcessor", new Exception("Current Location Stack"));
                    if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:rollback() : _manualTX=" + _manualTX, module);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Error setting rollback only", module);
                    throw new GenericDataSourceException("Error setting rollback only", e);
                }
            }
        } catch (SQLException sqle2) {
            Debug.logWarning("[SQLProcessor.rollback]: SQL Exception while rolling back insert. Error was:" + sqle2, module);
            Debug.logWarning(sqle2, module);
        }
    }

    /**
     * Commit if required and remove all allocated resources
     *
     * @throws GenericDataSourceException
     */
    public void close() throws GenericDataSourceException {        
        if (_manualTX) {
            if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:close() calling commit : _manualTX=" + _manualTX, module);
            commit();
        }

        _sql = null;

        if (_rs != null) {
            try {
                _rs.close();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:close() result close : _manualTX=" + _manualTX, module);
            } catch (SQLException sqle) {
                Debug.logWarning(sqle.getMessage(), module);
            }

            _rs = null;
        }

        if (_ps != null) {
            try {
                _ps.close();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:close() preparedStatement close : _manualTX=" + _manualTX, module);
            } catch (SQLException sqle) {
                Debug.logWarning(sqle.getMessage(), module);
            }

            _ps = null;
        }

        if (_stmt != null) {
            try {
                _stmt.close();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:close() statement close : _manualTX=" + _manualTX, module);
            } catch (SQLException sqle) {
                Debug.logWarning(sqle.getMessage(), module);
            }

            _stmt = null;
        }

        if ((_connection != null) && _bDeleteConnection) {
            try {
                _connection.close();
                if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:close() connection close : _manualTX=" + _manualTX, module);
            } catch (SQLException sqle) {
                Debug.logWarning(sqle.getMessage(), module);
            }

            _connection = null;
        }               
    }

    /**
     * Get a connection from the ConnectionFactory
     *
     * @return  The connection created
     *
     * @throws GenericDataSourceException
     * @throws GenericEntityException
     */
    public Connection getConnection() throws GenericDataSourceException, GenericEntityException {
        if (_connection != null)
            return _connection;

        _manualTX = true;

        try {
            _connection = ConnectionFactory.getConnection(helperName);
            if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:connection() : manualTx=" + _manualTX, module);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("Unable to esablish a connection with the database.", sqle);
        }

        // make sure we actually did get a connection
        if (_connection == null) {
            throw new GenericDataSourceException("Unable to esablish a connection with the database. Connection was null!");
        }

        // test the connection
        testConnection(_connection);
        
        /* causes problems w/ postgres ??
        if (Debug.verboseOn()) {
            int isoLevel = -999;
            try {
                isoLevel = _connection.getTransactionIsolation();
            } catch (SQLException e) {
                Debug.logError(e, "Problems getting the connection's isolation level", module);
            }
            if (isoLevel == Connection.TRANSACTION_NONE) {
                Debug.logVerbose("Transaction isolation level set to 'None'.", module);
            } else if (isoLevel == Connection.TRANSACTION_READ_COMMITTED) {
                Debug.logVerbose("Transaction isolation level set to 'ReadCommited'.", module);
            } else if (isoLevel == Connection.TRANSACTION_READ_UNCOMMITTED) {
                Debug.logVerbose("Transaction isolation level set to 'ReadUncommitted'.", module);
            } else if (isoLevel == Connection.TRANSACTION_REPEATABLE_READ) {
                Debug.logVerbose("Transaction isolation level set to 'RepeatableRead'.", module);
            } else if (isoLevel == Connection.TRANSACTION_SERIALIZABLE) {
                Debug.logVerbose("Transaction isolation level set to 'Serializable'.", module);
            }
        }
        */
                            
        // always try to set auto commit to false, but if we can't then later on we won't commit
        try {
            if (_connection.getAutoCommit()) {
                try {
                    _connection.setAutoCommit(false);
                    if (Debug.verboseOn()) Debug.logVerbose("SQLProcessor:setAutoCommit(false) : manualTx=" + _manualTX, module);
                } catch (SQLException sqle) {
                    _manualTX = false;
                }
            }
        } catch (SQLException e) {
            throw new GenericDataSourceException("Cannot get autoCommit status from connection", e);            
        }

        try {
            if (TransactionUtil.getStatus() == TransactionUtil.STATUS_ACTIVE) {
                if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.getConnection] : active transaction", module);
                _manualTX = false;
            }
        } catch (GenericTransactionException e) {
            // nevermind, don't worry about it, but print the exc anyway
            Debug.logWarning("[SQLProcessor.getConnection]: Exception was thrown trying to check " +
                "transaction status: " + e.toString(), module);
        }

        if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.getConnection] : con=" + _connection, module);
        
        _bDeleteConnection = true;        
        return _connection;
    }

    /**
     * Prepare a statement. In case no connection has been given, allocate a
     * new one.
     *
     * @param sql  The SQL statement to be executed
     *
     * @throws GenericDataSourceException
     * @throws GenericEntityException
     */
    public void prepareStatement(String sql) throws GenericDataSourceException, GenericEntityException {
        this.prepareStatement(sql, false, 0, 0, -1, -1);
    }

    /**
     * Prepare a statement. In case no connection has been given, allocate a
     * new one.
     *
     * @param sql  The SQL statement to be executed
     *
     * @throws GenericDataSourceException
     * @throws GenericEntityException
     */
    public void prepareStatement(String sql, boolean specifyTypeAndConcur, int resultSetType, int resultSetConcurrency) throws GenericDataSourceException, GenericEntityException {
        this.prepareStatement(sql, specifyTypeAndConcur, resultSetType, resultSetConcurrency, -1, -1);
    }

    /**
     * Prepare a statement. In case no connection has been given, allocate a
     * new one.
     *
     * @param sql  The SQL statement to be executed
     *
     * @throws GenericDataSourceException
     * @throws GenericEntityException
     */
    public void prepareStatement(String sql, boolean specifyTypeAndConcur, int resultSetType, int resultSetConcurrency, int fetchSize, int maxRows) throws GenericDataSourceException, GenericEntityException {
        if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.prepareStatement] sql=" + sql, module);

        if (_connection == null) {
            getConnection();
        }

        try {
            _sql = sql;
            _ind = 1;
            if (specifyTypeAndConcur) {
                _ps = _connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
                if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.prepareStatement] _ps=" + _ps, module);
            } else {
                _ps = _connection.prepareStatement(sql);
                if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.prepareStatement] (def) _ps=" + _ps, module);
            }
            if (maxRows > 0) {
                _ps.setMaxRows(maxRows);
                if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.prepareStatement] max rows set : " + maxRows, module);
            }
            this.setFetchSize(_ps, fetchSize);
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while executing the following:" + sql, sqle);
        }
    }

    /**
     * Execute a query based on the prepared statement
     *
     * @return The result set of the query
     * @throws GenericDataSourceException
     */
    public ResultSet executeQuery() throws GenericDataSourceException {
        try {
            // if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.executeQuery] ps=" + _ps.toString(), module);
            _rs = _ps.executeQuery();
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }

        return _rs;
    }

    /**
     * Execute a query baed ont SQL string given
     *
     * @param sql  The SQL string to be executed
     * @return  The result set of the query
     * @throws GenericEntityException
     * @throws GenericDataSourceException
     */
    public ResultSet executeQuery(String sql) throws GenericDataSourceException, GenericEntityException {
        prepareStatement(sql);
        return executeQuery();
    }

    /**
     * Execute updates
     *
     * @return  The number of rows updated
     * @throws GenericDataSourceException
     */
    public int executeUpdate() throws GenericDataSourceException {
        try {
            // if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.executeUpdate] ps=" + _ps.toString(), module);
            return _ps.executeUpdate();
        } catch (SQLException sqle) {
            // don't display this here, may not be critical, allow handling further up... Debug.logError(sqle, "SQLProcessor.executeUpdate() : ERROR : ", module);
            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }
    }

    /**
     * Execute update based on the SQL statement given
     *
     * @param sql  SQL statement to be executed
     * @throws GenericDataSourceException
     */
    public int executeUpdate(String sql) throws GenericDataSourceException {
        Statement stmt = null;

        try {
            stmt = _connection.createStatement();
            return stmt.executeUpdate(sql);
        } catch (SQLException sqle) {
            Debug.logError(sqle, "SQLProcessor.executeUpdate(sql) : ERROR : ", module);
            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqle) {
                    Debug.logWarning("Unable to close 'statement': " + sqle.getMessage(), module);
                }
            }
        }
    }

    /**
     * Test if there more records available
     *
     * @return true, if there more records available
     *
     * @throws GenericDataSourceException
     */
    public boolean next() throws GenericDataSourceException {
        try {
            return _rs.next();
        } catch (SQLException sqle) {
            throw new GenericDataSourceException("SQL Exception while executing the following:" + _sql, sqle);
        }
    }

    /**
     * Getter: get the currently activ ResultSet
     *
     * @return ResultSet
     */
    public ResultSet getResultSet() {
        return _rs;
    }

    /**
     * Getter: get the prepared statement
     *
     * @return PreparedStatement
     */
    public PreparedStatement getPreparedStatement() {
        return _ps;
    }

    /**
     * Execute a query based on the SQL string given. For each record
     * of the ResultSet return, execute a callback function
     *
     * @param sql       The SQL string to be executed
     * @param aListener The callback function object
     *
     * @throws GenericEntityException
     */
    public void execQuery(String sql, ExecQueryCallbackFunctionIF aListener) throws GenericEntityException {
        if (_connection == null) {
            getConnection();
        }

        try {
            if (Debug.verboseOn()) Debug.logVerbose("[SQLProcessor.execQuery]: " + sql, module);
            executeQuery(sql);

            // process the results by calling the listener for
            // each row...
            boolean keepGoing = true;

            while (keepGoing && _rs.next()) {
                keepGoing = aListener.processNextRow(_rs);
            }

            if (_manualTX) {
                _connection.commit();
            }

        } catch (SQLException sqle) {
            Debug.logWarning("[SQLProcessor.execQuery]: SQL Exception while executing the following:\n" +
                sql + "\nError was:", module);
            Debug.logWarning(sqle.getMessage(), module);
            throw new GenericEntityException("SQL Exception while executing the following:" + _sql, sqle);
        } finally {
            close();
        }
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(String field) throws SQLException {
        //ResultSetMetaData rsmd = this.getResultSetMetaData();
        //this doesn't seem to work, query not yet parsed: int colType = rsmd.getColumnType(_ind);
        if (field != null) {
            //if (field.length() > 4000) {
                //Clob clb = new Cl
                // doesn't work with Oracle drivers, need the funky work-around: _ps.setCharacterStream(_ind, new StringReader(field), field.length());
                //_needClobWorkAroundWrite.put(new Integer(_ind), field);
                //_ps.setString(_ind, " ");
            //} else {
                _ps.setString(_ind, field);
            //}
        } else {
            // silly workaround for Derby (Cloudscape 10 beta Bug #5928)
            // this should be removed after the know bug is fixed
            try {
                _ps.setNull(_ind, Types.VARCHAR);
            } catch (SQLException e) {
                try {
                    _ps.setString(_ind, null);
                } catch (SQLException e2) {
                    Debug.logError(e2, module);
                    throw e;
                }
            }
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(java.sql.Timestamp field) throws SQLException {
        if (field != null) {
            _ps.setTimestamp(_ind, field);
        } else {
            _ps.setNull(_ind, Types.TIMESTAMP);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(java.sql.Time field) throws SQLException {
        if (field != null) {
            _ps.setTime(_ind, field);
        } else {
            _ps.setNull(_ind, Types.TIME);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(java.sql.Date field) throws SQLException {
        if (field != null) {
            _ps.setDate(_ind, field);
        } else {
            _ps.setNull(_ind, Types.DATE);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Integer field) throws SQLException {
        if (field != null) {
            _ps.setInt(_ind, field.intValue());
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Long field) throws SQLException {
        if (field != null) {
            _ps.setLong(_ind, field.longValue());
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Float field) throws SQLException {
        if (field != null) {
            _ps.setFloat(_ind, field.floatValue());
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Double field) throws SQLException {
        if (field != null) {
            _ps.setDouble(_ind, field.doubleValue());
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(BigDecimal field) throws SQLException {
        if (field != null) {
            _ps.setBigDecimal(_ind, field);
        } else {
            _ps.setNull(_ind, Types.NUMERIC);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Boolean field) throws SQLException {
        if (field != null) {
            _ps.setBoolean(_ind, field.booleanValue());
        } else {
            _ps.setNull(_ind, Types.NULL); // TODO: really should be Types.BOOLEAN, but that wasn't introduced until Java 1.4... hmmm what to do?
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement.
     *
     * @param field
     *
     * @throws SQLException
     */
    public void setValue(Object field) throws SQLException {
        if (field != null) {
            _ps.setObject(_ind, field, Types.JAVA_OBJECT);
        } else {
            _ps.setNull(_ind, Types.JAVA_OBJECT);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement
     * 
     * @param field
     * 
     * @throws SQLException
     */
    public void setValue(Blob field) throws SQLException {
        if (field != null) {
            _ps.setBlob(_ind, field);
        } else {
            _ps.setNull(_ind, Types.BLOB);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement
     * 
     * @param field
     * 
     * @throws SQLException
     */
    public void setValue(Clob field) throws SQLException {
        if (field != null) {
            _ps.setClob(_ind, field);
        } else {
            _ps.setNull(_ind, Types.CLOB);
        }
        _ind++;
    }

    /**
     * Set the next binding variable of the currently active prepared statement
     * to write the serialized data of 'field' to a BLOB.
     * 
     * @param field
     * 
     * @throws SQLException
     */
    public void setBinaryStream(Object field) throws SQLException {
        if (field != null) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(field);
                oos.close();
    
                byte[] buf = os.toByteArray();
                os.close();
                ByteArrayInputStream is = new ByteArrayInputStream(buf);
                _ps.setBinaryStream(_ind, is, buf.length);
                is.close();
            } catch (IOException ex) {
                throw new SQLException(ex.getMessage());
            }
        } else {
            DatasourceInfo datasourceInfo = EntityConfigUtil.getDatasourceInfo(this.helperName);
            if (datasourceInfo.useBinaryTypeForBlob) {
                _ps.setNull(_ind, Types.BINARY);
            } else {
                _ps.setNull(_ind, Types.BLOB);
            }
        }

        _ind++;
    }

    protected void finalize() throws Throwable {
        try {
            this.close();
        } catch (Exception e) {
            Debug.logError(e, "Error closing the result, connection, etc in finalize SQLProcessor", module);
        }
        super.finalize();
    }
    
    protected void testConnection(Connection con) throws GenericEntityException {
        if (SQLProcessor.ENABLE_TEST) {
            if (SQLProcessor.CONNECTION_TEST_LIST.contains(con.toString())) {
                throw new GenericEntityException("Connection the exact same as index " + SQLProcessor.CONNECTION_TEST_LIST.indexOf(con.toString()));            
            } 
            SQLProcessor.CONNECTION_TEST_LIST.add(con.toString());
            if (SQLProcessor.CONNECTION_TEST_LIST.size() > SQLProcessor.MAX_CONNECTIONS) {
                SQLProcessor.CONNECTION_TEST_LIST.remove(0);    
            }
        }
    }

    protected void setFetchSize(Statement stmt, int fetchSize) throws SQLException {
        // do not set fetch size when using the cursor connection
        if (_connection instanceof CursorConnection) return;

        // check if the statement was called with a specific fetchsize, if not grab the default from the datasource
        if (fetchSize < 0) {
            DatasourceInfo ds = EntityConfigUtil.getDatasourceInfo(helperName);
            if (ds != null) {
                fetchSize = ds.resultFetchSize;
            } else {
                Debug.logWarning("DatasourceInfo is null, not setting fetch size!", module);
            }
        }

        // otherwise only set if the size is > -1 (0 is sometimes used to note ALL rows)
        if (fetchSize > -1) {
            stmt.setFetchSize(fetchSize);
        }
    }
}
