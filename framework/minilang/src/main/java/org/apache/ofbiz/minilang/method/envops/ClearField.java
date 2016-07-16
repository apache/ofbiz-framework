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

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;clear-field&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBADMIN/Mini-language+Reference#Mini-languageReference-{{<clearfield>}}">Mini-language Reference</a>
 */
public final class ClearField extends MethodOperation {

    private final FlexibleMapAccessor<Object> fieldFma;

    public ClearField(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        fieldFma.put(methodContext.getEnvMap(), null);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<set ");
        sb.append("field=\"").append(this.fieldFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;clear-field&gt; element.
     */
    public static final class ClearFieldFactory implements Factory<ClearField> {
        @Override
        public ClearField createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ClearField(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "clear-field";
        }
    }
}
