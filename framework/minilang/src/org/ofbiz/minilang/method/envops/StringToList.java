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

import java.text.*;
import java.util.*;

import org.w3c.dom.*;

import javolution.util.FastList;

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Appends the specified String to a List
 */
public class StringToList extends MethodOperation {
    public static final class StringToListFactory implements Factory<StringToList> {
        public StringToList createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new StringToList(element, simpleMethod);
        }

        public String getName() {
            return "string-to-list";
        }
    }

    public static final String module = StringToList.class.getName();

    String string;
    ContextAccessor<List<Object>> listAcsr;
    ContextAccessor<List<? extends Object>> argListAcsr;
    String messageFieldName;

    public StringToList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        listAcsr = new ContextAccessor<List<Object>>(element.getAttribute("list"), element.getAttribute("list-name"));
        argListAcsr = new ContextAccessor<List<? extends Object>>(element.getAttribute("arg-list"), element.getAttribute("arg-list-name"));
        messageFieldName = UtilValidate.isNotEmpty(element.getAttribute("message-field")) ? element.getAttribute("message-field") : element.getAttribute("message-field-name");
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        String valueStr = methodContext.expandString(string);
        if (!argListAcsr.isEmpty()) {
            List<? extends Object> argList = argListAcsr.get(methodContext);
            if (UtilValidate.isNotEmpty(argList)) {
                valueStr = MessageFormat.format(valueStr, argList.toArray());
            }
        }

        Object value;
        if (UtilValidate.isNotEmpty(this.messageFieldName)) {
            value = new MessageString(valueStr, this.messageFieldName, true);
        } else {
            value = valueStr;
        }

        List<Object> toList = listAcsr.get(methodContext);
        if (toList == null) {
            if (Debug.verboseOn()) Debug.logVerbose("List not found with name " + listAcsr + ", creating new List", module);
            toList = FastList.newInstance();
            listAcsr.put(methodContext, toList);
        }
        toList.add(value);

        return true;
    }

    @Override
    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-to-list string=\"" + this.string + "\" list-name=\"" + this.listAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
