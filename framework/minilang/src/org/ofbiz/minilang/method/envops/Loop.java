/*
 * $Id$
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.minilang.method.envops;

import java.util.List;
import java.util.LinkedList;

import org.w3c.dom.Element;

import org.ofbiz.minilang.method.MethodOperation;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.base.util.Debug;

/**
 * Loop
 *
 * @author      <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version     $Rev$
 * @since       3.5
 */
public class Loop extends MethodOperation {

    public static final String module = Loop.class.getName();
    protected List subOps = new LinkedList();
    protected ContextAccessor fieldAcsr;
    protected String countStr;


    public Loop(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        this.fieldAcsr = new ContextAccessor(element.getAttribute("field"));
        this.countStr = element.getAttribute("count");

        SimpleMethod.readOperations(element, subOps, simpleMethod);
    }

    public boolean exec(MethodContext methodContext) {
        String countStrExp = methodContext.expandString(this.countStr);
        int count = 0;
        try {
            Double ctDbl = new Double(countStrExp);
            if (ctDbl != null) {
                count = ctDbl.intValue();    
            }
        } catch (NumberFormatException e) {
            Debug.logError(e, module);
            return false;
        }

        if (count < 1) {
            Debug.logWarning("Count is less than one, not doing nothing: " + rawString(), module);
            return false;
        }

        for (int i = 0; i < count; i++) {
            fieldAcsr.put(methodContext, new Integer(i));
            if (!SimpleMethod.runSubOps(subOps, methodContext)) {
                // only return here if it returns false, otherwise just carry on
                return false;
            }
        }

        return true;
    }

    public String rawString() {
        return "<loop count=\"" + this.countStr + "\"/>";
    }

    public String expandedString(MethodContext methodContext) {
        return this.rawString();
    }
}
