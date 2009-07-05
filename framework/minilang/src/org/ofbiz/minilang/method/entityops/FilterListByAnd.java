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

import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 */
public class FilterListByAnd extends MethodOperation {
    public static final class FilterListByAndFactory implements Factory<FilterListByAnd> {
        public FilterListByAnd createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new FilterListByAnd(element, simpleMethod);
        }

        public String getName() {
            return "filter-list-by-and";
        }
    }

    ContextAccessor<List<GenericEntity>> listAcsr;
    ContextAccessor<List<GenericEntity>> toListAcsr;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;

    public FilterListByAnd(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor<List<GenericEntity>>(element.getAttribute("list"), element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor<List<GenericEntity>>(element.getAttribute("to-list"), element.getAttribute("to-list-name"));
        if (toListAcsr.isEmpty()) {
            toListAcsr = listAcsr;
        }
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        Map<String, ? extends Object> theMap = null;

        if (!mapAcsr.isEmpty()) {
            theMap = mapAcsr.get(methodContext);
        }
        toListAcsr.put(methodContext, EntityUtil.filterByAnd(listAcsr.get(methodContext), theMap));
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<filter-list-by-and/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
