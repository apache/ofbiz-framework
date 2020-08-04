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


import java.util.ArrayList;
import java.util.List;


/**
 *  ModelDataFile
 */

public class ModelDataFile {

    public static final String SEP_FIXED_LENGTH = "fixed-length";
    public static final String SEP_FIXED_RECORD = "fixed-record";
    public static final String SEP_DELIMITED = "delimited";

    /** The name of the File */
    public String name = "";

    /** The type code of the File, ususally contained somewhere in the file and can be used to identify it */
    public String typeCode = "";

    /** The entity that generally sends the file */
    public String sender = "";

    /** The entity that generally receives the file */
    public String receiver = "";

    /** The length in bytes of a single record, ONLY if it uses fixed length records */
    public int recordLength = -1;

    /** Start the file read at line */
    public int startLine = 0;

    /** The delimiter used in the file, if delimiter separated fields are used */
    public char delimiter = '|';

    /** The text delimiter, like quots, used in the file, if delimiter separated fields are used */
    public String textDelimiter = null;

    /** The field serparator style, either fixed-length, or delimited */
    public String separatorStyle = "";

    /** A free form description of the file */
    public String description = "";
    /** file enconding, by default UTF-8 is used */
    public String encodingType = "UTF-8";

    /**
     * the End Of Line type (CRLF or CR)
     */
    private String eolType = null;

    /** List of record definitions for the file */
    public List<ModelRecord> records = new ArrayList<>();

    public ModelRecord getModelRecord(String recordName) {
        for (ModelRecord curRecord: records) {

            if (curRecord.name.equals(recordName)) {
                return curRecord;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public int getRecordLength() {
        return recordLength;
    }

    public void setRecordLength(int recordLength) {
        this.recordLength = recordLength;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public String getSeparatorStyle() {
        return separatorStyle;
    }

    public void setSeparatorStyle(String separatorStyle) {
        this.separatorStyle = separatorStyle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEOLType() {
        return eolType;
    }

    public void setEOLType(String eolType) {
        this.eolType = eolType;
    }

    public List<ModelRecord> getRecords() {
        return records;
    }

    public void setRecords(List<ModelRecord> records) {
        this.records = records;
    }
    public String getEncodingType() {
        return encodingType;
    }

    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

}
