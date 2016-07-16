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
import java.util.Map;

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
 * Implements the &lt;filter-list-by-and&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cfilterlistbyand%3E}}">Mini-language Reference</a>
 */
public final class FilterListByAnd extends MethodOperation {

    private final FlexibleMapAccessor<List<GenericEntity>> listFma;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<List<GenericEntity>> toListFma;

    public FilterListByAnd(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "map", "to-list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list", "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "map", "to-list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        String toListAttribute = element.getAttribute("to-list");
        if (toListAttribute.isEmpty()) {
            toListFma = listFma;
        } else {
            toListFma = FlexibleMapAccessor.getInstance(toListAttribute);
        }
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Map<String, ? extends Object> theMap = mapFma.get(methodContext.getEnvMap());
        toListFma.put(methodContext.getEnvMap(), EntityUtil.filterByAnd(listFma.get(methodContext.getEnvMap()), theMap));
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<filter-list-by-and ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        sb.append("map=\"").append(this.mapFma).append("\" ");
        sb.append("to-list=\"").append(this.toListFma).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;filter-list-by-and&gt; element.
     */
    public static final class FilterListByAndFactory implements Factory<FilterListByAnd> {
        @Override
        public FilterListByAnd createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FilterListByAnd(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "filter-list-by-and";
        }
    }
}
