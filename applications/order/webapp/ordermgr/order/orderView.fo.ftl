<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Si Chen (sichen@opensourcestrategies.com)
-->
<?xml version="1.0" encoding="UTF-8"?>

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <fo:layout-master-set>
        <fo:simple-page-master master-name="main-page"
            margin-top="1in" margin-bottom="1in"
            margin-left="1in" margin-right="1in">
          <fo:region-body margin-top="3.5in" margin-bottom="1in"/>  <#-- main body -->
            <fo:region-after extent="1in"/>  <#-- a footer -->
            <fo:region-before extent="3.5in"/>  <#-- a header -->
        </fo:simple-page-master>
  </fo:layout-master-set>
  
  <fo:page-sequence master-reference="main-page">
       <#-- the region-before and -after must be declared as fo:static-content and before the fo:flow.  only 1 fo:flow per
            fo:page-sequence -->
       <fo:static-content flow-name="xsl-region-before">
         <#-- a nest of tables to put company information on left and invoice information on the right -->
        <fo:table>
           <fo:table-column column-width="3.5in"/>
           <fo:table-column column-width="3in"/>
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell>Importing a screen
                   ${screens.render("component://order/widget/ordermgr/OrderPrintForms.xml#CompanyLogo")}
                </fo:table-cell>
                <fo:table-cell>
                  <fo:table>
                    <fo:table-column column-width="1.5in"/>
                    <fo:table-column column-width="1.5in"/>
                    <fo:table-body>
                    <fo:table-row>
                      <fo:table-cell>
                         <fo:block number-columns-spanned="2" font-weight="bold">${orderHeader.getRelatedOne("OrderType").get("description",locale)} ${uiLabelMap.OrderOrder}</fo:block>
                      </fo:table-cell>
                    </fo:table-row>
                    
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderDateOrdered}</fo:block></fo:table-cell>
                      <#assign dateFormat = Static["java.text.DateFormat"].LONG>
                      <#assign orderDate = Static["java.text.DateFormat"].getDateInstance(dateFormat).format(orderHeader.get("orderDate"))>
                      <fo:table-cell><fo:block>${orderDate}</fo:block></fo:table-cell>
                    </fo:table-row>
                                  
                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderOrder} #</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block>${orderId}</fo:block></fo:table-cell>
                    </fo:table-row>

                    <fo:table-row>
                      <fo:table-cell><fo:block>${uiLabelMap.OrderCurrentStatus}</fo:block></fo:table-cell>
                      <fo:table-cell><fo:block font-weight="bold">${currentStatus.get("description",locale)}</fo:block></fo:table-cell>
                    </fo:table-row>
                  </fo:table-body>
                </fo:table>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>

       <#-- Inserts a newline.  white-space-collapse="false" specifies that the stuff inside fo:block is to repeated verbatim -->
       <fo:block white-space-collapse="false"> </fo:block> 

       <fo:table border-spacing="3pt">
           <fo:table-column column-width="3.75in"/>
          <fo:table-column column-width="3.75in"/>
          <fo:table-body>
            <fo:table-row>    <#-- this part could use some improvement -->
             <#list orderContactMechValueMaps as orderContactMechValueMap>
               <#assign contactMech = orderContactMechValueMap.contactMech>
               <#assign contactMechPurpose = orderContactMechValueMap.contactMechPurposeType>
               <#if contactMech.contactMechTypeId == "POSTAL_ADDRESS">
               <fo:table-cell>
                 <fo:block white-space-collapse="false">
