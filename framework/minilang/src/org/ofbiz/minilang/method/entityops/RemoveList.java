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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.MiniLangRuntimeException;
import org.ofbiz.minilang.MiniLangValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;remove-list&gt; element.
 */
public final class RemoveList extends MethodOperation {

    public static final String module = RemoveList.class.getName();

    private final FlexibleStringExpander doCacheClearFse;
    private final FlexibleMapAccessor<List<GenericValue>> listFma;

    public RemoveList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "do-cache-clear");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        doCacheClearFse = FlexibleStringExpander.getInstance(element.getAttribute("do-cache-clear"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<GenericValue> values = listFma.get(methodContext.getEnvMap());
        if (values == null) {
            throw new MiniLangRuntimeException("Entity value list not found with name: " + listFma, this);
        }
        boolean doCacheClear = !"false".equals(doCacheClearFse.expandString(methodContext.getEnvMap()));
        try {
            methodContext.getDelegator().removeAll(values, doCacheClear);
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while removing entities: " + e.getMessage();
            Debug.logWarning(e, errMsg, module);
            simpleMethod.addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<remove-list ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        if (!doCacheClearFse.isEmpty()) {
            sb.append("do-cache-clear=\"").append(this.doCacheClearFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;remove-list&gt; element.
     */
    public static final class RemoveListFactory implements Factory<RemoveList> {
        @Override
        public RemoveList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RemoveList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "remove-list";
        }
    }
}
