/*
 * $Id: GetRelated.java 5462 2005-08-05 18:35:48Z jonesde $
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
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.minilang.SimpleMethod;
import org.ofbiz.minilang.method.ContextAccessor;
import org.ofbiz.minilang.method.MethodContext;
import org.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Gets a list of related entity instance according to the specified relation-name
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @version    $Rev$
 * @since      2.0
 */
public class GetRelated extends MethodOperation {
    
    public static final String module = GetRelated.class.getName();
    
    ContextAccessor valueAcsr;
    ContextAccessor mapAcsr;
    ContextAccessor orderByListAcsr;
    String relationName;
    String useCacheStr;
    ContextAccessor listAcsr;

    public GetRelated(Element element, SimpleMethod simpleMethod) {
        super(element, simpleMethod);
        valueAcsr = new ContextAccessor(element.getAttribute("value-name"));
        relationName = element.getAttribute("relation-name");
        listAcsr = new ContextAccessor(element.getAttribute("list-name"));
        mapAcsr = new ContextAccessor(element.getAttribute("map-name"));
        orderByListAcsr = new ContextAccessor(element.getAttribute("order-by-list-name"));

        useCacheStr = element.getAttribute("use-cache");
    }

    public boolean exec(MethodContext methodContext) {
        String relationName = methodContext.expandString(this.relationName);
        String useCacheStr = methodContext.expandString(this.useCacheStr);
        boolean useCache = "true".equals(useCacheStr);
        
        List orderByNames = null;
        if (!orderByListAcsr.isEmpty()) {
            orderByNames = (List) orderByListAcsr.get(methodContext);
        }
        Map constraintMap = null;
        if (!mapAcsr.isEmpty()) {
            constraintMap = (Map) mapAcsr.get(methodContext);
        }

        GenericValue value = (GenericValue) valueAcsr.get(methodContext);
        if (value == null) {
            Debug.logWarning("Value not found with name: " + valueAcsr + ", not getting related...", module);
            return true;
        }
        try {
            if (useCache) {
                listAcsr.put(methodContext, value.getRelatedCache(relationName, constraintMap, orderByNames));
            } else {
                listAcsr.put(methodContext, value.getRelated(relationName, constraintMap, orderByNames));
            }
        } catch (GenericEntityException e) {
            String errMsg = "ERROR: Could not complete the " + simpleMethod.getShortDescription() + " process [problem getting related from entity with name " + value.getEntityName() + " for the relation-name: " + relationName + ": " + e.getMessage() + "]";
            Debug.logError(e, errMsg, module);
            methodContext.setErrorReturn(errMsg, simpleMethod);
            return false;
        }
        return true;
    }

    public String rawString() {
        // TODO: something more than the empty tag
        return "<get-related/>";
    }
    public String expandedString(MethodContext methodContext) {
        // TODO: something more than a stub/dummy
        return this.rawString();
    }
}
