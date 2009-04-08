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
    <fo:table space-after="0.3in">
        <fo:table-column column-width="1in"/>
        <fo:table-column column-width="5.5in"/>

        <fo:table-body>
          <fo:table-row>
            <fo:table-cell>
              <fo:block font-size="10pt" font-weight="bold">${uiLabelMap.AccountingOrderNr}:</fo:block>
            </fo:table-cell>
            <fo:table-cell>
              <fo:block font-size ="10pt" font-weight="bold"><#list orders as order> ${order} </#list></fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
    </fo:table>
    </#if>
    <#-- TODO: put shipment information here or somewhere -->

    <fo:table>
    <fo:table-column column-width="20mm"/>
    <fo:table-column column-width="20mm"/>
    <fo:table-column column-width="65mm"/>
    <fo:table-column column-width="15mm"/>
    <fo:table-column column-width="25mm"/>
    <fo:table-column column-width="25mm"/>

    <fo:table-header height="14px">
      <fo:table-row>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingItemNr}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.AccountingProduct}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold">${uiLabelMap.CommonDescription}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold" text-align="center">${uiLabelMap.CommonQty}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold" text-align="center">${uiLabelMap.AccountingUnitPrice}</fo:block>
        </fo:table-cell>
        <fo:table-cell border-bottom-style="solid" border-bottom-width="thin" border-bottom-color="black">
          <fo:block font-weight="bold" text-align="center">${uiLabelMap.CommonAmount}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>


    <fo:table-body font-size="10pt">
        <#assign currentShipmentId = "">
        <#assign newShipmentId = "">
        <#-- if the item has a description, then use its description.  Otherwise, use the description of the invoiceItemType -->
        <#list invoiceItems as invoiceItem>
            <#assign itemType = invoiceItem.getRelatedOne("InvoiceItemType")>
            <#assign taxRate = invoiceItem.getRelatedOne("TaxAuthorityRateProduct")?if_exists>
            <#assign itemBillings = invoiceItem.getRelated("OrderItemBilling")?if_exists>
            <#if itemBillings?has_content>
                <#assign itemBilling = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(itemBillings)>
                <#if itemBilling?has_content>
                    <#assign itemIssuance = itemBilling.getRelatedOne("ItemIssuance")?if_exists>
                    <#if itemIssuance?has_content>
                        <#assign newShipmentId = itemIssuance.shipmentId>
                    </#if>
                </#if>
            </#if>
            <#if invoiceItem.description?has_content>
                <#assign description=invoiceItem.description>
            <#elseif taxRate?has_content & taxRate.get("description",locale)?has_content>
                <#assign description=taxRate.get("description",locale)>
            <#elseif itemType.get("description",locale)?has_content>
                <#assign description=itemType.get("description",locale)>
            </#if>

            <#if newShipmentId?exists & newShipmentId != currentShipmentId>
                <#-- the shipment id is printed at the beginning for each
                     group of invoice items created for the same shipment
                -->
                <fo:table-row height="14px">
                    <fo:table-cell number-columns-spanned="6">
                            <fo:block></fo:block>
                       </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px">
                   <fo:table-cell number-columns-spanned="6">
                        <fo:block font-weight="bold"> ${uiLabelMap.ProductShipmentId}: ${newShipmentId} </fo:block>
                   </fo:table-cell>
                </fo:table-row>
                <#assign currentShipmentId = newShipmentId>
            </#if>
                <fo:table-row height="7px">
                    <fo:table-cell number-columns-spanned="6">
                        <fo:block></fo:block>
                    </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px" space-start=".15in">
                    <fo:table-cell>
                        <fo:block> ${invoiceItem.invoiceItemSeqId} </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                        <fo:block text-align="left">${invoiceItem.productId?if_exists} </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                        <fo:block text-align="right">${description?if_exists}</fo:block>
                    </fo:table-cell>
                      <fo:table-cell>
                        <fo:block text-align="center"> <#if invoiceItem.quantity?exists>${invoiceItem.quantity?string.number}</#if> </fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block> <#if invoiceItem.quantity?exists><@ofbizCurrency amount=invoiceItem.amount?if_exists isoCode=invoice.currencyUomId?if_exists/></#if> </fo:block>
                    </fo:table-cell>
                    <fo:table-cell text-align="right">
                        <fo:block> <@ofbizCurrency amount=(Static["org.ofbiz.accounting.invoice.InvoiceWorker"].getInvoiceItemTotal(invoiceItem)) isoCode=invoice.currencyUomId?if_exists/> </fo:block>
                    </fo:table-cell>
                </fo:table-row>
        </#list>

        <#-- blank line -->
        <fo:table-row height="7px">
            <fo:table-cell number-columns-spanned="4"><fo:block><#-- blank line --></fo:block></fo:table-cell>
        </fo:table-row>

        <#-- the grand total -->
        <fo:table-row>
           <fo:table-cell number-columns-spanned="3">
              <fo:block/>
           </fo:table-cell>
           <fo:table-cell>
              <fo:block font-weight="bold">${uiLabelMap.AccountingTotalCapital}</fo:block>
           </fo:table-cell>
           <fo:table-cell text-align="right" number-columns-spanned="2">
              <fo:block font-weight="bold"><@ofbizCurrency amount=invoiceTotal isoCode=invoice.currencyUomId?if_exists/></fo:block>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row height="7px">
           <fo:table-cell>
              <fo:block/>
           </fo:table-cell>
        </fo:table-row>
        <fo:table-row height="14px">
           <fo:table-cell number-columns-spanned="3">
              <fo:block/>
           </fo:table-cell>
           <fo:table-cell number-columns-spanned="2">
              <fo:block>${uiLabelMap.AccountingTotalExclTax}</fo:block>
           </fo:table-cell>
           <fo:table-cell number-columns-spanned="1" text-align="right">
              <fo:block>
                 <@ofbizCurrency amount=invoiceNoTaxTotal isoCode=invoice.currencyUomId?if_exists/>
              </fo:block>
           </fo:table-cell>
        </fo:table-row>
    </fo:table-body>
 </fo:table>

 <#-- a block with the invoice message-->
 <#if invoice.invoiceMessage?has_content><fo:block>${invoice.invoiceMessage}</fo:block></#if>
 <fo:block></fo:block>
 <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
</#escape>
