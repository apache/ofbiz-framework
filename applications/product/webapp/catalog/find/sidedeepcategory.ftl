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
  <#if curCategoryId?exists && curCategoryId == category.productCategoryId>
    <div class="browsecategorytext">
        <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?has_content>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")} [${category.productCategoryId}]</a>
        <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?has_content>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")} [${category.productCategoryId}]</a>
        <#else>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybuttondisabled">${category.categoryName?default(category.description)?if_exists} [${category.productCategoryId}]</a>
       </#if>
    </div>
  <#else>
    <div class="browsecategorytext">
        <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?has_content>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")} [${category.productCategoryId}]</a>            
        <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?has_content>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")} [${category.productCategoryId}]</a>            
        <#else>
          -&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="browsecategorybutton">${category.categoryName?default(category.description)?if_exists} [${category.productCategoryId}]</a>
       </#if>          
    </div>
  </#if>
  
  <#if (Static["org.ofbiz.product.category.CategoryWorker"].checkTrailItem(request, category.getString("productCategoryId"))) || (curCategoryId?exists && curCategoryId == category.productCategoryId)>
    <#local subCatList = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "subCatList", category.getString("productCategoryId"), true)>
    <#if subCatList?exists>
      <#list subCatList as subCat>
        <div style="margin-left: 10px">
          <@categoryList parentCategory=category category=subCat/>
        </div>
      </#list>
    </#if>
  </#if>
</#macro>

<div class="screenlet">
    <div class="screenlet-header">
        <div class="simple-right-small">
            <#if isOpen>
                <a href='<@ofbizUrl>main?BrowseCategoriesState=close</@ofbizUrl>' class='lightbuttontext'>&nbsp;_&nbsp;</a>
            <#else>
                <a href='<@ofbizUrl>main?BrowseCategoriesState=open</@ofbizUrl>' class='lightbuttontext'>&nbsp;[]&nbsp;</a>
            </#if>
        </div>
        <div class="boxhead">${uiLabelMap.ProductBrowseCategories}</div>
    </div>
<#if isOpen>
    <div class="screenlet-body">
        <div><a href='<@ofbizUrl>ChooseTopCategory</@ofbizUrl>' class='buttontext'>${uiLabelMap.ProductChooseTopCategory}</a></div>
        <div style='margin-left: 10px;'>
        <#if currentTopCategory?exists>
          <#if curCategoryId?exists && curCategoryId == currentTopCategory.productCategoryId>
            <div class='tabletext' style='text-indent: -10px;'><b>-&nbsp;${currentTopCategory.categoryName?default("No Name")} [${currentTopCategory.productCategoryId}]</b></div>
          <#else>
            <div class='browsecategorytext'>-&nbsp;<a href="<@ofbizUrl>EditCategory?productCategoryId=${currentTopCategory.productCategoryId}</@ofbizUrl>" class='browsecategorybutton'>${currentTopCategory.categoryName?default(currentTopCategory.description)?if_exists} [${currentTopCategory.productCategoryId}]</a></div>
          </#if>
        </#if>
          <div style='margin-left: 10px;'>
            <#if topLevelList?exists>
              <#list topLevelList as category>
                <@categoryList parentCategory=category category=category/>
              </#list>
            </#if>
          </div>
        </div>
    </div>
</#if>
</div>
