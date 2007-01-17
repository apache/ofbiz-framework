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

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to remove entity values constrained by anding the map fields
 */
public class RemoveByAnd extends MethodOperation {
    
    public static final String module = RemoveByAnd.class.getName();
    
    String entityName;
    ContextAccessor mapAcsr;
    String doCacheClearStr;

    public RemoveByAnd(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        doCacheClearStr = element.getAttribute("do-cache-clear");
    }

    public boolean exec(MethodContext methodContext) {
        boolean doCacheClear = !"false".equals(doCacheClearStr);
        String entityName = methodContext.expandString(this.entityName);
        
        try {
            methodContext.getDelegator().removeByAnd(entityName, (Map) mapAcsr.get(methodContext), doCacheClear);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem removing the " + entityName + " entity by and: " + e.getMessage() + "]";

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
        return "<remove-by-and/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
