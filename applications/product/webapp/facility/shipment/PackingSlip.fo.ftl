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
        <#assign shipGroup = shipment.getRelatedOne("PrimaryOrderItemShipGroup")?if_exists>
        <#assign carrier = (shipGroup.carrierPartyId)?default("N/A")>
        <fo:page-sequence master-reference="main">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">

            <#list packages as package>

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

            <fo:block font-size="14pt">${uiLabelMap.ProductShipmentId} #${shipmentId} / Package ${package_index + 1}</fo:block>
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
                <fo:table-column column-width="67pt"/>
                <fo:table-column column-width="67pt"/>
                <fo:table-column column-width="67pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>Requested</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>In this Package</fo:block></fo:table-cell>
                        <fo:table-cell padding="2pt" background-color="#D4D0C8"><fo:block>Total Shipped</fo:block></fo:table-cell>
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
                    <#if shipGroup.giftMessage?exists >
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

        </fo:flow>
        </fo:page-sequence>

    <#else>
        <fo:block font-size="14pt">
            ${uiLabelMap.ProductFacilityViewPermissionError}
        </fo:block>
    </#if>

</fo:root>
</#escape>
