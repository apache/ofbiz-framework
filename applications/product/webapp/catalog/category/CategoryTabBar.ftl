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

<#if requestAttributes.uiLabelMap?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

    <div class="tabContainer">
        <a href="<@ofbizUrl>EditCategory?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategory?default(unselectedClassName)}">${uiLabelMap.ProductCategory}</a>
        <a href="<@ofbizUrl>EditCategoryContent?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryContent?default(unselectedClassName)}">${uiLabelMap.ProductCategoryContent}</a>
        <a href="<@ofbizUrl>EditCategoryRollup?showProductCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryRollup?default(unselectedClassName)}">${uiLabelMap.ProductRollupShort}</a>
        <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryProducts?default(unselectedClassName)}">${uiLabelMap.ProductProducts}</a>
        <a href="<@ofbizUrl>EditCategoryProdCatalogs?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryProdCatalogs?default(unselectedClassName)}">${uiLabelMap.ProductCatalogs}</a>
        <a href="<@ofbizUrl>EditCategoryFeatureCats?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryFeatureCats?default(unselectedClassName)}">${uiLabelMap.ProductFeatureCats}</a>
        <a href="<@ofbizUrl>EditCategoryParties?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryParties?default(unselectedClassName)}">${uiLabelMap.PartyParties}</a>
        <a href="<@ofbizUrl>EditCategoryAttributes?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditCategoryAttributes?default(unselectedClassName)}">${uiLabelMap.ProductAttributes}</a>
        <a href="<@ofbizUrl>EditProductCategoryLinks?productCategoryId=${productCategoryId}</@ofbizUrl>" class="${selectedClassMap.EditProductCategoryLinks?default(unselectedClassName)}">${uiLabelMap.ProductCategoryLinks}</a>
    </div>
    
