package com.fidelissd.invoice

import org.ofbiz.base.util.UtilMisc
import org.ofbiz.entity.Delegator
import org.ofbiz.entity.GenericValue
import org.ofbiz.service.LocalDispatcher
import org.ofbiz.service.ServiceUtil

def invoiceId = parameters.invoiceId;


// Find Order related to this invoice
List<GenericValue> associatedOrderBillings = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("invoiceId", invoiceId), null, false);
if(associatedOrderBillings!=null && associatedOrderBillings.size()>0) {
    GenericValue entry = associatedOrderBillings.get(0);
    orderId = entry.getString("orderId");
    List<GenericValue> orderNotes = getOrderNotes(delegator, orderId);
    orderNotes.each { orderNote ->
        GenericValue noteGv = orderNote.getRelatedOne("NoteData", false);
        createInvoiceNote(dispatcher, userLogin, invoiceId, noteGv.getString("noteName"), noteGv.getString("noteInfo"), orderNote.getString("internalNote"), orderNote.getString("noteTypeId"));
    }
}

return ServiceUtil.returnSuccess();

private static List<GenericValue> getOrderNotes(Delegator delegator, String orderId) {
    List<GenericValue> orderNotes = delegator.findByAnd("OrderHeaderNote", UtilMisc.toMap("orderId", orderId), null, false);
    return orderNotes;
}

private static void createInvoiceNote(LocalDispatcher dispatcher, GenericValue userLogin,
                               String invoiceId, String noteName,
                               String noteInfo, String isInternal, String noteTypeId) {
    def fsdCreateInvoiceNoteResp = dispatcher.runSync("fsdCreateInvoiceNote", UtilMisc.toMap("invoiceId", invoiceId,
            "noteName", noteName,
            "noteInfo",noteInfo,
            "isInternal", isInternal,
            "noteTypeId", noteTypeId,
            "userLogin", userLogin));
    if(!ServiceUtil.isSuccess(fsdCreateInvoiceNoteResp)) {
        //Error creating invoice note
    }
}

