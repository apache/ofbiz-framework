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
        <#if records?has_content>         
            <fo:page-sequence master-reference="main">
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block text-align="right" line-height="12pt" font-size="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                        ${uiLabelMap.CommonPage} <fo:page-number/>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body" font-family="Helvetica">                    
                    <fo:block><fo:leader/></fo:block>
                    <fo:block font-size="14pt">${uiLabelMap.ManufacturingOperationRunForShipment}:${shipmentIdPar}</fo:block>
                    <fo:block><fo:leader/></fo:block>              
                    <fo:block space-after.optimum="8pt" font-size="8pt"/>
                    <fo:table font-size="8pt" border="0.5pt solid black">
                        <fo:table-column column-width="45pt"/>
                        <fo:table-column column-width="45pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="90pt"/>
                        <fo:table-column column-width="90pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-column column-width="60pt"/>
                        <fo:table-header>
                            <fo:table-row font-weight="bold">                                
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ManufacturingShopOrder}</fo:block>
                                </fo:table-cell>                            
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ManufacturingWorkCenter}</fo:block>
                                </fo:table-cell>                                                        
                                <fo:table-cell padding="2pt">
                                    <fo:block>${uiLabelMap.ProductProductId}</fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="2pt">                                     
                                    <fo:block>${uiLabelMap.CommonDescription}</fo:block>
                                </fo:table-cell>                                    
                                <fo:table-cell padding="2pt">                                     
                                    <fo:block>${uiLabelMap.ManufacturingOperationCodeAndDescription}</fo:block>
                                </fo:table-cell>                                    
                                <fo:table-cell padding="2pt">                                     
                                    <fo:block>${uiLabelMap.ManufacturingTaskRunTime}</fo:block>
                                </fo:table-cell>                                    
                                <fo:table-cell padding="2pt">                                     
                                    <fo:block>${uiLabelMap.ManufacturingTaskSetupTime}</fo:block>
                                </fo:table-cell>                                                                                                           
                            </fo:table-row>
                        </fo:table-header>                          
                        <fo:table-body/>                                            
                    </fo:table> 
                    <#list records as record>  
                        <fo:table font-size="8pt">   
                            <fo:table-column column-width="45pt"/>
                               <fo:table-column column-width="45pt"/>
                            <fo:table-column column-width="60pt"/>
                            <fo:table-column column-width="90pt"/>
                            <fo:table-column column-width="90pt"/>
                            <fo:table-column column-width="60pt"/>
                            <fo:table-column column-width="60pt"/>     
                            <fo:table-header/>                                         
                            <fo:table-body>
                                <fo:table-row>
                                    <fo:table-cell padding="2pt">
                                        <fo:block>${record.get("workEffortId")}</fo:block>
                                    </fo:table-cell>                            
                                    <fo:table-cell padding="2pt">
                                        <fo:block><#if estimatedReadyDatePar?has_content>${record.get("fixedAssetId")}</#if></fo:block>
                                    </fo:table-cell>         
                                    <fo:table-cell padding="2pt">
                                        <fo:block>${record.get("productId")}</fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="2pt">                                     
                                        <fo:block>${record.get("productName")}</fo:block>
                                    </fo:table-cell>                                    
                                    <fo:table-cell padding="2pt">                                     
                                        <fo:block>${record.get("taskName")} ${record.get("taskDescription")}</fo:block>
                                    </fo:table-cell>                                    
                                    <fo:table-cell padding="2pt" text-align="right">                                     
                                        <fo:block>${record.get("taskEstimatedTime")}</fo:block>
                                    </fo:table-cell>                                    
                                    <fo:table-cell padding="2pt" text-align="right">                                     
                                        <fo:block>${record.get("taskEstimatedSetup")}</fo:block>
                                    </fo:table-cell>                                    
                                </fo:table-row>
                            </fo:table-body>      
                        </fo:table>                                                                                                          
                       </#list>               
                       <fo:table font-size="8pt">   
                        <fo:table-column column-width="450pt"/>     
                        <fo:table-header/>                                         
                        <fo:table-body>
                            <fo:table-row>
                                <fo:table-cell padding="2pt" text-align="right">                                     
                                    <fo:block>${fixedAssetTime}</fo:block>
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
