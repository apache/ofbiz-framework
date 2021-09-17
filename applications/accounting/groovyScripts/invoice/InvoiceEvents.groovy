package invoice
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

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil

//Create a new Invoice Item with Payrol Item Type
def createInvoiceItemPayrol() {
    List<GenericValue> payRolList = from("InvoiceItemType").queryList()
    from("InvoiceItemType")
            .where("parentTypeId", "PAYROL")
            .queryList()
            .each { payRolGroup ->
                payRolList.each { payRol ->
                    if (payRol.parentTypeId == payRolGroup.invoiceItemTypeId) {
                        Map createInvoiceItem = [invoiceId        : parameters.invoiceId,
                                                 invoiceItemTypeId: payRol.invoiceItemTypeId,
                                                 description      : "${payRolGroup.description}: ${payRol.description}",
                                                 quantity         : parameters."${payRol.invoiceItemTypeId}_Quantity" ?: 1d,
                                                 amount           : parameters."${payRol.invoiceItemTypeId}_Amount" ?: 0d]

                        if (parameters."${payRol.invoiceItemTypeId}_Quantity" ||
                                parameters."${payRol.invoiceItemTypeId}_Amount") {
                            if ("PAYROL_EARN_HOURS" != payRolGroup.invoiceItemTypeId) {
                                createInvoiceItem.amount = createInvoiceItem.amount.negate()
                            }
                            Map serviceResult = run service: 'createInvoiceItem', with: createInvoiceItem
                            if (ServiceUtil.isError(serviceResult)) {
                                return serviceResult
                            }
                        }
                    }
                }
            }

    return  success()
}

