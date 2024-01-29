package com.simbaquartz.xapi.connect.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;


/**
 * Represents a Invoice object.
 **/

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaResteasyServerCodegen", date = "2018-05-18T14:37:04.465+05:30")
@Data
public class Invoice  {

    @JsonProperty("id")
    private String id = null;
    @JsonProperty("invoiceTypeId")
    private String invoiceTypeId = null;
    @JsonProperty("partyIdFrom")
    private String partyIdFrom = null;
    @JsonProperty("partyId")
    private String partyId = null;
    @JsonProperty("roleTypeId")
    private String roleTypeId = null;
    @JsonProperty("statusId")
    private String statusId = null;
    @JsonProperty("billingAccountId")
    private String billingAccountId = null;
    @JsonProperty("contactMechId")
    private String contactMechId = null;
    @JsonProperty("invoiceDate")
    private Timestamp invoiceDate = null;
    @JsonProperty("dueDate")
    private Timestamp dueDate = null;
    @JsonProperty("paidDate")
    private Timestamp paidDate = null;
    @JsonProperty("invoiceMessage")
    private String invoiceMessage = null;
    @JsonProperty("referenceNumber")
    private String referenceNumber = null;
    @JsonProperty("description")
    private String description = null;
    @JsonProperty("currencyUomId")
    private String currencyUomId = null;
    @JsonProperty("recurrenceInfoId")
    private String recurrenceInfoId = null;
    @JsonProperty("invoiceStatus")
    private String invoiceStatus = null;

    @JsonProperty("cmInvoiceNumber")
    private String cmInvoiceNumber = null;

    @JsonProperty("purchaseObligatonNumber")
    private String purchaseObligatonNumber = null;

    @JsonProperty("invoiceNumber")
    private String invoiceNumber = null;

    @JsonProperty("contractReferenceNumber")
    private String contractReferenceNumber = null;

    @JsonProperty("shippingDestination")
    private String shippingDestination = null;

    @JsonProperty("invoiceItem")
    private List<InvoiceItem> invoiceItem = null;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Invoice invoice = (Invoice) o;
        return Objects.equals(id, invoice.id) &&
                Objects.equals(invoiceTypeId , invoice.invoiceTypeId) &&
                Objects.equals(partyIdFrom, invoice.partyIdFrom) &&
                Objects.equals(partyId, invoice.partyId) &&
                Objects.equals(roleTypeId, invoice.roleTypeId) &&
                Objects.equals(statusId, invoice.statusId) &&
                Objects.equals(billingAccountId, invoice.billingAccountId) &&
                Objects.equals(contactMechId, invoice.contactMechId) &&
                Objects.equals(invoiceDate, invoice.invoiceDate) &&
                Objects.equals(dueDate, invoice.dueDate) &&
                Objects.equals(paidDate, invoice.paidDate) &&
                Objects.equals(invoiceMessage, invoice.invoiceMessage) &&
                Objects.equals(referenceNumber, invoice.referenceNumber) &&
                Objects.equals(description, invoice.description) &&
                Objects.equals(currencyUomId, invoice.currencyUomId) &&
                Objects.equals(recurrenceInfoId, invoice.recurrenceInfoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, invoiceTypeId, partyIdFrom, partyId, roleTypeId, statusId, billingAccountId, contactMechId, invoiceDate, dueDate, paidDate, invoiceMessage, referenceNumber, description, currencyUomId, recurrenceInfoId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Task {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    invoiceTypeId: ").append(toIndentedString(invoiceTypeId)).append("\n");
        sb.append("    partyIdFrom: ").append(toIndentedString(partyIdFrom)).append("\n");
        sb.append("    partyId: ").append(toIndentedString(partyId)).append("\n");
        sb.append("    roleTypeId: ").append(toIndentedString(roleTypeId)).append("\n");
        sb.append("    statusId: ").append(toIndentedString(statusId)).append("\n");
        sb.append("    billingAccountId: ").append(toIndentedString(billingAccountId)).append("\n");
        sb.append("    contactMechId: ").append(toIndentedString(contactMechId)).append("\n");
        sb.append("    invoiceDate: ").append(toIndentedString(invoiceDate)).append("\n");
        sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
        sb.append("    paidDate: ").append(toIndentedString(paidDate)).append("\n");
        sb.append("    invoiceMessage: ").append(toIndentedString(invoiceMessage)).append("\n");
        sb.append("    referenceNumber: ").append(toIndentedString(referenceNumber)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    currencyUomId: ").append(toIndentedString(currencyUomId)).append("\n");
        sb.append("    recurrenceInfoId: ").append(toIndentedString(recurrenceInfoId)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}

