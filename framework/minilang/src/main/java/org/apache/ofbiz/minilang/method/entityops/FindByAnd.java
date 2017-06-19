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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.artifact.ArtifactInfoContext;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;find-by-and&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class FindByAnd extends EntityOperation {

    public static final String module = FindByAnd.class.getName();

    private final FlexibleStringExpander entityNameFse;
    private final FlexibleMapAccessor<Collection<String>> fieldsToSelectListFma;
    private final FlexibleMapAccessor<Object> listFma;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<List<String>> orderByListFma;
    private final FlexibleStringExpander useCacheFse;
    private final FlexibleStringExpander useIteratorFse;

    public FindByAnd(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "entity-name", "use-cache", "fields-to-select-list", "use-iterator", "list", "map", "order-by-list", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entity-name", "list", "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "map", "fields-to-select-list", "order-by-list", "delegator-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
        orderByListFma = FlexibleMapAccessor.getInstance(element.getAttribute("order-by-list"));
        fieldsToSelectListFma = FlexibleMapAccessor.getInstance(element.getAttribute("fields-to-select-list"));
        useCacheFse = FlexibleStringExpander.getInstance(element.getAttribute("use-cache"));
        useIteratorFse = FlexibleStringExpander.getInstance(element.getAttribute("use-iterator"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String entityName = entityNameFse.expandString(methodContext.getEnvMap());
        boolean useCache = "true".equals(useCacheFse.expandString(methodContext.getEnvMap()));
        boolean useIterator = "true".equals(useIteratorFse.expandString(methodContext.getEnvMap()));
        List<String> orderByNames = orderByListFma.get(methodContext.getEnvMap());
        Delegator delegator = getDelegator(methodContext);
        try {
            EntityCondition whereCond = null;
            Map<String, ? extends Object> fieldMap = mapFma.get(methodContext.getEnvMap());
            if (fieldMap != null) {
                whereCond = EntityCondition.makeCondition(fieldMap);
            }
            if (useIterator) {
                listFma.put(methodContext.getEnvMap(), EntityQuery.use(delegator)
                                                                  .select(UtilMisc.toSet(fieldsToSelectListFma.get(methodContext.getEnvMap())))
                                                                  .from(entityName)
                                                                  .where(whereCond)
                                                                  .orderBy(orderByNames)
                                                                  .queryIterator());
            } else {
                listFma.put(methodContext.getEnvMap(), EntityQuery.use(delegator)
                                                                  .select(UtilMisc.toSet(fieldsToSelectListFma.get(methodContext.getEnvMap())))
                                                                  .from(entityName)
                                                                  .where(whereCond)
                                                                  .orderBy(orderByNames)
                                                                  .cache(useCache)
                                                                  .queryList());
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
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
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
        if (!fieldsToSelectListFma.isEmpty()) {
            sb.append("fields-to-select-list=\"").append(this.fieldsToSelectListFma).append("\" ");
        }
        if (!useCacheFse.isEmpty()) {
            sb.append("use-cache=\"").append(this.useCacheFse).append("\" ");
        }
        if (!useIteratorFse.isEmpty()) {
            sb.append("use-iterator=\"").append(this.useIteratorFse).append("\" ");
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