<fo:block font-weight="bold">${contactMechPurpose.get("description",locale)} : </fo:block><#assign postalAddress = orderContactMechValueMap.postalAddress><#if postalAddress?has_content><#if postalAddress.toName?has_content>${postalAddress.toName}</#if><#if postalAddress.attnName?has_content>
${postalAddress.attnName}</#if>
${postalAddress.address1}<#if postalAddress.address2?has_content>
${postalAddress.address2}</#if>
${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if></#if><#if postalAddress.postalCode?has_content>${postalAddress.postalCode}</#if>
</fo:block>
                </fo:table-cell>
                </#if>
             </#list>
             <#if orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
             <#if supplierGeneralContactMechValueMap?exists>
               <#assign contactMech = supplierGeneralContactMechValueMap.contactMech>
               <fo:table-cell>
                 <fo:block white-space-collapse="false">
<fo:block font-weight="bold">${uiLabelMap.ProductSupplier}:</fo:block><#assign postalAddress = supplierGeneralContactMechValueMap.postalAddress><#if postalAddress?has_content><#if postalAddress.toName?has_content>${postalAddress.toName}</#if><#if postalAddress.attnName?has_content>
${postalAddress.attnName}</#if>
${postalAddress.address1}<#if postalAddress.address2?has_content>
${postalAddress.address2}</#if>
${postalAddress.city}<#if postalAddress.stateProvinceGeoId?has_content>, ${postalAddress.stateProvinceGeoId} </#if></#if><#if postalAddress.postalCode?has_content>${postalAddress.postalCode}</#if>
</fo:block>
               </fo:table-cell>
             </#if>
             </#if>
            </fo:table-row>
         </fo:table-body>
       </fo:table>

       <fo:block white-space-collapse="false"> </fo:block> 

       <fo:table border-spacing="3pt">
          <fo:table-column column-width="1.75in"/>
          <fo:table-column column-width="4.25in"/>
          
  <#-- payment info -->                
          <fo:table-body>
            <fo:table-row>
                <fo:table-cell><fo:block>${uiLabelMap.AccountingPaymentInformation}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>
                   <#if orderPaymentPreferences?has_content>
                      <#list orderPaymentPreferences as orderPaymentPreference>
                         <#assign paymentMethodType = orderPaymentPreference.getRelatedOne("PaymentMethodType")?if_exists>
                         <#if orderPaymentPreference.getString("paymentMethodTypeId") == "CREDIT_CARD">
                           <#assign creditCard = orderPaymentPreference.getRelatedOne("PaymentMethod").getRelatedOne("CreditCard")>
                             ${Static["org.ofbiz.party.contact.ContactHelper"].formatCreditCard(creditCard)}
                         <#else>
                             ${paymentMethodType.get("description",locale)?if_exists}
                         </#if>
                      </#list>
                   </#if>
                         </fo:block>
                 </fo:table-cell>
            </fo:table-row>
        <#-- shipping method.  currently not shown for PO's because we are not recording a shipping method for PO's in order entry -->
           <#if orderHeader.getString("orderTypeId") == "SALES_ORDER">
            <fo:table-row>
               <fo:table-cell><fo:block>${uiLabelMap.OrderShipmentInformation}:</fo:block></fo:table-cell>
                  <fo:table-cell>
                 <#if shipGroups?has_content>
                   <#list shipGroups as shipGroup>
                   <#-- TODO: List all full details of each ship group here -->
                        <fo:block>
                      ${shipGroup.shipmentMethodTypeId?if_exists}
                     </fo:block>
                   </#list>
                  </#if>
               </fo:table-cell>
             </fo:table-row>
           </#if>
       <#-- order terms information -->
             <#if orderTerms?has_content>
             <fo:table-row>
               <fo:table-cell><fo:block>${uiLabelMap.OrderOrderTerms}: </fo:block></fo:table-cell>
               <fo:table-cell white-space-collapse="false"><fo:block><#list orderTerms as orderTerm>${orderTerm.getRelatedOne("TermType").get("description",locale)} ${orderTerm.termValue?default("")} ${orderTerm.termDays?default("")}
</#list></fo:block></fo:table-cell>
             </fo:table-row>
             </#if>
          </fo:table-body>
       </fo:table>

       </fo:static-content>
         
       <#-- this part is the footer.  Use it for standard boilerplate text. -->
       <fo:static-content flow-name="xsl-region-after">
<#if orderHeader.getString("orderTypeId") == "SALES_ORDER">
           <fo:block font-size="14pt" font-weight="bold" text-align="center">THANK YOU FOR YOUR PATRONAGE!</fo:block>
           <fo:block font-size="8pt" white-space-collapse="false">
Here is a good place to put policies and return information.
</fo:block>
<#elseif orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
           <fo:block font-size="8pt" white-space-collapse="false">
Here is a good place to put boilerplate terms and conditions for a purchase order.
</fo:block>
</#if>
           <#-- displays page number.  "theEnd" is an id of a fo:block at the very end -->    
           <fo:block font-size="10pt" text-align="center">Page <fo:page-number/> of <fo:page-number-citation ref-id="theEnd"/></fo:block>
       </fo:static-content>
       <#-- end of footer -->
       
    <fo:flow flow-name="xsl-region-body">
    <#-- order items -->    
    <#if orderHeader?has_content>
        <fo:table border-spacing="3pt">

       <fo:table-column column-width="3.5in"/>
       <fo:table-column column-width="1in"/>
          <fo:table-column column-width="1in"/>
       <fo:table-column column-width="1in"/>
  
       <fo:table-header>
           <fo:table-row>
               <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
               <fo:table-cell text-align="center"><fo:block font-weight="bold">${uiLabelMap.OrderQuantity}</fo:block></fo:table-cell>
               <fo:table-cell text-align="center"><fo:block font-weight="bold">${uiLabelMap.OrderUnitList}</fo:block></fo:table-cell>
               <fo:table-cell text-align="center"><fo:block font-weight="bold">${uiLabelMap.OrderSubTotal}</fo:block></fo:table-cell>
           </fo:table-row>        
       </fo:table-header>
        
            <fo:table-body>
           <#list orderItemList as orderItem>
                 <#assign orderItemType = orderItem.getRelatedOne("OrderItemType")?if_exists>
                 <#assign productId = orderItem.productId?if_exists>
                    <#assign remainingQuantity = (orderItem.quantity?default(0) - orderItem.cancelQuantity?default(0))>
                 <#assign itemAdjustment = Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemAdjustmentsTotal(orderItem, orderAdjustments, true, false, false)>
                  <fo:table-row>
                        <fo:table-cell>
                            <fo:block>
                               <#if productId?exists>
                                ${orderItem.productId?default("N/A")} - ${orderItem.itemDescription?xml?if_exists}
                              <#elseif orderItemType?exists>
                                ${orderItemType.get("description",locale)} - ${orderItem.itemDescription?xml?if_exists}
                              <#else>
                                ${orderItem.itemDescription?xml?if_exists}
                              </#if>
                               </fo:block>
                          </fo:table-cell>
                              <fo:table-cell text-align="right"><fo:block>${remainingQuantity}</fo:block></fo:table-cell>            
                            <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></fo:block></fo:table-cell>
                            <fo:table-cell text-align="right"><fo:block>
                            <#if orderItem.statusId != "ITEM_CANCELLED">
                                  <@ofbizCurrency amount=Static["org.ofbiz.order.order.OrderReadHelper"].getOrderItemSubTotal(orderItem, orderAdjustments) isoCode=currencyUomId/>
                            <#else>
                                <@ofbizCurrency amount=0.00 isoCode=currencyUomId/>
                            </#if></fo:block></fo:table-cell>
                       </fo:table-row>
                       <#if itemAdjustment != 0>
                       <fo:table-row>
                        <fo:table-cell number-columns-spanned="2"><fo:block><fo:inline font-style="italic">${uiLabelMap.OrderAdjustments}</fo:inline>: <@ofbizCurrency amount=itemAdjustment isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                    </#if>
           </#list>
          <#list orderHeaderAdjustments as orderHeaderAdjustment>
            <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType")>
            <#assign adjustmentAmount = Static["org.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
            <#if adjustmentAmount != 0>
            <fo:table-row>
               <fo:table-cell></fo:table-cell>
               <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${adjustmentType.get("description",locale)} : <#if orderHeaderAdjustment.get("description")?has_content>(${orderHeaderAdjustment.get("description")?if_exists})</#if> </fo:block></fo:table-cell>
               <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
            </fo:table-row>
            </#if>
          </#list>

           <#-- summary of order amounts --> 
                    <fo:table-row>
                        <fo:table-cell></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.OrderItemsSubTotal}</fo:block></fo:table-cell>
                        <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=orderSubTotal isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                  <#if otherAdjAmount != 0>
                    <fo:table-row>
                        <fo:table-cell></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.OrderTotalOtherOrderAdjustments}</fo:block></fo:table-cell>
                        <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=otherAdjAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                  </#if>
                  <#if shippingAmount != 0>
                    <fo:table-row>
                        <fo:table-cell></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.OrderTotalShippingAndHandling}</fo:block></fo:table-cell>
                        <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=shippingAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                  </#if>
                  <#if taxAmount != 0>
                    <fo:table-row>
                        <fo:table-cell></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2"><fo:block font-weight="bold">${uiLabelMap.OrderTotalSalesTax}</fo:block></fo:table-cell>
                        <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=taxAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                  </#if>
                  <#if grandTotal != 0>
                    <fo:table-row>
                        <fo:table-cell></fo:table-cell>
                        <fo:table-cell number-columns-spanned="2" background-color="#EEEEEE"><fo:block font-weight="bold">${uiLabelMap.OrderTotalDue}</fo:block></fo:table-cell>
                        <fo:table-cell text-align="right"><fo:block><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></fo:block></fo:table-cell>
                    </fo:table-row>
                  </#if>

           <#-- notes -->
           <#if orderNotes?has_content>
                   <fo:table-row >
                       <fo:table-cell number-columns-spanned="3">
                           <fo:block font-weight="bold">${uiLabelMap.OrderNotes}</fo:block>
                       </fo:table-cell>    
                   </fo:table-row>    
                <#list orderNotes as note>
                 <#if (note.internalNote?has_content) && (note.internalNote != "Y")>
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="6">
                            <fo:block><fo:leader leader-length="19cm" leader-pattern="rule" /></fo:block>    
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell number-columns-spanned="1">
                        <fo:block>${note.noteInfo?if_exists}</fo:block>    
                    </fo:table-cell>
                        <fo:table-cell number-columns-spanned="2">
                        <#assign notePartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", note.noteParty, "compareDate", note.noteDateTime, "lastNameFirst", "Y", "userLogin", userLogin))/>
                        <fo:block>${uiLabelMap.CommonBy}: ${notePartyNameResult.fullName?default("${uiLabelMap.OrderPartyNameNotFound}")}</fo:block>
                    </fo:table-cell>
                        <fo:table-cell number-columns-spanned="1">
                        <fo:block>${uiLabelMap.CommonAt}: ${note.noteDateTime?string?if_exists}</fo:block>    
                    </fo:table-cell>
                  </fo:table-row>
                  </#if>                  
                  </#list>
            </#if>
            </fo:table-body>
    </fo:table>    
    </#if>
    <fo:block id="theEnd"/>  <#-- marks the end of the pages and used to identify page-number at the end -->
    </fo:flow>
    </fo:page-sequence>
</fo:root>
