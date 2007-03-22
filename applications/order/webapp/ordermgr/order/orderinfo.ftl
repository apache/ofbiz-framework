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
    <div class="screenlet-header">
        <div class="boxlink">
            <#if currentStatus.statusId == "ORDER_CREATED" || currentStatus.statusId == "ORDER_PROCESSING">
                <div class="tabletext"><a href="<@ofbizUrl>changeOrderItemStatus?statusId=ITEM_APPROVED&amp;${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderApproveOrder}</a></div>
            <#elseif currentStatus.statusId == "ORDER_APPROVED">
                <div class="tabletext"><a href="<@ofbizUrl>changeOrderStatus/orderview?statusId=ORDER_HOLD&amp;${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderHold}</a></div>
            <#elseif currentStatus.statusId == "ORDER_HOLD">
                <div class="tabletext"><a href="<@ofbizUrl>changeOrderItemStatus?statusId=ITEM_APPROVED&amp;${paramString}</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderApproveOrder}</a></div>
            </#if>
            <#if setOrderCompleteOption>
                  <div class="tabletext"><a href="<@ofbizUrl>changeOrderStatus?orderId=${orderId}&statusId=ORDER_COMPLETED</@ofbizUrl>" class="buttontext">${uiLabelMap.OrderCompleteOrder}</a></div>
            </#if>
        </div>
        <#if orderHeader.externalId?has_content>
          <#assign externalOrder = "(" + orderHeader.externalId + ")"/>
        </#if>
        <div class="boxhead">&nbsp;${uiLabelMap.OrderOrder}&nbsp;#<a href="<@ofbizUrl>/orderview?orderId=${orderId}</@ofbizUrl>" class="lightbuttontext">${orderId}</a>&nbsp;${externalOrder?if_exists} ${uiLabelMap.CommonInformation} [<a href="<@ofbizUrl>order.pdf?orderId=${orderId}</@ofbizUrl>" class="buttontext" target="_blank">PDF</a> ]</div>
    </div>
    <div class="screenlet-body">
          <table width="100%" border="0" cellpadding="1" cellspacing="0">
            <#-- order name -->
            <#if orderHeader.orderName?has_content>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOrderName}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">${orderHeader.orderName}</div> 
              </td>  
            </tr>    
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            </#if>   
            <#-- order status history -->
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderStatusHistory}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">${uiLabelMap.OrderCurrentStatus}: ${currentStatus.get("description",locale)}</div>
                <#if orderHeaderStatuses?has_content>
                  <hr class="sepbar"/>
                  <#list orderHeaderStatuses as orderHeaderStatus>
                    <#assign loopStatusItem = orderHeaderStatus.getRelatedOne("StatusItem")>
                    <div class="tabletext">
                      ${loopStatusItem.get("description",locale)} - ${orderHeaderStatus.statusDatetime?default("0000-00-00 00:00:00")?string}
                    </div>
                  </#list>
                </#if>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDateOrdered}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  ${orderHeader.orderDate.toString()}
                </div>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.CommonCurrency}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  ${orderHeader.currencyUom?default("???")}
                </div>
              </td>
            </tr>
            <#if orderHeader.internalCode?has_content>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderInternalCode}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  ${orderHeader.internalCode}
                </div>
              </td>
            </tr>
            </#if>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderSalesChannel}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if orderHeader.salesChannelEnumId?has_content>
                    <#assign channel = orderHeader.getRelatedOne("SalesChannelEnumeration")>
                    ${(channel.get("description",locale))?default("N/A")}
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                </div>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderProductStore}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if orderHeader.productStoreId?has_content>
                    <a href="/catalog/control/EditProductStore?productStoreId=${orderHeader.productStoreId}" target="catalogmgr" class="buttontext">${orderHeader.productStoreId}</a>
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                </div>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderOriginFacility}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if orderHeader.originFacilityId?has_content>
                    <a href="/facility/control/EditFacility?facilityId=${orderHeader.originFacilityId}${externalKeyParam}" target="facilitymgr" class="buttontext">${orderHeader.originFacilityId}</a>
                  <#else>
                    ${uiLabelMap.CommonNA}
                  </#if>
                </div>
              </td>
            </tr>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderCreatedBy}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#if orderHeader.createdBy?has_content>
                    <a href="/partymgr/control/viewprofile?userlogin_id=${orderHeader.createdBy}" target="partymgr" class="buttontext">${orderHeader.createdBy}</a>
                  <#else>
                    [${uiLabelMap.CommonNotSet}]
                  </#if>
                </div>
              </td>
            </tr>

            <#if distributorId?exists>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderDistributor}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#assign distPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", distributorId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${distPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                </div>
              </td>
            </tr>
            </#if>
            <#if affiliateId?exists>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderAffiliate}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <#assign affPartyNameResult = dispatcher.runSync("getPartyNameForDate", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", affiliateId, "compareDate", orderHeader.orderDate, "userLogin", userLogin))/>
                  ${affPartyNameResult.fullName?default("[${uiLabelMap.OrderPartyNameNotFound}]")}
                </div>
              </td>
            </tr>
            </#if>

            <#if orderContentWrapper.get("IMAGE_URL")?has_content>
            <tr><td colspan="7"><hr class="sepbar"/></td></tr>
            <tr>
              <td align="right" valign="top" width="15%">
                <div class="tabletext">&nbsp;<b>${uiLabelMap.OrderImage}</b></div>
              </td>
              <td width="5">&nbsp;</td>
              <td align="left" valign="top" width="80%">
                <div class="tabletext">
                  <a href="<@ofbizUrl>viewimage?orderId=${orderId}&orderContentTypeId=IMAGE_URL</@ofbizUrl>" target="_orderImage" class="buttontext">${uiLabelMap.OrderViewImage}</a>
                </div>
              </td>
            </tr>
            </#if>
          </table>
    </div>
</div>
