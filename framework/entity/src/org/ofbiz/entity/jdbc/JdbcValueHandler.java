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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;

/**
 * An object that handles getting/setting column values in JDBC
 * <code>PreparedStatement</code> and <code>ResultSet</code> objects.
 *
 */
public abstract class JdbcValueHandler {
    public static final String module = JdbcValueHandler.class.getName();
    private static final Map<String, JdbcValueHandler> JdbcValueHandlerMap = createJdbcValueHandlerMap();
    private static final Map<String, Integer> SqlTypeMap = createSqlTypeMap();

    private static Map<String, JdbcValueHandler> createJdbcValueHandlerMap() {
        /*
        This Map is used to select the correct JdbcValueHandler
        for the specified Java type. The JdbcValueHandler instances are
        initialized with the SQL type recommended by Sun/Oracle.
         */
        Map<String, JdbcValueHandler> result = FastMap.newInstance();
        // JDBC 1
        result.put("byte[]", new ByteArrayJdbcValueHandler(Types.LONGVARBINARY));
        result.put("java.lang.Boolean", new BooleanJdbcValueHandler(Types.BOOLEAN));
        result.put("Boolean", new BooleanJdbcValueHandler(Types.BOOLEAN));
        result.put("java.lang.Double", new DoubleJdbcValueHandler(Types.DOUBLE));
        result.put("Double", new DoubleJdbcValueHandler(Types.DOUBLE));
        result.put("java.lang.Float", new FloatJdbcValueHandler(Types.FLOAT));
        result.put("Float", new FloatJdbcValueHandler(Types.FLOAT));
        result.put("java.lang.Integer", new IntegerJdbcValueHandler(Types.INTEGER));
        result.put("Integer", new IntegerJdbcValueHandler(Types.INTEGER));
        result.put("java.lang.Long", new LongJdbcValueHandler(Types.BIGINT));
        result.put("Long", new LongJdbcValueHandler(Types.BIGINT));
        result.put("java.lang.Short", new ShortJdbcValueHandler(Types.SMALLINT));
        result.put("Short", new ShortJdbcValueHandler(Types.SMALLINT));
        result.put("java.lang.String", new StringJdbcValueHandler(Types.CHAR));
        result.put("String", new StringJdbcValueHandler(Types.CHAR));
        result.put("java.sql.Date", new DateJdbcValueHandler(Types.DATE));
        result.put("Date", new DateJdbcValueHandler(Types.DATE));
        result.put("java.sql.Time", new TimeJdbcValueHandler(Types.TIME));
        result.put("Time", new TimeJdbcValueHandler(Types.TIME));
        result.put("java.sql.Timestamp", new TimestampJdbcValueHandler(Types.TIMESTAMP));
        result.put("Timestamp", new TimestampJdbcValueHandler(Types.TIMESTAMP));
        // JDBC 2
        result.put("java.math.BigDecimal", new BigDecimalJdbcValueHandler(Types.DECIMAL));
        result.put("BigDecimal", new BigDecimalJdbcValueHandler(Types.DECIMAL));
        result.put("java.sql.Blob", new BlobJdbcValueHandler(Types.BLOB));
        result.put("Blob", new BlobJdbcValueHandler(Types.BLOB));
        result.put("java.sql.Clob", new ClobJdbcValueHandler(Types.CLOB));
        result.put("Clob", new ClobJdbcValueHandler(Types.CLOB));
        // Non-JDBC Types
        result.put("java.lang.Object", new ObjectJdbcValueHandler(Types.BLOB));
        result.put("Object", new ObjectJdbcValueHandler(Types.BLOB));
        return result;
    }

