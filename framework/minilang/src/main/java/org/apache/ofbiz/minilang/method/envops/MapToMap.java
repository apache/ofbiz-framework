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

import java.util.HashMap;
import java.util.Map;

import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;map-to-map&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class MapToMap extends MethodOperation {

    private final FlexibleMapAccessor<Map<String, Object>> mapFma;
    private final FlexibleMapAccessor<Map<String, Object>> toMapFma;

    public MapToMap(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "to-map", "map");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "to-map", "map");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
        toMapFma = FlexibleMapAccessor.getInstance(element.getAttribute("to-map"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Map<String, Object> fromMap = mapFma.get(methodContext.getEnvMap());
        if (fromMap != null) {
            if (!toMapFma.isEmpty()) {
                Map<String, Object> toMap = toMapFma.get(methodContext.getEnvMap());
                if (toMap == null) {
                    toMap = new HashMap<>();
                    toMapFma.put(methodContext.getEnvMap(), toMap);
                }
               toMap.putAll(fromMap);
            } else {
                methodContext.putAllEnv(fromMap);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<map-to-map ");
        sb.append("map=\"").append(this.mapFma).append("\" ");
        if (!toMapFma.isEmpty()) {
            sb.append("to-map=\"").append(this.toMapFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;map-to-map&gt; element.
     */
    public static final class MapToMapFactory implements Factory<MapToMap> {
        @Override
        public MapToMap createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new MapToMap(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "map-to-map";
        }
    }
}
