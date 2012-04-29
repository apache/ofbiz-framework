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

import javolution.util.FastList;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Represents the top-level element and only mounted operation for the more flexible if structure.
 */
public class MasterIf extends MethodOperation {

    Conditional condition;
    List<ElseIf> elseIfs = null;
    List<MethodOperation> elseSubOps = null;
    List<MethodOperation> thenSubOps;

    public MasterIf(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);
        Element thenElement = UtilXml.firstChildElement(element, "then");
        this.thenSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(thenElement, simpleMethod));
        List<? extends Element> elseIfElements = UtilXml.childElementList(element, "else-if");
        if (UtilValidate.isNotEmpty(elseIfElements)) {
            elseIfs = FastList.newInstance();
            for (Element elseIfElement : elseIfElements) {
                elseIfs.add(new ElseIf(elseIfElement, simpleMethod));
            }
        }
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            this.elseSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(elseElement, simpleMethod));
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        // if conditions fails, always return true; if a sub-op returns false
        // return false and stop, otherwise return true
        // return true;
        // only run subOps if element is empty/null
        boolean runSubOps = condition.checkCondition(methodContext);
        if (runSubOps) {
            return SimpleMethod.runSubOps(thenSubOps, methodContext);
        } else {
            // try the else-ifs
            if (UtilValidate.isNotEmpty(elseIfs)) {
                for (ElseIf elseIf : elseIfs) {
                    if (elseIf.checkCondition(methodContext)) {
                        return elseIf.runSubOps(methodContext);
                    }
                }
            }
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: fill in missing details, if needed
        StringBuilder messageBuf = new StringBuilder();
        this.condition.prettyPrint(messageBuf, methodContext);
        return "<if><condition>" + messageBuf + "</condition></if>";
    }

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.thenSubOps);
        if (this.elseSubOps != null)
            allSubOps.addAll(this.elseSubOps);
        if (elseIfs != null) {
            for (ElseIf elseIf : elseIfs) {
                allSubOps.addAll(elseIf.getThenSubOps());
            }
        }
        return allSubOps;
    }

    @Override
    public String rawString() {
        return expandedString(null);
    }

    public static final class MasterIfFactory implements Factory<MasterIf> {
        public MasterIf createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new MasterIf(element, simpleMethod);
        }

        public String getName() {
            return "if";
        }
    }
}
