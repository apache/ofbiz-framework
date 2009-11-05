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
import java.util.Map;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Process sub-operations for each entry in the map
 */
public class IterateMap extends MethodOperation {
    public static final class IterateMapFactory implements Factory<IterateMap> {
        public IterateMap createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new IterateMap(element, simpleMethod);
        }

        public String getName() {
            return "iterate-map";
        }
    }

    public static final String module = IterateMap.class.getName();

    List<MethodOperation> subOps = FastList.newInstance();

    ContextAccessor<Object> keyAcsr;
    ContextAccessor<Object> valueAcsr;
    ContextAccessor<Map<? extends Object, ? extends Object>> mapAcsr;

    public IterateMap(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.keyAcsr = new ContextAccessor<Object>(element.getAttribute("key"), element.getAttribute("key-name"));
        this.valueAcsr = new ContextAccessor<Object>(element.getAttribute("value"), element.getAttribute("value-name"));
        this.mapAcsr = new ContextAccessor<Map<? extends Object, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));

        SimpleMethod.readOperations(element, subOps, simpleMethod);
    }

    @Override
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

        Map<? extends Object, ? extends Object> theMap = mapAcsr.get(methodContext);
        if (theMap == null) {
            if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", doing nothing: " + rawString(), module);
            return true;
        }
        if (theMap.size() == 0) {
            if (Debug.verboseOn()) Debug.logVerbose("Map with name " + mapAcsr + " has zero entries, doing nothing: " + rawString(), module);
            return true;
        }

        for (Map.Entry<? extends Object, ? extends Object> theEntry: theMap.entrySet()) {
            keyAcsr.put(methodContext, theEntry.getKey());
            valueAcsr.put(methodContext, theEntry.getValue());

            if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                // only return here if it returns false, otherwise just carry on
                return false;
            }
        }

        return true;
    }

    public List<MethodOperation> getSubOps() {
        return this.subOps;
    }

    @Override
    public String rawString() {
        return "<iterate-map map-name=\"" + this.mapAcsr + "\" key=\"" + this.keyAcsr + "\" value=\"" + this.valueAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
