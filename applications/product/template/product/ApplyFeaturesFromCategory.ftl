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

<#if curProductFeatureCategory??>
<a href="<@ofbizUrl>EditFeature?productFeatureCategoryId=${productFeatureCategoryId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductCreateNewFeature}</a>
<#elseif productFeatureGroup??>
<a href="<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId!}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit} ${productFeatureGroup.description!}</a>
</#if>
<#if productId?has_content>
    <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductReturnToEditProduct}</a>
    <a href="<@ofbizUrl>EditProductFeatures?productId=${productId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductReturnToEditProductFeatures}</a>
</#if>

<#if (listSize > 0)>
<#assign selectedFeatureApplTypeId = selFeatureApplTypeId!>

    <#if productId?has_content>
      <#assign productString = "&amp;productId=" + productId>
    </#if>
    <table cellspacing="0" class="basic-table">
        <tr>
        <td align="right">
            <span>
            <b>
            <#if (viewIndex > 0)>
            <a href="<@ofbizUrl>ApplyFeaturesFromCategory?productFeatureCategoryId=${productFeatureCategoryId!}&amp;productFeatureApplTypeId=${selectedFeatureApplTypeId!}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex-1}${productString!}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
            </#if>
            ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
            <#if (listSize > highIndex)>
            | <a href="<@ofbizUrl>ApplyFeaturesFromCategory?productFeatureCategoryId=${productFeatureCategoryId!}&amp;productFeatureApplTypeId=${selectedFeatureApplTypeId!}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndex+1}${productString!}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
            </#if>
            </b>
            </span>
        </td>
        </tr>
    </table>
</#if>
<form method="post" action="<@ofbizUrl>ApplyFeaturesToProduct</@ofbizUrl>" name="selectAllForm">
<table cellspacing="0" class="basic-table">
  <input type="hidden" name="_useRowSubmit" value="Y" />
  <input type="hidden" name="_checkGlobalScope" value="Y" />
  <input type="hidden" name="productId" value="${productId}" />
  <tr class="header-row">
    <td><b>${uiLabelMap.CommonId}</b></td>
    <td><b>${uiLabelMap.CommonDescription}</b></td>
    <td><b>${uiLabelMap.ProductFeatureType}</b></td>
    <td><b>${uiLabelMap.ProductApplType}</b></td>
    <td><b>${uiLabelMap.CommonFromDate}</b></td>
    <td><b>${uiLabelMap.CommonThruDate}</b></td>
    <td><b>${uiLabelMap.ProductAmount}</b></td>
    <td><b>${uiLabelMap.CommonSequence}</b></td>
    <td><b><label>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" class="selectAll" onclick="highlightAllRows(this, 'productFeatureId_tableRow_', 'selectAllForm');" /></labe></b></td>
  </tr>
<#assign rowCount = 0>
<#assign rowClass = "2">
<#if (listSize > 0)>
<#list productFeatures as productFeature>
  <#assign curProductFeatureType = productFeature.getRelatedOne("ProductFeatureType", true)>
    <tr id="productFeatureId_tableRow_${rowCount}"  valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
        <input type="hidden" name="productFeatureId_o_${rowCount}" value="${productFeature.productFeatureId}" />
        <td><a href="<@ofbizUrl>EditFeature?productFeatureId=${productFeature.productFeatureId}</@ofbizUrl>" class="buttontext">${productFeature.productFeatureId}</a></td>
        <td>${productFeature.description!}</td>
        <td><#if curProductFeatureType??>${curProductFeatureType.description!}<#else> [${productFeature.productFeatureTypeId}]</#if></td>
        <td>
          <select name="productFeatureApplTypeId_o_${rowCount}" size="1">
            <#list productFeatureApplTypes as productFeatureApplType>
              <option value="${productFeatureApplType.productFeatureApplTypeId}" <#if (selectedFeatureApplTypeId?has_content) && (productFeatureApplType.productFeatureApplTypeId == selectedFeatureApplTypeId)>selected="selected"</#if>>${productFeatureApplType.get("description", locale)}</option>
            </#list>
          </select>
        </td>
        <td>
            <@htmlTemplate.renderDateTimeField name="fromDate_o_${rowCount}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate_o_${rowCount}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
        </td>
        <td>
           <@htmlTemplate.renderDateTimeField name="thruDate_o_${rowCount}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="thruDate_o_${rowCount}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
        </td>
        <td><input type="text" size="6" name="amount_o_${rowCount}" value="${productFeature.defaultAmount!}" /></td>
        <td><input type="text" size="5" name="sequenceNum_o_${rowCount}" value="${productFeature.defaultSequenceNum!}" /></td>
        <td align="right">
            <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="highlightRow(this,'productFeatureId_tableRow_${rowCount}');" />
        </td>
    </tr>
    <#assign rowCount = rowCount + 1>
    <#-- toggle the row color -->
    <#if rowClass == "2">
        <#assign rowClass = "1">
    <#else>
        <#assign rowClass = "2">
    </#if>
</#list>
<tr><td colspan="9" align="center"><input type="submit" value="${uiLabelMap.CommonApply}" /></td></tr>
</#if>
<input type="hidden" name="_rowCount" value="${rowCount!}"/>
</table>
</form>
