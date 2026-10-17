<#--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
<#escape x as x?xml>
    <#-- list of orders -->
    <#if orders?has_content>
    <fo:table table-layout="fixed" width="100%">
        <fo:table-column column-width="1in"/>
        <fo:table-column column-width="5.5in"/>

        <fo:table-body>
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-weight="bold">${uiLabelMap.AccountingOrderNr}:</fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block font-size ="10pt" font-weight="bold"><#list orders as order>${order} </#list></fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
    </fo:table>
    </#if>

    <#-- list of terms -->
    <#if terms?has_content>
    <fo:table table-layout="fixed" width="100%" space-before="0.1in">
        <fo:table-column column-width="6.5in"/>

        <fo:table-header height="14px">
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-weight="bold">${uiLabelMap.AccountingAgreementItemTerms}</fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-header>

        <fo:table-body>
          <#list terms as term>
          <#assign termType = term.getRelatedOne("TermType", false)/>
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-size ="10pt" font-weight="bold">${termType.description!} ${term.description!} ${term.termDays!} ${term.textValue!}</fo:block>
            </fo:table-cell>
          </fo:table-row>
          </#list>
        </fo:table-body>
    </fo:table>
    </#if>

    <fo:table table-layout="fixed" width="100%" space-before="0.2in">
    <fo:table-column column-width="20mm"/>
    <fo:table-column column-width="85mm"/>
    <fo:table-column column-width="15mm"/>
    <fo:table-column column-width="25mm"/>
    <fo:table-column column-width="25mm"/>

    <fo:table-header height="14px">
      <fo:table-row border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
        <fo:table-cell>
          <fo:block font-weight="bold">${uiLabelMap.AccountingProduct}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block font-weight="bold">${uiLabelMap.CommonDescription}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonQty}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block font-weight="bold" text-align="right">${uiLabelMap.AccountingUnitPrice}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block font-weight="bold" text-align="right">${uiLabelMap.CommonAmount}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>


    <fo:table-body font-size="10pt">
        <#assign currentShipmentId = "">
        <#assign newShipmentId = "">
        <#-- if the item has a description, then use its description.  Otherwise, use the description of the invoiceItemType -->
        <#list invoiceItems as invoiceItem>
            <#assign itemType = invoiceItem.getRelatedOne("InvoiceItemType", false)>
            <#assign isItemAdjustment = Static["org.apache.ofbiz.entity.util.EntityTypeUtil"].hasParentType(delegator, "InvoiceItemType", "invoiceItemTypeId", itemType.getString("invoiceItemTypeId"), "parentTypeId", "INVOICE_ADJ")/>

            <#assign taxRate = invoiceItem.getRelatedOne("TaxAuthorityRateProduct", false)!>
            <#assign itemBillings = invoiceItem.getRelated("OrderItemBilling", null, null, false)!>
            <#if itemBillings?has_content>
                <#assign itemBilling = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(itemBillings)>
                <#if itemBilling?has_content>
                    <#assign itemIssuance = itemBilling.getRelatedOne("ItemIssuance", false)!>
                    <#if itemIssuance?has_content>
                        <#assign newShipmentId = itemIssuance.shipmentId>
                        <#assign issuedDateTime = itemIssuance.issuedDateTime/>
                    </#if>
                </#if>
            </#if>
            <#assign description = Static["org.apache.ofbiz.accounting.invoice.InvoiceWorker"].getInvoiceItemDescription(dispatcher, invoiceItem, locale)>

            <#if newShipmentId?? & newShipmentId != currentShipmentId>
                <#-- the shipment id is printed at the beginning for each
                     group of invoice items created for the same shipment
                -->
                <fo:table-row height="14px">
                    <fo:table-cell number-columns-spanned="5">
                            <fo:block></fo:block>
                       </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px">
                   <fo:table-cell number-columns-spanned="5">
                        <fo:block font-weight="bold"> ${uiLabelMap.ProductShipmentId}: ${newShipmentId}<#if issuedDateTime??> ${uiLabelMap.CommonDate}: ${Static["org.apache.ofbiz.base.util.UtilDateTime"].toDateString(issuedDateTime)}</#if></fo:block>
                   </fo:table-cell>
                </fo:table-row>
                <#assign currentShipmentId = newShipmentId>
            </#if>
            <#if !isItemAdjustment>
                <fo:table-row height="14px" space-start=".15in">
                    <fo:table-cell>
                        <fo:block text-align="left">${invoiceItem.productId!} </fo:block>
                    </fo:table-cell>
                    <fo:table-cell border-top-style="solid" border-top-width="thin" border-top-color="black">
                        <fo:block text-align="left">${description!}</fo:block>
                    </fo:table-cell>
                      <fo:table-cell>
                        <fo:block text-align="right"> <#if invoiceItem.quantity??>${invoiceItem.quantity?string.number}</#if> </fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block> <#if invoiceItem.quantity??><@ofbizCurrency amount=invoiceItem.amount! isoCode=invoice.currencyUomId!/></#if> </fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block> <@ofbizCurrency amount=(Static["org.apache.ofbiz.accounting.invoice.InvoiceWorker"].getInvoiceItemTotal(invoiceItem)) isoCode=invoice.currencyUomId!/> </fo:block>
                    </fo:table-cell>
                </fo:table-row>
            <#else>
                <#if !(invoiceItem.parentInvoiceId?? && invoiceItem.parentInvoiceItemSeqId??)>
                    <fo:table-row>
                        <fo:table-cell><fo:block/></fo:table-cell>
                        <fo:table-cell border-top-style="solid" border-top-width="thin" border-top-color="black"><fo:block/></fo:table-cell>
                        <fo:table-cell number-columns-spanned="3"><fo:block/></fo:table-cell>
                    </fo:table-row>
                </#if>
                <fo:table-row height="14px" space-start=".15in">
                    <fo:table-cell number-columns-spanned="2">
                        <fo:block text-align="right">${description!}</fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right" number-columns-spanned="3">
                        <fo:block> <@ofbizCurrency amount=(Static["org.apache.ofbiz.accounting.invoice.InvoiceWorker"].getInvoiceItemTotal(invoiceItem)) isoCode=invoice.currencyUomId!/> </fo:block>
                    </fo:table-cell>
                </fo:table-row>
            </#if>
        </#list>

        <#-- blank line -->
        <fo:table-row height="7px">
            <fo:table-cell number-columns-spanned="5"><fo:block><#-- blank line --></fo:block></fo:table-cell>
        </fo:table-row>

        <#-- the grand total -->
        <fo:table-row>
           <fo:table-cell number-columns-spanned="2">
              <fo:block/>
           </fo:table-cell>
           <fo:table-cell number-columns-spanned="2">
              <fo:block font-weight="bold">${uiLabelMap.AccountingTotalCapital}</fo:block>
           </fo:table-cell>
           <fo:table-cell text-align="right" border-top-style="solid" border-top-width="thin" border-top-color="black">
              <fo:block><@ofbizCurrency amount=invoiceTotal isoCode=invoice.currencyUomId!/></fo:block>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row height="7px">
           <fo:table-cell number-columns-spanned="5">
              <fo:block/>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row height="14px">
           <fo:table-cell number-columns-spanned="2">
              <fo:block/>
           </fo:table-cell>
           <fo:table-cell number-columns-spanned="2">
              <fo:block>${uiLabelMap.AccountingTotalExclTax}</fo:block>
           </fo:table-cell>
           <fo:table-cell text-align="right" border-top-style="solid" border-top-width="thin" border-top-color="black">
              <fo:block>
                 <@ofbizCurrency amount=invoiceNoTaxTotal isoCode=invoice.currencyUomId!/>
              </fo:block>
           </fo:table-cell>
        </fo:table-row>
    </fo:table-body>
 </fo:table>

