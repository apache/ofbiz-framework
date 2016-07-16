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
        <#if invoiceDetail.billToParty?has_content>
          <#assign billToParty = invoiceDetail.billToParty />
          <#assign partyName = delegator.findOne("PartyNameView", {"partyId" : billToParty.partyId}, true)>
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
                                <fo:block number-columns-spanned="2" font-weight="bold">${invoice.getRelatedOne("InvoiceType", false).get("description",locale)!}</fo:block>
                              </fo:table-cell>
                            </fo:table-row>
                            <fo:table-row>
                              <fo:table-cell><fo:block>${uiLabelMap.AccountingInvoiceDateAbbr}:</fo:block></fo:table-cell>
                              <fo:table-cell><fo:block>${invoiceDetail.invoiceDate!}</fo:block></fo:table-cell>
                            </fo:table-row>
                            <fo:table-row>
                              <fo:table-cell><fo:block>${uiLabelMap.AccountingCustNr}:</fo:block></fo:table-cell>
                              <fo:table-cell>
                                <fo:block>
                                  <#if partyName?has_content>${partyName.firstName!} ${partyName.lastName!} ${partyName.groupName!}</#if>
                                </fo:block>
                              </fo:table-cell>
                            </fo:table-row>
                            <#if invoiceDetail.billToPartyTaxId?has_content>
                              <fo:table-row>
                                <fo:table-cell><fo:block>${uiLabelMap.PartyTaxId}:</fo:block></fo:table-cell>
                                <fo:table-cell><fo:block> ${invoiceDetail.billToPartyTaxId}</fo:block></fo:table-cell>
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
              
            <#if billToParty?has_content>
              <fo:block>
                <fo:table width="100%" table-layout="fixed" space-after="0.3in">
                  <fo:table-column column-width="3.5in"/>
                  <fo:table-body>
                    <fo:table-row >
                      <fo:table-cell>
                        <fo:block>${uiLabelMap.CommonTo}: </fo:block>
                        <#if invoiceDetail.billingAddress?has_content>
                          <#assign billingAddress = invoiceDetail.billingAddress />
                          <#assign billToPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", billToParty.partyId, "compareDate", invoice.invoiceDate, "userLogin", userLogin))/>
                          <fo:block>${billToPartyNameResult.fullName?default(billingAddress.toName)?default("Billing Name Not Found")}</fo:block>
                          <#if billingAddress.attnName??>
                            <fo:block>${billingAddress.attnName}</fo:block>
                          </#if>
                          <fo:block>${billingAddress.address1!}</fo:block>
                          <#if billingAddress.address2??>
                            <fo:block>${billingAddress.address2}</fo:block>
                          </#if>
                          <fo:block>
                            <#assign stateGeo = (delegator.findOne("Geo", {"geoId", billingAddress.stateProvinceGeoId!}, false))! />
                            ${billingAddress.city!} <#if stateGeo?has_content>${stateGeo.geoName!}</#if> ${billingAddress.postalCode!}
                          </fo:block>
                        <#else>
                          <fo:block>${uiLabelMap.AccountingNoGenBilAddressFound} <#if partyName?has_content>${partyName.firstName!} ${partyName.lastName!} ${partyName.groupName!}</#if></fo:block>
                        </#if>
                      </fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
              </fo:block>
            </#if>
            
            <fo:block>
              <#if invoiceDetail.orders?has_content>
                <#assign orders = invoiceDetail.orders! />
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
                <#assign invoiceItems = invoiceDetail.invoiceItems! />
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
                      <#assign itemType = invoiceItem.getRelatedOne("InvoiceItemType", false)>
                      <#assign taxRate = invoiceItem.getRelatedOne("TaxAuthorityRateProduct", false)!>
                      <#assign itemBillings = invoiceItem.getRelated("OrderItemBilling", null, null, false)!>
                      <#if itemBillings?has_content>
                        <#assign itemBilling = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(itemBillings)>
                        <#if itemBilling?has_content>
                          <#assign itemIssuance = itemBilling.getRelatedOne("ItemIssuance", false)!>
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
  
                      <#if newShipmentId?? & newShipmentId != currentShipmentId>
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
                          <fo:block text-align="left">${invoiceItem.productId!} </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                          <fo:block text-align="right">${description!}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                          <fo:block text-align="center"> <#if invoiceItem.quantity??>${invoiceItem.quantity?string.number}</#if> </fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                          <fo:block> <#if invoiceItem.quantity??><@ofbizCurrency amount=invoiceItem.amount! isoCode=invoice.currencyUomId!/></#if> </fo:block>
                        </fo:table-cell>
                        <fo:table-cell text-align="right">
                          <fo:block> <@ofbizCurrency amount=(Static["org.apache.ofbiz.accounting.invoice.InvoiceWorker"].getInvoiceItemTotal(invoiceItem)) isoCode=invoice.currencyUomId!/> </fo:block>
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
                            <#assign invoiceTotal = invoiceDetail.invoiceTotal! />
                            <@ofbizCurrency amount=invoiceTotal isoCode=invoice.currencyUomId!/>
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
                            <#assign invoiceNoTaxTotal = invoiceDetail.invoiceNoTaxTotal! />
                            <@ofbizCurrency amount=invoiceNoTaxTotal isoCode=invoice.currencyUomId!/>
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
