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
package org.apache.ofbiz.minilang.method.conditional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;if-compare&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class CompareCondition extends MethodOperation implements Conditional {

    private final Compare compare;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander formatFse;
    private final String operator;
    private final Class<?> targetClass;
    private final String type;
    private final FlexibleStringExpander valueFse;
    // Sub-operations are used only when this is a method operation.
    private final List<MethodOperation> elseSubOps;
    private final List<MethodOperation> subOps;

    public CompareCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "format", "operator", "type", "value");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "operator", "value");
            MiniLangValidate.constantAttributes(simpleMethod, element, "operator", "type");
            MiniLangValidate.constantPlusExpressionAttributes(simpleMethod, element, "value");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.formatFse = FlexibleStringExpander.getInstance(element.getAttribute("format"));
        this.operator = element.getAttribute("operator");
        this.compare = Compare.getInstance(this.operator);
        if (this.compare == null) {
            MiniLangValidate.handleError("Invalid operator " + this.operator, simpleMethod, element);
        }
        this.type = element.getAttribute("type");
        Class<?> targetClass = null;
        if (!this.type.isEmpty()) {
            if ("contains".equals(this.operator)) {
                MiniLangValidate.handleError("Operator \"contains\" does not support type conversions (remove the type attribute).", simpleMethod, element);
                targetClass = Object.class;
            } else {
                try {
                    targetClass = ObjectType.loadClass(this.type);
                } catch (ClassNotFoundException e) {
                    MiniLangValidate.handleError("Invalid type " + this.type, simpleMethod, element);
                }
            }
        }
        this.targetClass = targetClass;
        this.valueFse = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        Element childElement = UtilXml.firstChildElement(element);
        if (childElement != null && !"else".equals(childElement.getTagName())) {
            this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
        } else {
            this.subOps = null;
        }
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            this.elseSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(elseElement, simpleMethod));
        } else {
            this.elseSubOps = null;
        }
    }

    @Override
    public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
        if (this.compare == null) {
            throw new MiniLangRuntimeException("Invalid operator \"" + this.operator + "\"", this);
        }
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        Class<?> targetClass = this.targetClass;
        if (targetClass == null) {
            targetClass = MiniLangUtil.getObjectClassForConversion(fieldVal);
        }
        String value = valueFse.expandString(methodContext.getEnvMap());
        String format = formatFse.expandString(methodContext.getEnvMap());
        try {
            // We use en locale here so constant (literal) values are converted properly.
            return this.compare.doCompare(fieldVal, value, targetClass, Locale.ENGLISH, methodContext.getTimeZone(), format);
        } catch (Exception e) {
            simpleMethod.addErrorMessage(methodContext, e.getMessage());
        }
        return false;
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (checkCondition(methodContext)) {
            if (this.subOps != null) {
                return SimpleMethod.runSubOps(subOps, methodContext);
            }
        } else {
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            }
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        if (this.subOps != null) {
            for (MethodOperation method : this.subOps) {
                method.gatherArtifactInfo(aic);
            }
        }
        if (this.elseSubOps != null) {
            for (MethodOperation method : this.elseSubOps) {
                method.gatherArtifactInfo(aic);
            }
        }
    }

    @Override
    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        String value = valueFse.expandString(methodContext.getEnvMap());
        String format = formatFse.expandString(methodContext.getEnvMap());
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        messageBuffer.append("[");
        messageBuffer.append(this.fieldFma);
        messageBuffer.append("=");
        messageBuffer.append(fieldVal);
        messageBuffer.append("] ");
        messageBuffer.append(operator);
        messageBuffer.append(" ");
        messageBuffer.append(value);
        messageBuffer.append(" as ");
        messageBuffer.append(type);
        if (UtilValidate.isNotEmpty(format)) {
            messageBuffer.append(":");
            messageBuffer.append(format);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<if-compare ");
        sb.append("field=\"").append(this.fieldFma).append("\" operator=\"").append(operator).append("\" ");
        if (!this.valueFse.isEmpty()) {
            sb.append("value=\"").append(this.valueFse).append("\" ");
        }
        if (!this.type.isEmpty()) {
            sb.append("type=\"").append(this.type).append("\" ");
        }
        if (!this.formatFse.isEmpty()) {
            sb.append("format=\"").append(this.formatFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A &lt;if-compare&gt; element factory. 
     */
    public static final class CompareConditionFactory extends ConditionalFactory<CompareCondition> implements Factory<CompareCondition> {
        @Override
        public CompareCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CompareCondition(element, simpleMethod);
        }

        @Override
        public CompareCondition createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CompareCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-compare";
        }
    }
}
