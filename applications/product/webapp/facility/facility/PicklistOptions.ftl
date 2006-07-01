<#--
 *    Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a 
 *    copy of this software and associated documentation files (the "Software"), 
 *    to deal in the Software without restriction, including without limitation 
 *    the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *    and/or sell copies of the Software, and to permit persons to whom the 
 *    Software is furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included 
 *    in all copies or substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *    OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *    MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *    IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *    CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *    OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *    THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author         David E. Jones (jonesde@ofbiz.org)
 *@author         Andy Zeneski (jaz@ofbiz.org)
 *@author         thierry.grauss@etu.univ-tours.fr (migration to uiLabelMap)
 *@version        $Rev$
 *@since            2.2
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
