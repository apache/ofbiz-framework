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
        <table class="basic-table">
            <tr>
                <th>${uiLabelMap.ProductShipmentMethod}</th>
                <th>${uiLabelMap.ProductReadyToPick}</th>
                <th>${uiLabelMap.ProductNeedStockMove}</th>
                <th>&nbsp;</th>
            </tr>
            <#if rushOrderInfo?has_content>
                <#assign orderReadyToPickInfoList = rushOrderInfo.orderReadyToPickInfoList?if_exists>
                <#assign orderNeedsStockMoveInfoList = rushOrderInfo.orderNeedsStockMoveInfoList?if_exists>
                <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                <tr>
                    <td>[Rush Orders, all Methods]</td>
                    <td>${orderReadyToPickInfoListSize}</td>
                    <td>${orderNeedsStockMoveInfoListSize}</td>
                    <td>
                        <#if orderReadyToPickInfoList?has_content>
                            <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                <input type="hidden" name="facilityId" value="${facilityId}"/>
                                <input type="hidden" name="isRushOrder" value="Y"/>
                                ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                                <input type="submit" value="${uiLabelMap.ProductCreatePicklist}"/>
                            </form>
                        <#else>
                            &nbsp;
                        </#if>
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
                        <td>${orderReadyToPickInfoListSize}</td>
                        <td>${orderNeedsStockMoveInfoListSize}</td>
                        <td>
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
                                    ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                                    <input type="submit" value="${uiLabelMap.ProductCreatePicklist}"/>
                                </form>
                            <#else>
                                &nbsp;
                            </#if>
                        </td>
                    </tr>
                </#list>
                <tr>
                    <th>${uiLabelMap.CommonAllMethods}</div></th>
                    <th>${orderReadyToPickInfoListSizeTotal}</div></th>
                    <th>${orderNeedsStockMoveInfoListSizeTotal}</div></th>
                    <td>
                      <#if (orderReadyToPickInfoListSizeTotal > 0)>
                        <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                            <input type="hidden" name="facilityId" value="${facilityId}"/>
                            ${uiLabelMap.ProductPickFirst}:<input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                            <input type="submit" value="${uiLabelMap.ProductCreatePicklist}"/>
                        </form>
                      <#else>
                        &nbsp;
                      </#if>
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
        <table class="basic-table">
            <tr>
                <th>Order ID</th>
                <th>Order Date</th>
                <th>Channel</th>
                <th>Order Item Id</th>
                <th>Description</th>
                <th>Ship Grp Id</th>
                <th>Quantity</th>
            </tr>
            <#list toPickList as toPick>
                <#assign oiasgal = toPick.orderItemAndShipGroupAssocList>
                <#assign header = toPick.orderHeader>
                <#assign channel = header.getRelatedOne("SalesChannelEnumeration")?if_exists>

                <#list oiasgal as oiasga>
                    <#assign product = oiasga.getRelatedOne("Product")?if_exists>
                    <tr>
                        <td><a href="/ordermgr/control/orderview?orderId=${oiasga.orderId}${externalKeyParam}" class="linktext" target="_blank">${oiasga.orderId}</a></td>
                        <td>${header.orderDate?string}</td>
                        <td>${(channel.description)?if_exists}</td>
                        <td>${oiasga.orderItemSeqId}</td>
                        <td><a href="/catalog/control/EditProduct?productId=${oiasga.productId?if_exists}${externalKeyParam}" class="linktext" target="_blank">${(product.internalName)?if_exists}</a></td>
                        <td>${oiasga.shipGroupSeqId}</td>
                        <td>${oiasga.quantity}</td>
                    </tr>
                </#list>
                <tr>
                    <td colspan="7"><hr/></td>
                </tr>
            </#list>
        </table>
    </div>
</div>
</#if>
