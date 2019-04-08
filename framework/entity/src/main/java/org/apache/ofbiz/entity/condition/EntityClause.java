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

package org.apache.ofbiz.entity.condition;


import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelReader;


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
    private EntityOperator<?,?,?> interFieldOperation = null;
    private EntityOperator<?,?,?> intraFieldOperation = null;

    private Object value = null;
    public EntityClause() {}

    public EntityClause(String firstEntity, String secondEntity, String firstField, String secondField, EntityOperator<?, ?, ?> interFieldOperation, EntityOperator<?, ?, ?> intraFieldOperation) {
        this.firstEntity = firstEntity;
        this.secondEntity = secondEntity;
        this.firstField = firstField;
        this.secondField = secondField;
        this.interFieldOperation = interFieldOperation;
        this.intraFieldOperation = intraFieldOperation;
    }

    public EntityClause(String firstEntity, String firstField, Object value, EntityOperator<?, ?, ?> interFieldOperation, EntityOperator<?, ?, ?> intraFieldOperation) {
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

    public <L,R,T> EntityOperator<L,R,T> getInterFieldOperation() {
        return UtilGenerics.cast(interFieldOperation);
    }

    public <L,R,T> EntityOperator<L,R,T> getIntraFieldOperation() {
        return UtilGenerics.cast(intraFieldOperation);
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

    public <L,R,T> void setInterFieldOperation(EntityOperator<L,R,T> interFieldOperation) {
        this.interFieldOperation = interFieldOperation;
    }

    public <L,R,T> void setIntraFieldOperation(EntityOperator<L,R,T> intraFieldOperation) {
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

    @Override
    public String toString() {
        StringBuilder outputBuffer = new StringBuilder();

        outputBuffer.append("[firstEntity,").append(firstEntity == null ? "null" : firstEntity).append("]");
        outputBuffer.append("[secondEntity,").append(secondEntity == null ? "null" : secondEntity).append("]");
        outputBuffer.append("[firstField,").append(firstField == null ? "null" : firstField).append("]");
        outputBuffer.append("[secondField,").append(secondField == null ? "null" : secondField).append("]");
        outputBuffer.append("[firstModelEntity,").append(firstModelEntity == null ? "null" : (firstModelEntity.getEntityName() == null ? "null" : firstModelEntity.getEntityName())).append("]");
        outputBuffer.append("[secondModelEntity,").append(secondModelEntity == null ? "null" : (secondModelEntity.getEntityName() == null ? "null" : secondModelEntity.getEntityName())).append("]");
        outputBuffer.append("[interFieldOperation,").append(interFieldOperation == null ? "null" : (interFieldOperation.getCode() == null ? "null" : interFieldOperation.getCode())).append("]");
        outputBuffer.append("[intraFieldOperation,").append(intraFieldOperation == null ? "null" : (intraFieldOperation.getCode() == null ? "null" : intraFieldOperation.getCode())).append("]");
        outputBuffer.append("[value,").append(getValue().toString() == null ? "null" : getValue().toString()).append("]");
        return outputBuffer.toString();
    }

}
