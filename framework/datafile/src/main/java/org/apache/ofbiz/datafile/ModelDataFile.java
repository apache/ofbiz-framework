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
    private String name = "";

    /** The type code of the File, ususally contained somewhere in the file and can be used to identify it */
    private String typeCode = "";

    /** The entity that generally sends the file */
    private String sender = "";

    /** The entity that generally receives the file */
    private String receiver = "";

    /** The length in bytes of a single record, ONLY if it uses fixed length records */
    private int recordLength = -1;

    /** Start the file read at line */
    private int startLine = 0;

    /** The delimiter used in the file, if delimiter separated fields are used */
    private char delimiter = '|';

    /**
     * Gets text delimiter.
     * @return the text delimiter
     */
    public String getTextDelimiter() {
        return textDelimiter;
    }

    /**
     * Sets text delimiter.
     * @param textDelimiter the text delimiter
     */
    public void setTextDelimiter(String textDelimiter) {
        this.textDelimiter = textDelimiter;
    }

    /** The text delimiter, like quots, used in the file, if delimiter separated fields are used */
    private String textDelimiter = null;

    /** The field serparator style, either fixed-length, or delimited */
    private String separatorStyle = "";

    /** A free form description of the file */
    private String description = "";
    /** file enconding, by default UTF-8 is used */
    private String encodingType = "UTF-8";

    /**
     * the End Of Line type (CRLF or CR)
     */
    private String eolType = null;

    /** List of record definitions for the file */
    private List<ModelRecord> records = new ArrayList<>();

    /**
     * Gets model record.
     * @param recordName the record name
     * @return the model record
     */
    public ModelRecord getModelRecord(String recordName) {
        for (ModelRecord curRecord: records) {

            if (curRecord.getName().equals(recordName)) {
                return curRecord;
            }
        }
        return null;
    }

    /**
     * Gets name.
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets type code.
     * @return the type code
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * Sets type code.
     * @param typeCode the type code
     */
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Gets sender.
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * Sets sender.
     * @param sender the sender
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * Gets receiver.
     * @return the receiver
     */
    public String getReceiver() {
        return receiver;
    }

    /**
     * Sets receiver.
     * @param receiver the receiver
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    /**
     * Gets record length.
     * @return the record length
     */
    public int getRecordLength() {
        return recordLength;
    }

    /**
     * Sets record length.
     * @param recordLength the record length
     */
    public void setRecordLength(int recordLength) {
        this.recordLength = recordLength;
    }

    /**
     * Gets delimiter.
     * @return the delimiter
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Sets delimiter.
     * @param delimiter the delimiter
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Gets start line.
     * @return the start line
     */
    public int getStartLine() {
        return startLine;
    }

    /**
     * Sets start line.
     * @param startLine the start line
     */
    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    /**
     * Gets separator style.
     * @return the separator style
     */
    public String getSeparatorStyle() {
        return separatorStyle;
    }

    /**
     * Sets separator style.
     * @param separatorStyle the separator style
     */
    public void setSeparatorStyle(String separatorStyle) {
        this.separatorStyle = separatorStyle;
    }

    /**
     * Gets description.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets eol type.
     * @return the eol type
     */
    public String getEOLType() {
        return eolType;
    }

    /**
     * Sets eol type.
     * @param eolType the eol type
     */
    public void setEOLType(String eolType) {
        this.eolType = eolType;
    }

    /**
     * Gets records.
     * @return the records
     */
    public List<ModelRecord> getRecords() {
        return records;
    }

    /**
     * Sets records.
     * @param records the records
     */
    public void setRecords(List<ModelRecord> records) {
        this.records = records;
    }

    /**
     * Gets encoding type.
     * @return the encoding type
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Sets encoding type.
     * @param encodingType the encoding type
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

}
