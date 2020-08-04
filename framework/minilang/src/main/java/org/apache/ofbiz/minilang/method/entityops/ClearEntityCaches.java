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

import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.w3c.dom.Element;

/**
 * Implements the &lt;clear-entity-caches&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class ClearEntityCaches extends EntityOperation {

    public ClearEntityCaches(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "delegator-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "delegator-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Delegator delegator = getDelegator(methodContext);
        delegator.clearAllCaches();
        return true;
    }

    @Override
    public String toString() {
        return "<clear-entity-caches/>";
    }

    /**
     * A factory for the &lt;clear-entity-caches&gt; element.
     */
    public static final class ClearEntityCachesFactory implements Factory<ClearEntityCaches> {
        @Override
        public ClearEntityCaches createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ClearEntityCaches(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "clear-entity-caches";
        }
    }
}
