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
package org.ofbiz.minilang.method.ifops;

import java.util.*;

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Iff the specified field is not empty process sub-operations
 */
public class IfEmpty extends MethodOperation {
    public static final class IfEmptyFactory implements Factory<IfEmpty> {
        public IfEmpty createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new IfEmpty(element, simpleMethod);
        }

        public String getName() {
            return "if-empty";
        }
    }

    public static final String module = IfEmpty.class.getName();

    List<MethodOperation> subOps = FastList.newInstance();
    List<MethodOperation> elseSubOps = null;

    ContextAccessor<Map<String, ? extends Object>> mapAcsr;
    ContextAccessor<Object> fieldAcsr;

    public IfEmpty(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        // NOTE: this is still supported, but is deprecated
        this.mapAcsr = new ContextAccessor<Map<String, ? extends Object>>(element.getAttribute("map-name"));
        this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field"));
        if (this.fieldAcsr.isEmpty()) {
            // NOTE: this is still supported, but is deprecated
            this.fieldAcsr = new ContextAccessor<Object>(element.getAttribute("field-name"));
        }

        SimpleMethod.readOperations(element, subOps, simpleMethod);

        Element elseElement = UtilXml.firstChildElement(element, "else");
        if (elseElement != null) {
            elseSubOps = FastList.newInstance();
            SimpleMethod.readOperations(elseElement, elseSubOps, simpleMethod);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        // if conditions fails, always return true; if a sub-op returns false
        // return false and stop, otherwise return true
        // return true;

        // only run subOps if element is empty/null
        boolean runSubOps = false;
        Object fieldVal = null;

        if (!mapAcsr.isEmpty()) {
            Map<String, ? extends Object> fromMap = mapAcsr.get(methodContext);
            if (fromMap == null) {
                if (Debug.infoOn()) Debug.logInfo("Map not found with name " + mapAcsr + ", running operations", module);
            } else {
                fieldVal = fieldAcsr.get(fromMap, methodContext);
            }
        } else {
            // no map name, try the env
            fieldVal = fieldAcsr.get(methodContext);
        }

        runSubOps = ObjectType.isEmpty(fieldVal);

        if (runSubOps) {
            return SimpleMethod.runSubOps(subOps, methodContext);
        } else {
            if (elseSubOps != null) {
                return SimpleMethod.runSubOps(elseSubOps, methodContext);
            } else {
                return true;
            }
        }
    }

    public List<MethodOperation> getAllSubOps() {
        List<MethodOperation> allSubOps = FastList.newInstance();
        allSubOps.addAll(this.subOps);
        if (this.elseSubOps != null) allSubOps.addAll(this.elseSubOps);
        return allSubOps;
    }

    @Override
    public String rawString() {
        return "<if-empty field-name=\"" + this.fieldAcsr + "\" map-name=\"" + this.mapAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
