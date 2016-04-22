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

<#-- NOTE: this template is used for the orderstatus screen in ecommerce AND for order notification emails through the OrderNoticeEmail.ftl file -->
<#-- the "urlPrefix" value will be prepended to URLs by the ofbizUrl transform if/when there is no "request" object in the context -->
<#if baseEcommerceSecureUrl??><#assign urlPrefix = baseEcommerceSecureUrl/></#if>

<table width="100%" border="0" cellpadding="0" cellspacing="0">
  <tr>
    <#-- left side -->
    <td width="50%" valign="top">

    <div class="screenlet">
        <div class="screenlet-title-bar">
            <div class="boxlink">
                <#if maySelectItems?default("N") == "Y" && returnLink?default("N") == "Y" && (orderHeader.statusId)! == "ORDER_COMPLETED">
                    <a href="<@ofbizUrl>makeReturn?orderId=${orderHeader.orderId}</@ofbizUrl>" class="submenutextright">${uiLabelMap.OrderRequestReturn}</a>
                </#if>
            </div>
            <div class="h3">${uiLabelMap.OrderOrder}&nbsp;<#if orderHeader?has_content>${uiLabelMap.CommonNbr}<a href="<@ofbizUrl>orderstatus?orderId=${orderHeader.orderId}</@ofbizUrl>" class="lightbuttontext">${orderHeader.orderId}</a>&nbsp;</#if>${uiLabelMap.CommonInformation}</div>
        </div>
        <div class="screenlet-body">
            <table width="100%" border="0" cellpadding="1">
                <#-- placing customer information -->
                <#if localOrderReadHelper?? && orderHeader?has_content>
                  <#assign displayParty = localOrderReadHelper.getPlacingParty()!/>
                  <#if displayParty?has_content>
                      <#assign displayPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", displayParty.partyId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  </#if>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div>&nbsp;<b>${uiLabelMap.PartyName}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                      <div>
                        ${(displayPartyNameResult.fullName)?default("[Name Not Found]")}
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="7"><hr /></td></tr>
                </#if>
                <#-- order status information -->
                <tr>
                  <td align="right" valign="top" width="15%">
                    <div>&nbsp;<b>${uiLabelMap.CommonStatus}</b></div>
                  </td>
                  <td width="5">&nbsp;</td>
                  <td valign="top" width="80%">
                    <#if orderHeader?has_content>
                      <div>${localOrderReadHelper.getStatusString(locale)}</div>
                    <#else>
                      <div><b>${uiLabelMap.OrderNotYetOrdered}</b></div>
                    </#if>
                  </td>
                </tr>
                <#-- ordered date -->
                <#if orderHeader?has_content>
                  <tr><td colspan="7"><hr /></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div>&nbsp;<b>${uiLabelMap.CommonDate}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                      <div>${orderHeader.orderDate.toString()}</div>
                    </td>
                  </tr>
                </#if>
                <#if distributorId??>
                  <tr><td colspan="7"><hr /></td></tr>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div>&nbsp;<b>${uiLabelMap.OrderDistributor}</b></div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                      <div>${distributorId}</div>
                    </td>
                  </tr>
                </#if>
            </table>
        </div>
    </div>
        ${screens.render("component://ecommerce/widget/OrderScreens.xml#quickAnonPaymentInformation")}
    </td>

    <td width="1">&nbsp;&nbsp;</td>
    <#-- right side -->

    <td width="50%" valign="top">
      <#if orderItemShipGroups?has_content>

    <div class="screenlet">
        <div class="screenlet-title-bar">
            <div class="h3">${uiLabelMap.OrderShippingInformation}</div>
        </div>
        <div class="screenlet-body">
        <#-- shipping address -->
            <#assign groupIdx = 0>
            <#list orderItemShipGroups as shipGroup>
                <#if orderHeader?has_content>
                  <#assign shippingAddress = shipGroup.getRelatedOne("PostalAddress", false)!>
                  <#assign groupNumber = shipGroup.shipGroupSeqId!>
                <#else>
                  <#assign shippingAddress = cart.getShippingAddress(groupIdx)!>
                  <#assign groupNumber = groupIdx + 1>
                </#if>

              <table width="100%" border="0" cellpadding="1">
                <#if shippingAddress?has_content>
                  <tr>
                    <td align="right" valign="top" width="15%">
                      <div>&nbsp;<b>${uiLabelMap.OrderDestination}</b> [${groupNumber}]</div>
                    </td>
                    <td width="5">&nbsp;</td>
                    <td valign="top" width="80%">
                      <div>
                        <#if shippingAddress.toName?has_content><b>${uiLabelMap.CommonTo}:</b> ${shippingAddress.toName}<br /></#if>
                        <#if shippingAddress.attnName?has_content><b>${uiLabelMap.PartyAddrAttnName}:</b> ${shippingAddress.attnName}<br /></#if>
                        ${shippingAddress.address1}<br />
                        <#if shippingAddress.address2?has_content>${shippingAddress.address2}<br /></#if>
                        ${shippingAddress.city}<#if shippingAddress.stateProvinceGeoId?has_content>, ${shippingAddress.stateProvinceGeoId} </#if>
                        ${shippingAddress.postalCode!}<br />
                        ${shippingAddress.countryGeoId!}
                      </div>
                    </td>
                  </tr>
                  <tr><td colspan="7"><hr /></td></tr>
                </#if>
                  <tr><td colspan="7">
                     ${screens.render("component://ecommerce/widget/OrderScreens.xml#quickAnonOptionSettings")}
                  </td></tr>
              </table>

                <#assign groupIdx = groupIdx + 1>
            </#list><#-- end list of orderItemShipGroups -->
        </div>
    </div>

      </#if>
    </td>
  </tr>
</table>
