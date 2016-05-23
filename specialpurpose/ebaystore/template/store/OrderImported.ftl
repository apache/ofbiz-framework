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
<script language="JavaScript" type="text/javascript">
<!-- //
function uploadTrackingCode(orderId, productStoreId) {
    document.uploadTracking.orderId.value = orderId;
    document.uploadTracking.productStoreId.value = productStoreId;
    document.uploadTracking.submit();
}
// -->
</script>
<div id="findOrdersList" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
        <li class="h3">${uiLabelMap.EbayListOrderImported}</li>
    </ul>
  </div>
  <div class="screenlet-body">
  <form name="uploadTracking" action="<@ofbizUrl>uploadTrackingCodeBack</@ofbizUrl>" method="post">
      <input type="hidden" name="orderId" value=""/>
      <input type="hidden" name="productStoreId" value=""/>
  </form>
  <form name="listOrdersImported" method="post">
      <input type="hidden" name="viewSize"/>
      <input type="hidden" name="viewIndex"/>
      <table class="basic-table hover-bar" cellspacing='0'>
        <tr class="header-row">
          <td width="5%">${uiLabelMap.OrderOrderType}</td>
          <td width="5%">${uiLabelMap.OrderOrderId}</td>
          <td width="20%">${uiLabelMap.PartyName}</td>
          <td width="5%" align="right">${uiLabelMap.OrderSurvey}</td>
          <td width="5%" align="right">${uiLabelMap.OrderItemsOrdered}</td>
          <td width="5%" align="right">${uiLabelMap.OrderItemsBackOrdered}</td>
          <td width="5%" align="right">${uiLabelMap.OrderItemsReturned}</td>
          <td width="10%" align="right">${uiLabelMap.OrderRemainingSubTotal}</td>
          <td width="10%" align="right">${uiLabelMap.OrderOrderTotal}</td>
          <td width="5%">&nbsp;</td>
            <#if (requestParameters.filterInventoryProblems?default("N") == "Y") || (requestParameters.filterPOsOpenPastTheirETA?default("N") == "Y") || (requestParameters.filterPOsWithRejectedItems?default("N") == "Y") || (requestParameters.filterPartiallyReceivedPOs?default("N") == "Y")>
              <td width="15%">${uiLabelMap.CommonStatus}</td>
              <td width="5%">${uiLabelMap.CommonFilter}</td>
            <#else>
              <td width="20%">${uiLabelMap.CommonStatus}</td>
            </#if>
          <td width="20%">${uiLabelMap.CommonDate}</td>
        </tr>
        <#if orderList?has_content>
          <#assign alt_row = false>
          <#list orderList as orderHeader>
            <#assign orh = Static["org.ofbiz.order.order.OrderReadHelper"].getHelper(orderHeader)>
            <#assign statusItem = orderHeader.getRelatedOne("StatusItem", true)>
            <#assign orderType = orderHeader.getRelatedOne("OrderType", true)>
            <#if orderType.orderTypeId == "PURCHASE_ORDER">
              <#assign displayParty = orh.getSupplierAgent()!>
            <#else>
              <#assign displayParty = orh.getPlacingParty()!>
            </#if>
            <#assign partyId = displayParty.partyId?default("_NA_")>
            <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
              <td>${orderType.get("description",locale)?default(orderType.orderTypeId?default(""))}</td>
              <td><a href="#" onclick="javascript:uploadTrackingCode('${orderHeader.orderId}','${productStoreId}')" class='buttontext'>${orderHeader.orderId}</a></td>
              <td>
                <div>
                  <#if displayParty?has_content>
                      <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                      ${displayPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                  </div>
              </td>
              <td align="right">${orh.hasSurvey()?string.number}</td>
              <td align="right">${orh.getTotalOrderItemsQuantity()?string.number}</td>
              <td align="right">${orh.getOrderBackorderQuantity()?string.number}</td>
              <td align="right">${orh.getOrderReturnedQuantity()?string.number}</td>
              <td align="right"><@ofbizCurrency amount=orderHeader.remainingSubTotal isoCode=orh.getCurrency()/></td>
              <td align="right"><@ofbizCurrency amount=orderHeader.grandTotal isoCode=orh.getCurrency()/></td>

              <td>&nbsp;</td>
              <td>${statusItem.get("description",locale)?default(statusItem.statusId?default("N/A"))}</td>
              </td>
              <#if (requestParameters.filterInventoryProblems?default("N") == "Y") || (requestParameters.filterPOsOpenPastTheirETA?default("N") == "Y") || (requestParameters.filterPOsWithRejectedItems?default("N") == "Y") || (requestParameters.filterPartiallyReceivedPOs?default("N") == "Y")>
                  <td>
                      <#if filterInventoryProblems.contains(orderHeader.orderId)>
                        Inv&nbsp;
                      </#if>
                      <#if filterPOsOpenPastTheirETA.contains(orderHeader.orderId)>
                        ETA&nbsp;
                      </#if>
                      <#if filterPOsWithRejectedItems.contains(orderHeader.orderId)>
                        Rej&nbsp;
                      </#if>
                      <#if filterPartiallyReceivedPOs.contains(orderHeader.orderId)>
                        Part&nbsp;
                      </#if>
                  </td>
              </#if>
              <td>${orderHeader.getString("orderDate")}</td>
            </tr>
            <#-- toggle the row color -->
            <#assign alt_row = !alt_row>
          </#list>
        <#else>
          <tr>
            <td colspan='4'><h3>${uiLabelMap.EbayNoOrderImported}.</h3></td>
          </tr>
        </#if>
        <#if lookupErrorMessage??>
          <tr>
            <td colspan='4'><h3>${lookupErrorMessage}</h3></td>
          </tr>
        </#if>
      </table>
  </form>
  </div>
</div>