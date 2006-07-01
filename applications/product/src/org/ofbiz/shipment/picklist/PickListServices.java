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
package org.ofbiz.shipment.picklist;

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;

/**
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      3.5
 */
public class PickListServices {

    public static final String module = PickListServices.class.getName();

    public static Map convertOrderIdListToHeaders(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

        List orderHeaderList = (List) context.get("orderHeaderList");
        List orderIdList = (List) context.get("orderIdList");

        // we don't want to process if there is already a header list
        if (orderHeaderList == null) {
            // convert the ID list to headers
            if (orderIdList != null) {
                List conditionList1 = new ArrayList();
                List conditionList2 = new ArrayList();

                // we are only concerned about approved sales orders
                conditionList2.add(new EntityExpr("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                conditionList2.add(new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));

                // build the expression list from the IDs
                Iterator i = orderIdList.iterator();
                while (i.hasNext()) {
                    String orderId = (String) i.next();
                    conditionList1.add(new EntityExpr("orderId", EntityOperator.EQUALS, orderId));
                }

                // create the conditions
                EntityCondition idCond = new EntityConditionList(conditionList1, EntityOperator.OR);
                conditionList2.add(idCond);

                EntityCondition cond = new EntityConditionList(conditionList2, EntityOperator.AND);

                // run the query
                try {
                    orderHeaderList = delegator.findByCondition("OrderHeader", cond, null, UtilMisc.toList("+orderDate"));
                } catch (GenericEntityException e) {
                    Debug.logError(e, module);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.log("Recieved orderIdList  - " + orderIdList, module);
                Debug.log("Found orderHeaderList - " + orderHeaderList, module);
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("orderHeaderList", orderHeaderList);
        return result;
    }
}
