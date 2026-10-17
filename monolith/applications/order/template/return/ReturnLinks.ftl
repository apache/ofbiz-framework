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
<#assign selected = tabButtonItem?default("void")>
<#if returnHeader??>
  <div class="button-bar tab-bar">
    <ul>
      <li>
    <ul>
      <li<#if selected="OrderReturnHeader"> class="selected"</#if>><a href="<@ofbizUrl>returnMain?returnId=${returnId!}</@ofbizUrl>">${uiLabelMap.OrderReturnHeader}</a></li>
      <li<#if selected="OrderReturnItems"> class="selected"</#if>><a href="<@ofbizUrl>returnItems?returnId=${returnId!}</@ofbizUrl>">${uiLabelMap.OrderReturnItems}</a></li>
      <li<#if selected="OrderReturnHistory"> class="selected"</#if>><a href="<@ofbizUrl>ReturnHistory?returnId=${returnId!}</@ofbizUrl>">${uiLabelMap.OrderReturnHistory}</a></li>
    </ul>
      </li>
    </ul>
    <br />
  </div>
  <#if selected != "OrderReturnHistory">
    <div class="button-bar button-style-1">
      <ul>
        <li>
          <ul>
      <li><a href="<@ofbizUrl>return.pdf?returnId=${returnId!}</@ofbizUrl>" target="_BLANK" >${uiLabelMap.CommonPdf}</a></li>
      <#if returnId??>
        <#assign returnItems = EntityQuery.use(delegator).from("ReturnItem").where("returnId", returnId!, "returnTypeId", "RTN_REFUND").queryList()!/>
        <#if returnItems?has_content>
          <#assign orderId = (Static["org.apache.ofbiz.entity.util.EntityUtil"].getFirst(returnItems)).getString("orderId")/>
          <#assign partyId = "${(returnHeader.fromPartyId)!}"/>
          <li><a href="<@ofbizUrl>setOrderCurrencyAgreementShipDates?partyId=${partyId!}&amp;originOrderId=${orderId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCreateExchangeOrder} ${uiLabelMap.CommonFor} ${orderId!}</a></li>
        </#if>
      </#if>
          </ul>
        </li>
      </ul>
    </div>
  </#if>
<#else>
  <h1>${uiLabelMap.OrderCreateNewReturn}</h1>
  <#if requestParameters.returnId?has_content>
    <h2>${uiLabelMap.OrderNoReturnFoundWithId} : ${requestParameters.returnId}</h2>
  </#if>
  <br />
</#if>
