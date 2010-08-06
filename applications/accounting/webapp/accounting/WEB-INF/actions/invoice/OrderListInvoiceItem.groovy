/*
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
 */

import java.util.*;
import org.ofbiz.entity.*;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.util.EntityUtil;
import javolution.util.FastMap;

invoiceId = context.invoiceId;
if (!invoiceId) return;

List invoiceItems = [];
invoiceItemList = delegator.findByAnd("InvoiceItem", [invoiceId : invoiceId], ["invoiceItemSeqId"]);
if (invoiceItemList) {
    invoiceItemList.each { invoiceItem ->
        invoiceItemSeqId = invoiceItem.invoiceItemSeqId;
        invoiceId = invoiceItem.invoiceId;
        orderItemBilling = EntityUtil.getFirst(delegator.findByAnd("OrderItemBilling", [invoiceId : invoiceId, invoiceItemSeqId : invoiceItemSeqId]));
        Map invoiceItemMap = FastMap.newInstance();
        invoiceItemMap.putAll((Map) invoiceItem);
        if (orderItemBilling) {
            orderId = orderItemBilling.orderId;
            invoiceItemMap.orderId = orderId;
        } else {
            orderAdjustmentBilling = EntityUtil.getFirst(delegator.findByAnd("OrderAdjustmentBilling", [invoiceId : invoiceId, invoiceItemSeqId : invoiceItemSeqId]));
            if (orderAdjustmentBilling) {
                orderAdjustment = EntityUtil.getFirst(delegator.findByAnd("OrderAdjustment", [orderAdjustmentId : orderAdjustmentBilling.orderAdjustmentId]))
                if (orderAdjustment) {
                    orderId = orderAdjustment.orderId;
                    invoiceItemMap.orderId = orderId;
                }
            }
        }
        invoiceItems.add(invoiceItemMap);
    }
    context.invoiceItems = invoiceItems;
}
