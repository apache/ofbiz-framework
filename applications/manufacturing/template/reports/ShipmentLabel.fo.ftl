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
                margin-top="0.278in" margin-bottom="0.278in" margin-left="0.278in" margin-right="0.278in">
            <fo:region-body margin-top="1in"/>
            <fo:region-before extent="1in"/>
            <fo:region-after extent="1in"/>
        </fo:simple-page-master>
    </fo:layout-master-set>
    <#if hasPermission>
        <#if records?has_content>
            <fo:page-sequence master-reference="main">
                <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
                    <#assign index = 1>
                    <#list records as record>
                        <#if index == 1>
                               <fo:table table-layout="fixed" border="0.5pt solid black">
                                <fo:table-column column-width="252pt"/>
                                <fo:table-body>
                                    <fo:table-row>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${uiLabelMap.ManufacturingShipTo}:</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                    <fo:table-row>
                                        <fo:table-cell padding="2pt">
                                            <fo:block font-size="18pt">${record.get("shippingAddressName")?if_exists}</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                    <fo:table-row>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${record.get("shippingAddressAddress")?if_exists}</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                    <fo:table-row>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${record.get("shippingAddressCity")?if_exists}</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                </fo:table-body>
                            </fo:table>
                            <fo:block space-after.optimum="10pt" font-size="10pt"/>
                            <fo:table>
                                <fo:table-column column-width="63pt"/>
                                <fo:table-column column-width="63pt"/>
                                <fo:table-column column-width="93pt"/>
                                <fo:table-column column-width="33pt"/>
                                <fo:table-body border="0.5pt solid black">
                                    <fo:table-row>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${uiLabelMap.OrderOrderId}</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${uiLabelMap.ProductProductId}</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${uiLabelMap.CommonDescription}</fo:block>
                                        </fo:table-cell>
                                        <fo:table-cell padding="2pt">
                                            <fo:block>${uiLabelMap.CommonQuantity}</fo:block>
                                        </fo:table-cell>
                                    </fo:table-row>
                                </fo:table-body>
                            </fo:table>
                        </#if>
                        <fo:table>
                            <fo:table-column column-width="63pt"/>
                            <fo:table-column column-width="63pt"/>
                            <fo:table-column column-width="93pt"/>
                            <fo:table-column column-width="33pt"/>
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell padding="2pt">
                                        <fo:block>
                                            ${record.get("orderId")?if_exists} ${record.get("orderItemSeqId")?if_exists}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block>
                                            ${record.get("productId")?if_exists}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block>
                                            ${record.get("productName")?if_exists}
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">
                                        <fo:block text-align="right">
                                            ${record.get("quantity")?if_exists}
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                        <#assign shipmentPackageSeqId = record.get("shipmentPackageSeqId")>
                           <#if estimatedReadyDatePar?has_content>
                               <#assign shipDate = record.get("shipDate")>
                           </#if>
                           <#assign index = index + 1>
                    </#list>
                    <fo:table table-layout="fixed" border="0.5pt solid black">
                        <fo:table-column column-width="84pt"/>
                        <fo:table-column column-width="84pt"/>
                        <fo:table-column column-width="84pt"/>
                        <fo:table-body>
                            <fo:table-row>
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ManufacturingPackage}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ProductShipmentPlan}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ManufacturingEstimatedShipDate}</fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <fo:table-row>
                                <fo:table-cell padding="2pt">
                                    <fo:block>
                                        ${shipmentIdPar?if_exists}/${shipmentPackageSeqId?if_exists}
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">
                                    <fo:block>
                                        ${shipmentIdPar?if_exists}
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">
                                    <fo:block>
                                        <#if shipDate?has_content>${shipDate?if_exists}</#if>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
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
