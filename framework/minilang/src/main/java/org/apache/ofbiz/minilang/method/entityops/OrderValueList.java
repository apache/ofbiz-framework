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

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.util.EntityUtil;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;order-value-list&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class OrderValueList extends MethodOperation {

    private final FlexibleMapAccessor<List<? extends GenericEntity>> listFma;
    private final FlexibleMapAccessor<List<String>> orderByListFma;
    private final FlexibleMapAccessor<List<? extends GenericEntity>> toListFma;

    public OrderValueList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "order-by-list", "to-list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list", "order-by-list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "order-by-list", "to-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        orderByListFma = FlexibleMapAccessor.getInstance(element.getAttribute("order-by-list"));
        String toListAttribute = element.getAttribute("to-list");
        if (toListAttribute.isEmpty()) {
            toListFma = listFma;
        } else {
            toListFma = FlexibleMapAccessor.getInstance(toListAttribute);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<String> orderByList = orderByListFma.get(methodContext.getEnvMap());
        toListFma.put(methodContext.getEnvMap(), EntityUtil.orderBy(listFma.get(methodContext.getEnvMap()), orderByList));
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<order-value-list ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        sb.append("order-by-list=\"").append(this.orderByListFma).append("\" ");
        sb.append("to-list=\"").append(this.toListFma).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;order-value-list&gt; element.
     */
    public static final class OrderValueListFactory implements Factory<OrderValueList> {
        @Override
        public OrderValueList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new OrderValueList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "order-value-list";
        }
    }
}
