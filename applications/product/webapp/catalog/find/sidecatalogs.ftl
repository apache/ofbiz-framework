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
<div class="browsecategorylist">
  <#assign sortList = Static["org.ofbiz.base.util.UtilMisc"].toList("prodCatalogCategoryTypeId", "sequenceNum", "productCategoryId")>
  <#list prodCatalogs as prodCatalog>
  <#if curProdCatalogId?? && curProdCatalogId == prodCatalog.prodCatalogId>
    <#assign prodCatalogCategories = prodCatalog.getRelated("ProdCatalogCategory", null, sortList, true)>
    <div class='browsecategorytext'><a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class='browsecategorybutton'>${prodCatalog.catalogName!}</a></div>
      <div class="browsecategorylist">
        <#list prodCatalogCategories as prodCatalogCategory>
          <#assign productCategory = prodCatalogCategory.getRelatedOne("ProductCategory", true)>
          <div class='browsecategorytext'><a href='<@ofbizUrl>EditCategory?CATALOG_TOP_CATEGORY=${prodCatalogCategory.productCategoryId}&amp;productCategoryId=${prodCatalogCategory.productCategoryId}</@ofbizUrl>' class="browsecategorybutton">${(productCategory.categoryName)?default(productCategory.description)?default(productCategory.productCategoryId)}</a></div>
        </#list>
      </div>
  <#else>
    <div class='browsecategorytext'><a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class='browsecategorybutton'>${prodCatalog.catalogName!}</a></div>
  </#if>
  </#list>
</div>
