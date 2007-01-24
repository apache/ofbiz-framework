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

<#list productionRuns as productionRun>



    <fo:page-sequence master-reference="first" language="en" hyphenate="true">

<!--  nota: codice documento    -->

        <fo:static-content flow-name="xsl-region-before">
            <fo:block line-height="8pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                
Doc.D
          
            </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

<!-- inizio riquadro -->
<fo:table table-layout="fixed">
             <fo:table-column column-width="18.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">
<!--  nota: righe o celle dell'etichetta - 1° riga   -->

            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>     
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ManufacturingProductionRun}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>     
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${productionRun.productionRun.workEffortId}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>     
                        </fo:table-cell>
                        <fo:table-cell>     
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 2° riga   -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.CustomerOrderId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>     
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${productionRun.productionRunOrder.orderId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>     
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 3° riga  -->

            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="6.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.CommonDestination}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${productionRun.address.address1} - ${productionRun.address.city}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                       </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 4° riga  -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline  font-size="8pt">${uiLabelMap.CommonCustomer}</fo:inline> 
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${productionRun.customer.firstName}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 5° riga  -->

            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.CommonReference} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">{CommonReference} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 6° riga -->

            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.80cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="2.50cm"/>
               <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ProductShipmentPlan}:</fo:inline> 
                            </fo:block>
                        </fo:table-cell>
                               <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${productionRun.plan}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                                <fo:inline font-size="8pt">${uiLabelMap.CommonModel} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="8pt">${productionRun.product.brandName?if_exists}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 7° riga -->

            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${productionRun.product.productId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${productionRun.product.internalName?if_exists}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                     </fo:table-row>
                </fo:table-body>
            </fo:table>
            
<!--  nota: righe o celle dell'etichetta - 8° riga  -->

            <fo:table text-align="center" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
                 <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${productionRun.product.productHeight?default("---")} x ${productionRun.product.productWidth?default("---")} </fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 9° riga -->
            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="2.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="12pt">${productionRun.location?if_exists}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${uiLabelMap.CommonPrintoutDate} : ${Static["org.ofbiz.base.util.UtilDateTime"].nowDateString("dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="9pt">${uiLabelMap.CommonCompletionDate} : ${Static["org.ofbiz.base.util.UtilDateTime"].toDateString(productionRun.productionRun.estimatedCompletionDate, "dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!-- fine riquadro -->
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

</fo:flow>
</fo:page-sequence>
</#list>

</fo:root>
</#escape>

