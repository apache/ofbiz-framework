<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-half">
            <a href="<@ofbizUrl>PicklistOptions?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="submenutext">${uiLabelMap.ProductPicklistOptions}</a>
            <a href="<@ofbizUrl>PicklistManage?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="submenutext">${uiLabelMap.ProductPicklistManage}</a>
            <a href="<@ofbizUrl>PickMoveStock?facilityId=${facilityId?if_exists}</@ofbizUrl>" class="submenutextright">${uiLabelMap.ProductStockMoves}</a>
        </div>
        <div class="boxhead">${uiLabelMap.ProductFindOrdersToPick}</div>
    </div>
    <div class="screenlet-body">
        <table border="1" cellspacing="0" cellpadding="2">
            <tr>
                <td><div class="tableheadtext">${uiLabelMap.ProductShipmentMethod}</div></td>
                <td><div class="tableheadtext">${uiLabelMap.ProductReadyToPick}</div></td>
                <td><div class="tableheadtext">${uiLabelMap.ProductNeedStockMove}</div></td>
                <td><div class="tableheadtext">&nbsp;</div></td>
            </tr>
            <#if rushOrderInfo?has_content>
                <#assign orderReadyToPickInfoList = rushOrderInfo.orderReadyToPickInfoList?if_exists>
                <#assign orderNeedsStockMoveInfoList = rushOrderInfo.orderNeedsStockMoveInfoList?if_exists>
                <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                <tr>
                    <td><div class="tabletext">[Rush Orders, all Methods]</div></td>
                    <td><div class="tabletext">${orderReadyToPickInfoListSize}</div></td>
                    <td><div class="tabletext">${orderNeedsStockMoveInfoListSize}</div></td>
                    <td>
                        <div class="tabletext">
                            <#if orderReadyToPickInfoList?has_content>
                                <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                    <input type="hidden" name="facilityId" value="${facilityId}"/>
                                    <input type="hidden" name="isRushOrder" value="Y"/>
                                    ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20" class="inputBox"/>
                                    <input type="submit" value="${uiLabelMap.ProductCreatePicklist}" class="smallSubmit"/>
                                </form>
                            <#else>
                                &nbsp;
                            </#if>
                        </div>
                    </td>
                </tr>
            </#if>
            <#if pickMoveByShipmentMethodInfoList?has_content>
                <#assign orderReadyToPickInfoListSizeTotal = 0>
                <#assign orderNeedsStockMoveInfoListSizeTotal = 0>
                <#list pickMoveByShipmentMethodInfoList as pickMoveByShipmentMethodInfo>
                    <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType>
                    <#assign orderReadyToPickInfoList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists>
                    <#assign orderNeedsStockMoveInfoList = pickMoveByShipmentMethodInfo.orderNeedsStockMoveInfoList?if_exists>
                    <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                    <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                    <#assign orderReadyToPickInfoListSizeTotal = orderReadyToPickInfoListSizeTotal + orderReadyToPickInfoListSize>
                    <#assign orderNeedsStockMoveInfoListSizeTotal = orderNeedsStockMoveInfoListSizeTotal + orderNeedsStockMoveInfoListSize>
                    <tr>
                        <td><div class="tabletext">${shipmentMethodType.description}</div></td>
                        <td><div class="tabletext">${orderReadyToPickInfoListSize}</div></td>
                        <td><div class="tabletext">${orderNeedsStockMoveInfoListSize}</div></td>
                        <td>
                            <div class="tabletext">
                                <#if orderReadyToPickInfoList?has_content>
                                    <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                                        <input type="hidden" name="shipmentMethodTypeId" value="${shipmentMethodType.shipmentMethodTypeId}"/>
                                        ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20" class="inputBox"/>
                                        <input type="submit" value="${uiLabelMap.ProductCreatePicklist}" class="smallSubmit"/>
                                    </form>
                                <#else>
                                    &nbsp;
                                </#if>
                            </div>
                        </td>
                    </tr>
                </#list>
                <tr>
                    <td><div class="tableheadtext">${uiLabelMap.CommonAllMethods}</div></td>
                    <td><div class="tableheadtext">${orderReadyToPickInfoListSizeTotal}</div></td>
                    <td><div class="tableheadtext">${orderNeedsStockMoveInfoListSizeTotal}</div></td>
                    <td>
                        <div class="tabletext">
                          <#if (orderReadyToPickInfoListSizeTotal > 0)>
                            <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                <input type="hidden" name="facilityId" value="${facilityId}"/>
                                ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20" class="inputBox"/>
                                <input type="submit" value="${uiLabelMap.ProductCreatePicklist}" class="smallSubmit"/>
                            </form>
                          <#else>
                            &nbsp;
                          </#if>
                        </div>
                    </td>
                </tr>
            <#else>
                <tr><td colspan="4"><div class="head3">${uiLabelMap.ProductNoOrdersFoundReadyToPickOrNeedStockMoves}.</div></td></tr>
            </#if>
        </table>
    </div>
</div>
