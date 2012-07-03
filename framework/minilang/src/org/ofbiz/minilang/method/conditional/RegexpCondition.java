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
package org.ofbiz.minilang.method.conditional;

import java.util.Collections;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;
import org.ofbiz.base.util.CompilerMatcher;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangUtil;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;if-regexp&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/OFBADMIN/mini-language-reference.html#Mini-languageReference-{{%3Cifregexp%3E}}">Mini-language Reference</a>
 */
public class RegexpCondition extends MethodOperation implements Conditional {

    public static final String module = RegexpCondition.class.getName();
    private transient static ThreadLocal<CompilerMatcher> compilerMatcher = CompilerMatcher.getThreadLocal();

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander exprFse;
    // Sub-operations are used only when this is a method operation.
    private final List<MethodOperation> elseSubOps;
    private final List<MethodOperation> subOps;

    public RegexpCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "expr");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "expr");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.exprFse = FlexibleStringExpander.getInstance(element.getAttribute("expr"));
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
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal == null) {
            fieldVal = "";
        } else if (!(fieldVal instanceof String)) {
            try {
                fieldVal = MiniLangUtil.convertType(fieldVal, String.class, methodContext.getLocale(), methodContext.getTimeZone(), null);
            } catch (Exception e) {
                throw new MiniLangRuntimeException(e, this);
            }
        }
        String regExp = exprFse.expandString(methodContext.getEnvMap());
        try {
            return compilerMatcher.get().matches((String) fieldVal, regExp);
        } catch (MalformedPatternException e) {
            throw new MiniLangRuntimeException(e, this);
        }
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

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("regexp[");
        messageBuffer.append("[");
        messageBuffer.append(this.fieldFma);
        messageBuffer.append("=");
        messageBuffer.append(fieldFma.get(methodContext.getEnvMap()));
        messageBuffer.append("] matches ");
        messageBuffer.append(exprFse.expandString(methodContext.getEnvMap()));
        messageBuffer.append("]");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<if-regexp ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        if (!this.exprFse.isEmpty()) {
            sb.append("expr=\"").append(this.exprFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A &lt;if-regexp&gt; element factory. 
     */
    public static final class RegexpConditionFactory extends ConditionalFactory<RegexpCondition> implements Factory<RegexpCondition> {
        @Override
        public RegexpCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RegexpCondition(element, simpleMethod);
        }

        @Override
        public RegexpCondition createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RegexpCondition(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "if-regexp";
        }
    }
}
