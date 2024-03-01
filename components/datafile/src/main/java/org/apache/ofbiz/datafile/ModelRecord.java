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
 *  ModelRecord
 */

public class ModelRecord {

    private static final String LIMIT_ONE = "one";
    private static final String LIMIT_MANY = "many";

    /** The name of the Record */
    private String name = "";

    /** The type-code of the Record */
    private String typeCode = "";

    /** The minimum type-code of the Record, an alternative to the single type code */
    private String tcMin = "";
    private long tcMinNum = -1;

    /** The maximum type-code of the Record, an alternative to the single type code */
    private String tcMax = "";
    private long tcMaxNum = -1;

    /** specifies whether or not the type min and max are numbers, if so does a number compare, otherwise a String compare */
    private boolean tcIsNum = true;

    /** The position of the type-code of the Record */
    private int tcPosition = -1;

    /** The length of the type-code of the Record - optional */
    private int tcLength = -1;

    /** A free form description of the Record */
    private String description = "";

    /** The name of the parent record for this record, if any */
    private String parentName = "";

    /** The number limit of records to go under the parent, may be one or many */
    private String limit = "";

    private ModelRecord parentRecord = null;
    private List<ModelRecord> childRecords = new ArrayList<>();

    /** List of the fields that compose this record */
    private List<ModelField> fields = new ArrayList<>();

    /**
     * Gets model field.
     * @param fieldName the field name
     * @return the model field
     */
    ModelField getModelField(String fieldName) {
        for (ModelField curField: fields) {

            if (curField.getName().equals(fieldName)) {
                return curField;
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
     * Gets parent name.
     * @return the parent name
     */
    public String getParentName() {
        return parentName;
    }

    /**
     * Sets parent name.
     * @param parentName the parent name
     */
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    /**
     * Gets fields.
     * @return the fields
     */
    public List<ModelField> getFields() {
        return fields;
    }

    /**
     * Sets fields.
     * @param fields the fields
     */
    public void setFields(List<ModelField> fields) {
        this.fields = fields;
    }

    /**
     * Gets type code.
     * @return the type code
     */
    public String getTypeCode() {
        return typeCode;
    }

    /**
     * Gets tc position.
     * @return the tc position
     */
    public int getTcPosition() {
        return tcPosition;
    }

    /**
     * Sets type code.
     * @param typeCode the type code
     */
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    /**
     * Sets tc min.
     * @param tcMin the tc min
     */
    public void setTcMin(String tcMin) {
        this.tcMin = tcMin;
    }

    /**
     * Sets tc max.
     * @param tcMax the tc max
     */
    public void setTcMax(String tcMax) {
        this.tcMax = tcMax;
    }

    /**
     * Sets tc is num.
     * @param tcIsNum the tc is num
     */
    public void setTcIsNum(boolean tcIsNum) {
        this.tcIsNum = tcIsNum;
    }

    /**
     * Gets tc min.
     * @return the tc min
     */
    public String getTcMin() {
        return tcMin;
    }

    /**
     * Gets tc max.
     * @return the tc max
     */
    public String getTcMax() {
        return tcMax;
    }

    /**
     * Gets tc length.
     * @return the tc length
     */
    public int getTcLength() {
        return tcLength;
    }

    /**
     * Sets tc position.
     * @param tcPosition the tc position
     */
    public void setTcPosition(int tcPosition) {
        this.tcPosition = tcPosition;
    }

    /**
     * Sets tc min num.
     * @param tcMinNum the tc min num
     */
    public void setTcMinNum(long tcMinNum) {
        this.tcMinNum = tcMinNum;
    }

    /**
     * Sets tc max num.
     * @param tcMaxNum the tc max num
     */
    public void setTcMaxNum(long tcMaxNum) {
        this.tcMaxNum = tcMaxNum;
    }

    /**
     * Gets tc min num.
     * @return the tc min num
     */
    public long getTcMinNum() {
        return tcMinNum;
    }

    /**
     * Gets tc max num.
     * @return the tc max num
     */
    public long getTcMaxNum() {
        return tcMaxNum;
    }

    /**
     * Is tc is num boolean.
     * @return the boolean
     */
    public boolean isTcIsNum() {
        return tcIsNum;
    }

    /**
     * Sets tc length.
     * @param tcLength the tc length
     */
    public void setTcLength(int tcLength) {
        this.tcLength = tcLength;
    }

    /**
     * Sets limit.
     * @param limit the limit
     */
    public void setLimit(String limit) {
        this.limit = limit;
    }

    /**
     * Gets parent record.
     * @return the parent record
     */
    public ModelRecord getParentRecord() {
        return parentRecord;
    }

    /**
     * Gets child records.
     * @return the child records
     */
    public List<ModelRecord> getChildRecords() {
        return childRecords;
    }

    /**
     * Sets parent record.
     * @param parentRecord the parent record
     */
    public void setParentRecord(ModelRecord parentRecord) {
        this.parentRecord = parentRecord;
    }
}

