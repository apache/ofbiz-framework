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

import java.sql.Timestamp;
import java.util.List;

import org.apache.ofbiz.base.util.UtilDateTime;
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
 * Implements the &lt;filter-list-by-date&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cfilterlistbydate%3E}}">Mini-language Reference</a>
 */
public final class FilterListByDate extends MethodOperation {

    private final FlexibleMapAccessor<List<GenericEntity>> listFma;
    private final FlexibleMapAccessor<List<GenericEntity>> toListFma;
    private final FlexibleMapAccessor<Timestamp> validDateFma;
    private final String fromFieldName;
    private final String thruFieldName;

    public FilterListByDate(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "to-list", "valid-date", "fromDate", "thruDate");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "to-list", "valid-date");
            MiniLangValidate.constantAttributes(simpleMethod, element, "fromDate", "thruDate");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        String toListAttribute = element.getAttribute("to-list");
        if (toListAttribute.isEmpty()) {
            toListFma = listFma;
        } else {
            toListFma = FlexibleMapAccessor.getInstance(toListAttribute);
        }
        validDateFma = FlexibleMapAccessor.getInstance(element.getAttribute("valid-date"));
        fromFieldName = MiniLangValidate.checkAttribute(element.getAttribute("from-field-name"), "fromDate");
        thruFieldName = MiniLangValidate.checkAttribute(element.getAttribute("thru-field-name"), "thruDate");
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (!validDateFma.isEmpty()) {
            toListFma.put(methodContext.getEnvMap(), EntityUtil.filterByDate(listFma.get(methodContext.getEnvMap()), validDateFma.get(methodContext.getEnvMap()), fromFieldName, thruFieldName, true));
        } else {
            toListFma.put(methodContext.getEnvMap(), EntityUtil.filterByDate(listFma.get(methodContext.getEnvMap()), UtilDateTime.nowTimestamp(), fromFieldName, thruFieldName, true));
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<filter-list-by-date ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        sb.append("to-list=\"").append(this.toListFma).append("\" ");
        sb.append("valid-date=\"").append(this.validDateFma).append("\" ");
        sb.append("from-field-name=\"").append(this.fromFieldName).append("\" ");
        sb.append("thru-field-name=\"").append(this.thruFieldName).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;filter-list-by-date&gt; element.
     */
    public static final class FilterListByDateFactory implements Factory<FilterListByDate> {
        @Override
        public FilterListByDate createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FilterListByDate(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "filter-list-by-date";
        }
    }
}
