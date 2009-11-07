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
package org.ofbiz.webslinger;

import java.util.Iterator;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldType;

public class EntityHttpUtil {
    public static GenericValue makeValidValue(String entityName, ServletRequest request) throws GeneralException {
        return makeValidValue(entityName, false, request);
    }

    public static GenericValue makeValidValue(String entityName, boolean includePks, ServletRequest request) throws GeneralException {
        if (request instanceof HttpServletRequest) return makeValidValue(entityName, includePks, (HttpServletRequest) request);
        throw new IllegalArgumentException("Not an HttpServletRequest");
    }

    public static GenericValue makeValidValue(String entityName, HttpServletRequest request) throws GeneralException {
        return makeValidValue(entityName, false, request);
    }

    public static GenericValue makeValidValue(String entityName, boolean includePks, HttpServletRequest request) throws GeneralException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue value = delegator.makeValue(entityName);
        ModelEntity model = value.getModelEntity();
        Iterator<ModelField> it = includePks ? model.getFieldsIterator() : model.getNopksIterator();
        Locale locale = UtilHttp.getLocale(request);
        while (it.hasNext()) {
            ModelField field = it.next();
            String fieldName = field.getName();
            String parameterValue = request.getParameter(fieldName);
            Object fieldValue;
            if (parameterValue == null) {
                fieldValue = null;
            } else {
                ModelFieldType fieldType = delegator.getEntityFieldType(model, field.getType());
                String wantedType = fieldType.getJavaType();
                fieldValue = ObjectType.simpleTypeConvert(parameterValue, wantedType, null, locale, true);
            }
            value.put(fieldName, fieldValue);
        }
        return value;
    }
}

