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

import java.util.List;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;first-from-list&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{%3Cfirstfromlist%3E}}">Mini-language Reference</a>
 */
public final class FirstFromList extends MethodOperation {

    private final FlexibleMapAccessor<Object> entryFma;
    private final FlexibleMapAccessor<List<Object>> listFma;

    public FirstFromList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.handleError("<first-from-list> element is deprecated (use <set>)", simpleMethod, element);
            MiniLangValidate.attributeNames(simpleMethod, element, "entry", "list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "entry", "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "entry", "list");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        entryFma = FlexibleMapAccessor.getInstance(element.getAttribute("entry"));
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<? extends Object> theList = listFma.get(methodContext.getEnvMap());
        if (UtilValidate.isEmpty(theList)) {
            entryFma.put(methodContext.getEnvMap(), null);
        } else {
            entryFma.put(methodContext.getEnvMap(), theList.get(0));
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<first-from-list ");
        sb.append("entry=\"").append(this.entryFma).append("\" ");
        sb.append("list=\"").append(this.listFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;first-from-list&gt; element.
     */
    public static final class FirstFromListFactory implements Factory<FirstFromList> {
        @Override
        public FirstFromList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FirstFromList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "first-from-list";
        }
    }
}
