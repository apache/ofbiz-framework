/*
 * $Id: FirstFromList.java 5462 2005-08-05 18:35:48Z jonesde $
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
package org.ofbiz.minilang.method.envops;

import java.util.*;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Get the first entry from the list
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FirstFromList extends MethodOperation {
    
    public static final String module = FirstFromList.class.getName();

    ContextAccessor entryAcsr;
    ContextAccessor listAcsr;

    public FirstFromList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.entryAcsr = new ContextAccessor(element.getAttribute("entry-name"));
        this.listAcsr = new ContextAccessor(element.getAttribute("list-name"));
    }

    public boolean exec(MethodContext methodContext) {
        Object fieldVal = null;

        if (listAcsr.isEmpty()) {
            Debug.logWarning("No list-name specified in iterate tag, doing nothing", module);
            return true;
        }

        List theList = (List) listAcsr.get(methodContext);

        if (theList == null) {
            if (Debug.infoOn()) Debug.logInfo("List not found with name " + listAcsr + ", doing nothing", module);
            return true;
        }
        if (theList.size() == 0) {
            if (Debug.verboseOn()) Debug.logVerbose("List with name " + listAcsr + " has zero entries, doing nothing", module);
            return true;
        }

        entryAcsr.put(methodContext, theList.get(0));
        return true;
    }

    public String rawString() {
        return "<first-from-list list-name=\"" + this.listAcsr + "\" entry-name=\"" + this.entryAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
