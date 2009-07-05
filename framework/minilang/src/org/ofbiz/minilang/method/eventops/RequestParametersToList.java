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
package org.ofbiz.minilang.method.eventops;

import java.util.*;

import org.w3c.dom.*;
import javolution.util.FastList;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a Servlet request parameter values to a list
 */
public class RequestParametersToList extends MethodOperation {
    public static final class RequestParametersToListFactory implements Factory<RequestParametersToList> {
        public RequestParametersToList createMethodOperation(Element element, SimpleMethod simpleMethod) {
            return new RequestParametersToList(element, simpleMethod);
        }

        public String getName() {
            return "request-parameters-to-list";
        }
    }

    public static final String module = RequestParametersToList.class.getName();

    ContextAccessor<List<String>> listAcsr;
    String requestName;

    public RequestParametersToList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        requestName = element.getAttribute("request-name");
        listAcsr = new ContextAccessor<List<String>>(element.getAttribute("list-name"), requestName);
    }

    @Override
    public boolean exec(MethodContext methodContext) {
        List<String> listVal = null;
        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
            String[] parameterValues = methodContext.getRequest().getParameterValues(requestName);
            if (parameterValues == null) {
                Debug.logWarning("Request parameter values not found with name " + requestName, module);
            } else {
                listVal = UtilMisc.toListArray(parameterValues);
            }
        }

        // if listVal is null, use a empty list;
        if (listVal == null) {
            listVal = FastList.newInstance();
        }

        List<String> toList = listAcsr.get(methodContext);

        if (toList == null) {
            if (Debug.verboseOn()) Debug.logVerbose("List not found with name " + listAcsr + ", creating new list", module);
            toList = FastList.newInstance();
            listAcsr.put(methodContext, toList);
        }

        toList.addAll(listVal);
        return true;
    }

    @Override
    public String rawString() {
        return "<request-parameters-to-list request-name=\"" + this.requestName + "\" list-name=\"" + this.listAcsr + "\"/>";
    }
    @Override
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
