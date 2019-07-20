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
package org.apache.ofbiz.datafile;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.common.login.LoginServices;

/**
 * Record
 */
@SuppressWarnings("serial")
public class Record implements Serializable {

    /** Contains a map with field data by name */
    protected Map<String, Object> fields;

    /** Contains the name of the record definition */
    protected String recordName;

    /** Contains the definition for the record */
    protected transient ModelRecord modelRecord;

    protected Record parentRecord = null;
    protected List<Record> childRecords = new ArrayList<>();

    /** Creates new Record */
    protected Record(ModelRecord modelRecord) {
        if (modelRecord == null) {
            throw new IllegalArgumentException("Cannont create a Record with a null modelRecord parameter");
        }
        this.recordName = modelRecord.name;
        this.modelRecord = modelRecord;
        this.fields = new HashMap<>();
    }

    /** Creates new Record from existing Map */
    protected Record(ModelRecord modelRecord, Map<String, Object> fields) {
        if (modelRecord == null) {
            throw new IllegalArgumentException("Cannont create a Record with a null modelRecord parameter");
        }
        this.recordName = modelRecord.name;
        this.modelRecord = modelRecord;
        this.fields = (fields == null ? new HashMap<>() : new HashMap<>(fields));
    }

    public String getRecordName() {
        return recordName;
    }

    public ModelRecord getModelRecord() {
        if (modelRecord == null) {
            throw new IllegalStateException("[Record.getModelRecord] could not find modelRecord for recordName " + recordName);
        }
        return modelRecord;
    }

    public synchronized Object get(String name) {
        if (getModelRecord().getModelField(name) == null) {
            throw new IllegalArgumentException("[Record.get] \"" + name + "\" is not a field of " + recordName);
        }
        return fields.get(name);
    }

    public String getString(String name) {
        Object object = get(name);

        if (object == null) {
            return null;
        }
        if (object instanceof java.lang.String) {
            return (String) object;
        }
        return object.toString();
    }

    public String getStringAndEmpty(String name) {
        Object object = get(name);

        if (object == null) {
            return "";
        }
        if (object instanceof java.lang.String) {
            return (String) object;
        }
        return object.toString();
    }

    public java.sql.Timestamp getTimestamp(String name) {
        return (java.sql.Timestamp) get(name);
    }

    public java.sql.Time getTime(String name) {
        return (java.sql.Time) get(name);
    }

    public java.sql.Date getDate(String name) {
        return (java.sql.Date) get(name);
    }

    public Integer getInteger(String name) {
        return (Integer) get(name);
    }

    public Long getLong(String name) {
        return (Long) get(name);
    }

    public Float getFloat(String name) {
        return (Float) get(name);
    }

    public Double getDouble(String name) {
        return (Double) get(name);
    }

    /** Sets the named field to the passed value, even if the value is null
     * @param name The field name to set
     * @param value The value to set
     */
    public void set(String name, Object value) {
        set(name, value, true);
    }

    /** Sets the named field to the passed value. If value is null, it is only
     *  set if the setIfNull parameter is true.
     * @param name The field name to set
     * @param value The value to set
     * @param setIfNull Specifies whether or not to set the value if it is null
     */
    public synchronized void set(String name, Object value, boolean setIfNull) {
        if (getModelRecord().getModelField(name) == null) {
            throw new IllegalArgumentException("[Record.set] \"" + name + "\" is not a field of " + recordName);
        }
        if (value != null || setIfNull) {
            if (value instanceof Boolean) {
                value = (Boolean) value ? "Y" : "N";
            }
            fields.put(name, value);
        }
    }

    /**
     * little endian reader for 2 byte short.
     */
    private static short readLEShort(byte[] byteArray) {
        return (short) ((byteArray[1] & 0xff) << 8 | (byteArray[0] & 0xff));

    }

    /**
     * little endian reader for 4 byte int.
     */
    private static int readLEInt(byte[] byteArray) {
        return (byteArray[3]) << 24 | (byteArray[2] & 0xff) << 16 | (byteArray[1] & 0xff) << 8 | (byteArray[0] & 0xff);
    }

