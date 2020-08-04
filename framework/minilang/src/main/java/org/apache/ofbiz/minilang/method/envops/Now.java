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
package org.apache.ofbiz.minilang.method.envops;

import org.apache.ofbiz.base.conversion.ConversionException;
import org.apache.ofbiz.base.conversion.Converter;
import org.apache.ofbiz.base.conversion.Converters;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.ValidationException;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implements the &lt;now&gt;, &lt;now-date-to-env&gt;, and &lt;now-timestamp&gt; elements.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class Now extends MethodOperation {

    // This method is needed only during the v1 to v2 transition
    private static boolean autoCorrect(Element element) {
        String tagName = element.getTagName();
        if ("now-date-to-env".equals(tagName) || "now-timestamp".equals(tagName)) {
            Document doc = element.getOwnerDocument();
            Element newElement = doc.createElement("now");
            newElement.setAttribute("field", element.getAttribute("field"));
            if ("now-date-to-env".equals(tagName)) {
                element.setAttribute("type", "java.sql.Date");
                newElement.setAttribute("type", "java.sql.Date");
            }
            element.getParentNode().replaceChild(newElement, element);
            return true;
        }
        return false;
    }

    private final FlexibleMapAccessor<Object> fieldFma;
    private final String type;
    private final Converter<Long, ? extends Object> converter;

    public Now(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            String tagName = element.getTagName();
            if ("now-date-to-env".equals(tagName) || "now-timestamp".equals(tagName)) {
                MiniLangValidate.handleError("Deprecated - use <now>", simpleMethod, element);
            }
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "type");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.constantAttributes(simpleMethod, element, "type");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        boolean elementModified = autoCorrect(element);
        if (elementModified && MiniLangUtil.autoCorrectOn()) {
            MiniLangUtil.flagDocumentAsCorrected(element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.type = element.getAttribute("type");
        Class<?> targetClass = null;
        try {
            if (!this.type.isEmpty()) {
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
    public String toString() {
        StringBuilder sb = new StringBuilder("<now ");
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        if (!this.type.isEmpty()) {
            sb.append("type=\"").append(this.type).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }


    /**
     * A factory for the &lt;now&gt; element.
     */
    public static final class NowFactory implements Factory<Now> {
        @Override
        public Now createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Now(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "now";
        }
    }

    /**
     * A factory for the &lt;now-date-to-env&gt; element.
     */
    public static final class NowDateToEnvFactory implements Factory<Now> {
        @Override
        public Now createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Now(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "now-date-to-env";
        }
    }

    /**
     * A factory for the &lt;now-timestamp&gt; element.
     */
    public static final class NowTimestampFactory implements Factory<Now> {
        @Override
        public Now createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Now(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "now-timestamp";
        }
    }
}
