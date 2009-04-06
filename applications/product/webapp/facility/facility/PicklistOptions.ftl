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
            <li class="h3">${uiLabelMap.FacilitySelectOptionsToGroupBy}</li>
        </ul>
        <br class="clear"/>
    </div>
    <form method="post" name="selectFactors" action="<@ofbizUrl>PicklistOptions</@ofbizUrl>">
        <input type="hidden" name="facilityId" value="${facilityId}"/>
        <table class="basic-table" cellspacing='0'>
            <tr>
                <td class='label'>${uiLabelMap.FacilityGroupByShippingMethod}</td>
                <td><input type="checkbox" name="groupByShippingMethod" value="Y" <#if "${requestParameters.groupByWarehouseArea?if_exists}" != "Y" && "${requestParameters.groupByNoOfOrderItems?if_exists}" != "Y" || "${requestParameters.groupByShippingMethod?if_exists}" == "Y">checked</#if>/></td>

                <td class='label'>${uiLabelMap.FacilityGroupByWarehouseArea}</td>
                <td><input type="checkbox" name="groupByWarehouseArea" value="Y" <#if "${requestParameters.groupByWarehouseArea?if_exists}" == "Y">checked</#if>/></td>

                <td class='label'>${uiLabelMap.FacilityGroupByNoOfOrderItems}</td>
                <td><input type="checkbox" name="groupByNoOfOrderItems" value="Y" <#if "${requestParameters.groupByNoOfOrderItems?if_exists}" == "Y">checked</#if>/></td>
            </tr>
            <tr>&nbsp;</tr>
        </table>
        <div align ='right'>
            <input type="submit" value="Submit" class="buttontext" align='right'/>
        </div>
    </form>
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductFindOrdersToPick}</li>
        </ul>
        <br class="clear"/>
    </div>
    <div class="screenlet-body">
        <table cellspacing="0" class="basic-table">
            <tr class="header-row">
                <td>${uiLabelMap.FacilityGroupName}</td>
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
            <#if pickMoveInfoList?has_content>
                <#assign orderReadyToPickInfoListSizeTotal = 0>
                <#assign orderNeedsStockMoveInfoListSizeTotal = 0>
                <#assign alt_row = false>
                <#assign seq = 0>
                <#list pickMoveInfoList as pickMoveInfo>
                    <#assign groupName = pickMoveInfo.groupName?if_exists>
                    <#assign groupName1 = pickMoveInfo.groupName1?if_exists>
                    <#assign groupName2 = pickMoveInfo.groupName2?if_exists>
                    <#assign groupName3 = pickMoveInfo.groupName3?if_exists>
                    <#assign orderReadyToPickInfoList = pickMoveInfo.orderReadyToPickInfoList?if_exists>
                    <#assign orderNeedsStockMoveInfoList = pickMoveInfo.orderNeedsStockMoveInfoList?if_exists>
                    <#assign orderReadyToPickInfoListSize = (orderReadyToPickInfoList.size())?default(0)>
                    <#assign orderNeedsStockMoveInfoListSize = (orderNeedsStockMoveInfoList.size())?default(0)>
                    <#assign orderReadyToPickInfoListSizeTotal = orderReadyToPickInfoListSizeTotal + orderReadyToPickInfoListSize>
                    <#assign orderNeedsStockMoveInfoListSizeTotal = orderNeedsStockMoveInfoListSizeTotal + orderNeedsStockMoveInfoListSize>
                    <#assign seq = seq + 1>
                    <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
                        <td>
                            <form name= "viewGroupDetail" action ="<@ofbizUrl>PicklistOptions</@ofbizUrl>" method="post">
                                <input type ="hidden" name="viewDetail" value= "${groupName?if_exists}"/>
                                <input type="hidden" name="groupByShippingMethod" value="${requestParameters.groupByShippingMethod?if_exists}"/>
                                <input type="hidden" name="groupByWarehouseArea" value="${requestParameters.groupByWarehouseArea?if_exists}"/>
                                <input type="hidden" name="groupByNoOfOrderItems" value="${requestParameters.groupByNoOfOrderItems?if_exists}"/>
                                <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
                                <table>
                                    <tr>
                                        <#if groupName1?has_content>
                                            <td><a id="one" href="javascript:document.viewGroupDetail.submit()" class="buttontext">${groupName1}</a></td>
                                        </#if>
                                        <#if groupName2?has_content>
                                            <td><a id="two" href="javascript:document.viewGroupDetail.submit()" class="buttontext">${groupName2}</a></td>
                                        </#if>
                                        <#if groupName3?has_content>
                                            <td><a id="three" href="javascript:document.viewGroupDetail.submit()" class="buttontext">${groupName3}</a></td>
                                        </#if>
                                    </tr>
                               </table>
                            </form>
                        </td>
                        <td>${orderReadyToPickInfoListSize}</td>
                        <td>${orderNeedsStockMoveInfoListSize}</td>
                        <td>
                            <#if orderReadyToPickInfoList?has_content>
                                <form method="post" action="<@ofbizUrl>createPicklistFromOrders</@ofbizUrl>">
                                    <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
                                    <input type="hidden" name="groupByShippingMethod" value="${requestParameters.groupByShippingMethod?if_exists}"/>
                                    <input type="hidden" name="groupByWarehouseArea" value="${requestParameters.groupByWarehouseArea?if_exists}"/>
                                    <input type="hidden" name="groupByNoOfOrderItems" value="${requestParameters.groupByNoOfOrderItems?if_exists}"/>
                                    <input type="hidden" name="orderIdList" value=""/>
                                    <#assign orderIdsForPickList = orderReadyToPickInfoList?if_exists>
                                    <#list orderIdsForPickList as orderIdForPickList>
                                        <input type="hidden" name="orderIdList" value="${orderIdForPickList.orderHeader.orderId}"/>
                                    </#list>
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
                            <input type="hidden" name="facilityId" value="${facilityId?if_exists}"/>
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
<#assign viewDetail = requestParameters.viewDetail?if_exists>
<#if viewDetail?has_content>
    <#list pickMoveInfoList as pickMoveInfo>
        <#assign groupName = pickMoveInfo.groupName?if_exists>
        <#if groupName?if_exists == viewDetail>
            <#assign toPickList = pickMoveInfo.orderReadyToPickInfoList?if_exists>
        </#if>
    </#list>
</#if>

<#if toPickList?has_content>
<div class="screenlet">
    <div class="screenlet-title-bar">
        <ul>
            <li class="h3">${uiLabelMap.ProductPickingDetail}</li>
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
                <td>${uiLabelMap.ProductProductDescription}</td>
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
