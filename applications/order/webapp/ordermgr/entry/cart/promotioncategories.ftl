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
<#if productPromoCategoryIncludeList?has_content || productPromoCategoryExcludeList?has_content || productPromoCategoryAlwaysList?has_content>
<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">&nbsp;${uiLabelMap.EcommercePromotionCategories}:</div>
    </div>
    <div class="screenlet-body">
        <#if productPromoCategoryIncludeList?has_content>
          <div class="tabletext">${uiLabelMap.EcommercePromotionProductsInCategories}:</div>
          <#list productPromoCategoryIncludeList as productPromoCategory>
            <#assign productCategory = productPromoCategory.getRelatedOneCache("ProductCategory")>
            <div class="tabletext">
                -&nbsp;<a href="<@ofbizUrl>category/~category_id=${productPromoCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${(productCategory.description)?default(productPromoCategory.productCategoryId)}</a>
                <#if productPromoCategory.includeSubCategories?if_exists = "Y">(${uiLabelMap.EcommerceIncludeSubCategories})</#if>
            </div>
          </#list>
        </#if>
        <#if productPromoCategoryExcludeList?has_content>
          <div class="tabletext">${uiLabelMap.EcommerceEcludeCategories}</div>
          <#list productPromoCategoryExcludeList as productPromoCategory>
            <#assign productCategory = productPromoCategory.getRelatedOneCache("ProductCategory")>
            <div class="tabletext">
                -&nbsp;<a href="<@ofbizUrl>category/~category_id=${productPromoCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${(productCategory.description)?default(productPromoCategory.productCategoryId)}</a>
                <#if productPromoCategory.includeSubCategories?if_exists = "Y">(${uiLabelMap.EcommerceIncludeSubCategories})</#if>
            </div>
          </#list>
        </#if>
        <#if productPromoCategoryAlwaysList?has_content>
          <div class="tabletext">${uiLabelMap.EcommerceAlwaysList}</div>
          <#list productPromoCategoryAlwaysList as productPromoCategory>
            <#assign productCategory = productPromoCategory.getRelatedOneCache("ProductCategory")>
            <div class="tabletext">
                -&nbsp;<a href="<@ofbizUrl>category/~category_id=${productPromoCategory.productCategoryId}</@ofbizUrl>" class="buttontext">${(productCategory.description)?default(productPromoCategory.productCategoryId)}</a>
                <#if productPromoCategory.includeSubCategories?if_exists = "Y">(${uiLabelMap.EcommerceIncludeSubCategories})</#if>
            </div>
          </#list>
        </#if>
    </div>
</div>
</#if>
