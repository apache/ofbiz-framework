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
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Copies an environment field to a list
 */
public class FieldToList extends MethodOperation {

    public static final String module = FieldToList.class.getName();

    ContextAccessor<Object> fieldAcsr;
    ContextAccessor<List<Object>> listAcsr;
    ContextAccessor<Map<String, ? extends Object>> mapAcsr;

    public FieldToList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        // the schema for this element now just has the "field" attribute, though the old "field-name" and "map-name" pair is still supported
        mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"), element.getAttribute("field-name"));
        listAcsr = new ContextAccessor<List<Object>>(element.getAttribute("list"), element.getAttribute("list-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = null;
        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                Debug.logWarning("Map not found with name " + mapAcsr + ", Not copying to list", module);
                return true;
            }
            fieldVal = fieldAcsr.get(fromMap, methodContext);
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }
        if (fieldVal == null) {
            Debug.logWarning("Field value not found with name " + fieldAcsr + " in Map with name " + mapAcsr + ", Not copying to list", module);
            return true;
        }
        List<Object> toList = listAcsr.get(methodContext);
        if (toList == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("List not found with name " + listAcsr + ", creating new list", module);
            toList = FastList.newInstance();
            listAcsr.put(methodContext, toList);
        }
        toList.add(fieldVal);
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        return "<field-to-list list-name=\"" + this.listAcsr + "\" field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }

    public static final class FieldToListFactory implements Factory<FieldToList> {
        public FieldToList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FieldToList(element, simpleMethod);
        }

        public String getName() {
            return "field-to-list";
        }
    }
}
