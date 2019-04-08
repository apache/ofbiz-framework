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

<#if shipGroups?? && shipGroups.size() gt 1>
    <fo:table table-layout="fixed" border-spacing="3pt" space-before="0.3in" font-size="9pt">
        <fo:table-column column-width="1in"/>
        <fo:table-column column-width="1in"/>
        <fo:table-column column-width="0.5in"/>
        <fo:table-header>
            <fo:table-row font-weight="bold">
                <fo:table-cell><fo:block>${uiLabelMap.OrderShipGroup}</fo:block></fo:table-cell>
                <fo:table-cell><fo:block>${uiLabelMap.OrderProduct}</fo:block></fo:table-cell>
                <fo:table-cell text-align="right"><fo:block>${uiLabelMap.OrderQuantity}</fo:block></fo:table-cell>
            </fo:table-row>
        </fo:table-header>
        <fo:table-body>
            <#list shipGroups as shipGroup>
                <#assign orderItemShipGroupAssocs = shipGroup.getRelated("OrderItemShipGroupAssoc", null, null, false)!>
                <#if orderItemShipGroupAssocs?has_content>
                    <#list orderItemShipGroupAssocs as shipGroupAssoc>
                        <#assign orderItem = shipGroupAssoc.getRelatedOne("OrderItem", false)!>
                        <fo:table-row>
                            <fo:table-cell><fo:block>${shipGroup.shipGroupSeqId}</fo:block></fo:table-cell>
                            <fo:table-cell><fo:block>${orderItem.productId!}</fo:block></fo:table-cell>
                            <fo:table-cell text-align="right"><fo:block>${shipGroupAssoc.quantity?string.number}</fo:block></fo:table-cell>
                        </fo:table-row>
                    </#list>
                </#if>
            </#list>
        </fo:table-body>
    </fo:table>
</#if>


<fo:block space-after="40pt"/>
<#if orderHeader.getString("orderTypeId") == "SALES_ORDER">
  <fo:block font-size="14pt" font-weight="bold" text-align="center">THANK YOU FOR YOUR PATRONAGE!</fo:block>
  <fo:block font-size="8pt">
    <#--    Here is a good place to put policies and return information. -->
  </fo:block>
<#elseif orderHeader.getString("orderTypeId") == "PURCHASE_ORDER">
  <fo:block font-size="8pt">
    <#-- Here is a good place to put boilerplate terms and conditions for a purchase order. -->
  </fo:block>
</#if>
</#escape>
