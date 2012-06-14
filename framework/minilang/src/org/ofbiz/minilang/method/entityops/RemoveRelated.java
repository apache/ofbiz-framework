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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;remove-related&gt; element.
 */
public final class RemoveRelated extends MethodOperation {

    public static final String module = RemoveRelated.class.getName();

    private final FlexibleStringExpander doCacheClearFse;
    private final FlexibleStringExpander relationNameFse;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public RemoveRelated(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "relation-name", "do-cache-clear");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "relation-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        relationNameFse = FlexibleStringExpander.getInstance(element.getAttribute("relation-name"));
        doCacheClearFse = FlexibleStringExpander.getInstance(element.getAttribute("do-cache-clear"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value == null) {
            throw new MiniLangRuntimeException("Entity value not found with name: " + valueFma, this);
        }
        String relationName = relationNameFse.expandString(methodContext.getEnvMap());
        boolean doCacheClear = !"false".equals(doCacheClearFse.expandString(methodContext.getEnvMap()));
        try {
            methodContext.getDelegator().removeRelated(relationName, value, doCacheClear);
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while removing related entities: " + e.getMessage();
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
        StringBuilder sb = new StringBuilder("<remove-related ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("relation-name=\"").append(this.relationNameFse).append("\" ");
        if (!doCacheClearFse.isEmpty()) {
            sb.append("do-cache-clear=\"").append(this.doCacheClearFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;remove-related&gt; element.
     */
    public static final class RemoveRelatedFactory implements Factory<RemoveRelated> {
        @Override
        public RemoveRelated createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RemoveRelated(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "remove-related";
        }
    }
}
