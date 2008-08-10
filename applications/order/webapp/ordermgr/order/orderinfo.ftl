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
            <li class="h3">&nbsp;${orderType?if_exists.get("description", locale)?default(uiLabelMap.OrderOrder)}&nbsp;#<a href="<@ofbizUrl>/orderview?orderId=${orderId}</@ofbizUrl>">${orderId}</a> ${externalOrder?if_exists} [&nbsp;<a href="<@ofbizUrl>order.pdf?orderId=${orderId}</@ofbizUrl>" target="_blank">PDF</a>&nbsp;]</li>
                       
            <#if currentStatus.statusId == "ORDER_CREATED" || currentStatus.statusId == "ORDER_PROCESSING">
                <li><a href="<@ofbizUrl>changeOrderStatus/orderview?statusId=ORDER_APPROVED&amp;setItemStatus=Y&amp;${paramString}</@ofbizUrl>">${uiLabelMap.OrderApproveOrder}</a></li>
            <#elseif currentStatus.statusId == "ORDER_APPROVED">
                <li><a href="<@ofbizUrl>changeOrderStatus/orderview?statusId=ORDER_HOLD&amp;${paramString}</@ofbizUrl>">${uiLabelMap.OrderHold}</a></li>
            <#elseif currentStatus.statusId == "ORDER_HOLD">
                <li><a href="<@ofbizUrl>changeOrderStatus/orderview?statusId=ORDER_APPROVED&amp;setItemStatus=Y&amp;${paramString}</@ofbizUrl>">${uiLabelMap.OrderApproveOrder}</a></li>
            </#if>
            <#if setOrderCompleteOption>
              <li><a href="<@ofbizUrl>changeOrderStatus?orderId=${orderId}&statusId=ORDER_COMPLETED</@ofbizUrl>">${uiLabelMap.OrderCompleteOrder}</a></li>
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
              <td align="right" valign="top" width="15%" class="label">&nbsp;${uiLabelMap.OrderCreatedBy}</td>
              <td width="5%">&nbsp;</td>
              <td valign="top" width="80%">
                  <#if orderHeader.createdBy?has_content>
                    <a href="/partymgr/control/viewprofile?userlogin_id=${orderHeader.createdBy}${externalKeyParam}" target="partymgr" class="buttontext">${orderHeader.createdBy}</a>
                  <#else>
                    ${uiLabelMap.CommonNotSet}
                  </#if>
              </td>
            </tr>
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
        </table>
    </div>
</div>