    private static Map<String, Integer> createSqlTypeMap() {
        /*
        This Map is used to select the correct SQL data type
        for the PreparedStatement.setNull method. The setNull
        method must be called with the correct type, or an
        exception will be thrown.
         */
        Map<String, Integer> result = FastMap.newInstance();
        // SQL 99 Data Types
        result.put("BIT", Types.BIT);
        result.put("BLOB", Types.BLOB);
        result.put("BOOLEAN", Types.BOOLEAN);
        result.put("CHAR", Types.CHAR);
        result.put("CHARACTER", Types.CHAR);
        result.put("CLOB", Types.CLOB);
        result.put("DATE", Types.DATE);
        result.put("DEC", Types.DECIMAL);
        result.put("DECIMAL", Types.DECIMAL);
        result.put("DOUBLE", Types.DOUBLE);
        result.put("DOUBLE PRECISION", Types.DOUBLE);
        result.put("FLOAT", Types.FLOAT);
        result.put("INT", Types.INTEGER);
        result.put("INTEGER", Types.INTEGER);
        result.put("NUMERIC", Types.NUMERIC);
        result.put("REAL", Types.REAL);
        result.put("SMALLINT", Types.SMALLINT);
        result.put("TIME", Types.TIME);
        result.put("TIMESTAMP", Types.TIMESTAMP);
        result.put("VARCHAR", Types.VARCHAR);
        result.put("CHAR VARYING", Types.VARCHAR);
        result.put("CHARACTER VARYING", Types.VARCHAR);
        // DB2, MS SQL Data Types
        result.put("LONGVARCHAR", Types.LONGVARCHAR);
        result.put("LONG VARCHAR", Types.LONGVARCHAR);
        result.put("BIGINT", Types.BIGINT);
        result.put("TEXT", Types.LONGVARCHAR);
        result.put("IMAGE", Types.BLOB);
        result.put("BINARY", Types.BINARY);
        result.put("VARBINARY", Types.VARBINARY);
        result.put("LONGVARBINARY", Types.LONGVARBINARY);
        result.put("LONG VARBINARY", Types.LONGVARBINARY);
        // Note: Do NOT map the DATETIME SQL data type, the
        // java-type will be used to select the correct data type
        return result;
    }

    /** Returns the <code>JdbcValueHandler</code> that corresponds to a field
     * type.
     *  
     * @param javaType The Java type specified in fieldtype*.xml
     * @param sqlType The SQL type specified in fieldtype*.xml
     * @return A <code>JdbcValueHandler</code> instance
     */
    public static JdbcValueHandler getInstance(String javaType, String sqlType) {
        JdbcValueHandler handler = JdbcValueHandlerMap.get(javaType);
        if (handler != null) {
            String key = parseSqlType(sqlType);
            Integer sqlTypeInt = SqlTypeMap.get(key);
            if (sqlTypeInt != null) {
                handler = handler.create(sqlTypeInt);
            }
        }
        return handler;
    }

    protected static String parseSqlType(String sqlType) {
        String result = sqlType.toUpperCase();
        int pos = result.indexOf("(");
        if (pos != -1) {
            result = result.substring(0, pos);
        }
        return result;
    }

