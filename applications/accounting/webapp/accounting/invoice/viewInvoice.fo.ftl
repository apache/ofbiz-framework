<?xml version="1.0" encoding="UTF-8" ?>
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
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <#-- master layout specifies the overall layout of the pages and its different sections. -->
    <fo:layout-master-set>
        <fo:simple-page-master master-name="my-page"
            margin-top="1in" margin-bottom="1in"
            margin-left="20mm" margin-right="20mm">
            <fo:region-body margin-top="3in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-before extent="4in"/>  <#-- a header -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

    <fo:page-sequence master-reference="my-page" initial-page-number="1">

       <#-- the region-before and -after must be declared as fo:static-content and before the fo:flow.  only 1 fo:flow per
            fo:page-sequence -->
       <fo:static-content flow-name="xsl-region-before">
            <fo:block space-after=".10in">
              ${screens.render("component://order/widget/ordermgr/OrderPrintForms.xml#CompanyLogo")}
            </fo:block>
       <fo:block white-space-collapse="false" > 
       </fo:block> 

        <fo:table>
           <fo:table-column column-width="3.5in"/>
           <fo:table-column column-width="3in"/>
            <fo:table-body>
              <fo:table-row >
                <fo:table-cell>
                       <fo:block>${uiLabelMap.CommonTo}: </fo:block>
               <#if billingAddress?has_content>
                <#assign billingPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", billingParty.partyId, "compareDate", invoice.invoiceDate, "userLogin", userLogin))/>
                <fo:block>${billingPartyNameResult.fullName?default(billingAddress.toName)?default("Billing Name Not Found")}</fo:block>
                <#if billingAddress.attnName?exists>
                    <fo:block>${billingAddress.attnName}</fo:block>
                </#if>
                    <fo:block>${billingAddress.address1?if_exists}</fo:block>
                <#if billingAddress.address2?exists>
                    <fo:block>${billingAddress.address2}</fo:block>
                </#if>
                <fo:block>${billingAddress.city?if_exists} ${billingAddress.stateProvinceGeoId?if_exists} ${billingAddress.postalCode?if_exists}</fo:block>
            <#else>
                <fo:block>${uiLabelMap.AccountingNoGenBilAddressFound}${billingParty.partyId}</fo:block>
            </#if>
            <#if billingPartyTaxId?has_content>
                <fo:block>Tax ID: ${billingPartyTaxId}</fo:block>
            </#if>            
                </fo:table-cell>
                <fo:table-cell><fo:block>
                
                  <fo:table>
                    <fo:table-column column-width="1in"/>
                    <fo:table-column column-width="1.5in"/>
                    <fo:table-body>
                    <fo:table-row>
                      <fo:table-cell>
                         <fo:block number-columns-spanned="2" font-weight="bold" wrap-option="no-wrap">${invoice.getRelatedOne("InvoiceType").get("description",locale)}</fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                    
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.AccountingInvoiceDateAbbr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${invoiceDate?if_exists}</fo:block></fo:table-cell>
                    </fo:table-row>
                                  
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.AccountingCustNr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block><#if invoice?has_content>${billingParty.partyId}</#if></fo:block></fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.AccountingInvNr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block><#if invoice?has_content>${invoice.invoiceId}</#if></fo:block></fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.AccountingDescr}:</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block><#if invoice?has_content>${invoice.description?if_exists}</#if></fo:block></fo:table-cell>
                    </fo:table-row>

                    <!--fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-weight="bold">${invoiceStatus.get("description",locale)}</fo:block></fo:table-cell>
                    </fo:table-row-->
                  </fo:table-body>
                </fo:table>
              </fo:block></fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>

            <#-- Inserts a newline.  white-space-collapse="false" specifies that the stuff inside fo:block is to repeated verbatim -->
            <fo:block white-space-collapse="false"> </fo:block> 
               
                
            <#-- list of orders -->
            <#if orders?has_content>
            <fo:table>
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

            <#-- Insert newline -->
            <fo:block white-space-collapse="false"> </fo:block>
       </fo:static-content>

       <#-- this part is the footer.  Use it for standard boilerplate text. -->
       <fo:static-content flow-name="xsl-region-after">
       <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->    
           <fo:block font-size="10pt" text-align="center">${uiLabelMap.CommonPage} <fo:page-number/> ${uiLabelMap.CommonOf} <fo:page-number-citation ref-id="theEnd"/></fo:block>
           <fo:block font-size="10pt"/>
           <fo:block font-size="8pt" text-align="center">Powered by OFBiz at www.ofbiz.org</fo:block>
       </fo:static-content>
       <#-- end of footer -->
       
       <#-- this part is the main body which lists the items -->
       <fo:flow flow-name="xsl-region-body">
            <fo:table>
            <fo:table-column column-width="20mm"/>
            <fo:table-column column-width="20mm"/>
            <fo:table-column column-width="65mm"/>
            <fo:table-column column-width="10mm"/>
            <fo:table-column column-width="20mm"/>
            <fo:table-column column-width="20mm"/>
            
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
                                    <fo:block><#-- blank line --></fo:block>
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
                                <fo:block><#-- blank line --></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row height="14px" space-start=".15in">
                            <fo:table-cell>
                                <fo:block> ${invoiceItem.invoiceItemSeqId} </fo:block>               
                            </fo:table-cell>    
                            <fo:table-cell>
                                <fo:block> ${invoiceItem.productId?if_exists} </fo:block>               
                            </fo:table-cell>    
                            <fo:table-cell>
                                <fo:block> ${description?if_exists} </fo:block>               
                            </fo:table-cell>       
                              <fo:table-cell>
                                <fo:block text-align="right"> <#if invoiceItem.quantity?exists>${invoiceItem.quantity?string.number}</#if> </fo:block>               
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                                <fo:block> <#if invoiceItem.quantity?exists><@ofbizCurrency amount=invoiceItem.amount?if_exists isoCode=invoice.currencyUomId?if_exists/></#if> </fo:block>               
                            </fo:table-cell>
                            <fo:table-cell text-align="right">
                                <fo:block> <#if invoiceItem.quantity?exists><@ofbizCurrency amount=(invoiceItem.quantity?double * invoiceItem.amount?double) isoCode=invoice.currencyUomId?if_exists/><#else><@ofbizCurrency amount=(invoiceItem.amount?double) isoCode=invoice.currencyUomId?if_exists/></#if> </fo:block>               
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
                   </fo:table-cell>
                </fo:table-row>
                <fo:table-row height="14px">
                   <fo:table-cell number-columns-spanned="3">
                   </fo:table-cell>
                   <fo:table-cell number-columns-spanned="2">
                      <fo:block>Total excl. tax</fo:block>
                   </fo:table-cell>
                   <fo:table-cell number-columns-spanned="1" text-align="right">
                      <fo:block>
                         <@ofbizCurrency amount=invoiceNoTaxTotal isoCode=invoice.currencyUomId?if_exists/>     
                      </fo:block>
                   </fo:table-cell>
                </fo:table-row>
            </fo:table-body>        
         </fo:table>

         <!-- a block with the invoice message-->
         <#if invoice.invoiceMessage?has_content><fo:block>${invoice.invoiceMessage}</fo:block></#if>
         <fo:block></fo:block>
         <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
       </fo:flow>
    </fo:page-sequence>
</fo:root>
</#escape>
