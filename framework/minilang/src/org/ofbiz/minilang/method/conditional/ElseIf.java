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
package org.ofbiz.minilang.method.conditional;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Implements the else-if alternate execution element.
 */
public class ElseIf {

    Conditional condition;
    List thenSubOps = new LinkedList();

    public ElseIf(Element element, SimpleMethod simpleMethod) {
        Element conditionElement = UtilXml.firstChildElement(element, "condition");
        Element conditionChildElement = UtilXml.firstChildElement(conditionElement);
        this.condition = ConditionalFactory.makeConditional(conditionChildElement, simpleMethod);
        
        Element thenElement = UtilXml.firstChildElement(element, "then");
        SimpleMethod.readOperations(thenElement, thenSubOps, simpleMethod);
    }

    public boolean checkCondition(MethodContext methodContext) {
        return condition.checkCondition(methodContext);
    }
    
    public boolean runSubOps(MethodContext methodContext) {
        return SimpleMethod.runSubOps(thenSubOps, methodContext);
    }
}
