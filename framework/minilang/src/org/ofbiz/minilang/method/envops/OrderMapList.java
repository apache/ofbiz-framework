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
package org.ofbiz.minilang.method.envops;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapComparator;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies an environment field to a list
 */
public class OrderMapList extends MethodOperation {

    public static final String module = FieldToList.class.getName();

    protected ContextAccessor<List<Map<Object, Object>>> listAcsr;
    protected MapComparator mc;
    protected List<FlexibleMapAccessor<String>> orderByAcsrList = FastList.newInstance();

    public OrderMapList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor<List<Map<Object, Object>>>(element.getAttribute("list"), element.getAttribute("list-name"));
        for (Element orderByElement : UtilXml.childElementList(element, "order-by")) {
            FlexibleMapAccessor<String> fma = FlexibleMapAccessor.getInstance(UtilValidate.isNotEmpty(orderByElement.getAttribute("field")) ? orderByElement.getAttribute("field") : orderByElement.getAttribute("field-name"));
            this.orderByAcsrList.add(fma);
        }
        this.mc = new MapComparator(this.orderByAcsrList);
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Map<Object, Object>> orderList = listAcsr.get(methodContext);
        if (orderList == null) {
            if (Debug.infoOn())
                Debug.logInfo("List not found with name " + listAcsr + ", not ordering/sorting list.", module);
            return true;
        }
        Collections.sort(orderList, mc);
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        return "<order-map-list list-name=\"" + this.listAcsr + "\"/>";
    }

    public static final class OrderMapListFactory implements Factory<OrderMapList> {
        public OrderMapList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new OrderMapList(element, simpleMethod);
        }

        public String getName() {
            return "order-map-list";
        }
    }
}
