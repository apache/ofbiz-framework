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

<div class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.ProductSearchCatalog}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
        <form name="keywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>">
          <input type="hidden" name="VIEW_SIZE" value="10"/>
          <div class="tabletext">
            <input type="text" class="inputBox" name="SEARCH_STRING" size="14" maxlength="50" value="${requestParameters.SEARCH_STRING?if_exists}"/>
          </div>
          <#if 0 < otherSearchProdCatalogCategories?size>
            <div class="tabletext">
              <select name="SEARCH_CATEGORY_ID" size="1" class="selectBox">
                <option value="${searchCategoryId?if_exists}">${uiLabelMap.ProductEntireCatalog}</option>
                <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
                  <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOneCache("ProductCategory")>
                  <#if searchProductCategory?exists>
                    <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
                  </#if>
                </#list>
              </select>
            </div>
          <#else>
            <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
          </#if>
          <div class="tabletext"><input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked="checked"</#if>/>${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked="checked"</#if>/>${uiLabelMap.CommonAll}&nbsp;<a href="javascript:document.keywordsearchform.submit()" class="buttontext">${uiLabelMap.CommonFind}</a></div>
        </form>
        <form name="advancedsearchform" method="post" action="<@ofbizUrl>advancedsearch</@ofbizUrl>">
          <#if 0 < otherSearchProdCatalogCategories?size>
            <div class="tabletext">${uiLabelMap.ProductAdvancedSearchIn}: </div>
            <div class="tabletext">
              <select name="SEARCH_CATEGORY_ID" size="1" class="selectBox">
                <option value="${searchCategoryId?if_exists}">${uiLabelMap.ProductEntireCatalog}</option>
                <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
                  <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOneCache("ProductCategory")>
                  <#if searchProductCategory?exists>
                    <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
                  </#if>
                </#list>
              </select>
            </div>
          <#else>
            <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId?if_exists}"/>
          </#if>
          <br/>
          <div>
            <a href="javascript:document.advancedsearchform.submit()" class="buttontext">${uiLabelMap.ProductAdvancedSearch}</a>
          </div>
        </form>
    </div>
</div>
