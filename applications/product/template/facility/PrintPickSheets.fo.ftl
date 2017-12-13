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

        <#list orderHeaderList as order>
            <fo:page-sequence master-reference="main">
                <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
                    <#include "component://order/template/order/CompanyHeader.fo.ftl"/>
                    <#assign orderId = order.orderId>
                    <#assign orderDate = order.orderDate>
                    <#list orderInfoList as orderInfo>
                        <#if orderInfo.get("${orderId}")??>
                            <#assign orderDetail = orderInfo.get("${orderId}")>
                            <#assign orderDate = orderDetail.orderDate>
                            <#if orderDetail.billingAddress??>
                                <#assign billAddress = orderDetail.billingAddress>
                            </#if>
                            <#assign shipAddress = orderDetail.shippingAddress>
                            <#assign shipmentMethodType = orderDetail.shipmentMethodType>
                            <#assign carrierPartyId = orderDetail.carrierPartyId>
                            <#assign shipGroupSeqId = orderDetail.shipGroupSeqId>

                            <fo:block text-align="right">
                                <fo:instream-foreign-object>
                                    <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                            message="${orderId}/${shipGroupSeqId}">
                                        <barcode:code39>
                                            <barcode:height>8mm</barcode:height>
                                        </barcode:code39>
                                    </barcode:barcode>
                                </fo:instream-foreign-object>
                            </fo:block>

                            <fo:table>
                                <fo:table-column column-width="200pt"/>
                                <fo:table-column column-width="200pt"/>
                                <fo:table-body>
                                    <fo:table-row>
                                         <fo:table-cell>
                                             <fo:block font-weight="bold">${uiLabelMap.OrderOrderId}:</fo:block><fo:block> ${orderId} (${shipGroupSeqId})</fo:block>
                                             <fo:block font-weight="bold">${uiLabelMap.OrderOrderDate}:</fo:block><fo:block> ${orderDate}</fo:block>
                                         </fo:table-cell>
                                         <fo:table-cell>
                                             <fo:table>
                                                 <fo:table-column column-width="200pt"/>
                                                 <fo:table-column column-width="200pt"/>
                                                 <fo:table-body>
                                                     <fo:table-row>
                                                         <fo:table-cell>
                                                             <fo:block font-weight="bold">${uiLabelMap.OrderShipToParty}:</fo:block>
                                                             <fo:block>${shipAddress.toName!}</fo:block>
                                                             <fo:block> ${shipAddress.address1!}</fo:block>
                                                             <fo:block> ${shipAddress.city!}</fo:block>
                                                             <fo:block> ${shipAddress.countryGeoId!}</fo:block>
                                                             <fo:block> ${shipAddress.postalCode!}</fo:block>
                                                             <fo:block> ${shipAddress.stateProvinceGeoId!}</fo:block>
                                                         </fo:table-cell>
                                                         <fo:table-cell>
                                                             <fo:table>
                                                                 <fo:table-column column-width="200pt"/>
                                                                 <fo:table-body>
                                                                     <fo:table-row>
                                                                         <fo:table-cell>
                                                                             <#if billAddress?has_content>
                                                                                 <fo:block font-weight="bold">${uiLabelMap.OrderOrderBillToParty}:</fo:block>
                                                                                 <fo:block> ${billAddress.toName!}</fo:block>
                                                                                 <fo:block> ${billAddress.address1!}</fo:block>
                                                                                 <fo:block> ${billAddress.city!}</fo:block>
                                                                                 <fo:block> ${billAddress.countryGeoId!}</fo:block>
                                                                                 <fo:block> ${billAddress.postalCode!}</fo:block>
                                                                                 <fo:block> ${billAddress.stateProvinceGeoId!}</fo:block>
                                                                             <#else>
                                                                                <fo:block></fo:block>
                                                                             </#if>
                                                                         </fo:table-cell>
                                                                     </fo:table-row>
                                                                 </fo:table-body>
                                                             </fo:table> 
                                                         </fo:table-cell>
                                                     </fo:table-row>
                                                 </fo:table-body>
                                             </fo:table>
                                         </fo:table-cell>
                                     </fo:table-row>
                                 </fo:table-body>
                             </fo:table>
                             <fo:block space-after.optimum="10pt" font-size="14pt">
                                 <fo:table>
                                     <fo:table-column column-width="50pt"/>
                                     <fo:table-column column-width="400pt"/>
                                     <fo:table-column column-width="50pt"/>
                                     <fo:table-body>
                                          <fo:table-row>
                                             <fo:table-cell><fo:block></fo:block></fo:table-cell>
                                             <fo:table-cell padding="2pt">
                                                 <fo:table table-layout="fixed" border-width="1pt" border-style="solid">
                                                     <fo:table-column column-width="150pt"/>
                                                     <fo:table-column column-width="250pt"/>
                                                     <fo:table-body>
                                                         <fo:table-row>
                                                             <fo:table-cell>
                                                                  <fo:block>${uiLabelMap.ProductShipmentMethod}:</fo:block>
                                                             </fo:table-cell>
                                                             <fo:table-cell>
                                                                 <fo:block font-weight="bold">${carrierPartyId!}-${shipmentMethodType!}</fo:block>
                                                             </fo:table-cell>
                                                         </fo:table-row>
                                                     </fo:table-body>
                                                 </fo:table>
                                             </fo:table-cell>
                                         </fo:table-row>
                                     </fo:table-body>
                                 </fo:table>
                             </fo:block>
                         </#if>
                     </#list>
                     <fo:block space-after.optimum="10pt" font-size="12pt">
                         <fo:table table-layout="fixed" border-width="1pt" border-style="solid">
                             <fo:table-column column-width="90pt"/>
                             <fo:table-column column-width="90pt"/>
                             <fo:table-column column-width="110pt"/>
                             <fo:table-column column-width="140pt"/>
                             <fo:table-column column-width="40pt"/>
                             <fo:table-column column-width="70pt"/>
                             <fo:table-body>
                                 <fo:table-row>
                                     <fo:table-cell><fo:block>${uiLabelMap.ProductLocation}</fo:block></fo:table-cell>
                                     <fo:table-cell><fo:block>${uiLabelMap.ProductItemId}</fo:block></fo:table-cell>
                                     <fo:table-cell><fo:block>${uiLabelMap.ProductProductName}</fo:block></fo:table-cell>
                                     <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_supplierProductId}</fo:block></fo:table-cell>
                                     <fo:table-cell><fo:block>${uiLabelMap.OrderQty}</fo:block></fo:table-cell>
                                     <fo:table-cell><fo:block>${uiLabelMap.OrderUnitPrice}</fo:block></fo:table-cell>
                                </fo:table-row >
                                <#assign totalQty = 0>
                                <#assign rowColor = "#D4D0C8"/>
                                <#list itemInfoList as itemInfo>
                                    <#if itemInfo.get("${orderId}")?? >
                                        <#assign infoItems = itemInfo.get("${orderId}")>
                                        <#list infoItems as infoItem>
                                                <#assign orderItemShipGrpInvRes = infoItem.orderItemShipGrpInvRes>
                                                <#assign quantityToPick = Static["java.lang.Integer"].parseInt("${orderItemShipGrpInvRes.quantity}") >
                                                <#if orderItemShipGrpInvRes.quantityNotAvailable?? >
                                                        <#assign quantityToPick = quantityToPick - Static["java.lang.Integer"].parseInt("${orderItemShipGrpInvRes.quantityNotAvailable}")>
                                                </#if>
                                                <#assign orderItem = orderItemShipGrpInvRes.getRelatedOne("OrderItem", false)>
                                                <#assign product = orderItem.getRelatedOne("Product", false)>
                                                <#assign supplierProduct = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(product.getRelated("SupplierProduct", null, null, false))!>
                                                <#assign inventoryItem = infoItem.inventoryItem>
                                            <#if (quantityToPick > 0)>
                                            <fo:table-row background-color="${rowColor}">
                                                <#if infoItem.facilityLocation?has_content>
                                                    <#assign facilityLocation = infoItem.facilityLocation>
                                                    <fo:table-cell><fo:block font-size="10pt">${facilityLocation.locationSeqId?default("_NA_")}</fo:block></fo:table-cell>
                                                <#else>
                                                    <fo:table-cell><fo:block font-size="10pt">_NA_</fo:block></fo:table-cell>
                                                </#if>
                                                <fo:table-cell><fo:block font-size="10pt">${product.productId} </fo:block></fo:table-cell>
                                                <fo:table-cell><fo:block font-size="10pt">${product.internalName!} </fo:block></fo:table-cell>
                                                <#if supplierProduct?has_content >
                                                    <fo:table-cell><fo:block font-size="10pt">${supplierProduct.supplierProductId!} </fo:block></fo:table-cell>
                                                <#else>
                                                    <fo:table-cell><fo:block font-size="10pt">  </fo:block></fo:table-cell>
                                                </#if>
                                                <#assign totalQty = totalQty + quantityToPick>
                                                <fo:table-cell><fo:block font-size="10pt">${quantityToPick!} </fo:block></fo:table-cell>
                                                <fo:table-cell><fo:block font-size="10pt"><@ofbizCurrency amount=orderItem.unitPrice isoCode=currencyUomId/></fo:block></fo:table-cell>
                                            </fo:table-row>
                                            </#if>
                                            <#if "MARKETING_PKG_AUTO" == product.productTypeId>
                                                <fo:table-row background-color="${rowColor}">
                                                    <fo:table-cell  number-columns-spanned="6">
                                                        <fo:block text-align="left" font-weight="bold">
                                                            ${uiLabelMap.OrderMarketingPackageComposedBy}
                                                        </fo:block>
                                                    </fo:table-cell>
                                                </fo:table-row>
                                                <#assign workOrderItemFulfillments = orderItem.getRelated("WorkOrderItemFulfillment", null, null, false)>
                                                <#if workOrderItemFulfillments?has_content>
                                                    <#assign workOrderItemFulfillment = Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(workOrderItemFulfillments)/>
                                                    <#if workOrderItemFulfillment?has_content>
                                                        <#assign workEffort = workOrderItemFulfillment.getRelatedOne("WorkEffort", false)/>
                                                        <#if workEffort?has_content>
                                                            <#assign workEffortTask = EntityQuery.use(delegator).from("WorkEffort").where("workEffortParentId", workEffort.workEffortId!).queryFirst()!/>
                                                            <#if workEffortTask?has_content>
                                                                <#assign workEffortInventoryAssigns = workEffortTask.getRelated("WorkEffortInventoryAssign", null, null, false)/>
                                                                <#if workEffortInventoryAssigns?has_content>
                                                                    <#list workEffortInventoryAssigns as workEffortInventoryAssign>
                                                                        <#assign inventoryItem = workEffortInventoryAssign.getRelatedOne("InventoryItem", false)/>
                                                                        <#assign product = inventoryItem.getRelatedOne("Product", false)/>
                                                                        <fo:table-row background-color="${rowColor}">
                                                                            <#-- bin location -->
                                                                            <fo:table-cell ><fo:block font-size="10pt"><#if inventoryItem??>${inventoryItem.locationSeqId?default("_NA_")}</#if></fo:block></fo:table-cell>

                                                                            <#-- product ID -->
                                                                            <#if product?has_content>
                                                                                <fo:table-cell ><fo:block font-size="10pt">${product.productId}</fo:block></fo:table-cell>
                                                                            <#else>
                                                                                <fo:table-cell ><fo:block font-size="10pt">[N/A]</fo:block></fo:table-cell>
                                                                            </#if>

                                                                            <#-- product name -->
                                                                            <#if product?has_content>
                                                                                <fo:table-cell ><fo:block font-size="10pt">${product.productName?default(product.internalName?default("[Not Internal Name Set!]"))?xml}</fo:block></fo:table-cell>
                                                                            <#else>
                                                                                <fo:table-cell ><fo:block font-size="10pt">[N/A]</fo:block></fo:table-cell>
                                                                            </#if>

                                                                            <#-- supplier -->
                                                                            <#if vendor?has_content > 
                                                                                <fo:table-cell><fo:block font-size="10pt">${vendor.supplierProductId!}</fo:block></fo:table-cell> 
                                                                            <#else>
                                                                                <fo:table-cell><fo:block font-size="10pt"> </fo:block></fo:table-cell>
                                                                            </#if>

                                                                            <#-- quantity -->
                                                                            <fo:table-cell><fo:block font-size="10pt">${workEffortInventoryAssign.quantity!}</fo:block></fo:table-cell>

                                                                            <#-- unit price -->
                                                                            <fo:table-cell ><fo:block></fo:block></fo:table-cell>
                                                                        </fo:table-row>
                                                                    </#list>
                                                                </#if>
                                                            </#if>
                                                        </#if>
                                                    </#if>
                                                </#if>
                                            </#if>
                                            <#if "#D4D0C8" == rowColor>
                                                 <#assign rowColor = "white"/>
                                            <#else>
                                                <#assign rowColor = "#D4D0C8"/>  
                                            </#if>
                                        </#list>
                                    </#if>
                                </#list>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>

                     <fo:block text-align="right">
                         <fo:table>
                             <fo:table-column column-width="425pt"/>
                             <fo:table-column column-width="100pt"/>
                             <fo:table-body>
                                 <#list orderHeaderAdjustments as orderHeaderAdjustment>
                                     <#assign adjustmentType = orderHeaderAdjustment.getRelatedOne("OrderAdjustmentType", false)>
                                     <#assign adjustmentAmount = Static["org.apache.ofbiz.order.order.OrderReadHelper"].calcOrderAdjustment(orderHeaderAdjustment, orderSubTotal)>
                                     <#if adjustmentAmount != 0>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${adjustmentType.get("description",locale)}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=adjustmentAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                     </#if>
                                 </#list>
                                 <#list orderChargeList as orderCharge>
                                     <#if orderCharge.get("${orderId}")?? >
                                         <#assign charges = orderCharge.get("${orderId}")>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${uiLabelMap.OrderSubTotal}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=charges.orderSubTotal isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${uiLabelMap.OrderTotalSalesTax}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=charges.taxAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${uiLabelMap.OrderTotalShippingAndHandling}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=charges.shippingAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${uiLabelMap.OrderTotalOtherOrderAdjustments}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=charges.otherAdjAmount isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                         <fo:table-row>
                                             <fo:table-cell><fo:block>${uiLabelMap.OrderGrandTotal}:</fo:block></fo:table-cell>
                                             <fo:table-cell><fo:block><@ofbizCurrency amount=charges.grandTotal isoCode=currencyUomId/></fo:block></fo:table-cell>
                                         </fo:table-row>
                                         <fo:table-row>
                                              <fo:table-cell><fo:block text-align="left"> ${uiLabelMap.OrderPickedBy}: ______________</fo:block></fo:table-cell>
                                          </fo:table-row>
                                          <fo:table-row>
                                              <fo:table-cell><fo:block text-align="center"> ${uiLabelMap.OrderTotalNoOfItems}: ${totalQty}</fo:block></fo:table-cell>
                                          </fo:table-row>
                                     </#if>
                                 </#list>
                             </fo:table-body>
                         </fo:table>
                     </fo:block>
                 </fo:flow>
             </fo:page-sequence>
         </#list>
     </fo:root>
 </#escape>