    protected static byte[] serializeObject(Object obj) throws SQLException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(os);
            oos.writeObject(obj);
            os.close();
        } catch (IOException e) {
            throw new SQLException(e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {}
            }
        }
        return os.toByteArray();
    }

    protected static byte[] toByteArray(java.sql.Blob blob) throws SQLException {
        InputStream inStream = null;
        try {
            inStream = blob.getBinaryStream();
            int blobLength = (int) blob.length();
            byte[] byteBuffer = new byte[blobLength];
            int offset = 0;
            int bytesRead = inStream.read(byteBuffer, offset, blobLength);
            while (bytesRead > 0) {
                offset += bytesRead;
                bytesRead = inStream.read(byteBuffer, offset, blobLength);
            }
            return byteBuffer;
        } catch (Exception e) {
            throw new SQLException(e);
        }
        finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {}
            }
        }
    }

    // The target database SQL type to be used for the
    // PreparedStatement.setNull method.
    private final int sqlType;

    protected JdbcValueHandler(int sqlType) {
        this.sqlType = sqlType;
    }

    /** Sets a value in a <code>PreparedStatement</code>. The
     * <code>obj</code> argument is converted to the correct data
     * type. Subclasses override this method to cast <code>obj</code>
     * to the correct data type and call the appropriate
     * <code>PreparedStatement.setXxx</code> method.
     * 
     * @param ps
     * @param parameterIndex
     * @param obj
     * @throws SQLException
     */
    protected abstract void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException;

    protected JdbcValueHandler create(int sqlType) {
        if (sqlType == this.getSqlType()) {
            return this;
        }
        return newInstance(sqlType);
    }

    /**
     * Returns the SQL type for this handler.
     * @return
     * @see <code>java.sql.Types</code>
     */
    public int getSqlType() {
        return this.sqlType;
    }

    /** Returns a value from a <code>ResultSet</code>. The returned
     * object is converted to the Java data type specified in the fieldtype
     * file.
     * 
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException
     */
    public abstract Object getValue(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * Returns a new instance of the object - initialized with
     * the specified SQL type.
     * @param sqlType
     * @return
     */
    protected abstract JdbcValueHandler newInstance(int sqlType);

    /** Sets a value in a <code>PreparedStatement</code>. The
     * <code>obj</code> argument is converted to the correct data
     * type.
     * 
     * @param ps
     * @param parameterIndex
     * @param obj
     * @throws SQLException
     */
    public void setValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
        if (obj == null) {
            ps.setNull(parameterIndex, this.getSqlType());
            return;
        }
        this.castAndSetValue(ps, parameterIndex, obj);
    }

    /**
     * A <code>java.math.BigDecimal</code> JDBC value handler.
     */
    protected static class BigDecimalJdbcValueHandler extends JdbcValueHandler {
        protected BigDecimalJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setBigDecimal(parameterIndex, (java.math.BigDecimal) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getBigDecimal(columnIndex);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new BigDecimalJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.sql.Blob</code> JDBC value handler.
     */
    protected static class BlobJdbcValueHandler extends JdbcValueHandler {
        protected BlobJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            try {
                // FIXME: This is here for backwards compatibility. Client code
                // that uses a Blob java-type for a byte array should use a
                // byte[] java-type instead.
                byte[] bytes = (byte[]) obj;
                Debug.logWarning("Blob java-type used for byte array. Use byte[] java-type instead.", module);
                ps.setBytes(parameterIndex, bytes);
                return;
            } catch (ClassCastException e) {}
            ps.setBlob(parameterIndex, (Blob) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            // FIXME: This code is here for backwards compatibility. Client code
            // that uses a Blob java-type for non-Blob types should be updated
            // to use the correct type.
            Object originalObject;
            byte[] fieldBytes;
            try {
                Blob theBlob = rs.getBlob(columnIndex);
                fieldBytes = toByteArray(theBlob);
                originalObject = theBlob;
            } catch (SQLException e) {
                // for backward compatibility if getBlob didn't work try getBytes
                fieldBytes = rs.getBytes(columnIndex);
                originalObject = fieldBytes;
            }
            if (originalObject != null) {
                // for backward compatibility, check to see if there is a serialized object and if so return that
                Object blobObject = null;
                ObjectInputStream in = null;
                try {
                    in = new ObjectInputStream(new ByteArrayInputStream(fieldBytes));
                    blobObject = in.readObject();
                } catch (IOException e) {
                    if (Debug.verboseOn()) Debug.logVerbose(e, "Unable to read BLOB data from input stream", module);
                } catch (ClassNotFoundException e) {
                    if (Debug.verboseOn()) Debug.logVerbose(e, "Class not found: Unable to cast BLOB data to an Java object while getting value, most likely because it is a straight byte[], so just using the raw bytes", module);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {}
                    }
                }
                if (blobObject != null) {
                    Debug.logWarning("Blob java-type used for java.lang.Object. Use java.lang.Object java-type instead.", module);
                    return blobObject;
                } else {
                    if (originalObject instanceof Blob) {
                        // NOTE using SerialBlob here instead of the Blob from the database to make sure we can pass it around, serialize it, etc
                        return new SerialBlob((Blob) originalObject);
                    } else {
                        Debug.logWarning("Blob java-type used for byte array. Use byte[] java-type instead.", module);
                        return originalObject;
                    }
                }
            }
            return null;
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new BlobJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Boolean</code> JDBC value handler.
     */
    protected static class BooleanJdbcValueHandler extends JdbcValueHandler {
        protected BooleanJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setBoolean(parameterIndex, (Boolean) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            boolean value = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : Boolean.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new BooleanJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>byte[]</code> JDBC value handler.
     */
    protected static class ByteArrayJdbcValueHandler extends JdbcValueHandler {
        protected ByteArrayJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setBytes(parameterIndex, (byte[]) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            byte[] value = rs.getBytes(columnIndex);
            return rs.wasNull() ? null : value;
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new ByteArrayJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.sql.Clob</code> JDBC value handler.
     */
    protected static class ClobJdbcValueHandler extends JdbcValueHandler {
        protected ClobJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            try {
                // FIXME: This is here for backwards compatibility. Client code
                // that uses a Clob java-type for a java.lang.String should use a
                // java.lang.String java-type instead.
                String str = (String) obj;
                Debug.logWarning("Clob java-type used for java.lang.String. Use java.lang.String java-type instead.", module);
                ps.setString(parameterIndex, str);
                return;
            } catch (ClassCastException e) {}
            ps.setClob(parameterIndex, (java.sql.Clob) obj);
            return;
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            java.sql.Clob clob = rs.getClob(columnIndex);
            if (clob == null || clob.length() == 0) {
                return null;
            }
            Reader clobReader = null;
            try {
                clobReader = clob.getCharacterStream();
                int clobLength = (int) clob.length();
                char[] charBuffer = new char[clobLength];
                int offset = 0;
                int charsRead = clobReader.read(charBuffer, offset, clobLength);
                while (charsRead > 0) {
                    offset += charsRead;
                    charsRead = clobReader.read(charBuffer, offset, clobLength);
                }
                // FIXME: This is here for backwards compatibility. Client code
                // that uses a Clob java-type for a java.lang.String should use a
                // java.lang.String java-type instead.
                return new String(charBuffer);
            } catch (IOException e) {
                throw new SQLException(e);
            }
            finally {
                if (clobReader != null) {
                    try {
                        clobReader.close();
                    } catch (IOException e) {}
                }
            }
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new ClobJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.sql.Date</code> JDBC value handler.
     */
    protected static class DateJdbcValueHandler extends JdbcValueHandler {
        protected DateJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setDate(parameterIndex, (java.sql.Date) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getDate(columnIndex);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new DateJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Double</code> JDBC value handler.
     */
    protected static class DoubleJdbcValueHandler extends JdbcValueHandler {
        protected DoubleJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setDouble(parameterIndex, (Double) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            double value = rs.getDouble(columnIndex);
            return rs.wasNull() ? null : Double.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new DoubleJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Float</code> JDBC value handler.
     */
    protected static class FloatJdbcValueHandler extends JdbcValueHandler {
        protected FloatJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setFloat(parameterIndex, (Float) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            float value = rs.getFloat(columnIndex);
            return rs.wasNull() ? null : Float.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new FloatJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Integer</code> JDBC value handler.
     */
    protected static class IntegerJdbcValueHandler extends JdbcValueHandler {
        protected IntegerJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setInt(parameterIndex, (Integer) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            int value = rs.getInt(columnIndex);
            return rs.wasNull() ? null : Integer.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new IntegerJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Long</code> JDBC value handler.
     */
    protected static class LongJdbcValueHandler extends JdbcValueHandler {
        protected LongJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setLong(parameterIndex, (Long) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            long value = rs.getLong(columnIndex);
            return rs.wasNull() ? null : Long.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new LongJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Object</code> JDBC value handler.
     */
    protected static class ObjectJdbcValueHandler extends JdbcValueHandler {
        protected ObjectJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setBytes(parameterIndex, serializeObject(obj));
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            ObjectInputStream in = null;
            InputStream bis = null;
            try {
                bis = rs.getBinaryStream(columnIndex);
                if (bis == null) {
                    return null;
                }
                in = new ObjectInputStream(bis);
                return in.readObject();
            } catch (Exception e) {
                throw new SQLException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {}
                }
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {}
                }
            }
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new ObjectJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.Short</code> JDBC value handler.
     */
    protected static class ShortJdbcValueHandler extends JdbcValueHandler {
        protected ShortJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setShort(parameterIndex, (Short) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            short value = rs.getShort(columnIndex);
            return rs.wasNull() ? null : Short.valueOf(value);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new ShortJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.lang.String</code> JDBC value handler.
     */
    protected static class StringJdbcValueHandler extends JdbcValueHandler {
        protected StringJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setString(parameterIndex, (String) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getString(columnIndex);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new StringJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.sql.Time</code> JDBC value handler.
     */
    protected static class TimeJdbcValueHandler extends JdbcValueHandler {
        protected TimeJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setTime(parameterIndex, (java.sql.Time) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getTime(columnIndex);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            return new TimeJdbcValueHandler(sqlType);
        }
    }

    /**
     * A <code>java.sql.Timestamp</code> JDBC value handler.
     * <p>This <code>JdbcValueHandler</code> accommodates databases that
     * don't support sub-second precision. If the date-time field type
     * is a <code>CHAR(30)</code> SQL type, <code>java.sql.Timestamp</code>s
     * will be stored as JDBC timestamp escape format strings
     * (<code>yyyy-mm-dd hh:mm:ss.fffffffff</code>), referenced to UTC.</p> 
     */
    protected static class TimestampJdbcValueHandler extends JdbcValueHandler {
        protected TimestampJdbcValueHandler(int jdbcType) {
            super(jdbcType);
        }
        @Override
        protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
            ps.setTimestamp(parameterIndex, (java.sql.Timestamp) obj);
        }
        @Override
        public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getTimestamp(columnIndex);
        }
        @Override
        protected JdbcValueHandler newInstance(int sqlType) {
            if (sqlType == Types.CHAR) {
                return new TimestampJdbcValueHandler(sqlType) {
                    protected void castAndSetValue(PreparedStatement ps, int parameterIndex, Object obj) throws SQLException {
                        ps.setString(parameterIndex, ((java.sql.Timestamp) obj).toString());
                    }
                    public Object getValue(ResultSet rs, int columnIndex) throws SQLException {
                        String str = rs.getString(columnIndex);
                        if (str == null) {
                            return null;
                        }
                        return Timestamp.valueOf(str);
                    }
                };
            } else {
                return new TimestampJdbcValueHandler(sqlType);
            }
        }
    }
}
