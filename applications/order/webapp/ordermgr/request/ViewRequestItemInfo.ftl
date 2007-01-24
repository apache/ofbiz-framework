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
        <div class="boxhead">&nbsp; ${uiLabelMap.OrderRequestItems}</div>
    </div>
    <div class="screenlet-body">
        <table width="100%" border="0" cellpadding="0">
            <tr align="left" valign="bottom">
                <td width="10%" align="left"><span class="tableheadtext"><b>${uiLabelMap.ProductItem}</b></span></td>
                <td width="35%" align="left"><span class="tableheadtext"><b>${uiLabelMap.EcommerceProduct}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.ProductQuantity}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderAmount}</b></span></td>
                <td width="10%" align="right"><span class="tableheadtext"><b>${uiLabelMap.OrderRequestMaximumAmount}</b></span></td>
                <td width="5%" align="right">&nbsp;</td>
            </tr>
            <#list requestItems as requestItem>
                <#if requestItem.productId?exists>
                    <#assign product = requestItem.getRelatedOne("Product")>
                </#if>

                <tr><td colspan="6"><hr class="sepbar"/></td></tr>
                <tr>
                    <td valign="top">
                        <div class="tabletext" style="font-size: xx-small;">
                        <#if showRequestManagementLinks?exists>
                            <a href="<@ofbizUrl>EditRequestItem?custRequestId=${requestItem.custRequestId}&amp;custRequestItemSeqId=${requestItem.custRequestItemSeqId}</@ofbizUrl>" class="buttontext">${requestItem.custRequestItemSeqId}</a>
                        <#else>
                            ${requestItem.custRequestItemSeqId}
                        </#if>
                        </div>
                    </td>
                    <td valign="top">
                        <div class="tabletext">
                            ${(product.internalName)?if_exists}&nbsp;
                            <#if showRequestManagementLinks?exists>
                                <a href="/catalog/control/EditProduct?productId=${requestItem.productId?if_exists}" class="buttontext">${requestItem.productId?if_exists}</a>
                            <#else>
                                <a href="<@ofbizUrl>product?product_id=${requestItem.productId?if_exists}</@ofbizUrl>" class="buttontext">${requestItem.productId?if_exists}</a>
                            </#if>
                        </div>
                    </td>
                    <td align="right" valign="top"><div class="tabletext">${requestItem.quantity?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext">${requestItem.selectedAmount?if_exists}</div></td>
                    <td align="right" valign="top"><div class="tabletext"><@ofbizCurrency amount=requestItem.maximumAmount isoCode=request.maximumAmountUomId/></div></td>
                </tr>
            </#list>
        </table>
    </div>
</div>
