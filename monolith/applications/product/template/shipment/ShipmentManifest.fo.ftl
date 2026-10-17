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
    <#if hasPermission>
        <#list shipmentPackageDatas as shipmentPackageData>
            <#assign shipmentRouteSegment = shipmentPackageData.get("shipmentRouteSegment")>
            <#assign shipmentPackage = shipmentPackageData.get("shipmentPackage")>
            <#assign shipmentItemsDatas = shipmentPackageData.get("shipmentItemsDatas")>
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
                                     <#assign carrierParty = shipmentRouteSegment.getRelatedOne("CarrierParty", false)!>
                                    <fo:block text-align="center">${uiLabelMap.ProductCarrier}: <#if carrierParty.description?has_content>${carrierParty.description}<#else>${carrierParty.partyId}</#if></fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <#assign shipmentMethodType = shipmentRouteSegment.getRelatedOne("ShipmentMethodType", false)!>
                                    <fo:block text-align="right">${uiLabelMap.ProductShipmentMethod}: <#if shipmentMethodType?has_content>${shipmentMethodType.description}<#else>${uiLabelMap.CommonNA}</#if></fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <fo:table-row>
                                <fo:table-cell padding="2pt">
                                    <fo:block>
                                        <#if originPostalAddress?has_content>
                                          <fo:block>${uiLabelMap.CommonFrom}: ${originPostalAddress.toName!}</fo:block>
                                          <#if originPostalAddress.attnName?has_content>
                                            <fo:block>${uiLabelMap.CommonAttn}: ${originPostalAddress.attnName!}</fo:block>
                                          </#if>
                                          <fo:block>${originPostalAddress.address1!}</fo:block>
                                          <fo:block>${originPostalAddress.address2!}</fo:block>
                                          <fo:block>
                                            ${originPostalAddress.city!}<#if originPostalAddress.stateProvinceGeoId?has_content>, ${originPostalAddress.stateProvinceGeoId}</#if>
                                            ${originPostalAddress.postalCode!} ${originPostalAddress.countryGeoId!}
                                          </fo:block>
                                        </#if>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">
                                    <fo:block text-align="center">
                                        <#if destinationPostalAddress?has_content>
                                          <fo:block>${uiLabelMap.CommonTo}: ${destinationPostalAddress.toName!}</fo:block>
                                          <#if destinationPostalAddress.attnName?has_content>
                                            <fo:block>${uiLabelMap.CommonAttn}: ${destinationPostalAddress.attnName!}</fo:block>
                                          </#if>
                                          <fo:block>${destinationPostalAddress.address1!}</fo:block>
                                          <fo:block>${destinationPostalAddress.address2!}</fo:block>
                                          <fo:block>
                                            ${destinationPostalAddress.city!}<#if destinationPostalAddress.stateProvinceGeoId?has_content>, ${destinationPostalAddress.stateProvinceGeoId}</#if>
                                            ${destinationPostalAddress.postalCode!} ${destinationPostalAddress.countryGeoId!}
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
                                    <fo:block text-align="center">
                                    <#if shipmentPackage.weight?has_content && shipmentPackage.weightUomId?has_content>
                                        <#assign weightUom = shipmentPackage.getRelatedOne("WeightUom", false)>
                                        ${uiLabelMap.ProductWeight}: ${shipmentPackage.weight} ${weightUom.get("description",locale)}
                                    </#if>
                                    </fo:block>
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
                                <#assign product = shipmentItem.getRelatedOne("Product", false)>
                                <#assign itemIssuances = shipmentItem.getRelated("ItemIssuance", null, null, false)>
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
        </#list>
    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>
</#escape>
