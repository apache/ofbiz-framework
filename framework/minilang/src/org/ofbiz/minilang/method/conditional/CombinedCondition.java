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

import java.util.*;
import javolution.util.FastList;
import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Implements generic combining conditions such as or, and, etc.
 */
public class CombinedCondition implements Conditional {
    public static final class OrConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CombinedCondition(element, OR, simpleMethod);
        }

        @Override
        public String getName() {
            return "or";
        }
    }

    public static final class XorConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CombinedCondition(element, XOR, simpleMethod);
        }

        @Override
        public String getName() {
            return "xor";
        }
    }

    public static final class AndConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CombinedCondition(element, AND, simpleMethod);
        }

        @Override
        public String getName() {
            return "and";
        }
    }

    public static final class NotConditionFactory extends ConditionalFactory<CombinedCondition> {
        @Override
        public CombinedCondition createCondition(Element element, SimpleMethod simpleMethod) {
            return new CombinedCondition(element, NOT, simpleMethod);
        }

        @Override
        public String getName() {
            return "not";
        }
    }

    public static final int OR = 1;
    public static final int XOR = 2;
    public static final int AND = 3;
    public static final int NOT = 4;

    SimpleMethod simpleMethod;
    int conditionType;
    List<Conditional> subConditions = FastList.newInstance();

    public CombinedCondition(Element element, int conditionType, SimpleMethod simpleMethod) {
        this.simpleMethod = simpleMethod;
        this.conditionType = conditionType;
        for (Element subElement: UtilXml.childElementList(element)) {
            subConditions.add(ConditionalFactory.makeConditional(subElement, simpleMethod));
        }
    }

    public boolean checkCondition(MethodContext methodContext) {
        if (subConditions.size() == 0) return true;

        Iterator<Conditional> subCondIter = subConditions.iterator();
        switch (this.conditionType) {
            case OR:
                while (subCondIter.hasNext()) {
                    Conditional subCond = subCondIter.next();
                    if (subCond.checkCondition(methodContext)) {
                        return true;
                    }
                }
                return false;
            case XOR:
                boolean trueFound = false;
                while (subCondIter.hasNext()) {
                    Conditional subCond = subCondIter.next();
                    if (subCond.checkCondition(methodContext)) {
                        if (trueFound) {
                            return false;
                        } else {
                            trueFound = true;
                        }
                    }
                }
                return trueFound;
            case AND:
                while (subCondIter.hasNext()) {
                    Conditional subCond = subCondIter.next();
                    if (!subCond.checkCondition(methodContext)) {
                        return false;
                    }
                }
                return true;
            case NOT:
                Conditional subCond = subCondIter.next();
                return !subCond.checkCondition(methodContext);
            default:
                return false;
        }
    }

    public void prettyPrint(StringBuilder messageBuffer, MethodContext methodContext) {
        messageBuffer.append("(");
        Iterator<Conditional> subCondIter = subConditions.iterator();
        while (subCondIter.hasNext()) {
            Conditional subCond = subCondIter.next();
            subCond.prettyPrint(messageBuffer, methodContext);
            if (subCondIter.hasNext()) {
                switch (this.conditionType) {
                case OR:
                    messageBuffer.append(" OR ");
                    break;
                case XOR:
                    messageBuffer.append(" XOR ");
                    break;
                case AND:
                    messageBuffer.append(" AND ");
                    break;
                case NOT:
                    messageBuffer.append(" NOT ");
                    break;
                default:
                    messageBuffer.append("?");
                }
            }
        }
        messageBuffer.append(")");
    }
}
