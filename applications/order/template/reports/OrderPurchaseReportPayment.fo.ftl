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
<#assign showProductStore = !parameters.productStoreId?has_content>
<#assign showOriginFacility = !parameters.originFacilityId?has_content>
<#assign showTerminal = !parameters.terminalId?has_content>
<#assign showStatus = !parameters.statusId?has_content>

<fo:layout-master-set>
    <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
            margin-top="0.5in" margin-bottom="1in" margin-left="0.5in" margin-right="0.5in">
        <fo:region-body margin-top="1in"/>
        <fo:region-before extent="1in"/>
        <fo:region-after extent="1in"/>
    </fo:simple-page-master>
</fo:layout-master-set>
<#if security.hasEntityPermission("ORDERMGR", "_VIEW", session)>

<#if orderPurchasePaymentSummaryList?has_content>
        <fo:page-sequence master-reference="main">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
            <fo:block font-size="14pt">${uiLabelMap.OrderReportPurchasesByPaymentMethod}</fo:block>
            <#if !showProductStore><fo:block font-size="10pt">${uiLabelMap.CommonFor} ${uiLabelMap.ProductProductStore}: ${parameters.productStoreId}</fo:block></#if>
            <#if !showOriginFacility><fo:block font-size="10pt">${uiLabelMap.CommonFor} ${uiLabelMap.FormFieldTitle_originFacilityId}: ${parameters.originFacilityId}</fo:block></#if>
            <#if !showTerminal><fo:block font-size="10pt">${uiLabelMap.CommonFor} ${uiLabelMap.FormFieldTitle_terminalId}: ${parameters.terminalId}</fo:block></#if>
            <#if !showStatus>
              <fo:block font-size="10pt">
                ${uiLabelMap.CommonFor} ${uiLabelMap.CommonStatus}: 
                <#list statusIds as statusId>
                  <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : statusId}, false)!/>
                  ${statusItem.description}
                </#list>
              </fo:block>
            </#if>
            <#if parameters.fromOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonFromDate}: ${parameters.fromOrderDate} (${uiLabelMap.OrderDate} &gt;= ${uiLabelMap.CommonFrom})</fo:block></#if>
            <#if parameters.thruOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonThruDate}: ${parameters.thruOrderDate} (${uiLabelMap.OrderDate} &lt; ${uiLabelMap.CommonFrom})</fo:block></#if>
            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <#if showProductStore><fo:table-column column-width="50pt"/></#if>
                <#if showOriginFacility><fo:table-column column-width="80pt"/></#if>
                <#if showTerminal><fo:table-column column-width="50pt"/></#if>
                <#if showStatus><fo:table-column column-width="110pt"/></#if>
                <fo:table-column column-width="100pt"/>
                <fo:table-column column-width="60pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <#if showProductStore><fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.FormFieldTitle_productStoreId}</fo:block></fo:table-cell></#if>
                        <#if showOriginFacility><fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.FormFieldTitle_facilityId}</fo:block></fo:table-cell></#if>
                        <#if showTerminal><fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.FormFieldTitle_terminalId}</fo:block></fo:table-cell></#if>
                        <#if showStatus><fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.CommonStatus}</fo:block></fo:table-cell></#if>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.FormFieldTitle_paymentMethodId}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderAmount}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list orderPurchasePaymentSummaryList as orderPurchasePaymentSummary>
                        <fo:table-row>
                            <#if showProductStore>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${orderPurchasePaymentSummary.productStoreId!}</fo:block>
                                </fo:table-cell>
                            </#if>
                            <#if showOriginFacility>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${orderPurchasePaymentSummary.originFacilityId!}</fo:block>
                                </fo:table-cell>
                            </#if>
                            <#if showTerminal>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${orderPurchasePaymentSummary.terminalId!}</fo:block>
                                </fo:table-cell>
                            </#if>
                            <#if showStatus>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${orderPurchasePaymentSummary.statusId!}</fo:block>
                                </fo:table-cell>
                            </#if>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${orderPurchasePaymentSummary.get("description",locale)!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${orderPurchasePaymentSummary.maxAmount!}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#-- toggle the row color -->
                        <#if "white" == rowColor>
                            <#assign rowColor = "#D4D0C8">
                        <#else>
                            <#assign rowColor = "white">
                        </#if>
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
            ${uiLabelMap.OrderNoPurchasePaymentMethod}
        </fo:block>
    </fo:flow>
    </fo:page-sequence>
</#if>

<#else>
    <fo:block font-size="14pt">
        ${uiLabelMap.OrderViewPermissionError}
    </fo:block>
</#if>

</fo:root>
</#escape>
