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
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;if&gt; element.
 */
public final class MasterIf extends MethodOperation {

    private final Conditional condition;
    private final List<ElseIf> elseIfs;
    private final List<MethodOperation> elseSubOps;
    private final List<MethodOperation> thenSubOps;

    public MasterIf(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.childElements(simpleMethod, element, "condition", "then", "else-if", "else");
            MiniLangValidate.requiredChildElements(simpleMethod, element, "condition", "then");
        }
        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);
        Element thenElement = UtilXml.firstChildElement(element, "then");
        this.thenSubOps = Collections.unmodifiableList(SimpleMethod.readOperations(thenElement, simpleMethod));
        List<? extends Element> elseIfElements = UtilXml.childElementList(element, "else-if");
        if (elseIfElements.isEmpty()) {
            this.elseIfs = null;
        } else {
            List<ElseIf> elseIfs = new ArrayList<ElseIf>(elseIfElements.size());
            for (Element elseIfElement : elseIfElements) {
                elseIfs.add(new ElseIf(elseIfElement, simpleMethod));
            }
            this.elseIfs = Collections.unmodifiableList(elseIfs);
        }
        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement == null) {
            this.elseSubOps = null;
        } else {
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
            if (elseIfs != null) {
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
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        for (MethodOperation method : this.thenSubOps) {
            method.gatherArtifactInfo(aic);
        }
        if (this.elseSubOps != null) {
            for (MethodOperation method : this.elseSubOps) {
                method.gatherArtifactInfo(aic);
            }
        }
        if (this.elseIfs != null) {
            for (ElseIf elseIf : elseIfs) {
                elseIf.gatherArtifactInfo(aic);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder messageBuf = new StringBuilder();
        this.condition.prettyPrint(messageBuf, null);
        return "<if><condition>" + messageBuf + "</condition></if>";
    }

    /**
     * A &lt;if&gt; element factory. 
     */
    public static final class MasterIfFactory implements Factory<MasterIf> {
        public MasterIf createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new MasterIf(element, simpleMethod);
        }

        public String getName() {
            return "if";
        }
    }
}
