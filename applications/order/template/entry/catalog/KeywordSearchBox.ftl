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

<div id="keywordsearchbox" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ProductSearchCatalog}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <form name="keywordsearchform" id="keywordsearchbox_keywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>">
      <fieldset class="inline">
        <input type="hidden" name="VIEW_SIZE" value="10" />
        <input type="hidden" name="PAGING" value="Y" />
        <div>
          <input type="text" name="SEARCH_STRING" size="14" maxlength="50" value="${requestParameters.SEARCH_STRING!}" />
        </div>
        <#if 0 &lt; otherSearchProdCatalogCategories?size>
          <div>
            <select name="SEARCH_CATEGORY_ID" size="1">
              <option value="${searchCategoryId!}">${uiLabelMap.ProductEntireCatalog}</option>
              <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
                <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOne("ProductCategory", true)>
                <#if searchProductCategory??>
                  <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
                </#if>
              </#list>
            </select>
          </div>
        <#else>
          <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId!}" />
        </#if>
        <div>
          <label for="SEARCH_OPERATOR_OR"><input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_OR" value="OR" <#if searchOperator == "OR">checked="checked"</#if> />${uiLabelMap.CommonAny}</label>
          <label for="SEARCH_OPERATOR_AND"><input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_AND" value="AND" <#if searchOperator == "AND">checked="checked"</#if> />${uiLabelMap.CommonAll}</label>
          <input type="submit" value="${uiLabelMap.CommonFind}" class="button" />
        </div>
      </fieldset>
    </form>
    <form name="advancedsearchform" id="keywordsearchbox_advancedsearchform" method="post" action="<@ofbizUrl>advancedsearch</@ofbizUrl>">
      <fieldset>
        <#if 0 &lt; otherSearchProdCatalogCategories?size>
            <label for="SEARCH_CATEGORY_ID">${uiLabelMap.ProductAdvancedSearchIn}: </label>
            <select name="SEARCH_CATEGORY_ID" id="SEARCH_CATEGORY_ID" size="1">
              <option value="${searchCategoryId!}">${uiLabelMap.ProductEntireCatalog}</option>
              <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
                <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOne("ProductCategory", true)>
                <#if searchProductCategory??>
                  <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
                </#if>
              </#list>
            </select>
        <#else>
          <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId!}" />
        </#if>
          <input type="submit" value="${uiLabelMap.ProductAdvancedSearch}" class="button" />
      </fieldset>
    </form>
  </div>
</div>