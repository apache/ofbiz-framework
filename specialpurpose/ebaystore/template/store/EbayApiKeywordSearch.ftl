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

<script type="text/javascript">
//<![CDATA[
    function selectChange(formId, elementId) {
        if (elementId.id == 'searchCatalogId') {
            if (document.getElementById('searchCategoryId').selectedIndex) {
               document.getElementById('searchCategoryId')[document.getElementById('searchCategoryId').selectedIndex].value = "";
           } else {
               document.getElementById('searchCategoryId').value = "";
           }
        }
        formId.action="<@ofbizUrl>main</@ofbizUrl>";
        formId.submit();
    }
    function submit (id) {
      var formId = id;
      if(!jQuery('#searchCatalogId').is(":empty")){
          document.getElementById(formId).submit();
      } else {
          if(jQuery('#searchCatalogId').is(":empty")) {
             jQuery('#catalogErrorMessage').fadeIn("fast");
          }
      }
    }
//]]>
</script>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductAdvancedSearchInCategory}</h3>
  </div>
  <div class="screenlet-body">
    <form id="productSearchform" method="post" action="<@ofbizUrl>productsearch</@ofbizUrl>" style="margin: 0;" name="productSearchform">
    <input type="hidden" name="productStoreId" value="${parameters.productStoreId!}" />
      <fieldset>
        <input type="hidden" name="VIEW_SIZE" value="25"/>
        <input type="hidden" name="PAGING" value="Y"/>
        <input type="hidden" name="noConditionFind" value="Y"/>
        <table cellspacing="0" class="basic-table">
          <tr>
              <td class="label" align="right" valign="top">
                ${uiLabelMap.ProductCatalog}:
              </td>
              <td valign="middle">
                <div>
                  <select name="SEARCH_CATALOG_ID" id="searchCatalogId" onchange="javascript:selectChange(document.getElementById('advToKeywordSearchform'), document.getElementById('searchCatalogId'));" class="required">
                    <#list prodCatalogList as prodCatalog>
                      <#assign displayDesc = prodCatalog.catalogName?default("${uiLabelMap.ProductNoDescription}") />
                      <#if (18 < displayDesc?length)>
                        <#assign displayDesc = displayDesc[0..15] + "...">
                      </#if>
                      <option value="${prodCatalog.prodCatalogId}" <#if searchCatalogId! == prodCatalog.prodCatalogId> selected="selected"</#if>>${displayDesc} [${prodCatalog.prodCatalogId}]</option>
                    </#list>
                  </select>
                  <span id="catalogErrorMessage" style="display:none;" class="errorMessage">${uiLabelMap.CommonRequired}</span>
                </div>
              </td>
            </tr>
            <tr>
              <td class="label" align="right" valign="top">
                ${uiLabelMap.ProductCategory}:
              </td>
              <td valign="middle">
                <div>
                  <#if categoryIds?has_content>
                    <select name="SEARCH_CATEGORY_ID" id="searchCategoryId">
                      <option value="">- ${uiLabelMap.ProductAnyCategory} -</option>
                      <#list categoryIds as categoryId>
                        <#assign productCategory = delegator.findOne("ProductCategory", {"productCategoryId" : categoryId}, true) />
                        <#assign displayDesc = productCategory.categoryName?default("${uiLabelMap.ProductNoDescription}") />
                        <#if (18 < displayDesc?length)>
                          <#assign displayDesc = displayDesc[0..15] + "...">
                        </#if>
                        <option value="${productCategory.productCategoryId}">${displayDesc} [${productCategory.productCategoryId}]</option>
                      </#list>
                    </select>
                  <#else>
                    <@htmlTemplate.lookupField value="${requestParameters.SEARCH_CATEGORY_ID!}" formName="productSearchform" name="SEARCH_CATEGORY_ID" id="searchCategoryId" fieldFormName="LookupProductCategory"/>
                  </#if>
                </div>
              </td>
          </tr>
          <tr>
            <td class="label" align="right" valign="top">
              ${uiLabelMap.ProductProductName}:
            </td>
            <td valign="middle">
              <div>
                <input type="text" name="SEARCH_PRODUCT_NAME" size="20" value="${requestParameters.SEARCH_PRODUCT_NAME!}" />
              </div>
            </td>
          </tr>
          <tr>
            <td class="label" align="right" valign="top">
              ${uiLabelMap.ProductInternalName}:
            </td>
            <td valign="middle">
              <div>
                <input type="text" name="SEARCH_INTERNAL_PROD_NAME" size="20" value="${requestParameters.SEARCH_INTERNAL_PROD_NAME!}" />
              </div>
            </td>
          </tr>
          <tr>
            <td class="label" align="right" valign="top">
              ${uiLabelMap.ProductKeywords}:
            </td>
            <td valign="middle">
              <div>
                <input type="text" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING!}" />&nbsp;
                <label>${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked="checked"</#if> /></label>
                <label>${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked="checked"</#if> /></label>
              </div>
            </td>
          </tr>
          <tr>
            <td align="center" colspan="2">
              <hr />
              <a href="javascript:submit('productSearchform');" class="buttontext">${uiLabelMap.CommonFind}</a>
            </td>
          </tr>
        </table>
      </fieldset>
    </form>
  </div>
</div>