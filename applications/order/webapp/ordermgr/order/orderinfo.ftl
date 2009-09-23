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

<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <#if orderHeader.externalId?has_content>
               <#assign externalOrder = "(" + orderHeader.externalId + ")"/>
            </#if>
            <#assign orderType = orderHeader.getRelatedOne("OrderType")/>
            <li class="h3">&nbsp;${orderType?if_exists.get("description", locale)?default(uiLabelMap.OrderOrder)}&nbsp;#<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>">${orderId}</a> ${externalOrder?if_exists} [&nbsp;<a href="<@ofbizUrl>order.pdf?orderId=${orderId}</@ofbizUrl>" target="_blank">PDF</a>&nbsp;]</li>
            <#if currentStatus.statusId == "ORDER_APPROVED" && orderHeader.orderTypeId == "SALES_ORDER">
              <li class="h3"><a href="javascript:document.PrintOrderPickSheet.submit()">${uiLabelMap.FormFieldTitle_printPickSheet}</a></li>
              <form name="PrintOrderPickSheet" method="post" action="<@ofbizUrl>orderPickSheet.pdf</@ofbizUrl>">
                <input type="hidden" name="facilityId" value="${storeFacilityId?if_exists}"/>
                <input type="hidden" name="orderId" value="${orderHeader.orderId?if_exists}"/>
                <input type="hidden" name="maxNumberOfOrdersToPrint" value="1"/>
              </form>
            </#if>
            <#if currentStatus.statusId == "ORDER_CREATED" || currentStatus.statusId == "ORDER_PROCESSING">
              <li><a href="javascript:document.OrderApproveOrder.submit()">${uiLabelMap.OrderApproveOrder}</a></li>
              <form name="OrderApproveOrder" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_APPROVED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId?if_exists}"/>
                <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                <input type="hidden" name="partyId" value="${assignPartyId?if_exists}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId?if_exists}"/>
                <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
              </form>
            <#elseif currentStatus.statusId == "ORDER_APPROVED">
              <li><a href="javascript:document.OrderHold.submit()">${uiLabelMap.OrderHold}</a></li>
              <form name="OrderHold" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_HOLD"/>
                <input type="hidden" name="workEffortId" value="${workEffortId?if_exists}"/>
                <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                <input type="hidden" name="partyId" value="${assignPartyId?if_exists}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId?if_exists}"/>
                <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
              </form>
            <#elseif currentStatus.statusId == "ORDER_HOLD">
              <li><a href="javascript:document.OrderApproveOrder.submit()">${uiLabelMap.OrderApproveOrder}</a></li>
              <form name="OrderApproveOrder" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_APPROVED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId?if_exists}"/>
                <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                <input type="hidden" name="partyId" value="${assignPartyId?if_exists}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId?if_exists}"/>
                <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
              </form>
            </#if>
            <#if currentStatus.statusId != "ORDER_COMPLETED" && currentStatus.statusId != "ORDER_CANCELLED">
              <li><a href="javascript:document.OrderCancel.submit()">${uiLabelMap.OrderCancelOrder}</a></li>
              <form name="OrderCancel" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_CANCELLED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId?if_exists}"/>
                <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                <input type="hidden" name="partyId" value="${assignPartyId?if_exists}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId?if_exists}"/>
                <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
              </form>
            </#if>             
            <#if setOrderCompleteOption>
              <li><a href="javascript:document.OrderCompleteOrder.submit()">${uiLabelMap.OrderCompleteOrder}</a></li>
              <form name="OrderCompleteOrder" method="post" action="<@ofbizUrl>changeOrderStatus</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_COMPLETED"/>
                <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
              </form>
            </#if>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table class="basic-table" cellspacing='0'>
            <#if orderHeader.orderName?has_content>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderOrderName}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">${orderHeader.orderName}</td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            </#if>
            <#-- order status history -->
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderStatusHistory}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                ${uiLabelMap.OrderCurrentStatus}: ${currentStatus.get("description",locale)}
                <#if orderHeaderStatuses?has_content>
                  <hr/>
                  <#list orderHeaderStatuses as orderHeaderStatus>
                    <#assign loopStatusItem = orderHeaderStatus.getRelatedOne("StatusItem")>
                    <#assign userlogin = orderHeaderStatus.getRelatedOne("UserLogin")>
                    <div>
                      ${loopStatusItem.get("description",locale)} - ${orderHeaderStatus.statusDatetime?default("0000-00-00 00:00:00")?string}
                      &nbsp;
                      ${uiLabelMap.CommonBy} - <#--${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, userlogin.getString("partyId"), true)}--> [${orderHeaderStatus.statusUserLogin}]
                    </div>
                  </#list>
                </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderDateOrdered}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">${orderHeader.orderDate.toString()}</td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.CommonCurrency}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">${orderHeader.currencyUom?default("???")}</td>
            </tr>
            <#if orderHeader.internalCode?has_content>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderInternalCode}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">${orderHeader.internalCode}</td>
            </tr>
            </#if>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderSalesChannel}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#if orderHeader.salesChannelEnumId?has_content>
                    <#assign channel = orderHeader.getRelatedOne("SalesChannelEnumeration")>
                    ${(channel.get("description",locale))?default("N/A")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderProductStore}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#if orderHeader.productStoreId?has_content>
                    <a href="/catalog/control/EditProductStore?productStoreId=${orderHeader.productStoreId}${externalKeyParam}" target="catalogmgr" class="buttontext">${orderHeader.productStoreId}</a>
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderOriginFacility}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#if orderHeader.originFacilityId?has_content>
                    <a href="/facility/control/EditFacility?facilityId=${orderHeader.originFacilityId}${externalKeyParam}" target="facilitymgr" class="buttontext">${orderHeader.originFacilityId}</a>
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.CommonCreatedBy}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#if orderHeader.createdBy?has_content>
                    <a href="/partymgr/control/viewprofile?userlogin_id=${orderHeader.createdBy}${externalKeyParam}" target="partymgr" class="buttontext">${orderHeader.createdBy}</a>
                  <#else>
                    ${uiLabelMap.CommonNotSet}
                  </#if>
              </td>
            </tr>
            <#if orderItem.cancelBackOrderDate?exists>
              <tr><td colspan="3"><hr/></td></tr>
              <tr>
                <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.FormFieldTitle_cancelBackOrderDate}</td>
                <td width="5%">&nbsp;</td>
                <td valign="top" width="80%">${orderItem.cancelBackOrderDate?if_exists}</td>
              </tr>
            </#if>
            <#if distributorId?exists>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderDistributor}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#assign distPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", distributorId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${distPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
              </td>
            </tr>
            </#if>
            <#if affiliateId?exists>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderAffiliate}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#assign affPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", affiliateId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${affPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                </div>
              </td>
            </tr>
            </#if>
            <#if orderContentWrapper.get("IMAGE_URL")?has_content>
            <tr><td colspan="3"><hr/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderImage}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <a href="<@ofbizUrl>viewimage?orderId=${orderId}&orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
              </td>
            </tr>
            </#if>
            <#if "SALES_ORDER" == orderHeader.orderTypeId>
              <form action="setOrderReservationPriority" method="post" name="setOrderReservationPriority">
                <input type = "hidden" name="orderId" value="${orderId}"/>
                <tr><td colspan="3"><hr/></td></tr>
                <tr>
                  <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.FormFieldTitle_priority}</td>
                  <td width="5%">&nbsp;</td>
                  <td valign="top" width="80%">
                    <select name="priority">
                      <option value="1" <#if (orderHeader.priority)?if_exists == "1">selected="selected" </#if>>${uiLabelMap.CommonHigh}</option>
                      <option value="2" <#if (orderHeader.priority)?if_exists == "2">selected="selected" <#elseif !(orderHeader.priority)?has_content>selected="selected"</#if>>${uiLabelMap.CommonNormal}</option>
                      <option value="3" <#if (orderHeader.priority)?if_exists == "3">selected="selected" </#if>>${uiLabelMap.CommonLow}</option>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.FormFieldTitle_reserveInventory}"/>
                  </td>
                </tr>
                <tr><td colspan="3"><hr/></td></tr>
              </form>
            </#if>
            <#if orderHeader.isViewed?has_content && orderHeader.isViewed == "Y">
            <tr>
              <td class="label">${uiLabelMap.OrderViewed}</td>
              <td width="5%"></td>
              <td valign="top" width="80%">
                ${uiLabelMap.CommonYes}
              </td>
            </tr>
            <#else>
            <tr id="isViewed">
              <td class="label">${uiLabelMap.OrderMarkViewed}</td>
              <td width="5%"></td>
              <td valign="top" width="80%">
                <form id="orderViewed">
                  <input type="checkbox" name="checkViewed" onclick="javascript:markOrderViewed();"/>
                  <input type="hidden" name="orderId" value="${orderId?if_exists}"/>
                  <input type="hidden" name="isViewed" value="Y"/>
                </form>
              </td>
            </tr>
            <tr id="viewed" style="display: none;">
              <td class="label">${uiLabelMap.OrderViewed}</td>
              <td width="5%"></td>
              <td valign="top" width="80%">
                ${uiLabelMap.CommonYes}
              </td>
            </tr>
            </#if>
        </table>
    </div>
</div>