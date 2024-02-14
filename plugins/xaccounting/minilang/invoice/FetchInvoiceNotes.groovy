package com.fidelissd.invoice

import org.apache.commons.lang.StringEscapeUtils
import org.ofbiz.base.util.UtilValidate
import org.ofbiz.entity.GenericValue

String invoiceId = parameters.invoiceId;

notes = from("InvoiceNote").where("invoiceId", invoiceId).orderBy("createdStamp").queryList();
context.invoiceNotes = notes;

//General Notes
List<GenericValue> generalNotesRaw = from("InvoiceNoteView").where("invoiceId", invoiceId, "isInternal", "N", "noteTypeId", "GENERAL_NOTE").orderBy("-noteDateTime").queryList()
//escape any HTML characters like < / & to &lt; / &amp;
List generalNotes = []
generalNotesRaw.each { generalNoteRaw ->
    Map noteWrapper = [:]
    noteWrapper.putAll(generalNoteRaw)
    String rawNoteInfo = generalNoteRaw.noteInfo
    String htmlEscapedNoteInfo = StringEscapeUtils.escapeXml(rawNoteInfo)
    noteWrapper.put("noteInfo", htmlEscapedNoteInfo)

    generalNotes.add(noteWrapper)
}
context.generalNotes = generalNotes

//Customer Notes
List<GenericValue> customerNotesRaw = from("InvoiceNoteView").where("invoiceId", invoiceId, "isInternal", "N", "noteTypeId", "CUSTOMER_NOTE").orderBy("-noteDateTime").queryList()
//escape any HTML characters like < / & to &lt; / &amp;
List customerNotes = []
customerNotesRaw.each { customerNoteRaw ->
    Map noteWrapper = [:]
    noteWrapper.putAll(customerNoteRaw)
    String rawNoteInfo = customerNoteRaw.noteInfo
    String htmlEscapedNoteInfo = StringEscapeUtils.escapeXml(rawNoteInfo)
    noteWrapper.put("noteInfo", htmlEscapedNoteInfo)

    customerNotes.add(noteWrapper)
}
context.customerNotes = customerNotes


//Supplier Notes
List<GenericValue> supplierNotesRaw = from("InvoiceNoteView").where("invoiceId", invoiceId, "isInternal", "N", "noteTypeId", "SUPPLIER_NOTE").orderBy("-noteDateTime").queryList()
//escape any HTML characters like < / & to &lt; / &amp;
List supplierNotes = []
supplierNotesRaw.each { supplierNoteRaw ->
    Map noteWrapper = [:]
    noteWrapper.putAll(supplierNoteRaw)
    String rawNoteInfo = supplierNoteRaw.noteInfo
    String htmlEscapedNoteInfo = StringEscapeUtils.escapeXml(rawNoteInfo)
    noteWrapper.put("noteInfo", htmlEscapedNoteInfo)

    supplierNotes.add(noteWrapper)
}
context.supplierNotes = supplierNotes
