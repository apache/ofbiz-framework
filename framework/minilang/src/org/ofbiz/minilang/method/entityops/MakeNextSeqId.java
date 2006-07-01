/*
 * $Id: MakeNextSeqId.java 5462 2005-08-05 18:35:48Z jonesde $
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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Look at existing values for a sub-entity with a sequenced secondary ID, and get the highest plus 1
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class MakeNextSeqId extends MethodOperation {

    public static final String module = MakeNextSeqId.class.getName();

    String seqFieldName;
    ContextAccessor valueAcsr;
    String numericPaddingStr;
    String incrementByStr;

    public MakeNextSeqId(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        seqFieldName = element.getAttribute("seq-field-name");
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));

        numericPaddingStr = element.getAttribute("numeric-padding");
        incrementByStr = element.getAttribute("increment-by");
    }

    public boolean exec(MethodContext methodContext) {
        String seqFieldName = methodContext.expandString(this.seqFieldName);
        String numericPaddingStr = methodContext.expandString(this.numericPaddingStr);
        String incrementByStr = methodContext.expandString(this.incrementByStr);
        int numericPadding = 5;
        int incrementBy = 1;
        try {
            if (UtilValidate.isNotEmpty(numericPaddingStr)) {
                numericPadding = Integer.parseInt(numericPaddingStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "numeric-padding format invalid for [" + numericPaddingStr + "]", module);
        }
        try {
            if (UtilValidate.isNotEmpty(incrementByStr)) {
                incrementBy = Integer.parseInt(incrementByStr);
            }
        } catch (Exception e) {
            Debug.logError(e, "increment-by format invalid for [" + incrementByStr + "]", module);
        }

        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        methodContext.getDelegator().setNextSubSeqId(value, seqFieldName, numericPadding, incrementBy);
        
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<make-next-seq-id/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
