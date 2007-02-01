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

<#if curProductFeatureCategory?exists>
<a href="<@ofbizUrl>EditFeature?productFeatureCategoryId=${productFeatureCategoryId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductCreateNewFeature}]</a>
<#elseif productFeatureGroup?exists>
<a href="<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit} ${productFeatureGroup.description?if_exists}]</a>
</#if>
<#if productId?has_content>
    <div>
        <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductReturnToEditProduct}]</a>
        <a href="<@ofbizUrl>EditProductFeatures?productId=${productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductReturnToEditProductFeatures}]</a>
    </div>
</#if>

<#if (listSize > 0)>
<#assign selectedFeatureApplTypeId = selFeatureApplTypeId?if_exists>

    <#if productId?has_content>
      <#assign productString = "&productId=" + productId>
    </#if>
    <table border="0" cellpadding="2">
        <tr>
        <td align="right">
            <span class="tabletext">
            <b>
            <#if (viewIndex > 0)>
            <a href="<@ofbizUrl>ApplyFeaturesFromCategory?productFeatureCategoryId=${productFeatureCategoryId?if_exists}&productFeatureApplTypeId=${selectedFeatureApplTypeId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}${productString?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
            </#if>
            ${lowIndex+1} - ${highIndex}${uiLabelMap.CommonOf} ${listSize}
            <#if (listSize > highIndex)>
            | <a href="<@ofbizUrl>ApplyFeaturesFromCategory?productFeatureCategoryId=${productFeatureCategoryId?if_exists}&productFeatureApplTypeId=${selectedFeatureApplTypeId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}${productString?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
            </#if>
            </b>
            </span>
        </td>
        </tr>
    </table>
</#if>
<table border="1" cellpadding="2" cellspacing="0">
<form method="post" action="<@ofbizUrl>ApplyFeaturesToProduct</@ofbizUrl>" name="selectAllForm">
  <input type="hidden" name="_useRowSubmit" value="Y">
  <input type="hidden" name="_checkGlobalScope" value="Y">
  <input type="hidden" name="productId" value="${productId}">

  <tr class="viewOneTR1">
    <td><div class="tabletext"><b>${uiLabelMap.CommonId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductFeatureType}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductApplType}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonThruDate}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.ProductAmount}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonSequence}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');"></div></td>
  </tr>
<#assign rowCount = 0>
<#if (listSize > 0)>
<#list productFeatures as productFeature>
  <#assign curProductFeatureType = productFeature.getRelatedOneCache("ProductFeatureType")>
    <tr valign="middle" class="viewOneTR1">
        <input type="hidden" name="productFeatureId_o_${rowCount}" value="${productFeature.productFeatureId}">
        <td><a href="<@ofbizUrl>EditFeature?productFeatureId=${productFeature.productFeatureId}</@ofbizUrl>" class="buttontext">${productFeature.productFeatureId}</a></td>
        <td>${productFeature.description}</td>
        <td><#if curProductFeatureType?exists>${curProductFeatureType.description}<#else> [${productFeature.productFeatureTypeId}]</#if></td>
        <td>
          <select name="productFeatureApplTypeId_o_${rowCount}" size="1" class="selectBox">
            <#list productFeatureApplTypes as productFeatureApplType>
              <option value="${productFeatureApplType.productFeatureApplTypeId}" <#if (selectedFeatureApplTypeId?has_content) && (productFeatureApplType.productFeatureApplTypeId == selectedFeatureApplTypeId)>selected</#if>>${productFeatureApplType.description}</option>
            </#list>
          </select>
        </td>
        <td><input type="text" size="25" name="fromDate_o_${rowCount}" class="inputBox"><a href="javascript:call_cal(document.selectAllForm.fromDate_o_${rowCount}, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a></td>
        <td><input type="text" size="25" name="thruDate_o_${rowCount}" class="inputBox"><a href="javascript:call_cal(document.selectAllForm.thruDate_o_${rowCount}, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a></td>
        <td><input type="text" size="6" name="amount_o_${rowCount}" class="inputBox" value="${productFeature.defaultAmount?if_exists}"></td>
        <td><input type="text" size="5" name="sequenceNum_o_${rowCount}" class="inputBox" value="${productFeature.defaultSequenceNum?if_exists}"></td>
        <td align="right">              
            <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
        </td>
    </tr>
    <#assign rowCount = rowCount + 1>
</#list>
<tr><td colspan="9" align="center"><input type="submit" value="Apply"></td></tr>
</#if>
<input type="hidden" name="_rowCount" value="${rowCount?if_exists}"/>
</form>
</table>