<#if vatTaxIds?has_content>
 <fo:table>
    <fo:table-column column-width="105mm"/>
    <fo:table-column column-width="40mm"/>
    <fo:table-column column-width="25mm"/>

    <fo:table-header>
      <fo:table-row>
        <fo:table-cell>
          <fo:block/>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingVat}</fo:block>
        </fo:table-cell>
        <fo:table-cell text-align="right" border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingAmount}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>

    <fo:table-body font-size="10pt">

    <#list vatTaxIds as vatTaxId>
    <#assign taxRate = delegator.findOne("TaxAuthorityRateProduct", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("taxAuthorityRateSeqId", vatTaxId), true)/>
    <fo:table-row>
        <fo:table-cell>
          <fo:block/>
        </fo:table-cell>
        <fo:table-cell number-columns-spanned="1">
            <fo:block>${taxRate.description}</fo:block>
        </fo:table-cell>
        <fo:table-cell number-columns-spanned="1" text-align="right">
            <fo:block font-weight="bold"><@ofbizCurrency amount=vatTaxesByType[vatTaxId] isoCode=invoice.currencyUomId!/></fo:block>
        </fo:table-cell>
    </fo:table-row>
    </#list>
    </fo:table-body>
 </fo:table>
</#if>

 <#-- a block with the invoice message-->
 <#if invoice.invoiceMessage?has_content><fo:block>${invoice.invoiceMessage}</fo:block></#if>
 <fo:block></fo:block>
</#escape>
