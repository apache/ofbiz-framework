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

package org.ofbiz.entity.condition;


import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelReader;


/**
 * Generic Entity Clause - Used to string together entities to make a find clause
 */
public class EntityClause {

    private String firstEntity = "";
    private String secondEntity = "";
    private String firstField = "";
    private String secondField = "";
    private ModelEntity firstModelEntity = null;
    private ModelEntity secondModelEntity = null;
    private EntityOperator interFieldOperation = null;
    private EntityOperator intraFieldOperation = null;

    private Object value = null;
    public EntityClause() {}

    public EntityClause(String firstEntity, String secondEntity, String firstField, String secondField, EntityOperator interFieldOperation, EntityOperator intraFieldOperation) {
        this.firstEntity = firstEntity;
        this.secondEntity = secondEntity;
        this.firstField = firstField;
        this.secondField = secondField;
        this.interFieldOperation = interFieldOperation;
        this.intraFieldOperation = intraFieldOperation;
    }

    public EntityClause(String firstEntity, String firstField, Object value, EntityOperator interFieldOperation, EntityOperator intraFieldOperation) {
        this.firstEntity = firstEntity;
        this.firstField = firstField;
        this.value = value;
        this.interFieldOperation = interFieldOperation;
        this.intraFieldOperation = intraFieldOperation;
    }

    public String getFirstEntity() {
        return firstEntity;
    }

    public String getSecondEntity() {
        return secondEntity;
    }

    public String getFirstField() {
        return firstField;
    }

    public String getSecondField() {
        return secondField;
    }

    public Object getValue() {
        if (value == null) value = new Object();
        return value;
    }

    public EntityOperator getInterFieldOperation() {
        return interFieldOperation;
    }

    public EntityOperator getIntraFieldOperation() {
        return intraFieldOperation;
    }

    public void setFirstEntity(String firstEntity) {
        this.firstEntity = firstEntity;
    }

    public void setSecondEntity(String secondEntity) {
        this.secondEntity = secondEntity;
    }

    public void setFirstField(String firstField) {
        this.firstField = firstField;
    }

    public void setSecondField(String secondField) {
        this.secondField = secondField;
    }

    public void setInterFieldOperation(EntityOperator interFieldOperation) {
        this.interFieldOperation = interFieldOperation;
    }

    public void setIntraFieldOperation(EntityOperator intraFieldOperation) {
        this.intraFieldOperation = intraFieldOperation;
    }

    // --  Protected Methods  - for internal use only --//
    protected void setModelEntities(ModelReader modelReader) throws GenericEntityException {
        firstModelEntity = modelReader.getModelEntity(firstEntity);
        if (secondEntity != null && !secondEntity.equals("")) {
            secondModelEntity = modelReader.getModelEntity(secondEntity);
        }
    }

    protected ModelEntity getFirstModelEntity() {
        return firstModelEntity;
    }

    protected ModelEntity getSecondModelEntity() {
        return secondModelEntity;
    }

    public String toString() {
        StringBuffer outputBuffer = new StringBuffer();

        outputBuffer.append("[firstEntity," + (firstEntity == null ? "null" : firstEntity) + "]");
        outputBuffer.append("[secondEntity," + (secondEntity == null ? "null" : secondEntity) + "]");
        outputBuffer.append("[firstField," + (firstField == null ? "null" : firstField) + "]");
        outputBuffer.append("[secondField," + (secondField == null ? "null" : secondField) + "]");
        outputBuffer.append("[firstModelEntity," + (firstModelEntity == null ? "null" : (firstModelEntity.getEntityName() == null ? "null" : firstModelEntity.getEntityName())) + "]");
        outputBuffer.append("[secondModelEntity," + (secondModelEntity == null ? "null" : (secondModelEntity.getEntityName() == null ? "null" : secondModelEntity.getEntityName())) + "]");
        outputBuffer.append("[interFieldOperation," + (interFieldOperation == null ? "null" : (interFieldOperation.getCode() == null ? "null" : interFieldOperation.getCode())) + "]");
        outputBuffer.append("[intraFieldOperation," + (intraFieldOperation == null ? "null" : (intraFieldOperation.getCode() == null ? "null" : intraFieldOperation.getCode())) + "]");
        outputBuffer.append("[value," + (getValue().toString() == null ? "null" : getValue().toString()) + "]");
        return outputBuffer.toString();
    }

}
