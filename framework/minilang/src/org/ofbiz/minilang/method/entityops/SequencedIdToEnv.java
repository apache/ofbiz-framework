/*
 * $Id: SequencedIdToEnv.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
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
package org.ofbiz.minilang.method.entityops;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Gets a sequenced ID from the delegator and puts it in the env
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class SequencedIdToEnv extends MethodOperation {
    
    String seqName;
    ContextAccessor envAcsr;
    boolean getLongOnly;
    long staggerMax = 1;

    public SequencedIdToEnv(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        seqName = element.getAttribute("sequence-name");
        envAcsr = new ContextAccessor(element.getAttribute("env-name"));
        // default false, anything but true is false
        getLongOnly = "true".equals(element.getAttribute("get-long-only"));
        String staggerMaxStr = element.getAttribute("stagger-max");
        if (UtilValidate.isNotEmpty(staggerMaxStr)) {
            try {
                this.staggerMax = Long.parseLong(staggerMaxStr);
                if (this.staggerMax < 1) {
                    this.staggerMax = 1;
                }
            } catch (NumberFormatException e) {
                this.staggerMax = 1;
            }
        }
    }

    public boolean exec(MethodContext methodContext) {
        String seqName = methodContext.expandString(this.seqName);
        if (getLongOnly) {
            envAcsr.put(methodContext, methodContext.getDelegator().getNextSeqIdLong(seqName, staggerMax));
        } else {
            envAcsr.put(methodContext, methodContext.getDelegator().getNextSeqId(seqName, staggerMax));
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<sequenced-id-to-env/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
