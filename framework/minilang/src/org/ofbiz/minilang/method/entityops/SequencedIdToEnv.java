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
package org.ofbiz.minilang.method.entityops;

import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;sequenced-id&gt; element.
 */
public final class SequencedIdToEnv extends MethodOperation {

    private final FlexibleMapAccessor<Object> fieldFma;
    private final boolean getLongOnly;
    private final FlexibleStringExpander sequenceNameFse;
    private final long staggerMax;

    public SequencedIdToEnv(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "sequence-name", "field", "get-long-only", "stagger-max");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "sequence-name", "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        sequenceNameFse = FlexibleStringExpander.getInstance(element.getAttribute("sequence-name"));
        fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        getLongOnly = "true".equals(element.getAttribute("get-long-only"));
        long staggerMax = 1;
        String staggerMaxAttribute = element.getAttribute("stagger-max");
        if (!staggerMaxAttribute.isEmpty()) {
            try {
                staggerMax = Long.parseLong(staggerMaxAttribute);
                if (staggerMax < 1) {
                    staggerMax = 1;
                }
            } catch (NumberFormatException e) {
                MiniLangValidate.handleError("Invalid stagger-max attribute value: " + e.getMessage(), simpleMethod, element);
            }
        }
        this.staggerMax = staggerMax;
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String seqName = sequenceNameFse.expandString(methodContext.getEnvMap());
        if (getLongOnly) {
            fieldFma.put(methodContext.getEnvMap(), methodContext.getDelegator().getNextSeqIdLong(seqName, staggerMax));
        } else {
            fieldFma.put(methodContext.getEnvMap(), methodContext.getDelegator().getNextSeqId(seqName, staggerMax));
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    @Override
    public String rawString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<sequenced-id ");
        sb.append("sequence-name=\"").append(this.sequenceNameFse).append("\" ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        sb.append("stagger-max=\"").append(this.staggerMax).append("\" ");
        if (this.getLongOnly) {
            sb.append("get-long-only=\"true\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;sequenced-id&gt; element.
     */
    public static final class SequencedIdFactory implements Factory<SequencedIdToEnv> {
        @Override
        public SequencedIdToEnv createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new SequencedIdToEnv(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "sequenced-id";
        }
    }
}
