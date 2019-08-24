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

<div class="screenlet order-info">
    <div class="screenlet-title-bar">
        <ul>
            <#if orderHeader.externalId?has_content>
               <#assign externalOrder = "(" + orderHeader.externalId + ")"/>
            </#if>
            <#assign orderType = orderHeader.getRelatedOne("OrderType", false)/>
            <li class="h3">&nbsp;${(orderType.get("description", locale))?default(uiLabelMap.OrderOrder)}&nbsp;${uiLabelMap.CommonNbr}&nbsp;<a href="<@ofbizUrl>orderview?orderId=${orderId}</@ofbizUrl>">${orderId}</a> ${externalOrder!} [&nbsp;<a href="<@ofbizUrl>order.pdf?orderId=${orderId}</@ofbizUrl>" target="_blank">${uiLabelMap.CommonPdf}</a>&nbsp;]</li>
            <div class="basic-nav">
              <ul>
            <#if "ORDER_APPROVED" == currentStatus.statusId && "SALES_ORDER" == orderHeader.orderTypeId>
              <li><a href="javascript:document.PrintOrderPickSheet.submit()">${uiLabelMap.FormFieldTitle_printPickSheet}</a>
              <form name="PrintOrderPickSheet" method="post" action="<@ofbizUrl>orderPickSheet.pdf</@ofbizUrl>" target="_BLANK">
                <input type="hidden" name="facilityId" value="${storeFacilityId!}"/>
                <input type="hidden" name="orderId" value="${orderHeader.orderId!}"/>
                <input type="hidden" name="maxNumberOfOrdersToPrint" value="1"/>
              </form>
              </li>
            </#if>
            <#if "ORDER_CREATED" == currentStatus.statusId || "ORDER_PROCESSING" == currentStatus.statusId>
              <li><a href="javascript:document.OrderApproveOrder.submit()">${uiLabelMap.OrderApproveOrder}</a>
              <form class ="basic-form" name="OrderApproveOrder" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_APPROVED"/>
                <input type="hidden" name="newStatusId" value="ORDER_APPROVED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId!}"/>
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="partyId" value="${assignPartyId!}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId!}"/>
                <input type="hidden" name="fromDate" value="${fromDate!}"/>
              </form>
              </li>
            <#elseif "ORDER_APPROVED" == currentStatus.statusId>
              <li><a href="javascript:document.OrderHold.submit()">${uiLabelMap.OrderHold}</a>
              <form class="basic-form" name="OrderHold" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_HOLD"/>
                <input type="hidden" name="workEffortId" value="${workEffortId!}"/>
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="partyId" value="${assignPartyId!}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId!}"/>
                <input type="hidden" name="fromDate" value="${fromDate!}"/>
              </form>
              </li>
            <#elseif "ORDER_HOLD" == currentStatus.statusId>
              <li><a href="javascript:document.OrderApproveOrder.submit()">${uiLabelMap.OrderApproveOrder}</a>
              <form class="basic-form" name="OrderApproveOrder" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_APPROVED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId!}"/>
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="partyId" value="${assignPartyId!}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId!}"/>
                <input type="hidden" name="fromDate" value="${fromDate!}"/>
              </form>
              </li>
            </#if>
            <#if currentStatus.statusId != "ORDER_COMPLETED" && currentStatus.statusId != "ORDER_CANCELLED">
              <li><a href="javascript:document.OrderCancel.submit()">${uiLabelMap.OrderCancelOrder}</a>
              <form class="basic-form" name="OrderCancel" method="post" action="<@ofbizUrl>changeOrderStatus/orderview</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_CANCELLED"/>
                <input type="hidden" name="setItemStatus" value="Y"/>
                <input type="hidden" name="workEffortId" value="${workEffortId!}"/>
                <input type="hidden" name="orderId" value="${orderId!}"/>
                <input type="hidden" name="partyId" value="${assignPartyId!}"/>
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId!}"/>
                <input type="hidden" name="fromDate" value="${fromDate!}"/>
              </form>
              </li>
            </#if>
            <#if setOrderCompleteOption>
              <li><a href="javascript:document.OrderCompleteOrder.submit()">${uiLabelMap.OrderCompleteOrder}</a>
              <form class="basic-form" name="OrderCompleteOrder" method="post" action="<@ofbizUrl>changeOrderStatus</@ofbizUrl>">
                <input type="hidden" name="statusId" value="ORDER_COMPLETED"/>
                <input type="hidden" name="orderId" value="${orderId!}"/>
              </form>
              </li>
            </#if>
            </ul>
          </div>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table class="basic-table form-table" cellspacing='0'>
            <#if orderHeader.orderName?has_content>
            <tr>
              <td class="label"><label>${uiLabelMap.OrderOrderName}</label></td>
              <td>${orderHeader.orderName}</td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            </#if>
            <#-- order status history -->
            <tr>
              <td class="label"><label>${uiLabelMap.OrderStatusHistory}</label></td>
              <td<#if currentStatus.statusCode?has_content> class="${currentStatus.statusCode}"</#if>>
                <span class="current-status">${uiLabelMap.OrderCurrentStatus}: ${currentStatus.get("description",locale)}</span>
                <#if orderHeaderStatuses?has_content>
                  <hr />
                  <#list orderHeaderStatuses as orderHeaderStatus>
                    <#assign loopStatusItem = orderHeaderStatus.getRelatedOne("StatusItem", false)>
                    <#assign userlogin = orderHeaderStatus.getRelatedOne("UserLogin", false)>
                    <div>
                      ${loopStatusItem.get("description",locale)} <#if orderHeaderStatus.statusDatetime?has_content>- ${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(orderHeaderStatus.statusDatetime, "", locale, timeZone)?default("0000-00-00 00:00:00")}</#if>
                      &nbsp;
                      ${uiLabelMap.CommonBy} - ${Static["org.apache.ofbiz.party.party.PartyHelper"].getPartyName(delegator, userlogin.getString("partyId"), false)}
                    </div>
                  </#list>
                </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderDateOrdered}</td>
              <td><#if orderHeader.orderDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(orderHeader.orderDate, "", locale, timeZone)!}</#if></td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.CommonCurrency}</td>
              <td>${orderHeader.currencyUom?default("???")}</td>
            </tr>
            <#if orderHeader.internalCode?has_content>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderInternalCode}</td>
              <td>${orderHeader.internalCode}</td>
            </tr>
            </#if>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderSalesChannel}</td>
              <td>
                  <#if orderHeader.salesChannelEnumId?has_content>
                    <#assign channel = orderHeader.getRelatedOne("SalesChannelEnumeration", false)>
                    ${(channel.get("description",locale))?default("N/A")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            <#if productStore?has_content>
              <tr>
                <td class="label">&nbsp;${uiLabelMap.OrderProductStore}</td>
                <td>
                  ${productStore.storeName!}&nbsp;<a href="/catalog/control/EditProductStore?productStoreId=${productStore.productStoreId}${StringUtil.wrapString(externalKeyParam)}" target="catalogmgr" class="buttontext">(${productStore.productStoreId})</a>
                </td>
              </tr>
              <tr><td colspan="3"><hr /></td></tr>
            </#if>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderOriginFacility}</td>
              <td>
                  <#if orderHeader.originFacilityId?has_content>
                    <a href="/facility/control/EditFacility?facilityId=${orderHeader.originFacilityId}${StringUtil.wrapString(externalKeyParam)}" target="facilitymgr" class="buttontext">${orderHeader.originFacilityId}</a>
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
              </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.CommonCreatedBy}</td>
              <td>
                  <#if orderHeader.createdBy?has_content>
                    <a href="/partymgr/control/viewprofile?userlogin_id=${orderHeader.createdBy}${StringUtil.wrapString(externalKeyParam)}" target="partymgr" class="buttontext">${orderHeader.createdBy}</a>
                  <#else>
                    ${uiLabelMap.CommonNotSet}
                  </#if>
              </td>
            </tr>
            <#if orderHeader.agreementId?has_content>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.AccountingAgreement}</td>
              <td>
                    <a href="<@ofbizUrl>/accounting/control/EditAgreement?agreementId=${orderHeader.agreementId}</@ofbizUrl>" class="buttontext">${orderHeader.agreementId}</a>
              </td>
            </tr>
            </#if>
            <#if (orderItem.cancelBackOrderDate)??>
              <tr><td colspan="3"><hr /></td></tr>
              <tr>
                <td class="label">&nbsp;${uiLabelMap.FormFieldTitle_cancelBackOrderDate}</td>
                <td><#if orderItem.cancelBackOrderDate?has_content>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].formatDateTime(orderItem.cancelBackOrderDate, "", locale, timeZone)!}</#if></td>
              </tr>
            </#if>
            <#if distributorId??>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderDistributor}</td>
                <td>
                  <#assign distPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", distributorId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${distPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
              </td>
            </tr>
            </#if>
            <#if affiliateId??>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.OrderAffiliate}</td>
              <td>
                  <#assign affPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", affiliateId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${affPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                </div>
              </td>
            </tr>
            </#if>
            <#if orderContentWrapper.get("IMAGE_URL", "url")?has_content>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.CommonAttachments}</td>
              <td>
                <#assign orderContents = EntityQuery.use(delegator).from("OrderContent").where("orderId", orderId!).queryList()!>
                <#if orderContents?has_content>
                  <#list orderContents as orderContent>
                    <div>
                      <a href="<@ofbizUrl>stream?contentId=${orderContent.contentId!}</@ofbizUrl>" target="_blank" class="buttontext">${orderContent.contentId}</a>
                    </div>
                  </#list>
                <#else>
                  <div>
                    ${uiLabelMap.CommonNo} ${uiLabelMap.CommonAttachments}
                  </div>
                </#if>
                <a href="<@ofbizUrl>AddOrderAttachments?orderId=${orderId!}</@ofbizUrl>" class="buttontext">Add Attachment</a>
              </td>
            </tr>
            </#if>
            <#if "SALES_ORDER" == orderHeader.orderTypeId>
            <tr><td colspan="3"><hr /></td></tr>
                <tr>
                  <td class="label">&nbsp;${uiLabelMap.FormFieldTitle_priority}</td>
                  <td>
                     <form class="basic-form" name="setOrderReservationPriority" method="post" action="<@ofbizUrl>setOrderReservationPriority</@ofbizUrl>">
                     <input type = "hidden" name="orderId" value="${orderId}"/>
                    <select name="priority">
                      <option value="1" <#if "1" == (orderHeader.priority)!>selected="selected" </#if>>${uiLabelMap.CommonHigh}</option>
                      <option value="2" <#if "2" == (orderHeader.priority)!>selected="selected" <#elseif !(orderHeader.priority)?has_content>selected="selected"</#if>>${uiLabelMap.CommonNormal}</option>
                      <option value="3" <#if "3" == (orderHeader.priority)!>selected="selected" </#if>>${uiLabelMap.CommonLow}</option>
                    </select>
                    <input type="submit" class="smallSubmit" value="${uiLabelMap.FormFieldTitle_reserveInventory}"/>
                    </form>
                  </td>
                </tr>
            </#if>
            <tr><td colspan="3"><hr /></td></tr>
            <tr>
              <td class="label">&nbsp;${uiLabelMap.AccountingInvoicePerShipment}</td>
              <td>
                 <form class="basic-form" name="setInvoicePerShipment" method="post" action="<@ofbizUrl>setInvoicePerShipment</@ofbizUrl>">
                 <input type = "hidden" name="orderId" value="${orderId}"/>
                <select name="invoicePerShipment">
                  <option value="Y" <#if "Y" == (orderHeader.invoicePerShipment)!>selected="selected" </#if>>${uiLabelMap.CommonYes}</option>
                  <option value="N" <#if "N" == (orderHeader.invoicePerShipment)!>selected="selected" </#if>>${uiLabelMap.CommonNo}</option>
                </select>
                <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonUpdate}"/>
                </form>
              </td>
            </tr>
            <tr><td colspan="3"><hr /></td></tr>
            <#if orderHeader.isViewed?has_content && "Y" == orderHeader.isViewed>
            <tr>
              <td class="label">${uiLabelMap.OrderViewed}</td>
              <td>
                ${uiLabelMap.CommonYes}
              </td>
            </tr>
            <#else>
            <tr id="isViewed">
              <td class="label"><label for="checkViewed">${uiLabelMap.OrderMarkViewed}</label></td>
              <td>
                <form class="basic-form" id="orderViewed" action="">
                  <input type="checkbox" id="checkViewed" name="checkViewed" onclick="javascript:markOrderViewed();"/>
                  <input type="hidden" name="orderId" value="${orderId!}"/>
                  <input type="hidden" name="isViewed" value="Y"/>
                </form>
              </td>
            </tr>
            <tr id="viewed" style="display: none;">
              <td class="label">${uiLabelMap.OrderViewed}</td>
              <td>
                ${uiLabelMap.CommonYes}
              </td>
            </tr>
            </#if>
        </table>
    </div>
</div>
