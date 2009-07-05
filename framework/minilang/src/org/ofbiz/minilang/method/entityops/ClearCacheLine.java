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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Uses the delegator to clear elements from the cache; intelligently looks at
 *  the map passed to see if it is a byPrimaryKey, and byAnd, or an all.
 */
public class ClearCacheLine extends MethodOperation {
    public static final class ClearCacheLineFactory implements Factory<ClearCacheLine> {
        public ClearCacheLine createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new ClearCacheLine(element, simpleMethod);
        }

        public String getName() {
            return "clear-cache-line";
        }
    }

    public static final String module = ClearCacheLine.class.getName();

    String entityName;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;

    public ClearCacheLine(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        entityName = element.getAttribute("entity-name");
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map"), element.getAttribute("map-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String entityName = methodContext.expandString(this.entityName);

        if (mapAcsr.isEmpty()) {
            methodContext.getDelegator().clearCacheLine(entityName);
        } else {
            Map<String, ? extends Object> theMap = mapAcsr.get(methodContext);
            if (theMap == null) {
                Debug.logWarning("In clear-cache-line could not find map with name " + mapAcsr + ", not clearing any cache lines", module);
            } else {
                methodContext.getDelegator().clearCacheLine(entityName, theMap);
            }
        }
        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<clear-cache-line/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
