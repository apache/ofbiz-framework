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
<#macro displayReturnAdjustment returnAdjustment>
    <#assign returnHeader = returnAdjustment.getRelatedOne("ReturnHeader", false)>
    <#assign adjReturnType = returnAdjustment.getRelatedOne("ReturnType", false)!>
    <fo:table-row>
    <fo:table-cell><fo:block></fo:block></fo:table-cell>
    <fo:table-cell><fo:block></fo:block></fo:table-cell>
    <fo:table-cell number-columns-spanned="3" padding="1mm">
      <fo:block wrap-option="wrap">
        <#if returnAdjustment.comments?has_content>${returnAdjustment.comments}<#else>${returnAdjustment.description?default("N/A")}</#if>
      </fo:block>
    </fo:table-cell>
    <fo:table-cell><fo:block></fo:block></fo:table-cell>
    <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=returnAdjustment.amount isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
    </fo:table-row>
    <#if returnAdjustment.amount?has_content>
         <#assign total = total + returnAdjustment.get("amount")>
    </#if>
</#macro>

      <#-- Items returned -->
      <fo:block font-size="10pt">
        <fo:table table-layout="fixed" border-style="solid" border-width="0.2pt" width="7.25in" height="5in">
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="2.25in"/>
          <fo:table-column column-width="1.0in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-body>

            <fo:table-row text-align="center" font-weight="bold">
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderOrderId}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.ProductProductId}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.CommonDescription}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.CommonReason}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderQty}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderUnitPrice}</fo:block></fo:table-cell>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt" display-align="after"><fo:block>${uiLabelMap.OrderAmount}</fo:block></fo:table-cell>
            </fo:table-row>

            <#-- each item -->
            <#assign total = 0.0/>
            <#list returnItems as returnItem>
              <fo:table-row>
                <fo:table-cell padding="1mm" font-size="8pt">
                  <fo:block>${returnItem.orderId}</fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1mm" font-size="8pt">
                  <fo:block>
                    <#if returnItem.orderItemSeqId??>${returnItem.getRelatedOne("OrderItem", false).getString("productId")}</#if>
                  </fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1mm"><fo:block wrap-option="wrap">${returnItem.description!}</fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" font-size="8pt"><fo:block><#if returnItem.returnReasonId??>${(returnItem.getRelatedOne("ReturnReason", false)).get("description",locale)?default(returnItem.returnReasonId)}</#if></fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block>${returnItem.returnQuantity}</fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=returnItem.returnPrice isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
                <fo:table-cell padding="1mm" text-align="right"><fo:block><@ofbizCurrency amount=(returnItem.returnPrice * returnItem.returnQuantity) isoCode=returnHeader.currencyUomId/></fo:block></fo:table-cell>
              </fo:table-row>
              <#assign total = total + returnItem.returnQuantity.doubleValue() * returnItem.returnPrice.doubleValue()/>

              <#assign returnItemAdjustments = returnItem.getRelated("ReturnAdjustment", null, null, false)>
              <#if (returnItemAdjustments?has_content)>
                  <#list returnItemAdjustments as returnItemAdjustment>
                     <@displayReturnAdjustment returnAdjustment=returnItemAdjustment/>
                  </#list>
              </#if>
            </#list>

            <#-- order level adjustments -->
            <#if (returnAdjustments?has_content)>
                <#list returnAdjustments as returnAdjustment>
                    <@displayReturnAdjustment returnAdjustment=returnAdjustment/>
                </#list>
            </#if>

        </fo:table-body>
        </fo:table>
      </fo:block>

      <#-- total -->
        <fo:table table-layout="fixed" space-before="5mm" font-size="10pt">
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="2.25in"/>
          <fo:table-column column-width="1.0in"/>
          <fo:table-column column-width="0.5in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-column column-width="0.875in"/>
          <fo:table-body>
            <fo:table-row>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell/>
              <fo:table-cell padding="1mm" border-style="solid" border-width="0.2pt">
                <fo:block font-weight="bold" text-align="center">${uiLabelMap.CommonTotal}</fo:block>
              </fo:table-cell>
              <fo:table-cell text-align="right" padding="1mm" border-style="solid" border-width="0.2pt">
                <fo:block><@ofbizCurrency amount=total isoCode=returnHeader.currencyUomId/></fo:block>
              </fo:table-cell>
            </fo:table-row>
          </fo:table-body>
        </fo:table>
</#escape>

