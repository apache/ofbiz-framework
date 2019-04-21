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
package org.apache.ofbiz.minilang.method.envops;

import java.util.LinkedList;
import java.util.List;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;field-to-list&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class FieldToList extends MethodOperation {

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleMapAccessor<List<Object>> listFma;

    public FieldToList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.handleError("<field-to-list> element is deprecated (use <set>)", simpleMethod, element);
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = fieldFma.get(methodContext.getEnvMap());
        if (fieldVal != null) {
            List<Object> toList = listFma.get(methodContext.getEnvMap());
            if (toList == null) {
                toList = new LinkedList<>();
                listFma.put(methodContext.getEnvMap(), toList);
            }
            toList.add(fieldVal);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<field-to-list ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        sb.append("list=\"").append(this.listFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;field-to-list&gt; element.
     */
    public static final class FieldToListFactory implements Factory<FieldToList> {
        @Override
        public FieldToList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FieldToList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "field-to-list";
        }
    }
}
