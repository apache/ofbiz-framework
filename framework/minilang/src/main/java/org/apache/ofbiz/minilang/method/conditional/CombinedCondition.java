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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.minilang.MiniLangElement;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;and&gt;, &lt;or&gt;, &lt;not&gt;, and &lt;xor&gt; elements.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public abstract class CombinedCondition extends MiniLangElement implements Conditional {

    protected final List<Conditional> subConditions;

    public CombinedCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        List<? extends Element> childElements = UtilXml.childElementList(element);
        if (MiniLangValidate.validationOn() && childElements.isEmpty()) {
            MiniLangValidate.handleError("No conditional elements.", simpleMethod, element);
        }
        List<Conditional> conditionalList = new ArrayList<Conditional>(childElements.size());
        for (Element conditionalElement : UtilXml.childElementList(element)) {
            conditionalList.add(ConditionalFactory.makeConditional(conditionalElement, simpleMethod));
        }
        this.subConditions = Collections.unmodifiableList(conditionalList);
    }

    protected void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext, String combineText) {
        messageBuffer.append("(");
        for (Conditional subCond : subConditions) {
            subCond.prettyPrint(messageBuffer, methodContext);
            messageBuffer.append(combineText);
        }
        messageBuffer.append(")");
    }

    /**
     * A &lt;and&gt; element factory. 
     */
    public static final class AndConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CombinedCondition(element, simpleMethod) {
                @Override
                public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
                    if (subConditions.size() == 0)
                        return true;
                    for (Conditional subCond : subConditions) {
                        if (!subCond.checkCondition(methodContext)) {
                            return false;
                        }
                    }
                    return true;
                }
                @Override
                public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
                    prettyPrint(messageBuffer, methodContext, " AND ");
                }
            };
        }

        @Override
        public String getName() {
            return "and";
        }
    }

    /**
     * A &lt;not&gt; element factory. 
     */
    public static final class NotConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CombinedCondition(element, simpleMethod) {
                @Override
                public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
                    if (subConditions.size() == 0)
                        return true;
                    Conditional subCond = subConditions.get(0);
                    return !subCond.checkCondition(methodContext);
                }
                @Override
                public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
                    messageBuffer.append("( NOT ");
                    if (subConditions.size() > 0) {
                        Conditional subCond = subConditions.get(0);
                        subCond.prettyPrint(messageBuffer, methodContext);
                    }
                    messageBuffer.append(")");
                }
            };
        }

        @Override
        public String getName() {
            return "not";
        }
    }

    /**
     * A &lt;or&gt; element factory. 
     */
    public static final class OrConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CombinedCondition(element, simpleMethod) {
                @Override
                public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
                    if (subConditions.size() == 0)
                        return true;
                    for (Conditional subCond : subConditions) {
                        if (subCond.checkCondition(methodContext)) {
                            return true;
                        }
                    }
                    return false;
                }
                @Override
                public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
                    prettyPrint(messageBuffer, methodContext, " OR ");
                }
            };
        }

        @Override
        public String getName() {
            return "or";
        }
    }

    /**
     * A &lt;xor&gt; element factory. 
     */
    public static final class XorConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new CombinedCondition(element, simpleMethod) {
                @Override
                public boolean checkCondition(MethodContext methodContext) throws MiniLangException {
                    if (subConditions.size() == 0)
                        return true;
                    boolean trueFound = false;
                    for (Conditional subCond : subConditions) {
                        if (subCond.checkCondition(methodContext)) {
                            if (trueFound) {
                                return false;
                            } else {
                                trueFound = true;
                            }
                        }
                    }
                    return trueFound;
                }
                @Override
                public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
                    prettyPrint(messageBuffer, methodContext, " XOR ");
                }
            };
        }

        @Override
        public String getName() {
            return "xor";
        }
    }
}
