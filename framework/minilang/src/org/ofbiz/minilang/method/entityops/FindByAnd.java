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

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;find-by-and&gt; element.
 */
public final class FindByAnd extends MethodOperation {

    public static final String module = FindByAnd.class.getName();

    private final FlexibleStringExpander delegatorNameFse;
    private final FlexibleStringExpander entityNameFse;
    private final FlexibleMapAccessor<Object> listFma;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<List<String>> orderByListFma;
    private final FlexibleStringExpander useCacheFse;
    private final FlexibleStringExpander useIteratorFse;

    public FindByAnd(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entity-name", "use-cache", "use-iterator", "list", "map", "order-by-list", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entity-name", "list", "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "map", "order-by-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
        orderByListFma = FlexibleMapAccessor.getInstance(element.getAttribute("order-by-list"));
        useCacheFse = FlexibleStringExpander.getInstance(element.getAttribute("use-cache"));
        useIteratorFse = FlexibleStringExpander.getInstance(element.getAttribute("use-iterator"));
        delegatorNameFse = FlexibleStringExpander.getInstance(element.getAttribute("delegator-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String entityName = entityNameFse.expandString(methodContext.getEnvMap());
        String delegatorName = delegatorNameFse.expandString(methodContext.getEnvMap());
        boolean useCache = "true".equals(useCacheFse.expandString(methodContext.getEnvMap()));
        boolean useIterator = "true".equals(useIteratorFse.expandString(methodContext.getEnvMap()));
        List<String> orderByNames = orderByListFma.get(methodContext.getEnvMap());
        Delegator delegator = methodContext.getDelegator();
        if (!delegatorName.isEmpty()) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        try {
            if (useIterator) {
                EntityCondition whereCond = null;
                if (!mapFma.isEmpty()) {
                    whereCond = EntityCondition.makeCondition(mapFma.get(methodContext.getEnvMap()));
                }
                listFma.put(methodContext.getEnvMap(), delegator.find(entityName, whereCond, null, null, orderByNames, null));
            } else {
                listFma.put(methodContext.getEnvMap(), delegator.findByAnd(entityName, mapFma.get(methodContext.getEnvMap()), orderByNames, useCache));
            }
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while performing entity find: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        return FlexibleStringExpander.expandString(toString(), methodContext.getEnvMap());
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
    }

    @Override
    public String rawString() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<find-by-and ");
        sb.append("entity-name=\"").append(this.entityNameFse).append("\" ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        sb.append("map=\"").append(this.mapFma).append("\" ");
        if (!orderByListFma.isEmpty()) {
            sb.append("order-by-list=\"").append(this.orderByListFma).append("\" ");
        }
        if (!useCacheFse.isEmpty()) {
            sb.append("use-cache=\"").append(this.useCacheFse).append("\" ");
        }
        if (!useIteratorFse.isEmpty()) {
            sb.append("use-iterator=\"").append(this.useIteratorFse).append("\" ");
        }
        if (!delegatorNameFse.isEmpty()) {
            sb.append("delegator-name=\"").append(this.delegatorNameFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;find-by-and&gt; element.
     */
    public static final class FindByAndFactory implements Factory<FindByAnd> {
        @Override
        public FindByAnd createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FindByAnd(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "find-by-and";
        }
    }
}