    /**
     * little endian reader for 8 byte long.
     */
    private static long readLELong(byte[] byteArray) {
        return (long) (byteArray[7]) << 56 | /* long cast needed or shift done modulo 32 */
               (long) (byteArray[6] & 0xff) << 48 | (long) (byteArray[5] & 0xff) << 40 | (long) (byteArray[4] & 0xff) << 32 | (long) (byteArray[3] & 0xff) << 24
               | (long) (byteArray[2] & 0xff) << 16 | (long) (byteArray[1] & 0xff) << 8 | (byteArray[0] & 0xff);
    }

    /** Sets the named field to the passed value, converting the value from a String to the current type using <code>Type.valueOf()</code>
     * @param name The field name to set
     * @param value The String value to convert and set
     */
    public void setString(String name, String value) throws ParseException {
        if (name == null || value == null || value.equals("")) {
            return;
        }
        ModelField field = getModelRecord().getModelField(name);

        if (field == null) {
            set(name, value); // this will get an error in the set() method...
        }

        // if the string is all spaces ignore
        boolean nonSpace = false;

        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != ' ') {
                nonSpace = true;
                break;
            }
        }
        if (!nonSpace) {
            return;
        }

        String fieldType = field.type;

        // first the custom types that need to be parsed
        if (fieldType.equals("CustomTimestamp")) {
            // this custom type will take a string a parse according to date formatting
            // string then put the result in a java.sql.Timestamp
            // a common timestamp format for flat files is with no separators: yyyyMMddHHmmss
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.util.Date tempDate = sdf.parse(value);
            java.sql.Timestamp timestamp = new java.sql.Timestamp(tempDate.getTime());

            set(name, timestamp);
        } else if (fieldType.equals("CustomDate")) {
            // a common date only format for flat files is with no separators: yyyyMMdd or MMddyyyy
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.util.Date tempDate = sdf.parse(value);
            java.sql.Date date = new java.sql.Date(tempDate.getTime());

            set(name, date);
        } else if (fieldType.equals("CustomTime")) {
            // a common time only format for flat files is with no separators: HHmmss
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.util.Date tempDate = sdf.parse(value);
            java.sql.Time time = new java.sql.Time(tempDate.getTime());

            set(name, time);
        } else if (fieldType.equals("FixedPointDouble")) {
            // this custom type will parse a fixed point number according to the number
            // of decimal places in the formatting string then place it in a Double
            NumberFormat nf = NumberFormat.getNumberInstance();
            Number tempNum = nf.parse(value);
            double number = tempNum.doubleValue();
            double decimalPlaces = Double.parseDouble(field.format);
            double divisor = Math.pow(10.0, decimalPlaces);

            number = number / divisor;
            set(name, number);
        } // standard types
        else if (fieldType.equals("java.lang.String") || fieldType.equals("String")) {
            if (field.format.equals("EncryptedString")) {
                String hashType = LoginServices.getHashType();
                set(name, HashCrypt.digestHash(hashType, value.getBytes(StandardCharsets.UTF_8)));
            } else {
                set(name, value);
            }
        } else if (fieldType.equals("NullTerminatedString")) {
            int terminate = value.indexOf(0x0);
            set(name, terminate > 0 ? value.substring(0, terminate) : value);
        } else if (fieldType.equals("java.sql.Timestamp") || fieldType.equals("Timestamp")) {
            set(name, java.sql.Timestamp.valueOf(value));
        } else if (fieldType.equals("java.sql.Time") || fieldType.equals("Time")) {
            set(name, java.sql.Time.valueOf(value));
        } else if (fieldType.equals("java.sql.Date") || fieldType.equals("Date")) {
            set(name, java.sql.Date.valueOf(value));
        } else if (fieldType.equals("java.lang.Integer") || fieldType.equals("Integer")) {
            set(name, Integer.valueOf(value));
        } else if (fieldType.equals("java.lang.Long") || fieldType.equals("Long")) {
            set(name, Long.valueOf(value));
        } else if (fieldType.equals("java.lang.Float") || fieldType.equals("Float")) {
            set(name, Float.valueOf(value));
        } else if (fieldType.equals("java.lang.Double") || fieldType.equals("Double")) {
            set(name, Double.valueOf(value));
        } else if (fieldType.equals("LEShort")) {
            set(name, readLEShort(value.getBytes(StandardCharsets.UTF_8)));
        } else if (fieldType.equals("LEInteger")) {
            set(name, readLEInt(value.getBytes(StandardCharsets.UTF_8)));
        } else if (fieldType.equals("LELong")) {
            set(name, readLELong(value.getBytes(StandardCharsets.UTF_8)));
        } else {
            throw new IllegalArgumentException("Field type " + fieldType + " not currently supported. Sorry.");
        }
    }

    public String getFixedString(String name) {
        if (name == null) {
            return null;
        }
        ModelField field = getModelRecord().getModelField(name);

        if (field == null) {
            throw new IllegalArgumentException("Could not find model for field named \"" + name + "\"");
        }

        Object value = get(name);

        if (value == null) {
            return null;
        }

        String fieldType = field.type;
        String str = null;

        // first the custom types that need to be parsed
        if (fieldType.equals("CustomTimestamp")) {
            // a common timestamp format for flat files is with no separators: yyyyMMddHHmmss
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.sql.Timestamp timestamp = (java.sql.Timestamp) value;

            str = sdf.format(new Date(timestamp.getTime()));
        } else if (fieldType.equals("CustomDate")) {
            // a common date only format for flat files is with no separators: yyyyMMdd or MMddyyyy
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.sql.Date date = (java.sql.Date) value;

            str = sdf.format(new Date(date.getTime()));
        } else if (fieldType.equals("CustomTime")) {
            // a common time only format for flat files is with no separators: HHmmss
            SimpleDateFormat sdf = new SimpleDateFormat(field.format);
            java.sql.Time time = (java.sql.Time) value;

            str = sdf.format(new Date(time.getTime()));
        } else if (fieldType.equals("FixedPointDouble")) {
            // this custom type will parse a fixed point number according to the number
            // of decimal places in the formatting string then place it in a Double
            double decimalPlaces = Double.parseDouble(field.format);
            double multiplier = Math.pow(10.0, decimalPlaces);
            double dnum = multiplier * (Double) value;
            long number = Math.round(dnum);

            str = padFrontZeros(Long.toString(number), field.length);
        } // standard types
        else if (fieldType.equals("java.lang.String") || fieldType.equals("String")) {
            str = value.toString();
        } else if (fieldType.equals("java.sql.Timestamp") || fieldType.equals("Timestamp")) {
            str = value.toString();
        } else if (fieldType.equals("java.sql.Time") || fieldType.equals("Time")) {
            str = value.toString();
        } else if (fieldType.equals("java.sql.Date") || fieldType.equals("Date")) {
            str = value.toString();
        } else if (fieldType.equals("java.lang.Integer") || fieldType.equals("Integer")) {
            str = padFrontZeros(value.toString(), field.length);
        } else if (fieldType.equals("java.lang.Long") || fieldType.equals("Long")) {
            str = padFrontZeros(value.toString(), field.length);
        } else if (fieldType.equals("java.lang.Float") || fieldType.equals("Float")) {
            str = padFrontZeros(value.toString(), field.length);
        } else if (fieldType.equals("java.lang.Double") || fieldType.equals("Double")) {
            str = padFrontZeros(value.toString(), field.length);
        } else {
            throw new IllegalArgumentException("Field type " + fieldType + " not currently supported. Sorry.");
        }

        if (str != null && field.length > 0 && str.length() < field.length) {
            // pad the end with spaces
            StringBuilder strBuf = new StringBuilder(str);

            while (strBuf.length() < field.length) {
                strBuf.append(' ');
            }
            str = strBuf.toString();
        }
        return str;
    }

    public String writeLineString(ModelDataFile modelDataFile) throws DataFileException {
        ModelRecord modelRecord = getModelRecord();
        boolean isFixedRecord = ModelDataFile.SEP_FIXED_RECORD.equals(modelDataFile.separatorStyle);
        boolean isFixedLength = ModelDataFile.SEP_FIXED_LENGTH.equals(modelDataFile.separatorStyle);
        boolean isDelimited = ModelDataFile.SEP_DELIMITED.equals(modelDataFile.separatorStyle);

        StringBuilder lineBuf = new StringBuilder();

        for (ModelField modelField : modelRecord.fields) {
            String data = this.getFixedString(modelField.name);

            if (isDelimited && null != modelDataFile.textDelimiter) {
                lineBuf.append(modelDataFile.textDelimiter);
            }

            // if field is null (not set) then assume we want to pad the field
            char PAD_CHAR = ' ';

            if (data == null) {
                StringBuilder sb = new StringBuilder("");

                for (int i = 0; i < modelField.length; i++) {
                    sb.append(PAD_CHAR);
                }
                data = sb.toString();
            }

            // Pad the record
            if (isFixedRecord) {
                while (modelField.position > lineBuf.length()) {
                    lineBuf.append(" ");
                }
            }
            if (modelField.length > 0 && data.length() != modelField.length) {
                throw new DataFileException("Got field length " + data.length() + " but expected field length is " + modelField.length + " for field \"" + modelField.name
                                            + "\" of record \"" + modelRecord.name + "\" data is: \"" + data + "\"");
            }

            lineBuf.append(data);
            if (isDelimited) {
                if (null != modelDataFile.textDelimiter) {
                    lineBuf.append(modelDataFile.textDelimiter);
                }
                lineBuf.append(modelDataFile.delimiter);
            }
        }

        if (isDelimited) {
            // just remove the last delimiter to finish clean, otherwise shows as extra column
            lineBuf.setLength(lineBuf.length() - 1);
        }

        if ((isFixedRecord || isFixedLength) && modelDataFile.recordLength > 0 && lineBuf.length() != modelDataFile.recordLength) {
            throw new DataFileException("Got record length " + lineBuf.length() + " but expected record length is " + modelDataFile.recordLength + " for record \""
                                        + modelRecord.name + "\" data line is: \"" + lineBuf + "\"");
        }

        // for convenience, insert the type-code in where it is looked for, if exists
        if (modelRecord.tcPosition > 0 && modelRecord.typeCode.length() > 0) {
            lineBuf.replace(modelRecord.tcPosition, modelRecord.tcPosition + modelRecord.tcLength, modelRecord.typeCode);
        }

        if (isFixedLength || isDelimited) {
            if ("CRLF".equals(modelDataFile.getEOLType())) {
                lineBuf.append("\\r\\n");
            } else {
                lineBuf.append('\n');
            }
        }

        return lineBuf.toString();
    }

    String padFrontZeros(String str, int totalLength) {
        if (totalLength > 0 && str.length() < totalLength) {
            // pad the front with zeros
            StringBuilder zeros = new StringBuilder();
            int numZeros = totalLength - str.length();

            for (int i = 0; i < numZeros; i++) {
                zeros.append('0');
            }
            zeros.append(str);
            return zeros.toString();
        }
        return str;
    }

    public Record getParentRecord() {
        return parentRecord;
    }

    public List<Record> getChildRecords() {
        return childRecords;
    }

    public void addChildRecord(Record record) {
        childRecords.add(record);
    }

    /** Creates new Record
     * @param modelRecord
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return the Record Object created
     */
    public static Record createRecord(ModelRecord modelRecord) throws DataFileException {
        Record record = new Record(modelRecord);

        return record;
    }

    /** Creates new Record from existing fields Map
     * @param modelRecord
     * @param fields
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return the Record Object created
     */
    public static Record createRecord(ModelRecord modelRecord, Map<String, Object> fields) throws DataFileException {
        Record record = new Record(modelRecord, fields);

        return record;
    }

    /**
     * @param line
     * @param lineNum
     * @param modelRecord
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return the Record Object created
     */
    public static Record createRecord(String line, int lineNum, ModelRecord modelRecord) throws DataFileException {
        Record record = new Record(modelRecord);

        for (ModelField modelField : modelRecord.fields) {
            String strVal = null;

            try {
                strVal = line.substring(modelField.position, modelField.position + modelField.length);
            }
            catch (IndexOutOfBoundsException ioobe) {
                throw new DataFileException("Field " + modelField.name + " from " + modelField.position + " for " + modelField.length + " chars could not be read from a line ("
                                            + lineNum + ") with only " + line.length() + " chars.",
                        ioobe);
            }
            try {
                record.setString(modelField.name, strVal);
            }
            catch (java.text.ParseException e) {
                throw new DataFileException(
                        "Could not parse field " + modelField.name + ", format string \"" + modelField.format + "\" with value " + strVal + " on line " + lineNum, e);
            }
            catch (java.lang.NumberFormatException e) {
                throw new DataFileException(
                        "Number not valid for field " + modelField.name + ", format string \"" + modelField.format + "\" with value " + strVal + " on line " + lineNum, e);
            }
        }
        return record;
    }

    /**
     * @param line
     * @param lineNum
     * @param modelRecord
     * @param delimiter
     * @throws DataFileException Exception thown for various errors, generally has a nested exception
     * @return return a Record Object
     */
    public static Record createDelimitedRecord(String line, int lineNum, ModelRecord modelRecord, char delimiter, String textDelimiter) throws DataFileException {
        Record record = new Record(modelRecord);

        StringTokenizer st = null;
        if (line.endsWith(String.valueOf(delimiter))) {
            st = new StringTokenizer(line + " ", "" + delimiter, true);
        } else {
            st = new StringTokenizer(line, "" + delimiter, true);
        }
        for (ModelField modelField : modelRecord.fields) {
            String strVal = null;

            if (modelField.expression) {
                if (UtilValidate.isNotEmpty(modelField.refField)) {
                    strVal = record.getString(modelField.refField);
                }
                if (strVal == null) {
                    strVal = (String) modelField.defaultValue;
                }
            } else {
                //some input lines may be less than the header model.
                if (st.hasMoreTokens()) {
                    try {
                        strVal = st.nextToken();
                        if (strVal.equals("" + delimiter)) {
                            strVal = null;
                        } else if (st.hasMoreTokens()) {
                            st.nextToken();
                        }
                    }
                    catch (NoSuchElementException nsee) {
                        throw new DataFileException("Field " + modelField.name + " could not be read from a line (" + lineNum + ") with only " + line.length() + " chars.", nsee);
                    }
                }
            }
            try {
                if (textDelimiter != null && strVal != null && (strVal.startsWith(textDelimiter) && (!strVal.endsWith(textDelimiter) || strVal.length() == 1))) {
                    strVal = strVal.concat("" + delimiter);
                    while (!strVal.endsWith(textDelimiter)) {
                        strVal = strVal.concat(st.nextToken());
                    }
                    st.nextToken();
                }
                if (textDelimiter != null && strVal != null && (strVal.startsWith(textDelimiter) && strVal.endsWith(textDelimiter))) {
                    strVal = strVal.substring(textDelimiter.length(), strVal.length() - textDelimiter.length());
                }
                record.setString(modelField.name, strVal);
            }
            catch (java.text.ParseException e) {
                throw new DataFileException(
                        "Could not parse field " + modelField.name + ", format string \"" + modelField.format + "\" with value " + strVal + " on line " + lineNum, e);
            }
            catch (java.lang.NumberFormatException e) {
                throw new DataFileException(
                        "Number not valid for field " + modelField.name + ", format string \"" + modelField.format + "\" with value " + strVal + " on line " + lineNum, e);
            }
        }
        return record;
    }

}
