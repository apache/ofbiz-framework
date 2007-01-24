<?xml version="1.0" encoding="UTF-8"?>
<!--
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

<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:fox="http://xml.apache.org/fop/extensions">
    <fo:layout-master-set>
        <fo:simple-page-master margin-right="2.0cm" margin-left="2.0cm" margin-bottom="1.0cm" margin-top="1.0cm" page-width="21cm" page-height="29.7cm" master-name="first">
            <fo:region-before extent="1.5cm"/>
            <fo:region-body margin-bottom="1.5cm" margin-top="1.5cm"/>
            <fo:region-after extent="1.0cm"/>
        </fo:simple-page-master>
    </fo:layout-master-set>

<#if productionRuns?exists>

<#list productionRuns as productionRun>
    <#assign componentList = productionRun.componentList>
    <fo:page-sequence master-reference="first" language="en" hyphenate="true">
        <fo:static-content flow-name="xsl-region-before">
            <fo:block line-height="10pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">

                      Doc.G   

            </fo:block>
        </fo:static-content>
        <fo:static-content flow-name="xsl-region-after">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${uiLabelMap.CommonPage} <fo:page-number/>
            </fo:block>
        </fo:static-content>
        <fo:flow flow-name="xsl-region-body">
            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
            <fo:block line-height="20pt" font-weight="bold" font-size="18pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                ${uiLabelMap.CommonHardware}
            </fo:block>

            <fo:block line-height="10pt" font-weight="bold" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                ${productionRun.product.internalName}
            </fo:block>

            <fo:table text-align="left" table-layout="fixed">
                <fo:table-column column-width="10.15cm"/>
                <fo:table-column column-width="6.15cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="13pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.CustomerOrderId}:</fo:inline>
                                <fo:inline font-weight="bold" font-size="10pt">${productionRun.productionRunOrder.orderId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.estimatedCompletionDate}:</fo:inline>
                                <fo:inline font-weight="bold" font-size="10pt">${Static["org.ofbiz.base.util.UtilDateTime"].nowDateString("dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="left" table-layout="fixed">
                <fo:table-column column-width="10.15cm"/>
                <fo:table-column column-width="6.15cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.CommonCustomer}:</fo:inline>
                                <fo:inline font-weight="bold" font-size="10pt">${productionRun.customer.partyId} - ${productionRun.customer.firstName} - ${productionRun.address.city}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.CommonEffected}:</fo:inline>
                                <fo:inline font-size="10pt">___________________</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="left" table-layout="fixed">
                <fo:table-column column-width="10.15cm"/>
                <fo:table-column column-width="6.15cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.CommonRequester} :</fo:inline>
                                <fo:inline font-size="10pt">__________________</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.CommonApproved} :</fo:inline>
                                <fo:inline font-size="10pt">__________________</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:inline white-space-collapse="false">
            </fo:inline>

<fo:table text-align="left" table-layout="fixed">
<fo:table-column column-width="0.80cm"/>
<fo:table-column column-width="2.50cm"/>
<fo:table-column column-width="4.50cm"/>
<fo:table-column column-width="1.50cm"/>
<fo:table-column column-width="1.50cm"/>
<fo:table-column column-width="6.00cm"/>
<!-- 
<fo:table-column column-width="4.00cm"/>
-->
<fo:table-body>
    <fo:table-row>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${uiLabelMap.CommonLine}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${uiLabelMap.ProductProductId}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${uiLabelMap.CommonDescription}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                    ${uiLabelMap.CommonUom}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                    ${uiLabelMap.CommonQuantity}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                    ${uiLabelMap.CommonReleasedNote}
            </fo:block>
        </fo:table-cell>
<!--
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                    ${uiLabelMap.CommonNote}
            </fo:block>
        </fo:table-cell>
-->        
    </fo:table-row>

    <#assign row = 0>
        <#list componentList as product>
        <#assign row = row + 1>
    <fo:table-row>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                ${row}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${product.componentProduct.productId}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                ${product.componentProduct.internalName?if_exists}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                ${product.componentProduct.quantityUomId?if_exists}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                ${product.component.estimatedQuantity}
            </fo:block>
        </fo:table-cell>
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
               
            </fo:block>
        </fo:table-cell>
<!--
        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
            <fo:block line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                {Rel}
            </fo:block>
        </fo:table-cell>
-->
    </fo:table-row>
    </#list>
</fo:table-body>
</fo:table>
</fo:block>

</fo:flow>
</fo:page-sequence>
</#list>
</#if>
</fo:root>
</#escape>

