/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.entity.condition;

import java.io.Serializable;

import org.ofbiz.entity.model.ModelField;

/**
 * Represents a single parameter to be used in the preparedStatement
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
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

    public String toString() {
        return modelField.getColName() + "=" + fieldValue.toString();
    }
}
