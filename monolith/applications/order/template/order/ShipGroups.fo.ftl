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

<#list shipGroups as shipGroup>
  <#assign data = groupData.get(shipGroup.shipGroupSeqId)>

  <#-- print the order ID, ship group, and their bar codes -->

  <fo:table table-layout="fixed" space-after.optimum="10pt">
    <fo:table-column/>
    <fo:table-column/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell>
          <fo:block font-size="14pt">${uiLabelMap.OrderOrder} #${shipGroup.orderId}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block text-align="right">
            <fo:instream-foreign-object>
              <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="${shipGroup.orderId}">
                <barcode:code128><barcode:height>8mm</barcode:height></barcode:code128>
              </barcode:barcode>
            </fo:instream-foreign-object>
          </fo:block>
        </fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell>
          <fo:block font-size="14pt">${uiLabelMap.OrderShipGroup} #${shipGroup.shipGroupSeqId}</fo:block>
        </fo:table-cell>
        <fo:table-cell>
          <fo:block text-align="right">
            <fo:instream-foreign-object>
              <barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="${shipGroup.shipGroupSeqId}">
                <barcode:code128><barcode:height>8mm</barcode:height></barcode:code128>
              </barcode:barcode>
            </fo:instream-foreign-object>
          </fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-body>
  </fo:table>

  <#-- print the address, carrier, and shipment dates -->

  <fo:table table-layout="fixed" space-after.optimum="10pt">
    <fo:table-column column-width="proportional-column-width(2)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-body>
      <fo:table-row>
        <fo:table-cell number-rows-spanned="4">
          <#assign address = data.address!>
          <fo:block>${uiLabelMap.CommonTo}: ${address.toName!}</fo:block>
          <#if address.attnName?has_content>
          <fo:block>${uiLabelMap.CommonAttn}: ${address.attnName!}</fo:block>
          </#if>
          <fo:block>${address.address1!}</fo:block>
          <fo:block>${address.address2!}</fo:block>
          <fo:block>
            ${address.city!}<#if address.stateProvinceGeoId?has_content>, ${address.stateProvinceGeoId}</#if>
            ${address.postalCode!} ${address.countryGeoId!}
          </fo:block>

          <#if data.phoneNumber??>
            <fo:block><#if data.phoneNumber.areaCode??>(${data.phoneNumber.areaCode}) </#if>${data.phoneNumber.contactNumber}</fo:block>
          </#if>
        </fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.ProductShipmentMethod}</fo:block></fo:table-cell>
        <fo:table-cell><#if data.carrierShipmentMethod??><fo:block>${data.carrierShipmentMethod.partyId} ${data.shipmentMethodType.description}</fo:block></#if></fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderShipBeforeDate}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${shipGroup.shipByDate?default("N/A")}</fo:block></fo:table-cell>
      </fo:table-row>
      <fo:table-row>
        <fo:table-cell><fo:block font-weight="bold">${uiLabelMap.OrderShipAfterDate}</fo:block></fo:table-cell>
        <fo:table-cell><fo:block>${shipGroup.shipAfterDate?default("N/A")}</fo:block></fo:table-cell>
      </fo:table-row>
    </fo:table-body>
  </fo:table>

  <#assign lines = data.lines>
  <fo:table table-layout="fixed">
    <fo:table-column column-width="proportional-column-width(2)"/>
    <fo:table-column column-width="proportional-column-width(3)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>
    <fo:table-column column-width="proportional-column-width(1)"/>

    <fo:table-header>
      <fo:table-row font-weight="bold">
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.ProductProduct}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.CommonDescription}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.OrderQuantityInShipGroup}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.OrderQuantityShipped}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="#D4D0C8" text-align="right" height="20pt" display-align="center" border-top-style="solid" border-bottom-style="solid">
          <fo:block>${uiLabelMap.ProductOpenQuantity}</fo:block>
        </fo:table-cell>
      </fo:table-row>
    </fo:table-header>
    <fo:table-body>

      <#list lines as line>
        <#if ((line_index % 2) == 0)>
          <#assign rowColor = "white">
        <#else>
          <#assign rowColor = "#CCCCCC">
        </#if>

      <fo:table-row>
        <fo:table-cell background-color="${rowColor}">
          <fo:block>${line.product.productId}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block>${line.orderItem.itemDescription!}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityInGroup?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityShipped?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}">
          <fo:block text-align="right">${line.quantityOpen?default(0)}</fo:block>
        </fo:table-cell>

      </fo:table-row>

      <#list line.expandedList! as expandedLine>
      <fo:table-row>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block margin-left="20pt">${expandedLine.product.productId}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block margin-left="20pt">${expandedLine.product.internalName}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityInGroup?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityShipped?default(0)}</fo:block>
        </fo:table-cell>
        <fo:table-cell background-color="${rowColor}" font-style="italic">
          <fo:block text-align="right">${expandedLine.quantityOpen?default(0)}</fo:block>
        </fo:table-cell>
      </fo:table-row>
      </#list>

      </#list>

  </fo:table-body>
</fo:table>

  <#if shipGroup_has_next><fo:block break-before="page"/></#if>
</#list>

</#escape>
