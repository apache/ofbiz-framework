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

import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * An abstract base class for entity operations.
 */
public abstract class EntityOperation extends MethodOperation {

    private final FlexibleStringExpander delegatorNameFse;

    public EntityOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        this.delegatorNameFse = FlexibleStringExpander.getInstance(element.getAttribute("delegator-name"));
    }

    protected final Delegator getDelegator(MethodContext methodContext) {
        String delegatorName = delegatorNameFse.expandString(methodContext.getEnvMap());
        if (!delegatorName.isEmpty()) {
            return DelegatorFactory.getDelegator(delegatorName);
        }
        return methodContext.getDelegator();
    }
}
