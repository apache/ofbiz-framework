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

<#assign maxToShow = 4/>
<#assign searchOptionsHistoryList = Static["org.ofbiz.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)!/>
<#if searchOptionsHistoryList?has_content>
    <#if (searchOptionsHistoryList?size > maxToShow)><#assign limit=maxToShow/><#else><#assign limit=(searchOptionsHistoryList?size-1)/></#if>
    <div id="minilastproductsearches" class="screenlet">
      <div class="boxlink">
        <a href="<@ofbizUrl>clearLastViewed</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonClear}]</a>
        <#if (searchOptionsHistoryList?size > maxToShow)>
          <a href="<@ofbizUrl>advancedsearch</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonMore}]</a>
        </#if>
      </div>
      <h3>${uiLabelMap.OrderLastSearches}...</h3>
      <ul>
        <#list searchOptionsHistoryList[0..limit] as searchOptions>
          <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
          <li>
          ${uiLabelMap.EcommerceSearchNumber} ${searchOptions_index + 1}
            <ul>
              <li>
                <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&amp;clearSearch=N</@ofbizUrl>" class="button">${uiLabelMap.CommonSearch}</a>
                <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="button">${uiLabelMap.CommonRefine}</a>
              </li>
              <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
              <#list constraintStrings as constraintString>
                <li>${constraintString}</li>
                </#list>
            </ul>
          </li>
        </#list>
      </ul>
    </div>
</#if>
