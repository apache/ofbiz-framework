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
    <form class="basic-form" name="keywordsearchform" id="keywordsearchbox_keywordsearchform" method="post" action="<@ofbizUrl>keywordsearch</@ofbizUrl>">
      <input type="hidden" name="VIEW_SIZE" value="10" />
      <input type="hidden" name="PAGING" value="Y" />
    <table class="basic-table form-table">
      <tr>
          <tr>
            <td>
              <input type="text" name="SEARCH_STRING" size="14" maxlength="50" value="${requestParameters.SEARCH_STRING!}" />
            </td>
            <td>
            <#if 0 &lt; otherSearchProdCatalogCategories?size>
              <select name="SEARCH_CATEGORY_ID" size="1">
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
            </td>
          </tr>
          <tr>
            <td>
              <label for="SEARCH_OPERATOR_OR"><input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_OR" value="OR" <#if "OR" == searchOperator>checked="checked"</#if> />${uiLabelMap.CommonAny}</label>
              <label for="SEARCH_OPERATOR_AND"><input type="radio" name="SEARCH_OPERATOR" id="SEARCH_OPERATOR_AND" value="AND" <#if "AND" == searchOperator>checked="checked"</#if> />${uiLabelMap.CommonAll}</label>
            </td>
          </tr>
          <tr>
            <td>
              <input type="submit" value="${uiLabelMap.CommonFind}" class="button" />
            </td>
          </tr>
      </tr>
      </table>
    </form>
    <form class="basic-form" name="advancedsearchform" id="keywordsearchbox_advancedsearchform" method="post" action="<@ofbizUrl>advancedsearch</@ofbizUrl>">
    <table class="basic-table form-table">
          <tr>
          <#if 0 &lt; otherSearchProdCatalogCategories?size>
            <td class="label">
              <label for="SEARCH_CATEGORY_ID">${uiLabelMap.ProductAdvancedSearchIn}: </label>
            </td>
            <td>
              <select name="SEARCH_CATEGORY_ID" id="SEARCH_CATEGORY_ID" size="1">
                <option value="${searchCategoryId!}">${uiLabelMap.ProductEntireCatalog}</option>
                <#list otherSearchProdCatalogCategories as otherSearchProdCatalogCategory>
                  <#assign searchProductCategory = otherSearchProdCatalogCategory.getRelatedOne("ProductCategory", true)>
                    <#if searchProductCategory??>
                      <option value="${searchProductCategory.productCategoryId}">${searchProductCategory.description?default("No Description " + searchProductCategory.productCategoryId)}</option>
                    </#if>
                </#list>
              </select>
            </td>
            </#if>
            <td>
               <#if !(0 &lt; otherSearchProdCatalogCategories?size)>
                <input type="hidden" name="SEARCH_CATEGORY_ID" value="${searchCategoryId!}" />
              </#if>
              <input type="submit" value="${uiLabelMap.ProductAdvancedSearch}" class="button" />
            </td>
          </tr>
    </table>
    </form>
  </div>
</div>
