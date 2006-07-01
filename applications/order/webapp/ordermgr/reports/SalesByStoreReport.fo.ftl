<?xml version="1.0" encoding="UTF-8" ?>
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

<#--
 *  Copyright (c) 2004 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Leon Torres (leon@opensourcestrategies.com)
 *@version    $Rev:$
 *@since      3.2
-->

<#-- do not display columns associated with values specified in the request, ie constraint values -->
<#assign showProductStore = !parameters.productStoreId?has_content>
<#assign showToParty = !parameters.toPartyId?has_content>

<fo:layout-master-set>
    <fo:simple-page-master master-name="main" page-height="11in" page-width="8.5in"
            margin-top="0.5in" margin-bottom="1in" margin-left="1in" margin-right="1in">
        <fo:region-body margin-top="1in"/>
        <fo:region-before extent="1in"/>
        <fo:region-after extent="1in"/>
    </fo:simple-page-master>
</fo:layout-master-set>
<#if security.hasEntityPermission("ORDERMGR", "_SALES_ENTRY", session)>

<#if productReportList?has_content>
        <fo:page-sequence master-reference="main">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica">
            <fo:block font-size="14pt">${uiLabelMap.OrderReportSalesByStore}</fo:block>
            <#if !showProductStore><fo:block font-size="10pt">${uiLabelMap.CommonFor} ${uiLabelMap.ProductProductStore}: ${parameters.productStoreId} - ${productReportList.get(0).storeName?xml?if_exists}</fo:block></#if>
            <#if !showToParty><fo:block font-size="10pt">${uiLabelMap.PartyParty}: ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, toPartyId, false)}</fo:block></#if>
            <fo:block font-size="10pt">${uiLabelMap.FormFieldTitle_orderStatusId}: 
                <#if parameters.orderStatusId?has_content>${parameters.orderStatusId}<#else>${uiLabelMap.CommonAny}</#if>
            </fo:block>
            <#if parameters.fromOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonFromDate}: ${parameters.fromOrderDate} (${uiLabelMap.OrderDate} &gt;= ${uiLabelMap.CommonFrom})</fo:block></#if>
            <#if parameters.thruOrderDate?has_content><fo:block font-size="10pt">${uiLabelMap.CommonThruDate}: ${parameters.thruOrderDate} (${uiLabelMap.OrderDate} &lt; ${uiLabelMap.CommonFrom})</fo:block></#if>
            <fo:block space-after.optimum="10pt" font-size="10pt">
            <fo:table>
                <#if showProductStore>
                    <fo:table-column column-width="60pt"/>
                    <fo:table-column column-width="340pt"/>
                <#else>
                    <fo:table-column column-width="400pt"/>
                </#if>
                <fo:table-column column-width="40pt"/>
                <fo:table-column column-width="40pt"/>
                <fo:table-header>
                    <fo:table-row font-weight="bold">
                        <#if showProductStore><fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.FormFieldTitle_productStoreId}</fo:block></fo:table-cell></#if>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.ProductProduct}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderQuantitySold}</fo:block></fo:table-cell>
                        <fo:table-cell border-bottom="thin solid grey"><fo:block>${uiLabelMap.OrderValueSold}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list productReportList as productReport>
                        <fo:table-row>
                            <#if showProductStore>
                                <fo:table-cell padding="2pt" background-color="${rowColor}">
                                    <fo:block>${productReport.productStoreId?if_exists}</fo:block>
                                </fo:table-cell>
                            </#if>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.internalName?xml?if_exists} (${productReport.productId?if_exists})</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.quantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt" background-color="${rowColor}">
                                <fo:block>${productReport.unitPrice?if_exists}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                        <#-- toggle the row color -->
                        <#if rowColor == "white">
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
