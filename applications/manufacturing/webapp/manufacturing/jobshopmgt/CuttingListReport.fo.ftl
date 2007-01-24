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
<#-- do not display columns associated with values specified in the request, ie constraint values -->
<#--<#assign showProductStore = !parameters.productStoreId?has_content>-->
<fo:layout-master-set>
    <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
            margin-top="0.5in" margin-bottom="1in" margin-left="0.5in" margin-right="0.5in">
        <fo:region-body margin-top="1in"/>
        <fo:region-before extent="1in"/>
        <fo:region-after extent="1in"/>
    </fo:simple-page-master>
</fo:layout-master-set>
<#if security.hasEntityPermission("MANUFACTURING", "_VIEW", session)>

<#if cuttingList?has_content>
        <fo:page-sequence master-reference="main">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
            <fo:block font-size="16pt">${uiLabelMap.ManufacturingCuttingList}</fo:block>
            <fo:block font-size="12pt">${uiLabelMap.ProductShipmentPlan}: ${shipmentId} ${uiLabelMap.ManufacturingDeliveryDate}: ${shipment.estimatedShipDate?if_exists}</fo:block>
            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="60pt"/>
                <fo:table-column column-width="60pt"/>
                <fo:table-column column-width="80pt"/>
                <fo:table-column column-width="120pt"/>
                <fo:table-column column-width="180pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductWidth}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductHeight}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductDescription}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign dimColor = "#D4D0C8">
                    <#assign rowColor = "white">

                    <#list cuttingList as dimension>
                        <fo:table-row>
                            <fo:table-cell padding="2pt" background-color="${dimColor}">
                                <fo:block>${dimension.width?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${dimColor}">
                                <fo:block>${dimension.height?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${dimColor}">
                                <fo:block>${dimension.quantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${dimColor}">
                                <fo:block></fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${dimColor}">
                                <fo:block></fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#assign products = dimension.products>
                            <#list products as product>
                                <fo:table-row>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block></fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block></fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${product.quantity?if_exists}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${product.product.productId?if_exists}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${product.product.internalName?if_exists}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            </#list>
                    </#list>          
                </fo:table-body>
            </fo:table>
            </fo:block>
        </fo:flow>
        </fo:page-sequence>
<#else>
    <fo:page-sequence master-reference="main">
    <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
        <fo:block font-size="14pt">
            ${uiLabelMap.ManufacturingNoDataAvailable}
        </fo:block>
    </fo:flow>
    </fo:page-sequence>
</#if>

<#else>
    <fo:block font-size="14pt">
        ${uiLabelMap.ManufacturingViewPermissionError}
    </fo:block>
</#if>

</fo:root>
</#escape>
