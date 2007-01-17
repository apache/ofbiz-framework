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

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 */
public class FilterListByDate extends MethodOperation {
    
    ContextAccessor listAcsr;
    ContextAccessor toListAcsr;
    ContextAccessor validDateAcsr;
    String fromFieldName;
    String thruFieldName;
    String allSameStr;

    public FilterListByDate(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor(element.getAttribute("to-list-name"), element.getAttribute("list-name"));
        validDateAcsr = new ContextAccessor(element.getAttribute("valid-date-name"));

        fromFieldName = element.getAttribute("from-field-name");
        if (UtilValidate.isEmpty(fromFieldName)) fromFieldName = "fromDate";
        thruFieldName = element.getAttribute("thru-field-name");
        if (UtilValidate.isEmpty(thruFieldName)) thruFieldName = "thruDate";

        allSameStr = element.getAttribute("all-same");
    }

    public boolean exec(MethodContext methodContext) {

        if (!validDateAcsr.isEmpty()) {
            toListAcsr.put(methodContext, EntityUtil.filterByDate((List) listAcsr.get(methodContext), (java.sql.Timestamp) validDateAcsr.get(methodContext), fromFieldName, thruFieldName, true));
        } else {
            toListAcsr.put(methodContext, EntityUtil.filterByDate((List) listAcsr.get(methodContext), UtilDateTime.nowTimestamp(), fromFieldName, thruFieldName, true));
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<filter-list-by-date/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

