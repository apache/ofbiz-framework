/*
 * $Id: EntityClause.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * <p>Copyright (c) 2001 The Open For Business Project - www.ofbiz.org
 *
 * <p>Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 * <p>The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.ofbiz.entity.condition;


import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelReader;


/**
 * Generic Entity Clause - Used to string together entities to make a find clause
 *
 *@author     <a href='mailto:chris_maurer@altavista.com'>Chris Maurer</a>
 *@author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 *@created    Mon Nov 5, 2001
 *@version    1.0
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
        firstModelEntity = (ModelEntity) modelReader.getModelEntity(firstEntity);
        if (secondEntity != null && !secondEntity.equals("")) {
            secondModelEntity = (ModelEntity) modelReader.getModelEntity(secondEntity);
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
