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
 * Implements the &lt;list-to-list&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class ListToList extends MethodOperation {

    private final FlexibleMapAccessor<List<Object>> listFma;
    private final FlexibleMapAccessor<List<Object>> toListFma;

    public ListToList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "to-list", "list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "to-list", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "to-list", "list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        toListFma = FlexibleMapAccessor.getInstance(element.getAttribute("to-list"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> fromList = listFma.get(methodContext.getEnvMap());
        if (fromList != null) {
            List<Object> toList = toListFma.get(methodContext.getEnvMap());
            if (toList == null) {
                toList = new LinkedList<>();
                toListFma.put(methodContext.getEnvMap(), toList);
            }
            toList.addAll(fromList);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<list-to-list ");
        sb.append("to-list=\"").append(this.toListFma).append("\" ");
        sb.append("list=\"").append(this.listFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;list-to-list&gt; element.
     */
    public static final class ListToListFactory implements Factory<ListToList> {
        @Override
        public ListToList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ListToList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "list-to-list";
        }
    }
}
