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
<#if security.hasEntityPermission("ORDERMGR", "_PURCHASE_VIEW", session)>

<#if productReportList?has_content>
        <fo:page-sequence master-reference="main">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
            <fo:block font-size="14pt">${uiLabelMap.OrderReportPurchasesByOrganization}</fo:block>
            <#if partyIds?has_content>
            <fo:block font-size="10pt">${uiLabelMap.CommonFor}:
                <#list partyIds as toPartyId>
                    ${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, toPartyId, false)!} <#sep>,
                </#list>
            </fo:block>
            </#if>
            <#if fromPartyId?has_content><fo:block font-size="10pt">${uiLabelMap.CommonFrom}: ${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, fromPartyId, false)!}</fo:block></#if>
            <fo:block font-size="10pt">${uiLabelMap.FormFieldTitle_orderStatusId}:
                <#if orderStatusIds?has_content>
                  <#list orderStatusIds as orderStatusId>
                    <#assign statusItem = delegator.findOne("StatusItem", {"statusId" : orderStatusId}, false)!/>
                    ${statusItem.description}
                  </#list>
                <#else>
                  ${uiLabelMap.CommonAny}
                </#if>
            </fo:block>
            <#if parameters.fromOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonFromDate}: ${parameters.fromOrderDate} (${uiLabelMap.OrderDate} &gt;= ${uiLabelMap.CommonFrom})</fo:block></#if>
            <#if parameters.thruOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonThruDate}: ${parameters.thruOrderDate} (${uiLabelMap.OrderDate} &lt; ${uiLabelMap.CommonFrom})</fo:block></#if>
            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <fo:table-column column-width="420pt"/>
                <fo:table-column column-width="40pt"/>
                <fo:table-column column-width="40pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderQuantityPurchase}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderValuePurchase}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list productReportList as productReport>
                        <fo:table-row>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.internalName!} (${productReport.productId!})</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.quantity!}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.unitPrice!}</fo:block>
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
            ${uiLabelMap.OrderNoOrderFound}.
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
