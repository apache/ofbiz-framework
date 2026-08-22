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
package org.apache.ofbiz.accounting.accounting

import jakarta.servlet.http.HttpSession

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.order.shoppingcart.CheckOutEvents
import org.apache.ofbiz.order.shoppingcart.ShoppingCartEvents
import org.apache.ofbiz.security.Security
import org.apache.ofbiz.security.SecurityFactory
import org.apache.ofbiz.service.ModelService
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.apache.ofbiz.shipment.packing.PackingSession
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

@JunitJupiterTest
class InvoicePerShipmentTests implements JupiterTestHelper {

    @Test
    @Order(1)
    void testInvoicePerShipmentSetFalse() {
        /* Test Invoice Per Shipment
         Step 1) Set create.invoice.per.shipment=N in accounting.properties file.
         Step 2) Create order and approve order.
         Step 3) Pack Shipment For Ship Group.
         Step 4) Check invoice should not created.
         */
        String productId = testParams.productId ?: 'GZ-1000'
        String invoicePerShipment = testParams.invoicePerShipment ?: 'N'
        List invoices = testInvoicePerShipment(productId, invoicePerShipment)
        assert !invoices
    }

    @Test
    @Order(2)
    void testInvoicePerShipmentSetTrue() {
        /* Test Invoice Per Shipment
         Step 1) Set create.invoice.per.shipment=Y in accounting.properties file.
         Step 2) Create order and approve order.
         Step 3) Pack Shipment For Ship Group.
         Step 4) Check invoice should be created.
         */
        String productId = testParams.productId ?: 'GZ-1000'
        String invoicePerShipment = testParams.invoicePerShipment ?: 'Y'
        List invoices = testInvoicePerShipment(productId, invoicePerShipment)
        assert invoices
    }

    @Test
    @Order(3)
    void testInvoicePerShipmentSetOrderFalse() {
        /* Test Invoice Per Shipment
         Step 1) Create order and set invoicePerShipment=N.
         Step 2) Pack Shipment For Ship Group.
         Step 3) Check invoice should not be created.
         */
        String productId = testParams.productId ?: 'GZ-2644'
        String invoicePerShipment = testParams.invoicePerShipment ?: 'N'
        List invoices = testInvoicePerShipment(productId, invoicePerShipment)
        assert !invoices
    }

    @Test
    @Order(4)
    void testInvoicePerShipmentSetOrderTrue() {
        /* Test Invoice Per Shipment
         Step 1) Create order and set invoicePerShipment=Y
         Step 2) Pack Shipment For Ship Group.
         Step 3) Check invoice should be created.
         */
        String productId = testParams.productId ?: 'GZ-2644'
        String invoicePerShipment = testParams.invoicePerShipment ?: 'Y'
        List invoices = testInvoicePerShipment(productId, invoicePerShipment)
        assert invoices
    }

