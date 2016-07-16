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
package org.apache.ofbiz.minilang.method.entityops;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;get-related-one&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cgetrelatedone%3E}}">Mini-language Reference</a>
 */
public final class GetRelatedOne extends MethodOperation {

    public static final String module = GetRelatedOne.class.getName();

    private final FlexibleStringExpander relationNameFse;
    private final FlexibleMapAccessor<GenericValue> toValueFma;
    private final FlexibleStringExpander useCacheFse;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public GetRelatedOne(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "relation-name", "to-value-field", "use-cache");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "relation-name", "to-value-field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "to-value-field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        relationNameFse = FlexibleStringExpander.getInstance(element.getAttribute("relation-name"));
        toValueFma = FlexibleMapAccessor.getInstance(element.getAttribute("to-value-field"));
        useCacheFse = FlexibleStringExpander.getInstance(element.getAttribute("use-cache"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value == null) {
            throw new MiniLangRuntimeException("Entity value not found with name: " + valueFma, this);
        }
        String relationName = relationNameFse.expandString(methodContext.getEnvMap());
        boolean useCache = "true".equals(useCacheFse.expandString(methodContext.getEnvMap()));
        try {
            toValueFma.put(methodContext.getEnvMap(), value.getRelatedOne(relationName, useCache));
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while finding related value: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(relationNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<get-related-one ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("relation-name=\"").append(this.relationNameFse).append("\" ");
        sb.append("to-value-field=\"").append(this.toValueFma).append("\" ");
        if (!useCacheFse.isEmpty()) {
            sb.append("use-cache=\"").append(this.useCacheFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;get-related-one&gt; element.
     */
    public static final class GetRelatedOneFactory implements Factory<GetRelatedOne> {
        @Override
        public GetRelatedOne createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new GetRelatedOne(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "get-related-one";
        }
    }
}
