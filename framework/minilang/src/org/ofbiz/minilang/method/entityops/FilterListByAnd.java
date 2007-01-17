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
    
    ContextAccessor listAcsr;
    ContextAccessor toListAcsr;
    ContextAccessor mapAcsr;

    public FilterListByAnd(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor(element.getAttribute("to-list-name"), element.getAttribute("list-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
    }

    public boolean exec(MethodContext methodContext) {
        Map theMap = null;

        if (!mapAcsr.isEmpty()) {
            theMap = (Map) mapAcsr.get(methodContext);
        }
        toListAcsr.put(methodContext, EntityUtil.filterByAnd((List) listAcsr.get(methodContext), theMap));
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<filter-list-by-and/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
