/*
 * $Id: RequestParametersToList.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001, 2002 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.minilang.method.eventops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies a Servlet request parameter values to a list
 *
 * @author     <a href="mailto:quake.sh@ofbizchina.com">Quake Wang</a>
 * @version    $Rev$
 * @since      3.0
 */
public class RequestParametersToList extends MethodOperation {

    public static final String module = RequestParametersToList.class.getName();

	ContextAccessor listAcsr;
	String requestName;

    public RequestParametersToList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        requestName = element.getAttribute("request-name");
		listAcsr = new ContextAccessor(element.getAttribute("list-name"), requestName);
    }

    public boolean exec(MethodContext methodContext) {
        Object listVal = null;
        // only run this if it is in an EVENT context
        if (methodContext.getMethodType() == MethodContext.EVENT) {
			listVal = methodContext.getRequest().getParameterValues(requestName);
            if (listVal == null) {
                Debug.logWarning("Request parameter values not found with name " + requestName, module);
            }
        }

        // if listVal is null, use a empty list;
        if (listVal == null) {
			listVal = new ArrayList();
        } else if (listVal instanceof String[]) {
			listVal = UtilMisc.toListArray((String[]) listVal);
        }

		List toList = (List) listAcsr.get(methodContext);

		if (toList == null) {
			if (Debug.verboseOn()) Debug.logVerbose("List not found with name " + listAcsr + ", creating new list", module);
			toList = new ArrayList();
			listAcsr.put(methodContext, toList);
		}

		toList.addAll((Collection) listVal);
        return true;
    }

    public String rawString() {
        return "<request-parameters-to-list request-name=\"" + this.requestName + "\" list-name=\"" + this.listAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
