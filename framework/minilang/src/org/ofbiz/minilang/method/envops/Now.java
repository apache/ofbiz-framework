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
package org.ofbiz.minilang.method.envops;

import org.ofbiz.base.conversion.ConversionException;
import org.ofbiz.base.conversion.Converter;
import org.ofbiz.base.conversion.Converters;
import org.ofbiz.base.util.ObjectType;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.ValidationException;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Sets a field to the current system time.
 */
public final class Now extends MethodOperation {

    private final FlexibleMapAccessor<Object> fieldFma;
    private final String type;
    private final Converter<Long, ? extends Object> converter;

    public Now(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "type");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "type");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.type = element.getAttribute("type");
        Class<?> targetClass = null;
        try {
            if (this.type.length() > 0) {
                targetClass = ObjectType.loadClass(this.type);
            }
            if (targetClass == null) {
                targetClass = java.sql.Timestamp.class;
            }
            this.converter = Converters.getConverter(Long.class, targetClass);
        } catch (ClassNotFoundException e) {
            throw new ValidationException(e.getMessage(), simpleMethod, element);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        try {
            this.fieldFma.put(methodContext.getEnvMap(), this.converter.convert(System.currentTimeMillis()));
        } catch (ConversionException e) {
            throw new MiniLangRuntimeException(e.getMessage(), this);
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    @Override
    public String rawString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<now ");
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        if (this.type.length() > 0) {
            sb.append("type=\"").append(this.type).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }


    public static final class NowFactory implements Factory<Now> {
        public Now createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Now(element, simpleMethod);
        }

        public String getName() {
            return "now";
        }
    }
}
