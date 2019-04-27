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

<#list productionRuns as productionRun>
  <#assign stackInfos = productionRun.stackInfos>
  <#list stackInfos as stackInfo>
    <fo:page-sequence master-reference="first" language="en" hyphenate="true">


        <fo:static-content flow-name="xsl-region-before">
            <fo:block line-height="10pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">


            </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

<fo:table table-layout="fixed">
             <fo:table-column column-width="18.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">

            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="4.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingEstimatedCompletionDate}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingPlan}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingEstimatedCompletionDate}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="4.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="12pt">${Static["org.apache.ofbiz.base.util.UtilDateTime"].toDateString(productionRun.productionRun.estimatedCompletionDate, "dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="12pt">${productionRun.productionRun.workEffortName}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="12pt">${productionRun.productionRun.estimatedCompletionDate}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>


            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="4.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingPrintoutDate}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingProductionRunId}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.ManufacturingPrintoutDate}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="4.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="13pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-weight="bold" font-size="12pt">${Static["org.apache.ofbiz.base.util.UtilDateTime"].nowDateString("dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="13pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:instream-foreign-object>
                                  <barcode:barcode
                                        xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                        message="${productionRun.productionRun.workEffortId}">
                                    <barcode:code128>
                                      <barcode:height>8mm</barcode:height>
                                    </barcode:code128>
                                  </barcode:barcode>
                                </fo:instream-foreign-object>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="13pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-weight="bold" font-size="12pt">${nowTimestamp}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>


            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="6.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="0.50cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="6pt">${uiLabelMap.CommonQuantity} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">



                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.ProductFacilityLocation} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.CommonQuantity} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="6pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.ProductFacilityLocation} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>


            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="6.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="0.50cm"/>
              <fo:table-column column-width="3.00cm"/>
               <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="24pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-weight="bold" font-size="18pt">${stackInfo.qty}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                              <fo:instream-foreign-object>
                                <barcode:barcode
                                      xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                      message="${stackInfo.qty}">
                                  <barcode:code128>
                                    <barcode:height>8mm</barcode:height>
                                  </barcode:code128>
                                </barcode:barcode>
                              </fo:instream-foreign-object>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt"><#if productionRun.location??>${productionRun.location.locationSeqId!}</#if></fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="24pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-weight="bold" font-size="18pt">${stackInfo.qty}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt"><#if productionRun.location??>${productionRun.location.locationSeqId!}</#if></fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>


            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="15.00cm"/>
              <fo:table-column column-width="5.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${uiLabelMap.ManufacturingLabelNumber} : ${stackInfo.stackNum} di ${stackInfo.numOfStacks}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>


            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="8.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="6.00cm"/>
                 <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.ProductProductId} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.CommonDescription} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.ProductProductId} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="6pt">${uiLabelMap.CommonDescription} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="8.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="6.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="12pt">${productionRun.product.productId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="12pt">${productionRun.product.internalName}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="12pt">${productionRun.product.productId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${productionRun.product.internalName}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

</fo:flow>
</fo:page-sequence>
</#list>
</#list>

</fo:root>
</#escape>

