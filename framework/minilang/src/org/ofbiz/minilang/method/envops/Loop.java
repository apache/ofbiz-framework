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
package org.ofbiz.minilang.method.envops;

import java.util.List;
import java.util.LinkedList;

import org.w3c.dom.Element;

import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.base.util.Debug;

/**
 * Loop
 */
public class Loop extends MethodOperation {

    public static final String module = Loop.class.getName();
    protected List subOps = new LinkedList();
    protected ContextAccessor fieldAcsr;
    protected String countStr;


    public Loop(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field"));
        this.countStr = element.getAttribute("count");

        SimpleMethod.readOperations(element, subOps, simpleMethod);
    }

    public boolean exec(MethodContext methodContext) {
        String countStrExp = methodContext.expandString(this.countStr);
        int count = 0;
        try {
            Double ctDbl = new Double(countStrExp);
            if (ctDbl != null) {
                count = ctDbl.intValue();    
            }
        } catch (NumberFormatException e) {
            Debug.logError(e, module);
            return false;
        }

        if (count < 1) {
            Debug.logWarning("Count is less than one, not doing nothing: " + rawString(), module);
            return false;
        }

        for (int i = 0; i < count; i++) {
            fieldAcsr.put(methodContext, new Integer(i));
            if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                // only return here if it returns false, otherwise just carry on
                return false;
            }
        }

        return true;
    }

    public String rawString() {
        return "<loop count=\"" + this.countStr + "\"/>";
    }

    public String expandedString(MethodContext methodContext) {
        return this.rawString();
    }
}
