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
<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:fox="http://xml.apache.org/fop/extensions">
    <fo:layout-master-set>
        <fo:simple-page-master master-name="main" 
             margin-top="1.0cm" margin-bottom="1in" margin-left="0.5cm" margin-right="0.5cm">
          <fo:region-body margin-top="1.0cm" margin-bottom="1.0cm"/>  <#-- main body -->
            <fo:region-before extent="1.0cm"/>  <#-- a header -->
            <fo:region-after extent="1.0cm"/>  <#-- a footer -->
        </fo:simple-page-master>
    </fo:layout-master-set>

<#if productionRunId?has_content>
        <fo:page-sequence master-reference="main" language="en" hyphenate="true">
        <fo:flow flow-name="xsl-region-body" font-family="Helvetica" font-size="8pt">
            <fo:block>${uiLabelMap.ManufacturingProductionRunId}:${productionRunData.workEffortId?if_exists}</fo:block>
            <fo:block space-after.optimum="0.3cm"></fo:block>
            <fo:block>${uiLabelMap.ProductProductId}:${productionRunData.productId?if_exists}/${productionRunData.productName?if_exists}</fo:block>
            <fo:block space-after.optimum="1.0cm"></fo:block>
<#--<!--
            <fo:table>
                <fo:table-column column-width="4.0cm"/>
                <fo:table-column column-width="5.5cm"/>
                <fo:table-column column-width="4.0cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                        <fo:block font-size="10pt">${uiLabelMap.ManufacturingProductionRunId}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                        <fo:block font-size="12pt">${productionRunData.workEffortId?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                        <fo:block font-size="10pt">${uiLabelMap.ProductProductId}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                        <fo:block font-size="12pt">${productionRunData.productId?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                        <fo:block font-size="12pt">${productionRunData.productName?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:block space-after.optimum="0.5cm" font-size="10pt"></fo:block>
                    </fo:table-row>
               </fo:table-body>
            </fo:table>
-->
            <#assign dimColor = "#D4D0C8">
            <fo:table>
                <fo:table-column column-width="4.0cm"/>
                <fo:table-column column-width="5.5cm"/>
                <fo:table-column column-width="4.0cm"/>
                <fo:table-column column-width="5.0cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.FormFieldTitle_estimatedStartDate}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block><#if productionRunData.estimatedStartDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.estimatedStartDate, "dd/MM/yyyy")}</#if></fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.FormFieldTitle_actualStartDate}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block><#if productionRunData.actualStartDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.actualStartDate, "dd/MM/yyyy")}</#if></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.FormFieldTitle_estimatedCompletionDate}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block><#if productionRunData.estimatedCompletionDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.estimatedCompletionDate, "dd/MM/yyyy")}</#if></fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.FormFieldTitle_actualCompletionDate}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block><#if productionRunData.actualCompletionDate?exists>${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRunData.actualCompletionDate, "dd/MM/yyyy")}</#if></fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.ManufacturingQuantityToProduce}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${productionRunData.quantityToProduce?if_exists}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.ManufacturingQuantityProduced}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${productionRunData.quantityProduced?if_exists}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.ManufacturingQuantityRemaining}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${productionRunData.quantityRemaining}</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${uiLabelMap.ManufacturingQuantityRejected}:</fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="2pt">
                            <fo:block>${quantityRejected?if_exists}</fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
            <#-- Components   -->
            <fo:block space-after.optimum="0.3cm"></fo:block>
            <fo:table>
              <fo:table-column column-width="3.4cm"/>
              <fo:table-column column-width="6.0cm"/>
              <fo:table-column column-width="2.5cm"/>
              <fo:table-column column-width="2.5cm"/>
              <fo:table-column column-width="3.5cm"/>
                <fo:table-header>
                    <fo:table-row background-color="${dimColor}">
                        <fo:table-cell><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingIssuedQuantity}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingQuantityRemaining}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign dimColor = "#D4D0C8">
                    <#assign rowColor = "white">
                    <#list productionRunComponentsData as productionRunComponentData>
              
                    <#assign resQuantityComp = productionRunComponentData.estimatedQuantity - productionRunComponentData.issuedQuantity>
 
                       <fo:table-row>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunComponentData.productId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunComponentData.internalName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunComponentData.estimatedQuantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunComponentData.issuedQuantity?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${resQuantityComp?if_exists}</fo:block>
                             </fo:table-cell>
                        </fo:table-row>
                    </#list>          
                </fo:table-body>
            </fo:table>
            <#-- Tasks   -->
            <fo:block space-after.optimum="0.3cm"></fo:block>
            <fo:table>
              <fo:table-column column-width="3.5cm"/>
              <fo:table-column column-width="3.5cm"/>
              <fo:table-column column-width="5.0cm"/>
              <fo:table-column column-width="3.0cm"/>
              <fo:table-column column-width="3.0cm"/>
                <fo:table-header>
                    <fo:table-row background-color="${dimColor}">
                        <fo:table-cell><fo:block>${uiLabelMap.FormFieldTitle_fixedAssetId}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingRoutingTask}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingTaskEstimatedSetupMillis}</fo:block></fo:table-cell>
                        <fo:table-cell><fo:block>${uiLabelMap.ManufacturingTaskEstimatedMilliSeconds}</fo:block></fo:table-cell>
                    </fo:table-row>
                </fo:table-header>
                <fo:table-body>
                    <#assign rowColor = "white">
                    <#list productionRunRoutingTasks as productionRunRoutingTask>
                        <fo:table-row>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunRoutingTask.fixedAssetId?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunRoutingTask.workEffortName?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunRoutingTask.description?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunRoutingTask.estimatedSetupMillis?if_exists}</fo:block>
                            </fo:table-cell>
                            <fo:table-cell padding="2pt">
                                <fo:block>${productionRunRoutingTask.estimatedMilliSeconds?if_exists}</fo:block>
                            </fo:table-cell>
                        </fo:table-row>
                    </#list>          
                </fo:table-body>
            </fo:table>
        </fo:flow>
        </fo:page-sequence>
</#if>
</fo:root>
</#escape>
