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

import java.util.Collections;
import java.util.List;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.apache.ofbiz.minilang.method.envops.Break.BreakElementException;
import org.apache.ofbiz.minilang.method.envops.Continue.ContinueElementException;
import org.w3c.dom.Element;

/**
 * Implements the &lt;loop&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class Loop extends MethodOperation {

    private static final String MODULE = Loop.class.getName();

    private final FlexibleStringExpander countFse;
    private final FlexibleMapAccessor<Integer> fieldFma;
    private final List<MethodOperation> subOps;

    public Loop(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "count", "field");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "count");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "count", "field");
        }
        this.countFse = FlexibleStringExpander.getInstance(element.getAttribute("count"));
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.subOps = Collections.unmodifiableList(SimpleMethod.readOperations(element, simpleMethod));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String countStr = this.countFse.expandString(methodContext.getEnvMap());
        int count = 0;
        try {
            count = Double.valueOf(countStr).intValue();
        } catch (NumberFormatException e) {
            throw new MiniLangRuntimeException("Error while converting \"" + countStr + "\" to a number: " + e.getMessage(), this);
        }
        if (count < 0) {
            throw new MiniLangRuntimeException("Unable to execute loop operation because the count is negative: " + countStr, this);
        }
        for (int i = 0; i < count; i++) {
            this.fieldFma.put(methodContext.getEnvMap(), i);
            try {
                for (MethodOperation methodOperation : subOps) {
                    if (!methodOperation.exec(methodContext)) {
                        return false;
                    }
                }
            } catch (MiniLangException e) {
                if (e instanceof BreakElementException) {
                    break;
                }
                if (e instanceof ContinueElementException) {
                    continue;
                }
                throw e;
            }
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        for (MethodOperation method : this.subOps) {
            method.gatherArtifactInfo(aic);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<loop ");
        if (!this.countFse.isEmpty()) {
            sb.append("count=\"").append(this.countFse).append("\" ");
        }
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;loop&gt; element.
     */
    public static final class LoopFactory implements Factory<Loop> {
        @Override
        public Loop createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Loop(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "loop";
        }
    }
}
