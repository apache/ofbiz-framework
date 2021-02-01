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
package org.apache.ofbiz.accounting

import javax.servlet.http.HttpSession
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.order.shoppingcart.CheckOutEvents
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.security.SecurityFactory
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase
import org.apache.ofbiz.shipment.packing.PackingSession
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse


public class InvoicePerShipmentTests extends OFBizTestCase {
    public InvoicePerShipmentTests(String name) {
        super(name)
    }

    def testInvoicePerShipment(String productId, String invoicePerShipment) {
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()

        Security security = SecurityFactory.getInstance(delegator)
        request.setAttribute("security", security)
        request.setAttribute("delegator", delegator)
        request.setAttribute("dispatcher", dispatcher)
        HttpSession session = request.getSession()
        session.setAttribute("orderMode", null)

        String result = ShoppingCartEvents.routeOrderEntry(request, response)
        logInfo("===== >>> Event : routeOrderEntry, Response : " + result)

        request.setParameter("orderMode", "SALES_ORDER")
        request.setParameter("productStoreId", "9000")
        request.setParameter("partyId", "DemoCustomer")
        request.setParameter("currencyUom", "USD")
        session.setAttribute("userLogin", userLogin)

        result = ShoppingCartEvents.initializeOrderEntry(request, response)
        logInfo("===== >>> Event : initializeOrderEntry, Response : " + result)

        result = ShoppingCartEvents.setOrderCurrencyAgreementShipDates(request, response)
        logInfo("===== >>> Event : setOrderCurrencyAgreementShipDates, Response : " + result)

        request.setParameter("add_product_id", productId)

        result = ShoppingCartEvents.addToCart(request, response)
        logInfo("===== >>> Event : addToCart, Response : " + result)

        request.setParameter("checkoutpage", "quick")
        request.setParameter("shipping_contact_mech_id", "9015")
        request.setParameter("shipping_method", "GROUND@UPS")
        request.setParameter("checkOutPaymentId", "EXT_COD")
        request.setParameter("is_gift", "false")
        request.setParameter("may_split", "false")
        request.setAttribute("shoppingCart", null)

        result = CheckOutEvents.setQuickCheckOutOptions(request, response)
        logInfo("===== >>> Event : setQuickCheckOutOptions, Response : " + result)

        result = CheckOutEvents.createOrder(request, response)
        logInfo("===== >>> Event : createOrder, Response : " + result)

        result = CheckOutEvents.processPayment(request, response)
        logInfo("===== >>> Event : processPayment, Response : " + result)

        dispatcher.runAsync("sendOrderConfirmation", null)

        result = ShoppingCartEvents.destroyCart(request, response)
        logInfo("===== >>> Event : destroyCart, Response = " + result)

        // Step 3
        GenericValue orderHeader = from("OrderHeader").where("orderTypeId", "SALES_ORDER").orderBy("-entryDate").queryFirst()
        logInfo("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx : " + orderHeader)

        if (invoicePerShipment) {
            // if this value is available that means we need to set this on the order
            Map orderInput = [:]
            orderInput.orderId = orderHeader.orderId
            orderInput.invoicePerShipment = invoicePerShipment
            orderInput.userLogin = userLogin
            Map serviceResult = dispatcher.runSync("updateOrderHeader", orderInput)
            logInfo("===== >>> Service : updateOrderHeader / invoicePerShipment = N,  Response = " + serviceResult.responseMessage)
        }

        PackingSession packingSession = new PackingSession(dispatcher, userLogin)
        session.setAttribute("packingSession", packingSession)
        packingSession.setPrimaryOrderId(orderHeader.orderId)
        packingSession.setPrimaryShipGroupSeqId("00001")

        Map packInput = [:]
        packInput.orderId = orderHeader.orderId
        packInput.shipGroupSeqId = "00001"
        packInput.packingSession = packingSession
        packInput.nextPackageSeq = 1
        packInput.userLogin = userLogin

        // Items
        packInput.selInfo = [_1: "Y"]
        packInput.pkgInfo = [_1: "1"]
        packInput.qtyInfo = [_1: "1"]
        packInput.prdInfo = [_1: productId]
        packInput.iteInfo = [_1: "00001"]
        packInput.wgtInfo = [_1: "0"]
        packInput.numPackagesInfo = [_1: "1"]

        Map serviceResult = dispatcher.runSync("packBulkItems", packInput)
        assert ServiceUtil.isSuccess(serviceResult)
        logInfo("===== >>> Service: packBulkItems, Response = " + serviceResult.responseMessage)

        Map completePackInput = dispatcher.getDispatchContext().makeValidContext("completePack", ModelService.IN_PARAM, packInput)
        serviceResult = dispatcher.runSync("completePack", completePackInput)
        assert ServiceUtil.isSuccess(serviceResult)
        logInfo("===== >>> Service: completePack, shipmentId = " + serviceResult.shipmentId)

        // Step 4
        List invoices = from("OrderItemBillingAndInvoiceAndItem").where("orderId", orderHeader.orderId).queryList()
        return invoices
    }
    void testInvoicePerShipmentSetFalse() {
        /* Test Invoice Per Shipment
            Step 1) Set create.invoice.per.shipment=N in accounting.properties file.
            Step 2) Create order and approve order.
            Step 3) Pack Shipment For Ship Group.
            Step 4) Check invoice should not created.
        */
        UtilProperties.setPropertyValueInMemory("accounting", "create.invoice.per.shipment", "N")
        logInfo("===== >>> Set Accounting.properties / create.invoice.per.shipment = N")

        List invoices = testInvoicePerShipment("GZ-1000", null)
        assert UtilValidate.isEmpty(invoices)
    }

    void testInvoicePerShipmentSetTrue() {
        /* Test Invoice Per Shipment
             Step 1) Set create.invoice.per.shipment=Y in accounting.properties file.
             Step 2) Create order and approve order.
             Step 3) Pack Shipment For Ship Group.
             Step 4) Check invoice should be created.
         */
        UtilProperties.setPropertyValueInMemory("accounting", "create.invoice.per.shipment", "Y")
        logInfo("===== >>> Set Accounting.properties / create.invoice.per.shipment = Y")

        List invoices = testInvoicePerShipment("GZ-1000", null)
        assert UtilValidate.isNotEmpty(invoices)
    }

    void testInvoicePerShipmentSetOrderFalse() {
        /* Test Invoice Per Shipment
            Step 1) Create order and set invoicePerShipment=N.
            Step 2) Pack Shipment For Ship Group.
            Step 3) Check invoice should not be created.
        */
        List invoices = testInvoicePerShipment("GZ-2644", "N")
        assert UtilValidate.isEmpty(invoices)
    }

    void testInvoicePerShipmentSetOrderTrue() {
        /* Test Invoice Per Shipment
            Step 1) Create order and set invoicePerShipment=Y
            Step 2) Pack Shipment For Ship Group.
            Step 3) Check invoice should be created.
        */
        List invoices = testInvoicePerShipment("GZ-2644", "Y")
        assert UtilValidate.isNotEmpty(invoices)
    }
}