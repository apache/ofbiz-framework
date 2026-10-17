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

<div id="findOrdersList" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderOrderReceivedOn} ${Static["org.apache.ofbiz.base.util.UtilDateTime"].toDateString(filterDate)}</li>
      <#assign listSize = state.getSize()>
      <#if (listSize > 10)>
        <li><a href="<@ofbizUrl controlPath="/ordermgr/control">orderlist?viewIndex=${state.getViewIndex() + 1}&amp;viewSize=${state.getViewSize()}&amp;filterDate=${filterDate!}</@ofbizUrl>">${uiLabelMap.CommonMore}</a></li>
      </#if>
      <#if orderHeaderList?has_content> 
        <li>1-${orderHeaderList.size()} ${uiLabelMap.CommonOf} ${state.getSize()}</li>
      </#if>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if orderHeaderList?has_content>
      <table class="basic-table hover-bar" cellspacing='0'>
        <tr class="header-row">
          <td width="10%">${uiLabelMap.OrderOrder} ${uiLabelMap.CommonNbr}</td>
          <td width="15%">${uiLabelMap.OrderOrderBillToParty}</td>
          <td width="25%">${uiLabelMap.OrderProductStore}</td>
          <td width="10%">${uiLabelMap.CommonAmount}</td>
          <td width="20%">${uiLabelMap.OrderTrackingCode}</td>
          <td width="20%">${uiLabelMap.CommonStatus}</td>
        </tr>
        <#assign alt_row = false>
        <#list orderHeaderList as orderHeader>
          <#assign status = orderHeader.getRelatedOne("StatusItem", true)>
          <#assign orh = Static["org.apache.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeader)>
          <#assign billToParty = orh.getBillToParty()!>
          <#if billToParty?has_content>
            <#assign billToPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", billToParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
            <#assign billTo = billToPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")/>
          </#if>
          <#assign productStore = orderHeader.getRelatedOne("ProductStore", true)! />
          <tr<#if alt_row> class="alternate-row"</#if>>
            <#assign alt_row = !alt_row>
            <td><a href="<@ofbizUrl controlPath="/ordermgr/control">orderview?orderId=${orderHeader.orderId}</@ofbizUrl>" class="buttontext">${orderHeader.orderId}</a></td>
            <td>${billTo!}</td>
            <td><#if productStore?has_content>${productStore.storeName?default(productStore.productStoreId)}</#if></td>
            <td><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orderHeader.currencyUom/></td>
            <td>
              <#assign trackingCodes = orderHeader.getRelated("TrackingCodeOrder", null, null, false)>
              <#list trackingCodes as trackingCode>
                <#if trackingCode?has_content>
                  <a href="<@ofbizUrl controlPath="/marketing/control">FindTrackingCodeOrders?trackingCodeId=${trackingCode.trackingCodeId}&amp;externalLoginKey=${requestAttributes.externalLoginKey!}</@ofbizUrl>">${trackingCode.trackingCodeId}</a><br />
                </#if>
              </#list>
            </td>
            <td>${orderHeader.getRelatedOne("StatusItem", true).get("description",locale)}</td>
          </tr>
        </#list>
      </table>
    <#else>
      <h3>${uiLabelMap.OrderNoOrderFound}</h3>
    </#if>
  </div>
</div>
