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
<#-- variable setup and worker calls -->
<#assign topLevelList = requestAttributes.topLevelList?if_exists>
<#assign curCategoryId = requestAttributes.curCategoryId?if_exists>

<#-- looping macro -->
<#macro categoryList parentCategory category>
  <#if parentCategory.productCategoryId != category.productCategoryId>
    <#local pStr = "/~pcategory=" + parentCategory.productCategoryId>
  </#if>

  <#if (Static["org.ofbiz.product.category.CategoryWorker"].checkTrailItem(request, category.getString("productCategoryId"))) || (curCategoryId?exists && curCategoryId == category.productCategoryId)>
    <#if curCategoryId?exists && curCategoryId == category.productCategoryId>
        <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?exists>        
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="buttontextdisabled">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
        <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?exists>        
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="buttontextdisabled">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
        <#else>          
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="buttontextdisabled">${category.description?if_exists}</a>
        </#if>          
    <#else>
        <#if catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")?exists> 
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="linktext">${catContentWrappers[category.productCategoryId].get("CATEGORY_NAME")}</a>
        <#elseif catContentWrappers?exists && catContentWrappers[category.productCategoryId]?exists && catContentWrappers[category.productCategoryId].get("DESCRIPTION")?exists> 
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="linktext">${catContentWrappers[category.productCategoryId].get("DESCRIPTION")}</a>
        <#else>          
            -> <a href="<@ofbizUrl>category/~category_id=${category.productCategoryId}${pStr?if_exists}</@ofbizUrl>" class="linktext">${category.description?if_exists}</a>
        </#if>          
    </#if>
    <#local subCatList = Static["org.ofbiz.product.category.CategoryWorker"].getRelatedCategoriesRet(request, "subCatList", category.getString("productCategoryId"), true)>
    <#if subCatList?exists>
      <#list subCatList as subCat>
        <@categoryList parentCategory=category category=subCat/>
      </#list>
    </#if>
  </#if>
</#macro>

<div class="tabletext">
  <a href="<@ofbizUrl>main</@ofbizUrl>" class="linktext">${uiLabelMap.CommonMain}</a>
  <#-- Show the category branch -->
  <#list topLevelList as category>
    <@categoryList parentCategory=category category=category/>
  </#list>
  <#-- Show the product, if there is one -->
  <#if productContentWrapper?exists>
    -> ${productContentWrapper.get("PRODUCT_NAME")?if_exists}
  </#if>
</div>
<br/>
