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

<br/>

<form method="post" action="<@ofbizUrl>EditFeature</@ofbizUrl>">
  <input type="text" size="12" name="productFeatureId" value=""/>
  <input type="submit" value="${uiLabelMap.CommonEdit}"/>
</form>

<br/>

<table cellspacing="0" class="basic-table">
  <tr class="header-row">
    <td><b>${uiLabelMap.CommonId}</b></td>
    <td><b>${uiLabelMap.CommonDescription}</b></td>
    <td><b>${uiLabelMap.ProductParentCategory}</b></td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
  </tr>

<#assign rowClass = "2">
<#list productFeatureCategories as productFeatureCategory>
  <tr valign="middle"<#if rowClass == "1"> class="alternate-row"</#if>>
    <form method="post" action="<@ofbizUrl>UpdateFeatureCategory</@ofbizUrl>">
    <input type="hidden" name="productFeatureCategoryId" value="${productFeatureCategory.productFeatureCategoryId}">
    <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${productFeatureCategory.productFeatureCategoryId}</@ofbizUrl>" class="buttontext">${productFeatureCategory.productFeatureCategoryId}</a></td>
    <td><input type="text" size="30" name="description" value="${productFeatureCategory.description?if_exists}"></td>
    <td>
      <select name="parentCategoryId" size="1">
        ${productFeatureCategory}
        <#assign curProdFeatCat = productFeatureCategory.getRelatedOne("ParentProductFeatureCategory")?if_exists>
        <#if curProdFeatCat?has_content>
          <option value="${curProdFeatCat.productFeatureCategoryId}">${curProdFeatCat.description?if_exists}</option>
        </#if>
        <option value="">&nbsp;</option>
          <#list productFeatureCategories as dropDownProductFeatureCategory>
            <option value="${dropDownProductFeatureCategory.productFeatureCategoryId}">${dropDownProductFeatureCategory.description?if_exists}</option>
          </#list>
      </select>
    </td>
    <td><input type="submit" value="${uiLabelMap.CommonUpdate}"></td>
    <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${productFeatureCategory.productFeatureCategoryId}</@ofbizUrl>" class="buttontext">${uiLabelMap.ProductProductFeatureMaintenance}</a></td>
    </form>
  </tr>
  <#-- toggle the row color -->
  <#if rowClass == "2">
    <#assign rowClass = "1">
  <#else>
    <#assign rowClass = "2">
  </#if> 
</#list>
</table>