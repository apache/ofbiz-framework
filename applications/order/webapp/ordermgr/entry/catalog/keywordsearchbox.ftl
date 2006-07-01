<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
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
