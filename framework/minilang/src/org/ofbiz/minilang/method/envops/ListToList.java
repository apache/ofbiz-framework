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

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies an environment field to a list
 */
public class ListToList extends MethodOperation {
    
    public static final String module = ListToList.class.getName();
    
    ContextAccessor listAcsr;
    ContextAccessor toListAcsr;

    public ListToList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor(element.getAttribute("to-list-name"));
    }

    public boolean exec(MethodContext methodContext) {

        List fromList = (List) listAcsr.get(methodContext);
        List toList = (List) toListAcsr.get(methodContext);

        if (fromList == null) {
            if (Debug.infoOn()) Debug.logInfo("List not found with name " + listAcsr + ", not copying list", module);
            return true;
        }

        if (toList == null) {
            if (Debug.verboseOn()) Debug.logVerbose("List not found with name " + toListAcsr + ", creating new list", module);
            toList = new LinkedList();
            toListAcsr.put(methodContext, toList);
        }

        toList.addAll(fromList);
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<list-to-list/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