    private List testInvoicePerShipment(String productId, String invoicePerShipment) {
        MockHttpServletRequest request = new MockHttpServletRequest()
        MockHttpServletResponse response = new MockHttpServletResponse()

        Security security = SecurityFactory.getInstance(delegator)
        request.setAttribute('security', security)
        request.setAttribute('delegator', delegator)
        request.setAttribute('dispatcher', dispatcher)
        HttpSession session = request.getSession()
        session.setAttribute('orderMode', null)

        String result = ShoppingCartEvents.routeOrderEntry(request, response)
        logInfo('===== >>> Event : routeOrderEntry, Response : ' + result)

        request.setParameter('orderMode', testParams.orderMode ?: 'SALES_ORDER')
        request.setParameter('productStoreId', testParams.productStoreId ?: '9000')
        request.setParameter('partyId', testParams.partyId ?: 'DemoCustomer')
        request.setParameter('currencyUom', testParams.currencyUomId ?: 'USD')
        session.setAttribute('userLogin', userLogin)

        result = ShoppingCartEvents.initializeOrderEntry(request, response)
        logInfo('===== >>> Event : initializeOrderEntry, Response : ' + result)

        result = ShoppingCartEvents.setOrderCurrencyAgreementShipDates(request, response)
        logInfo('===== >>> Event : setOrderCurrencyAgreementShipDates, Response : ' + result)

        request.setParameter('add_product_id', productId)

        result = ShoppingCartEvents.addToCart(request, response)
        logInfo('===== >>> Event : addToCart, Response : ' + result)

        request.setParameter('checkoutpage', testParams.checkoutpage ?: 'quick')
        request.setParameter('shipping_contact_mech_id', testParams.shipping_contact_mech_id ?: '9015')
        request.setParameter('shipping_method', testParams.shipping_method ?: 'GROUND@UPS')
        request.setParameter('checkOutPaymentId', testParams.checkOutPaymentId ?: 'EXT_COD')
        request.setParameter('is_gift', testParams.is_gift ?: 'false')
        request.setParameter('may_split', testParams.may_split ?: 'false')
        request.setAttribute('shoppingCart', null)

        result = CheckOutEvents.setQuickCheckOutOptions(request, response)
        logInfo('===== >>> Event : setQuickCheckOutOptions, Response : ' + result)

        result = CheckOutEvents.createOrder(request, response)
        logInfo('===== >>> Event : createOrder, Response : ' + result)

        result = CheckOutEvents.processPayment(request, response)
        logInfo('===== >>> Event : processPayment, Response : ' + result)

        dispatcher.runAsync('sendOrderConfirmation', null)

        result = ShoppingCartEvents.destroyCart(request, response)
        logInfo('===== >>> Event : destroyCart, Response = ' + result)

        // Step 3
        GenericValue orderHeader = from('OrderHeader').where('orderTypeId', 'SALES_ORDER').orderBy('-entryDate').queryFirst()
        logInfo('===== >>> orderHeader : ' + orderHeader)

        if (invoicePerShipment) {
            // if this value is available that means we need to set this on the order
            Map orderInput = [:]
            orderInput.orderId = orderHeader.orderId
            orderInput.invoicePerShipment = invoicePerShipment
            orderInput.userLogin = userLogin
            Map serviceResult = dispatcher.runSync('updateOrderHeader', orderInput)
            logInfo('===== >>> Service : updateOrderHeader / invoicePerShipment = N,  Response = ' + serviceResult.responseMessage)
        }

        PackingSession packingSession = new PackingSession(dispatcher, userLogin)
        session.setAttribute('packingSession', packingSession)
        packingSession.setPrimaryOrderId(orderHeader.orderId)
        String shipGroupSeqId = testParams.shipGroupSeqId ?: '00001'
        packingSession.setPrimaryShipGroupSeqId(shipGroupSeqId)

        Map packInput = [
                orderId: orderHeader.orderId,
                shipGroupSeqId: shipGroupSeqId,
                packingSession: packingSession,
                nextPackageSeq: 1,
                userLogin: userLogin,
                selInfo: [_1: testParams.selInfo ?: 'Y'],
                pkgInfo: [_1: testParams.pkgInfo ?: '1'],
                qtyInfo: [_1: testParams.qtyInfo ?: '1'],
                prdInfo: [_1: productId],
                iteInfo: [_1: testParams.iteInfo ?: '00001'],
                wgtInfo: [_1: testParams.wgtInfo ?: '0'],
                numPackagesInfo: [_1: testParams.numPackagesInfo ?: '1']
        ]

        Map serviceResult = dispatcher.runSync('packBulkItems', packInput)
        assert ServiceUtil.isSuccess(serviceResult)
        logInfo('===== >>> Service: packBulkItems, Response = ' + serviceResult.responseMessage)

        Map completePackInput = dispatcher.getDispatchContext().makeValidContext('completePack', ModelService.IN_PARAM, packInput)
        serviceResult = dispatcher.runSync('completePack', completePackInput)
        assert ServiceUtil.isSuccess(serviceResult)
        logInfo('===== >>> Service: completePack, shipmentId = ' + serviceResult.shipmentId)

        // Step 4
        List invoices = from('OrderItemBillingAndInvoiceAndItem').where('orderId', orderHeader.orderId).queryList()
        return invoices
    }

}
