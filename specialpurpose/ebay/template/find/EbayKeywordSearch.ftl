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
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.ProductSearchProducts}, ${uiLabelMap.ProductSearchFor}:</h3>
  </div>
  <div class="screenlet-body">
    <#list searchConstraintStrings as searchConstraintString>
      <div>
        <a href="<@ofbizUrl>keywordsearch?removeConstraint=${searchConstraintString_index}&amp;clearSearch=N</@ofbizUrl>"
            class="buttontext">X</a>
        ${searchConstraintString}
      </div>
    </#list>
    <span class="label">${uiLabelMap.CommonSortedBy}:</span>${searchSortOrderString}
    <div>
      <a href="<@ofbizUrl>advancedsearch?SEARCH_CATEGORY_ID=${(requestParameters.SEARCH_CATEGORY_ID)!}</@ofbizUrl>"
          class="buttontext">${uiLabelMap.CommonRefineSearch}</a>
    </div>

    <#if !productIds?has_content>
      <div><h2>${uiLabelMap.ProductNoResultsFound}.</h2></div>
    </#if>

    <#if productIds?has_content>
      <#macro paginationPanel>
        <div class="clearfix">
          <div class="lefthalf margin-left">
            <input type="checkbox" name="selectAll" value="0" class="selectAll" form="products"/>
            <strong>${uiLabelMap.ProductProduct}</strong>
          </div>
          <div class="right">
            <strong>
              <#if 0 &lt; viewIndex?int>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>"
                    class="buttontext">${uiLabelMap.CommonPrevious}</a> |
              </#if>
              <#if 0 &lt; listSize?int>
                ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
              </#if>
              <#if highIndex?int &lt; listSize?int>
                | <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N/~PAGING=${paging}/~noConditionFind=${noConditionFind}</@ofbizUrl>"
                    class="buttontext">${uiLabelMap.CommonNext}</a>
              </#if>
              <#if paging == "Y">
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=99999/~clearSearch=N/~PAGING=N/~noConditionFind=${noConditionFind}</@ofbizUrl>"
                    class="buttontext">${uiLabelMap.CommonPagingOff}</a>
              <#else>
                <a href="<@ofbizUrl>keywordsearch/~VIEW_INDEX=0/~VIEW_SIZE=${previousViewSize}/~clearSearch=N/~PAGING=Y/~noConditionFind=${noConditionFind}</@ofbizUrl>"
                    class="buttontext">${uiLabelMap.CommonPagingOn}</a>
              </#if>
            </strong>
          </div>
        </div>
      </#macro>
      <@paginationPanel />
      <form method="post" name="products" action="" id="products">
          <input type="hidden" name="productStoreId" value="${parameters.productStoreId!}" />
          <input type="hidden" name="SEARCH_CATEGORY_ID" value="${(requestParameters.SEARCH_CATEGORY_ID)!}" />
          <table class="basic-table border-top border-bottom">
            <#assign listIndex = lowIndex />
            <#assign altRow = false />
            <#list productIds as productId>
              <#assign altRow = !altRow />
              <#assign product = delegator.findOne("Product", {"productId" : productId}, true) />
              <tr <#if altRow> class="alternate-row"</#if>>
                <td>
                  <input type="checkbox" name="selectResult" value="${productId}"/>
                  <a href="<@ofbizUrl>EditProduct?productId=${productId}</@ofbizUrl>"
                      class="buttontext">[${productId}] ${(product.internalName)!}</a>
                </td>
              </tr>
            </#list>
          </table>
      </form>
      <@paginationPanel />
    </#if>
  </div>
</div>
