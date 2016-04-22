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
        <#assign shipGroup = shipment.getRelatedOne("PrimaryOrderItemShipGroup", false)!>
        <#assign carrier = (shipGroup.carrierPartyId)?default("N/A")>
            <#if packages?has_content>
            <#list packages as package>

            <fo:block><fo:leader/></fo:block>

            <fo:block font-size="14pt">${uiLabelMap.ProductShipmentId} #${shipmentId} / ${uiLabelMap.ProductPackage} ${package_index + 1}<#if (packages?size > 1)> of ${packages?size}</#if></fo:block>
            <fo:block font-size="12pt">${uiLabelMap.ProductOrderId} #${shipment.primaryOrderId?default("N/A")} / ${shipment.primaryShipGroupSeqId?default("N/A")}</fo:block>
            <fo:block><fo:leader/></fo:block>

            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="150pt"/>
                <fo:table-column column-width="150pt"/>
                <fo:table-column column-width="150pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell padding="2pt" background-color="#D4D0C8">
                            <fo:block>${uiLabelMap.ProductShippingAddress}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8">
                            <fo:block text-align="center">${uiLabelMap.ProductShipmentMethod}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8">
                            <fo:block text-align="right">${uiLabelMap.ProductHandlingInstructions}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:block>
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
                        <fo:table-cell padding="2pt">
                            <fo:block text-align="center">
                                <#if carrier != "_NA_">
                                   ${carrier}
                                </#if>
                                <#if (shipGroup.shipmentMethodTypeId)??>
                                  ${(shipGroup.getRelatedOne("ShipmentMethodType", false).get("description", locale))?default(shipGroup.shipmentMethodTypeId)}
                                </#if>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block text-align="right">${shipment.handlingInstructions!}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
            </fo:block>

            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="250pt"/>
                <#if (packages?size > 1)>
                    <fo:table-column column-width="58pt"/>
                    <fo:table-column column-width="45pt"/>
                    <fo:table-column column-width="50pt"/>
                    <fo:table-column column-width="47pt"/>
                <#else>
                    <fo:table-column column-width="67pt"/>
                    <fo:table-column column-width="67pt"/>
                    <fo:table-column column-width="67pt"/>
                </#if>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityRequested}</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityOfPackage}</fo:block></fo:table-cell>
                        <#if (packages?size > 1)><fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityShippedOfPackage}</fo:block></fo:table-cell></#if>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityShipped}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#list package as line>
                            <#if ((line_index % 2) == 0)>
                                <#assign rowColor = "white">
                            <#else>
                                <#assign rowColor = "#CCCCCC">
                            </#if>

                            <fo:table-row>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <#if line.product?has_content>
                                        <fo:block>${line.product.internalName?default("Internal Name Not Set!")} [${line.product.productId}]</fo:block>
                                    <#else/>
                                        <fo:block>${line.getClass().getName()}&nbsp;</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${line.quantityRequested?default(0)}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${line.quantityInPackage?default(0)}</fo:block>
                                </fo:table-cell>
                                <#if (packages?size > 1)>
                                    <fo:table-cell padding="2pt" background-color="${rowColor}">
                                        <fo:block>${line.quantityInShipment?default(0)}</fo:block>
                                    </fo:table-cell>
                                </#if>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${line.quantityShipped?default(0)}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                    </#list>
                </fo:table-body>
            </fo:table>
            </fo:block>

            <#if shipGroup.giftMessage?has_content>
            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="450pt"/>
                <fo:table-body>
                    <#if shipGroup.giftMessage?? >
                        <fo:table-row font-weight="bold">
                            <fo:table-cell>
                                <fo:block>${uiLabelMap.OrderGiftMessage}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <fo:table-row >
                            <fo:table-cell>
                                <fo:block>${shipGroup.giftMessage}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#if>
                </fo:table-body>
            </fo:table>
            </fo:block>
          </#if>


            <#if package_has_next><fo:block break-before="page"/></#if>
            </#list> <#-- packages -->
            <#else>
                <fo:block font-size="14pt">
                    ${uiLabelMap.ProductErrorNoPackagesFoundForShipment}
                </fo:block>
            </#if>

    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>
</#escape>
