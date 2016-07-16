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

import java.io.Serializable;

import org.apache.ofbiz.entity.model.ModelField;

/**
 * Represents a single parameter to be used in the preparedStatement
 *
 */
@SuppressWarnings("serial")
public class EntityConditionParam implements Serializable {
    protected ModelField modelField;
    protected Object fieldValue;

    protected EntityConditionParam() {}

    public EntityConditionParam(ModelField modelField, Object fieldValue) {
        if (modelField == null) {
            throw new IllegalArgumentException("modelField cannot be null");
        }
        this.modelField = modelField;
        this.fieldValue = fieldValue;
    }

    public ModelField getModelField() {
        return modelField;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    @Override
    public String toString() {
        return modelField.getColName() + "=" + fieldValue.toString();
    }
}
