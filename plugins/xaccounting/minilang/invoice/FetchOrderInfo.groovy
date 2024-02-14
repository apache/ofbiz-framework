package com.fidelissd.invoice

import com.fidelissd.fsdOrderManager.orderentity.role.OrderEntityRole
import com.fidelissd.fsdOrderManager.orderentity.role.OrderEntityRoleHelper
import com.fidelissd.fsdOrderManager.orderentity.builders.InvoiceBuilder
import com.fidelissd.hierarchy.orderentity.OrderEntityType
import org.ofbiz.base.util.UtilMisc
import org.ofbiz.base.util.UtilValidate
import org.ofbiz.entity.GenericValue
import org.ofbiz.entity.condition.EntityCondition
import org.ofbiz.entity.condition.EntityOperator
import org.ofbiz.entity.util.EntityUtil
import com.fidelissd.sellercentral.util.common.CommonMethods

def invoiceId = parameters.invoiceId;
GenericValue order = InvoiceBuilder.invoice(delegator, invoiceId).order().build();
context.order = order;

// Get Supplier_POC from order
List<OrderEntityRole> orderSupplierPocRoles = []
if(order) {
    String orderIdToUse = order.orderId;
    if(order.orderTypeId == "PURCHASE_ORDER") {
        // Get the related Sales-Order Id
        List<GenericValue> orderItemAssocs = delegator.findByAnd("OrderItemAssoc", UtilMisc.toMap("toOrderId", order.orderId, "orderItemAssocTypeId", "DROP_SHIPMENT"), null, false);
        if(orderItemAssocs && orderItemAssocs .size()>0){
            GenericValue orderItemAssocGv = orderItemAssocs.first();
            orderIdToUse = orderItemAssocGv.orderId;
        }
    }

    GenericValue quote = InvoiceBuilder.invoice(delegator, invoiceId).order().quote().build()
    //get supplier roles sequence wise
    EntityCondition quoteRoleCond = EntityCondition.makeCondition([
            EntityCondition.makeCondition("roleTypeId", "SUPPLIER_POC"),
            EntityCondition.makeCondition("quoteId", EntityOperator.EQUALS, quote.quoteId)
    ], EntityOperator.AND)
    List<GenericValue> quoteOtherRoles = EntityUtil.filterByDate(delegator.findList("QuoteRole", quoteRoleCond, null, ["sequenceNum"], null, false));

    quoteOtherRoles.each {quoteOtherRole ->
        OrderEntityRole orderEntityRole = new OrderEntityRole(quoteOtherRole)
        if(UtilValidate.isNotEmpty(orderEntityRole.getPhoneNumberMap())) {
            CommonMethods.getPhoneInFormat(orderEntityRole)
        }
        orderSupplierPocRoles.add(orderEntityRole)
    }
}

context.orderSupplierPocRoles = orderSupplierPocRoles;
