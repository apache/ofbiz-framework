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

import java.util.List;
import java.util.Map;

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
 * Implements the &lt;get-related&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class GetRelated extends MethodOperation {

    private static final String MODULE = GetRelated.class.getName();

    private final FlexibleMapAccessor<Object> listFma;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<List<String>> orderByListFma;
    private final FlexibleStringExpander relationNameFse;
    private final FlexibleStringExpander useCacheFse;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public GetRelated(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "relation-name", "list", "map", "order-by-list", "use-cache");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "relation-name", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "list", "map", "order-by-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        relationNameFse = FlexibleStringExpander.getInstance(element.getAttribute("relation-name"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
        orderByListFma = FlexibleMapAccessor.getInstance(element.getAttribute("order-by-list"));
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
        List<String> orderByNames = orderByListFma.get(methodContext.getEnvMap());
        Map<String, ? extends Object> constraintMap = mapFma.get(methodContext.getEnvMap());
        try {
            listFma.put(methodContext.getEnvMap(), value.getRelated(relationName, constraintMap, orderByNames, useCache));
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while finding related values: " + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            getSimpleMethod().addErrorMessage(methodContext, errMsg);
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
        StringBuilder sb = new StringBuilder("<get-related ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("relation-name=\"").append(this.relationNameFse).append("\" ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        if (!mapFma.isEmpty()) {
            sb.append("map=\"").append(this.mapFma).append("\" ");
        }
        if (!orderByListFma.isEmpty()) {
            sb.append("order-by-list=\"").append(this.orderByListFma).append("\" ");
        }
        if (!useCacheFse.isEmpty()) {
            sb.append("use-cache=\"").append(this.useCacheFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;get-related&gt; element.
     */
    public static final class GetRelatedFactory implements Factory<GetRelated> {
        @Override
        public GetRelated createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new GetRelated(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "get-related";
        }
    }
}
