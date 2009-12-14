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
    <fo:layout-master-set>
      <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
        margin-top="0.5in" margin-bottom="1in" margin-left=".5in" margin-right="1in">
          <fo:region-body margin-top="1in"/>
          <fo:region-before extent="1in"/>
          <fo:region-after extent="1in"/>
      </fo:simple-page-master>
    </fo:layout-master-set>
    <#if invoiceDetailList?has_content>
      <#list invoiceDetailList as invoiceDetail>
        <#assign invoice = invoiceDetail.invoice />
        <#if invoiceDetail.billingParty?has_content>
          <#assign billingParty = invoiceDetail.billingParty />
          <#assign partyName = delegator.findOne("PartyNameView", {"partyId" : billingParty.partyId}, true)>
        </#if>
        <fo:page-sequence master-reference="main">
          <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
            <fo:block>
              <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="4in"/>
                <fo:table-column column-width="1in"/>
                <fo:table-body>
                  <fo:table-row>
                    <fo:table-cell>
                      <fo:block>
                        ${screens.render("component://order/widget/ordermgr/OrderPrintScreens.xml#CompanyLogo")}
                      </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block>
                        <fo:table width="100%" table-layout="fixed">
                          <fo:table-column column-width="1in"/>
                          <fo:table-column column-width="2.5in"/>
                          <fo:table-body>
                            <fo:table-row>
                              <fo:table-cell>
                                <fo:block number-columns-spanned="2" font-weight="bold">${invoice.getRelatedOne("InvoiceType").get("description",locale)?if_exists}</fo:block>
                              </fo:table-cell>
                            </fo:table-row>
                            <fo:table-row>
                              <fo:table-cell><fo:block>${uiLabelMap.AccountingInvoiceDateAbbr}:</fo:block></fo:table-cell>
                              <fo:table-cell><fo:block>${invoiceDetail.invoiceDate?if_exists}</fo:block></fo:table-cell>
                            </fo:table-row>
                            <fo:table-row>
                              <fo:table-cell><fo:block>${uiLabelMap.AccountingCustNr}:</fo:block></fo:table-cell>
                              <fo:table-cell>
                                <fo:block>
                                  <#if partyName?has_content>${partyName.firstName?if_exists} ${partyName.lastName?if_exists} ${partyName.groupName?if_exists}</#if>
                                </fo:block>
                              </fo:table-cell>
                            </fo:table-row>
                            <#if invoiceDetail.billingPartyTaxId?has_content>
                              <fo:table-row>
                                <fo:table-cell><fo:block>${uiLabelMap.PartyTaxId}:</fo:block></fo:table-cell>
                                <fo:table-cell><fo:block> ${invoiceDetail.billingPartyTaxId}</fo:block></fo:table-cell>
                              </fo:table-row>
                            </#if>
                            <fo:table-row>
                              <fo:table-cell><fo:block>${uiLabelMap.AccountingInvNr}:</fo:block></fo:table-cell>
                              <fo:table-cell><fo:block><#if invoice?has_content>${invoice.invoiceId}</#if></fo:block></fo:table-cell>
                            </fo:table-row>
                            <#if invoice?has_content && invoice.description?has_content>
                              <fo:table-row>
                                <fo:table-cell><fo:block>${uiLabelMap.AccountingDescr}:</fo:block></fo:table-cell>
                                <fo:table-cell><fo:block>${invoice.description}</fo:block></fo:table-cell>
                              </fo:table-row>
                            </#if>
                          </fo:table-body>
                        </fo:table>
                      </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                </fo:table-body>
              </fo:table>
            </fo:block>
              
            <#if billingParty?has_content>
              <fo:block>
                <fo:table width="100%" table-layout="fixed" space-after="0.3in">
                  <fo:table-column column-width="3.5in"/>
                  <fo:table-body>
                    <fo:table-row >
                      <fo:table-cell>
                        <fo:block>${uiLabelMap.CommonTo}: </fo:block>
                        <#if invoiceDetail.billingAddress?has_content>
                          <#assign billingAddress = invoiceDetail.billingAddress />
                          <#assign billingPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", billingParty.partyId, "compareDate", invoice.invoiceDate, "userLogin", userLogin))/>
                          <fo:block>${billingPartyNameResult.fullName?default(billingAddress.toName)?default("Billing Name Not Found")}</fo:block>
                          <#if billingAddress.attnName?exists>
                            <fo:block>${billingAddress.attnName}</fo:block>
                          </#if>
                          <fo:block>${billingAddress.address1?if_exists}</fo:block>
                          <#if billingAddress.address2?exists>
                            <fo:block>${billingAddress.address2}</fo:block>
                          </#if>
                          <fo:block>
                            <#assign stateGeo = (delegator.findOne("Geo", {"geoId", billingAddress.stateProvinceGeoId?if_exists}, false))?if_exists />
                            ${billingAddress.city?if_exists} <#if stateGeo?has_content>${stateGeo.geoName?if_exists}</#if> ${billingAddress.postalCode?if_exists}
                          </fo:block>
                        <#else>
                          <fo:block>${uiLabelMap.AccountingNoGenBilAddressFound} <#if partyName?has_content>${partyName.firstName?if_exists} ${partyName.lastName?if_exists} ${partyName.groupName?if_exists}</#if></fo:block>
                        </#if>
                      </fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
              </fo:block>
            </#if>
            
            <fo:block>
              <#if invoiceDetail.orders?has_content>
                <#assign orders = invoiceDetail.orders?if_exists />
                <fo:table width="100%" table-layout="fixed" space-after="0.3in">
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
              
              <#if invoiceDetail.invoiceItems?has_content>
                <#assign invoiceItems = invoiceDetail.invoiceItems?if_exists />
                <fo:table width="100%" table-layout="fixed">
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
  
                    <#-- the grand total -->
                    <fo:table-row>
                      <fo:table-cell number-columns-spanned="3">
                        <fo:block/>
                      </fo:table-cell>
                      <fo:table-cell>
                        <fo:block font-weight="bold">${uiLabelMap.AccountingTotalCapital}</fo:block>
                      </fo:table-cell>
                      <fo:table-cell text-align="right" number-columns-spanned="2">
                        <fo:block font-weight="bold">
                          <#if invoiceDetail.invoiceTotal?has_content>
                            <#assign invoiceTotal = invoiceDetail.invoiceTotal?if_exists />
                            <@ofbizCurrency amount=invoiceTotal isoCode=invoice.currencyUomId?if_exists/>
                          </#if>
                        </fo:block>
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
                          <#if invoiceDetail.invoiceNoTaxTotal?has_content>
                            <#assign invoiceNoTaxTotal = invoiceDetail.invoiceNoTaxTotal?if_exists />
                            <@ofbizCurrency amount=invoiceNoTaxTotal isoCode=invoice.currencyUomId?if_exists/>
                          </#if>
                        </fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
              </#if>
            </fo:block>
          </fo:flow>
        </fo:page-sequence>
      </#list>
    </#if>        
  </fo:root>
</#escape>
