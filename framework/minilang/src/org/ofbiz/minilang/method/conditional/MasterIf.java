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
 * Represents the top-level element and only mounted operation for the more flexible if structure.
 */
public class MasterIf extends MethodOperation {
    public static final class MasterIfFactory implements Factory<MasterIf> {
        public MasterIf createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new MasterIf (element, simpleMethod);
        }

        public String getName() {
            return "if";
        }
    }

    Conditional condition;

    List<MethodOperation> thenSubOps = FastList.newInstance();
    List<MethodOperation> elseSubOps = null;

    List<ElseIf> elseIfs = null;

    public MasterIf (Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);

        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);

        Element thenElement = UtilXml.firstChildElement(element, "then");
        SimpleMethod.readOperations(thenElement, thenSubOps, simpleMethod);

        List<? extends Element> elseIfElements = UtilXml.childElementList(element, "else-if");
        if (UtilValidate.isNotEmpty(elseIfElements)) {
            elseIfs = FastList.newInstance();
            for (Element elseIfElement: elseIfElements) {
                elseIfs.add(new ElseIf (elseIfElement, simpleMethod));
            }
        }

        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = FastList.newInstance();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
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
                for (ElseIf elseIf: elseIfs) {
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

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.thenSubOps);
        if (this.elseSubOps != null) allSubOps.addAll(this.elseSubOps);
        if (elseIfs != null) {
            for (ElseIf elseIf: elseIfs) {
                allSubOps.addAll(elseIf.getThenSubOps());
            }
        }

        return allSubOps;
    }

    @Override
    public String rawString() {
        return expandedString(null);
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: fill in missing details, if needed
        StringBuilder messageBuf = new StringBuilder();
        this.condition.prettyPrint(messageBuf, methodContext);
        return "<if><condition>" + messageBuf + "</condition></if>";
    }
}
