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

import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Appends the specified String to a List
 */
public class StringToList extends MethodOperation {
    
    public static final String module = StringToList.class.getName();
    
    String string;
    ContextAccessor listAcsr;
    ContextAccessor argListAcsr;
    String messageFieldName;

    public StringToList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        string = element.getAttribute("string");
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        argListAcsr = new ContextAccessor(element.getAttribute("arg-list-name"));
        messageFieldName = element.getAttribute("message-field-name");
    }

    public boolean exec(MethodContext methodContext) {
        String valueStr = methodContext.expandString(string);
        if (!argListAcsr.isEmpty()) {
            List argList = (List) argListAcsr.get(methodContext);
            if (argList != null && argList.size() > 0) {
                valueStr = MessageFormat.format(valueStr, argList.toArray());
            }
        }

        Object value;
        if (this.messageFieldName != null && this.messageFieldName.length() > 0) {
            value = new MessageString(valueStr, this.messageFieldName, true);
        } else {
            value = valueStr;
        }

        List toList = (List) listAcsr.get(methodContext);
        if (toList == null) {
            if (Debug.verboseOn()) Debug.logVerbose("List not found with name " + listAcsr + ", creating new List", module);
            toList = new LinkedList();
            listAcsr.put(methodContext, toList);
        }
        toList.add(value);

        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<string-to-list string=\"" + this.string + "\" list-name=\"" + this.listAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
