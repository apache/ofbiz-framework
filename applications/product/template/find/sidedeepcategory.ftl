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

<#-- looping macro -->
<#macro categoryList parentCategory category>
  <#if parentCategory.productCategoryId != category.productCategoryId>
    <#local pStr = "&amp;pcategory=" + parentCategory.productCategoryId>
  </#if>
  <#if curCategoryId?? && curCategoryId == category.productCategoryId>
    <div class="browsecategorytext">
        <#if catContentWrappers?? && catContentWrappers[category.productCategoryId]?? && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME", "html")?has_content>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME", "html")} [${category.productCategoryId}]</a>
        <#elseif catContentWrappers?? && catContentWrappers[category.productCategoryId]?? && catContentWrappers[category.productCategoryId].get("DESCRIPTION", "html")?has_content>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("DESCRIPTION", "html")} [${category.productCategoryId}]</a>
        <#else>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybuttondisabled">${category.categoryName?default(category.description)!} [${category.productCategoryId}]</a>
       </#if>
    </div>
  <#else>
    <div class="browsecategorytext">
        <#if catContentWrappers?? && catContentWrappers[category.productCategoryId]?? && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME", "html")?has_content>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME", "html")} [${category.productCategoryId}]</a>
        <#elseif catContentWrappers?? && catContentWrappers[category.productCategoryId]?? && catContentWrappers[category.productCategoryId].get("DESCRIPTION", "html")?has_content>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("DESCRIPTION", "html")} [${category.productCategoryId}]</a>
        <#else>
          <a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr!}</@ofbizUrl>" class="browsecategorybutton">${category.categoryName?default(category.description)!} [${category.productCategoryId}]</a>
       </#if>
    </div>
  </#if>

  <#if (Static["org.ofbiz.product.category.CategoryWorker"].checkTrailItem(request, category.getString("productCategoryId"))) || (curCategoryId?? && curCategoryId == category.productCategoryId)>
    <#local subCatList = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "subCatList", category.getString("productCategoryId"), true)>
    <#if subCatList??>
      <#list subCatList as subCat>
        <div class="browsecategorylist">
          <@categoryList parentCategory=category category=subCat/>
        </div>
      </#list>
    </#if>
  </#if>
</#macro>

<div><a href='<@ofbizUrl>ChooseTopCategory</@ofbizUrl>' class='buttontext'>${uiLabelMap.ProductChooseTopCategory}</a></div>
<div class="browsecategorylist">
<#if currentTopCategory??>
  <#if curCategoryId?? && curCategoryId == currentTopCategory.productCategoryId>
    <div style='text-indent: -10px;'><b>${currentTopCategory.categoryName?default("No Name")} [${currentTopCategory.productCategoryId}]</b></div>
  <#else>
    <div class='browsecategorytext'><a href="<@ofbizUrl>EditCategory?productCategoryId=${currentTopCategory.productCategoryId}</@ofbizUrl>" class='browsecategorybutton'>${currentTopCategory.categoryName?default(currentTopCategory.description)!} [${currentTopCategory.productCategoryId}]</a></div>
  </#if>
</#if>
  <div class="browsecategorylist">
    <#if topLevelList??>
      <#list topLevelList as category>
        <@categoryList parentCategory=category category=category/>
      </#list>
    </#if>
  </div>
</div>
