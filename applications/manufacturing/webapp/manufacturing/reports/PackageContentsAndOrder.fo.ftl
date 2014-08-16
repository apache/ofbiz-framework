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

<#list packages as package>
    <#assign components = package.components>

    <fo:page-sequence master-reference="first" language="en" hyphenate="true">

<!--  nota: codice documento    -->

        <fo:static-content flow-name="xsl-region-before">
            <fo:block line-height="10pt" font-size="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">

Doc.B

            </fo:block>
        </fo:static-content>
        <fo:flow flow-name="xsl-region-body">

<!--  nota: titolo    -->
<!--        <fo:block line-height="12pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always"> -->

<!--
            <fo:block line-height="20pt" font-weight="bold" font-size="18pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="center">
                ${uiLabelMap.ManufacturingProductionRun}
            </fo:block>
-->
<!-- inizio riquadro -->
          <fo:table table-layout="fixed">
             <fo:table-column column-width="18.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell border-style="solid" border-color="black" border-width="1pt">


<!--  nota: riga vuota   -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="12.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">  </fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 1� riga   -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="1.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ProductShipmentPlan}: </fo:inline>
                                <fo:inline font-size="11pt">${package.orderShipment.shipmentId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ManufacturingPackageNumber}: </fo:inline>
                                <fo:inline font-size="11pt">${package.packageId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${Static["org.ofbiz.base.util.UtilDateTime"].nowDateString("dd/MM/yyyy")}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 2� riga   -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="8.00cm"/>
              <fo:table-column column-width="1.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.OrderOrderId} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${package.orderShipment.orderId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="13pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="end">
                                <fo:instream-foreign-object>
                                  <barcode:barcode
                                        xmlns:barcode="http://barcode4j.krysalis.org/ns"
                                        message="${package.orderShipment.shipmentId}${package.packageId}">
                                    <barcode:code39>
                                      <barcode:height>8mm</barcode:height>
                                    </barcode:code39>
                                  </barcode:barcode>
                                </fo:instream-foreign-object>
                            </fo:block>

                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 3� riga  -->

            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="5.00cm"/>
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="2.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ManufacturingDestination}:</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${package.address.address1} - ${package.address.city}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                       </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
<!--  nota: righe o celle dell'etichetta - 4� riga  -->
            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="2.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.PartyParty}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="10pt">${package.party.firstName}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!--  nota: righe o celle dell'etichetta - 5� riga  -->

            <fo:table text-align="start" table-layout="fixed">
              <fo:table-column column-width="2.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="4.00cm"/>
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="3.00cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always">
                                <fo:inline font-size="8pt">${uiLabelMap.ManufacturingReference} :</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${uiLabelMap.ManufacturingReference}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

        <#list components as component>
<!--  nota: righe o celle dell'etichetta - intestazione righe collo  -->
            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="6.00cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="2.50cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="8pt">${uiLabelMap.ProductProductId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="8pt">${uiLabelMap.CommonDescription}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="8pt">${uiLabelMap.ManufacturingCustomLength}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="8pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="8pt">${uiLabelMap.CommonMeasures}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

            <fo:table text-align="left" table-layout="fixed">
              <fo:table-column column-width="3.00cm"/>
              <fo:table-column column-width="6.00cm"/>
              <fo:table-column column-width="2.50cm"/>
              <fo:table-column column-width="2.50cm"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${component.product.productId}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${component.product.internalName}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${component.orderItem.selectedAmount!}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block line-height="10pt" space-before.optimum="1.5pt" space-after.optimum="1.5pt" keep-together="always" text-align="start">
                                <fo:inline font-size="10pt">${component.product.productHeight} x ${component.product.productWidth}</fo:inline>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>

<!-- fine riquadro -->
      </#list>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
</fo:flow>
</fo:page-sequence>
</#list>

</fo:root>
</#escape>

