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
    <fo:layout-master-set>
        <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
                margin-top="0.5in" margin-bottom="1in" margin-left="1in" margin-right="1in">
            <fo:region-body margin-top="1in"/>
            <fo:region-before extent="1in"/>
            <fo:region-after extent="1in"/>
        </fo:simple-page-master>
    </fo:layout-master-set>
    <#if hasPermission>
        <#list shipmentPackageDatas as shipmentPackageData>
            <#assign shipmentRouteSegment = shipmentPackageData.get("shipmentRouteSegment")>
            <#assign shipmentPackage = shipmentPackageData.get("shipmentPackage")>
            <#assign shipmentItemsDatas = shipmentPackageData.get("shipmentItemsDatas")>   
            <fo:page-sequence master-reference="main">
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block text-align="right" line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                        ${uiLabelMap.CommonPage} <fo:page-number/>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
                    <fo:block>
                         ${screens.render("component://order/widget/ordermgr/OrderPrintForms.xml#CompanyLogo")}
                    </fo:block>
                    <fo:block text-align="right">
                        <fo:instream-foreign-object>
                            <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                    message="${shipment.shipmentId}">
                                <barcode:code39>
                                    <barcode:height>8mm</barcode:height>
                                </barcode:code39>
                            </barcode:barcode>
                        </fo:instream-foreign-object>
                    </fo:block>
                    <fo:block><fo:leader/></fo:block>
                    <fo:block font-size="14pt">${uiLabelMap.ProductShipmentManifest} #${shipmentId}</fo:block>
                    <fo:block><fo:leader/></fo:block>              
                    <fo:block space-after.optimum="10pt" font-size="10pt">
                    <fo:table>
                        <fo:table-column column-width="150pt"/>
                        <fo:table-column column-width="150pt"/>
                        <fo:table-column column-width="150pt"/>
                        <fo:table-header>
                            <fo:table-row font-weight="bold">
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductRouteSegment}: ${shipmentRouteSegment.shipmentRouteSegmentId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                     <#assign carrierParty = shipmentRouteSegment.getRelatedOne("CarrierParty")?if_exists>
                                    <fo:block text-align="center">${uiLabelMap.ProductCarrier}: <#if carrierParty.description?has_content>${carrierParty.description}<#else>${carrierParty.partyId}</#if></fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <#assign shipmentMethodType = shipmentRouteSegment.getRelatedOne("ShipmentMethodType")?if_exists>
                                    <fo:block text-align="right">${uiLabelMap.ProductShipmentMethod}: <#if shipmentMethodType?has_content>${shipmentMethodType.description}<#else>${uiLabelMap.CommonNA}</#if></fo:block>                              
                                </fo:table-cell>    
                            </fo:table-row>
                        </fo:table-header>                
                        <fo:table-body>
                            <fo:table-row>
                                <fo:table-cell padding="2pt">
                                    <fo:block>
                                        <#if originPostalAddress?has_content>
                                          <fo:block>${uiLabelMap.CommonFrom}: ${originPostalAddress.toName?if_exists}</fo:block>
                                          <#if originPostalAddress.attnName?has_content>
                                            <fo:block>${uiLabelMap.CommonAttn}: ${originPostalAddress.attnName?if_exists}</fo:block>
                                          </#if>
                                          <fo:block>${originPostalAddress.address1?if_exists}</fo:block>
                                          <fo:block>${originPostalAddress.address2?if_exists}</fo:block>
                                          <fo:block>
                                            ${originPostalAddress.city?if_exists}<#if originPostalAddress.stateProvinceGeoId?has_content>, ${originPostalAddress.stateProvinceGeoId}</#if>
                                            ${originPostalAddress.postalCode?if_exists} ${originPostalAddress.countryGeoId?if_exists}
                                          </fo:block>
                                        </#if>                                
                                    </fo:block>
                                </fo:table-cell>                       
                                <fo:table-cell padding="2pt">
                                    <fo:block text-align="center">
                                        <#if destinationPostalAddress?has_content>
                                          <fo:block>${uiLabelMap.CommonTo}: ${destinationPostalAddress.toName?if_exists}</fo:block>
                                          <#if destinationPostalAddress.attnName?has_content>
                                            <fo:block>${uiLabelMap.CommonAttn}: ${destinationPostalAddress.attnName?if_exists}</fo:block>
                                          </#if>
                                          <fo:block>${destinationPostalAddress.address1?if_exists}</fo:block>
                                          <fo:block>${destinationPostalAddress.address2?if_exists}</fo:block>
                                          <fo:block>
                                            ${destinationPostalAddress.city?if_exists}<#if destinationPostalAddress.stateProvinceGeoId?has_content>, ${destinationPostalAddress.stateProvinceGeoId}</#if>
                                            ${destinationPostalAddress.postalCode?if_exists} ${destinationPostalAddress.countryGeoId?if_exists}
                                          </fo:block>
                                        </#if>
                                    </fo:block>
                                </fo:table-cell>                  
                            </fo:table-row>
                        </fo:table-body>
                    </fo:table>                                              
                    </fo:block>                
                      <fo:block space-after.optimum="10pt" font-size="10pt">
                    <fo:table>
                        <fo:table-column column-width="225pt"/>
                        <fo:table-column column-width="225pt"/>
                        <fo:table-body>
                            <fo:table-row font-weight="bold">
                                   <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.FormFieldTitle_shipmentPackageSeqId}: ${shipmentPackage.shipmentPackageSeqId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">                              
                                    <#if shipmentPackage.weight?has_content && shipmentPackage.weightUomId?has_content>
                                        <#assign weightUom = shipmentPackage.getRelatedOne("WeightUom")>
                                        <fo:block text-align="center">${uiLabelMap.ProductWeight}: ${shipmentPackage.weight} ${weightUom.get("description",locale)}</fo:block>
                                    </#if>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-body>                                         
                    </fo:table>                                                              
                    </fo:block>                                    
                    <fo:table>
                        <fo:table-column column-width="180pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="90pt"/>
                        <fo:table-header>
                            <fo:table-row font-weight="bold">
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductProductId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductQuantityShipped}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductPackedQty}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductIssuedQuantity}</fo:block>
                                </fo:table-cell>                            
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.FormFieldTitle_orderItemSeqId}</fo:block>
                                </fo:table-cell>                            
                            </fo:table-row>
                        </fo:table-header> 
                        <fo:table-body>                    
                               <#list shipmentItemsDatas as shipmentItemsData>         
                                   <#assign shipmentItem = shipmentItemsData.get("shipmentItem")>                      
                                  <#assign shippedQuantity = shipmentItemsData.get("shippedQuantity")>                      
                                 <#assign packageQuantity = shipmentItemsData.get("packageQuantity")>                                                                         
                                <#assign product = shipmentItem.getRelatedOne("Product")>  
                                <#assign itemIssuances = shipmentItem.getRelated("ItemIssuance")>  
                                <fo:table-row>
                                       <fo:table-cell padding="2pt">
                                        <fo:block>${product.internalName} [${shipmentItem.productId}]</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${shippedQuantity}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">${packageQuantity}</fo:block>
                                    </fo:table-cell>    
                                </fo:table-row>                              
                                <#list itemIssuances as itemIssuance>                                     
                                       <fo:table-row>
                                           <fo:table-cell padding="2pt">
                                            <fo:block> </fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block> </fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block> </fo:block>
                                        </fo:table-cell>
                                           <fo:table-cell padding="2pt">
                                            <fo:block text-align="center">${itemIssuance.quantity}</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${itemIssuance.orderId}:${itemIssuance.orderItemSeqId}</fo:block>
                                        </fo:table-cell>
                                       </fo:table-row>
                                </#list>  
                            </#list>  
                        </fo:table-body>                       
                    </fo:table>                            
                </fo:flow>
            </fo:page-sequence>
        </#list>               
    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>
</fo:root>
</#escape>
