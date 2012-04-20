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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.MiniLangException;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Get the first entry from the list
 */
public class FirstFromList extends MethodOperation {

    public static final String module = FirstFromList.class.getName();

    ContextAccessor<Object> entryAcsr;
    ContextAccessor<List<? extends Object>> listAcsr;

    public FirstFromList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        this.entryAcsr = new ContextAccessor<Object>(element.getAttribute("entry"), element.getAttribute("entry-name"));
        this.listAcsr = new ContextAccessor<List<? extends Object>>(element.getAttribute("list"), element.getAttribute("list-name"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (listAcsr.isEmpty()) {
            Debug.logWarning("No list-name specified in iterate tag, doing nothing", module);
            return true;
        }
        List<? extends Object> theList = listAcsr.get(methodContext);
        if (UtilValidate.isEmpty(theList)) {
            entryAcsr.put(methodContext, null);
            return true;
        }
        entryAcsr.put(methodContext, theList.get(0));
        return true;
    }

    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }

    @Override
    public String rawString() {
        return "<first-from-list list-name=\"" + this.listAcsr + "\" entry-name=\"" + this.entryAcsr + "\"/>";
    }

    public static final class FirstFromListFactory implements Factory<FirstFromList> {
        public FirstFromList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FirstFromList(element, simpleMethod);
        }

        public String getName() {
            return "first-from-list";
        }
    }
}
