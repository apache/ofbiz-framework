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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.base.util.Debug;
import org.w3c.dom.Element;

/**
 * Process sub-operations for each entry in the map
 */
public class IterateMap extends MethodOperation {
    
    public static final String module = IterateMap.class.getName();

    List subOps = new LinkedList();

    ContextAccessor keyAcsr;
    ContextAccessor valueAcsr;
    ContextAccessor mapAcsr;

    public IterateMap(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.keyAcsr = new ContextAccessor(element.getAttribute("key-name"));
        this.valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        this.mapAcsr = new ContextAccessor(element.getAttribute("map-name"));

        SimpleMethod.readOperations(element, subOps, simpleMethod);
    }

    public boolean exec(MethodContext methodContext) {


        if (mapAcsr.isEmpty()) {
            Debug.logWarning("No map-name specified in iterate tag, doing nothing: " + rawString(), module);
            return true;
        }

        Object oldKey = keyAcsr.get(methodContext);
        Object oldValue = valueAcsr.get(methodContext);
        if (oldKey != null) {
            Debug.logWarning("In iterate-map the key had a non-null value before entering the loop for the operation: " + this.rawString(), module);
        }
        if (oldValue != null) {
            Debug.logWarning("In iterate-map the value had a non-null value before entering the loop for the operation: " + this.rawString(), module);
        }
        
        Map theMap = (Map) mapAcsr.get(methodContext);
        if (theMap == null) {
            if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", doing nothing: " + rawString(), module);
            return true;
        }
        if (theMap.size() == 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Map with name " + mapAcsr + " has zero entries, doing nothing: " + rawString(), module);
            return true;
        }

        Iterator theIterator = theMap.entrySet().iterator();
        while (theIterator.hasNext()) {
            Map.Entry theEntry = (Map.Entry) theIterator.next();
            keyAcsr.put(methodContext, theEntry.getKey());
            valueAcsr.put(methodContext, theEntry.getValue());

            if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                // only return here if it returns false, otherwise just carry on
                return false;
            }
        }

        return true;
    }

    public String rawString() {
        return "<iterate-map map-name=\"" + this.mapAcsr + "\" key=\"" + this.keyAcsr + "\" value=\"" + this.valueAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
