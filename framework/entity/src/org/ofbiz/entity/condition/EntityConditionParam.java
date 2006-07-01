/*
 * $Id: EntityConditionParam.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
