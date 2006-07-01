/*
 * $Id: FilterListByDate.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.util.List;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Uses the delegator to find entity values by anding the map fields
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class FilterListByDate extends MethodOperation {
    
    ContextAccessor listAcsr;
    ContextAccessor toListAcsr;
    ContextAccessor validDateAcsr;
    String fromFieldName;
    String thruFieldName;
    String allSameStr;

    public FilterListByDate(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        toListAcsr = new ContextAccessor(element.getAttribute("to-list-name"), element.getAttribute("list-name"));
        validDateAcsr = new ContextAccessor(element.getAttribute("valid-date-name"));

        fromFieldName = element.getAttribute("from-field-name");
        if (UtilValidate.isEmpty(fromFieldName)) fromFieldName = "fromDate";
        thruFieldName = element.getAttribute("thru-field-name");
        if (UtilValidate.isEmpty(thruFieldName)) thruFieldName = "thruDate";

        allSameStr = element.getAttribute("all-same");
    }

    public boolean exec(MethodContext methodContext) {
        String fromFieldName = methodContext.expandString(this.fromFieldName);
        String thruFieldName = methodContext.expandString(this.thruFieldName);
        String allSameStr = methodContext.expandString(this.allSameStr);
        
        boolean allSame = !"false".equals(allSameStr);
        
        if (!validDateAcsr.isEmpty()) {
            toListAcsr.put(methodContext, EntityUtil.filterByDate((List) listAcsr.get(methodContext), (java.sql.Timestamp) validDateAcsr.get(methodContext), fromFieldName, thruFieldName, true));
        } else {
            toListAcsr.put(methodContext, EntityUtil.filterByDate((List) listAcsr.get(methodContext), UtilDateTime.nowTimestamp(), fromFieldName, thruFieldName, true));
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<filter-list-by-date/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}

