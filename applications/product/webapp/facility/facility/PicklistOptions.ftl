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
            <li class="h3">${uiLabelMap.ProductFindOrdersToPick}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td>${uiLabelMap.ProductShipmentMethod}</td>
                <td>${uiLabelMap.ProductReadyToPick}</td>
                <td>${uiLabelMap.ProductNeedStockMove}</td>
                <td>&nbsp;</td>
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
                <#assign alt_row = false>
                <#list pickMoveByShipmentMethodInfoList as pickMoveByShipmentMethodInfo>
                    <#assign shipmentMethodType = pickMoveByShipmentMethodInfo.shipmentMethodType?if_exists>
                    <#assign orderReadyToPickInfoList = pickMoveByShipmentMethodInfo.orderReadyToPickInfoList?if_exists>
                    <#assign orderNeedsStockMoveInfoList = pickMoveByShipmentMethodInfo.orderNeedsStockMoveInfoList?if_exists>
                    <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                    <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                    <#assign orderReadyToPickInfoListSizeTotal = orderReadyToPickInfoListSizeTotal + orderReadyToPickInfoListSize>
                    <#assign orderNeedsStockMoveInfoListSizeTotal = orderNeedsStockMoveInfoListSizeTotal + orderNeedsStockMoveInfoListSize>
                    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                        <td><a href="<@ofbizUrl>PicklistOptions?viewDetail=${shipmentMethodType.shipmentMethodTypeId?if_exists}&facilityId=${facilityId?if_exists}</@ofbizUrl>" class="buttontext"><#if shipmentMethodType?exists && shipmentMethodType?has_content>${shipmentMethodType.description}<#else>${groupName?if_exists}</#if></a></td>
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
                                    <span class="label">${uiLabelMap.ProductPickFirst}</span>
                                    <input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                                    <input type="submit" value="${uiLabelMap.ProductCreatePicklist}"/>
                                </form>
                            <#else>
                                &nbsp;
                            </#if>
                        </td>
                    </tr>
                    <#-- toggle the row color -->
                    <#assign alt_row = !alt_row>
                </#list>
                <tr>
                    <th>${uiLabelMap.CommonAllMethods}</div></th>
                    <th>${orderReadyToPickInfoListSizeTotal}</div></th>
                    <th>${orderNeedsStockMoveInfoListSizeTotal}</div></th>
                    <td>
                      <#if (orderReadyToPickInfoListSizeTotal > 0)>
                        <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                            <input type="hidden" name="facilityId" value="${facilityId}"/>
                            <span class="label">${uiLabelMap.ProductPickFirst}</span>
                            <input type="text" size="4" name="maxNumberOfOrders" value="20"/>
                            <input type="submit" value="${uiLabelMap.ProductCreatePicklist}"/>
                        </form>
                      <#else>
                        &nbsp;
                      </#if>
                    </td>
                </tr>
            <#else>
                <tr><td colspan="4"><h3>${uiLabelMap.ProductNoOrdersFoundReadyToPickOrNeedStockMoves}.</h3></td></tr>
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
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${shipmentMethodType.description?if_exists} ${uiLabelMap.ProductPickingDetail}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td>${uiLabelMap.ProductOrderId}</td>
                <td>${uiLabelMap.FormFieldTitle_orderDate}</td>
                <td>${uiLabelMap.ProductChannel}</td>
                <td>${uiLabelMap.ProductOrderItem}</td>
                <td>${uiLabelMap.Description}</td>
                <td>${uiLabelMap.ProductOrderShipGroupId}</td>
                <td>${uiLabelMap.ProductQuantity}</td>
                <td>${uiLabelMap.ProductQuantityNotAvailable}</td>
            </tr>
            <#assign alt_row = false>
            <#list toPickList as toPick>
                <#assign oiasgal = toPick.orderItemShipGrpInvResList>
                <#assign header = toPick.orderHeader>
                <#assign channel = header.getRelatedOne("SalesChannelEnumeration")?if_exists>

                <#list oiasgal as oiasga>
                    <#assign orderProduct = oiasga.getRelatedOne("OrderItem").getRelatedOne("Product")?if_exists>
                    <#assign product = oiasga.getRelatedOne("InventoryItem").getRelatedOne("Product")?if_exists>
                    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                        <td><a href="/ordermgr/control/orderview?orderId=${oiasga.orderId}${externalKeyParam}" class="buttontext" target="_blank">${oiasga.orderId}</a></td>
                        <td>${header.orderDate?string}</td>
                        <td>${(channel.description)?if_exists}</td>
                        <td>${oiasga.orderItemSeqId}</td>
                        <td>
                            <a href="/catalog/control/EditProduct?productId=${orderProduct.productId?if_exists}${externalKeyParam}" class="buttontext" target="_blank">${(orderProduct.internalName)?if_exists}</a>
                            <#if orderProduct.productId != product.productId>
                                &nbsp;[<a href="/catalog/control/EditProduct?productId=${product.productId?if_exists}${externalKeyParam}" class="buttontext" target="_blank">${(product.internalName)?if_exists}</a>]
                            </#if>
                        </td>
                        <td>${oiasga.shipGroupSeqId}</td>
                        <td>${oiasga.quantity}</td>
                        <td>${oiasga.quantityNotAvailable?if_exists}</td>
                    </tr>
                </#list>
                <#-- toggle the row color -->
                <#assign alt_row = !alt_row>
            </#list>
        </table>
    </div>
</div>
</#if>
