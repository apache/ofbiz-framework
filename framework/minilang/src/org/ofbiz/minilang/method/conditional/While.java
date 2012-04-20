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

import java.util.List;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Continually processes sub-ops while the condition remains true
 */
public class While extends MethodOperation {

    Conditional condition;
    List<MethodOperation> thenSubOps = FastList.newInstance();

    public While(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);
        Element thenElement = UtilXml.firstChildElement(element, "then");
        SimpleMethod.readOperations(thenElement, thenSubOps, simpleMethod);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        // if conditions fails, always return true;
        // if a sub-op returns false return false and stop, otherwise drop though loop and
        // return true
        while (condition.checkCondition(methodContext)) {
            boolean runSubOpsResult = SimpleMethod.runSubOps(thenSubOps, methodContext);
            if (!runSubOpsResult) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: fill in missing details, if needed
        StringBuilder messageBuf = new StringBuilder();
        this.condition.prettyPrint(messageBuf, methodContext);
        return "<while><condition>" + messageBuf + "</condition></while>";
    }

    public List<MethodOperation> getThenSubOps() {
        return this.thenSubOps;
    }

    @Override
    public String rawString() {
        return expandedString(null);
    }

    public static final class WhileFactory implements Factory<While> {
        public While createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new While(element, simpleMethod);
        }

        public String getName() {
            return "while";
        }
    }
}
