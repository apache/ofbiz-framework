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
                    <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists>
                    <#assign orderReadyToPickInfoList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists>
                    <#assign orderNeedsStockMoveInfoList = pickMoveByShipmentMethodInfo.orderNeedsStockMoveInfoList?if_exists>
                    <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                    <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                    <#assign orderReadyToPickInfoListSizeTotal = orderReadyToPickInfoListSizeTotal + orderReadyToPickInfoListSize>
                    <#assign orderNeedsStockMoveInfoListSizeTotal = orderNeedsStockMoveInfoListSizeTotal + orderNeedsStockMoveInfoListSize>
                    <tr>
                        <td><a href="<@ofbizUrl>PicklistOptions?viewDetail=${shipmentMethodType.shipmentMethodTypeId?if_exists}&facilityId=${facilityId?if_exists}</@ofbizUrl>" class="linktext"><#if shipmentMethodType?exists && shipmentMethodType?has_content>${shipmentMethodType.description}<#else>${groupName?if_exists}</#if></a></td>
                        <td><div class="tabletext">${orderReadyToPickInfoListSize}</div></td>
                        <td><div class="tabletext">${orderNeedsStockMoveInfoListSize}</div></td>
                        <td>
                            <div class="tabletext">
                                <#if orderReadyToPickInfoList?has_content>
                                    <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                        <input type="hidden" name="facilityId" value="${facilityId}"/>
                                        <#if shipmentMethodType?exists && shipmentMethodType?has_content>
                                        <input type="hidden" name="shipmentMethodTypeId" value="${shipmentMethodType.shipmentMethodTypeId}"/>
                                        <#else>
                                            <input type="hidden" name="orderIdList" value=""/>
                                            <#assign orderIdsForPickList = orderReadyToPickInfoList?if_exists>
                                            <#list orderIdsForPickList as orderIdForPickList>
                                                <input type="hidden" name="orderIdList" value="${orderIdForPickList.orderHeader.orderId}"/>
                                            </#list>
                                        </#if>
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
<br/>

<#assign viewDetail = requestParameters.viewDetail?if_exists>
<#if viewDetail?has_content>
    <#list pickMoveByShipmentMethodInfoList as pickMoveByShipmentMethodInfo>
        <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists>
        <#if shipmentMethodType?if_exists.shipmentMethodTypeId == viewDetail>
            <#assign toPickList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists>
        </#if>                
    </#list>
</#if>

<#if toPickList?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${shipmentMethodType.description?if_exists} Detail</div>
    </div>
    <div class="screenlet-body">
        <table border="1" cellspacing="0" cellpadding="2">
            <tr>
                <#-- todo: internationalize -->
                <td><div class="tableheadtext">Order ID</div></td>
                <td><div class="tableheadtext">Order Date</div></td>
                <td><div class="tableheadtext">Channel</div></td>
                <td><div class="tableheadtext">Order Item ID</div></td>
                <td><div class="tableheadtext">Description</div></td>
                <td><div class="tableheadtext">Ship Grp ID</div></td>
                <td><div class="tableheadtext">Quantity</div></td>
            </tr>
            <#list toPickList as toPick>
                <#assign oiasgal = toPick.orderItemAndShipGroupAssocList>
                <#assign header = toPick.orderHeader>
                <#assign channel = header.getRelatedOne("SalesChannelEnumeration")?if_exists>

                <#list oiasgal as oiasga>
                    <#assign product = oiasga.getRelatedOne("Product")?if_exists>
                    <tr>
                        <td><a href="/ordermgr/control/orderview?orderId=${oiasga.orderId}${externalKeyParam}" class="linktext" target="_blank">${oiasga.orderId}</a></td>
                        <td><div class="tabletext">${header.orderDate?string}</div></td>
                        <td><div class="tabletext">${(channel.description)?if_exists}</div></td>
                        <td><div class="tabletext">${oiasga.orderItemSeqId}</div></td>
                        <td><a href="/catalog/control/EditProduct?productId=${oiasga.productId?if_exists}${externalKeyParam}" class="linktext" target="_blank">${(product.internalName)?if_exists}</a></td>
                        <td><div class="tabletext">${oiasga.shipGroupSeqId}</div></td>
                        <td><div class="tabletext">${oiasga.quantity}</div></td>
                    </tr>
                </#list>
                <tr>
                    <td colspan="7" bgcolor="#CCCCCC">&nbsp;</td>
                </tr>
            </#list>
        </table>
    </div>
</div>
</#if>
