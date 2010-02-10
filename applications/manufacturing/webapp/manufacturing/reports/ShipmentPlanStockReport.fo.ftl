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
        <fo:simple-page-master master-name="main" page-height="11.694in" page-width="8.264in"
                margin-top="0.278in" margin-bottom="0.278in" margin-left="0.417in" margin-right="0.417in">
            <fo:region-body margin-top="1in"/>
            <fo:region-before extent="1in"/>
            <fo:region-after extent="1in"/>
        </fo:simple-page-master>
    </fo:layout-master-set>
    <#if hasPermission>
        <#if records?has_content>
            <fo:page-sequence master-reference="main">
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block text-align="right" line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                        ${uiLabelMap.CommonPage} <fo:page-number/>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
                    <fo:block><fo:leader/></fo:block>
                    <fo:block font-size="14pt">${uiLabelMap.ManufacturingComponentsOfShipmentPlan}</fo:block>
                    <fo:block><fo:leader/></fo:block>
                    <fo:block space-after.optimum="10pt" font-size="10pt"/>

                    <fo:block>${uiLabelMap.ManufacturingShipmentId}: ${shipmentIdPar}</fo:block>
                    <fo:block>${uiLabelMap.ManufacturingEstimatedCompletionDate}: <#if estimatedReadyDatePar?has_content>${estimatedReadyDatePar}</#if></fo:block>
                    <fo:block>${uiLabelMap.ManufacturingEstimatedShipDate}: <#if estimatedShipDatePar?has_content>${estimatedShipDatePar}</#if></fo:block>

                    <fo:block space-after.optimum="10pt" font-size="10pt"/>
                    <fo:table>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-column column-width="120pt"/>
                        <fo:table-header>
                            <fo:table-row font-weight="bold">
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.OrderOrderId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ManufacturingProductsComponents}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.CommonDescription}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ManufacturingNeedQuantity}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt" background-color="#D4D0C8">
                                    <fo:block>${uiLabelMap.ProductQoh}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <#list records as record>
                                <fo:table-row>
                                    <fo:table-cell padding="2pt">
                                        <fo:block>
                                            ${record.get("ORDER_ID")}/${record.get("ORDER_ITEM_SEQ_ID")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="left">
                                            ${record.get("PRODUCT_ID")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="left">
                                            ${record.get("PRODUCT_NAME")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">
                                            ${record.get("QUANTITY")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block space-after.optimum="10pt" font-size="10pt"/>
                                    </fo:table-cell>
                                </fo:table-row>
                                <fo:table-row>
                                    <fo:table-cell padding="2pt">
                                        <fo:block space-after.optimum="10pt" font-size="10pt"/>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="left">
                                            ${record.get("componentId")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="left">
                                            ${record.get("componentName")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">
                                            ${record.get("componentQuantity")}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="center">
                                            ${record.get("componentOnHand")}
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </#list>
                        </fo:table-body>
                    </fo:table>
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
