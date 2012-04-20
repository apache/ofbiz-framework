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
public class ListToList extends MethodOperation {

    public static final String module = ListToList.class.getName();

    ContextAccessor<List<Object>> listAcsr;
    ContextAccessor<List<Object>> toListAcsr;

    public ListToList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor<List<Object>>(element.getAttribute("list"), element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor<List<Object>>(element.getAttribute("to-list"), element.getAttribute("to-list-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        List<Object> fromList = listAcsr.get(methodContext);
        List<Object> toList = toListAcsr.get(methodContext);
        if (fromList == null) {
            if (Debug.infoOn())
                Debug.logInfo("List not found with name " + listAcsr + ", not copying list", module);
            return true;
        }
        if (toList == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("List not found with name " + toListAcsr + ", creating new list", module);
            toList = FastList.newInstance();
            toListAcsr.put(methodContext, toList);
        }
        toList.addAll(fromList);
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<list-to-list/>";
    }

    public static final class ListToListFactory implements Factory<ListToList> {
        public ListToList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new ListToList(element, simpleMethod);
        }

        public String getName() {
            return "list-to-list";
        }
    }
}
