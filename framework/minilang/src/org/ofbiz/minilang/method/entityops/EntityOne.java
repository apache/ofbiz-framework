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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.finder.PrimaryKeyFinder;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by a primary key
 */
public class EntityOne extends MethodOperation {
    
    public static final String module = EntityOne.class.getName();
    
    protected PrimaryKeyFinder finder;

    public EntityOne(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.finder = new PrimaryKeyFinder(element);
    }

    public boolean exec(MethodContext methodContext) {
        try {
            GenericDelegator delegator = methodContext.getDelegator();
            this.finder.runFind(methodContext.getEnvMap(), delegator);
        } catch (GeneralException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process: " + e.getMessage();

            if (methodContext.getMethodType() == MethodContext.EVENT) {
                methodContext.putEnv(simpleMethod.getEventErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getEventResponseCodeName(), simpleMethod.getDefaultErrorCode());
            } else if (methodContext.getMethodType() == MethodContext.SERVICE) {
                methodContext.putEnv(simpleMethod.getServiceErrorMessageName(), errMsg);
                methodContext.putEnv(simpleMethod.getServiceResponseMessageName(), simpleMethod.getDefaultErrorCode());
            }
            return false;
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<entity-one/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

