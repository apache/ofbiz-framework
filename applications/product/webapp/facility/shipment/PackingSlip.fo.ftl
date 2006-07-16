<?xml version="1.0" encoding="UTF-8" ?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<fo:layout-master-set>
    <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
            margin-top="0.5in" margin-bottom="1in" margin-left="1in" margin-right="1in">
        <fo:region-body margin-top="1in"/>
        <fo:region-before extent="1in"/>
        <fo:region-after extent="1in"/>
    </fo:simple-page-master>
</fo:layout-master-set>

    <#if hasPermission>
        <#assign rowColor = "white">
        <#assign shipGroup = shipment.getRelatedOne("PrimaryOrderItemShipGroup")?if_exists>
        <#assign carrier = (shipGroup.carrierPartyId)?default("N/A")>
        <fo:page-sequence master-reference="main">
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

            <fo:block font-size="14pt">${uiLabelMap.ProductShipmentId} #${shipmentId}</fo:block>
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
                        <fo:table-cell padding="2pt">
                            <fo:block text-align="center">
                                <#if carrier != "_NA_">
                                   ${carrier}
                                </#if>
                                ${shipGroup.shipmentMethodTypeId?default("??")}
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block text-align="right">${shipment.handlingInstructions?if_exists}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
            </fo:block>

            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="250pt"/>
                <fo:table-column column-width="100pt"/>
                <fo:table-column column-width="100pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityRequested}</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductQuantityShipped}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#list shipmentItemDatas as shipmentItem>
                        <#assign itemIssuances = shipmentItem.itemIssuances>
                        <#list itemIssuances as issue>
                            <#assign orderItem = issue.getRelatedOne("OrderItem")>
                            <#assign product = shipmentItem.product>
                            <fo:table-row>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <#if product?has_content>
                                        <fo:block>${product.internalName?default("Internal Name Not Set!")?xml} [${product.productId}]</fo:block>
                                    <#else/>
                                        <fo:block>&nbsp;</fo:block>
                                    </#if>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${orderItem.quantity}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${issue.quantity}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <#-- toggle the row color -->
                            <#if rowColor == "white">
                                <#assign rowColor = "#CCCCCC">
                            <#else>
                                <#assign rowColor = "white">
                            </#if>
                        </#list>
                    </#list>
                </fo:table-body>
            </fo:table>
            </fo:block>
        </fo:flow>
        </fo:page-sequence>

    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>

</fo:root>

