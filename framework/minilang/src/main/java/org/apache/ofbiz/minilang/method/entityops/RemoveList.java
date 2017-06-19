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

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;remove-list&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class RemoveList extends EntityOperation {

    public static final String module = RemoveList.class.getName();
    private final FlexibleMapAccessor<List<GenericValue>> listFma;

    public RemoveList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "do-cache-clear", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "delegator-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<GenericValue> values = listFma.get(methodContext.getEnvMap());
        if (values == null) {
            throw new MiniLangRuntimeException("Entity value list not found with name: " + listFma, this);
        }
        try {
            Delegator delegator = getDelegator(methodContext);
            delegator.removeAll(values);
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
