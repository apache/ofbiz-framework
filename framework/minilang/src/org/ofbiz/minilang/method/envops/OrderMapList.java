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

import javolution.util.FastList;

import org.w3c.dom.*;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.collections.MapComparator;
import org.ofbiz.minilang.*;
import org.ofbiz.minilang.method.*;

/**
 * Copies an environment field to a list
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
