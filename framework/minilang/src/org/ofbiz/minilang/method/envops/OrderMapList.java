/*
 * $Id: OrderMapList.java 5462 2005-08-05 18:35:48Z jonesde $
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

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapComparator;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies an environment field to a list
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class OrderMapList extends MethodOperation {
    
    public static final String module = FieldToList.class.getName();
    
    protected ContextAccessor listAcsr;
    protected List orderByAcsrList = FastList.newInstance();
    protected MapComparator mc;

    public OrderMapList(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        
        List orderByElementList = UtilXml.childElementList(element, "order-by");
        Iterator orderByElementIter = orderByElementList.iterator();
        while (orderByElementIter.hasNext()) {
            Element orderByElement = (Element) orderByElementIter.next();
            this.orderByAcsrList.add(new FlexibleMapAccessor(orderByElement.getAttribute("field-name")));
        }

        this.mc = new MapComparator(this.orderByAcsrList);
    }

    public boolean exec(MethodContext methodContext) {
        Object fieldVal = null;

        List orderList = (List) listAcsr.get(methodContext);

        if (orderList == null) {
            if (Debug.infoOn()) Debug.logInfo("List not found with name " + listAcsr + ", not ordering/sorting list.", module);
            return true;
        }
        
        Collections.sort(orderList, mc);

        return true;
    }

    public String rawString() {
        return "<order-map-list list-name=\"" + this.listAcsr + "\"/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
