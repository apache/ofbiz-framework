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

<#if productIds?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommerceProductsForPromotion}:</div>
    </div>
    <div class="screenlet-body">
        <#if (listSize > 0)>
            <table border="0" width="100%" cellpadding="2">
                <tr>
                <td align="right">
                    <span class="tabletext">
                    <b>
                    <#if (viewIndex > 0)>
                    <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoId?if_exists}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
                    </#if>
                    ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
                    <#if (listSize > highIndex)>
                    | <a href="<@ofbizUrl>showPromotionDetails?productPromoId=${productPromoId?if_exists}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
                    </#if>
                    </b>
                    </span>
                </td>
                </tr>
            </table>
        </#if>

        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="boxbottom">
          <tr>
            <td><div class="tableheadtext">${uiLabelMap.CommonQualifier}</div></td>
            <td><div class="tableheadtext">${uiLabelMap.CommonBenefit}</div></td>
            <td><div class="tableheadtext">&nbsp;</div></td>
          </tr>
        <#if (listSize > 0)>
          <#list productIds[lowIndex..highIndex-1] as productId>
              <tr>
                <td><div class="tabletext">[<#if productIdsCond.contains(productId)>x<#else>&nbsp;</#if>]</div></td>
                <td><div class="tabletext">[<#if productIdsAction.contains(productId)>x<#else>&nbsp;</#if>]</div></td>
                <td>
                  ${setRequestAttribute("optProductId", productId)}
                  ${setRequestAttribute("listIndex", productId_index)}
                  ${screens.render(productsummaryScreen)}
                </td>
              </tr>
          </#list>
        </#if>
        </table>
    </div>
</div>
</#if>
