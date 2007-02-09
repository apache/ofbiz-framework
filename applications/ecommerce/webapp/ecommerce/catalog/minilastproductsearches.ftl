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
<#assign searchOptionsHistoryList = Static["org.ofbiz.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)?if_exists/>
<#if searchOptionsHistoryList?has_content>
    <#if (searchOptionsHistoryList?size > maxToShow)><#assign limit=maxToShow/><#else><#assign limit=(searchOptionsHistoryList?size-1)/></#if>
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxlink">
                <a href="<@ofbizUrl>clearLastViewed</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonClear}]</a>
                <#if (searchOptionsHistoryList?size > maxToShow)>
                    <a href="<@ofbizUrl>advancedsearch</@ofbizUrl>" class="lightbuttontextsmall">[${uiLabelMap.CommonMore}]</a>
                </#if>
            </div>
            <div class="boxhead">${uiLabelMap.EcommerceLastSearches}...</div>
        </div>
        <div class="screenlet-body">
            <#list searchOptionsHistoryList[0..limit] as searchOptions>
            <#-- searchOptions type is ProductSearchSession.ProductSearchOptions -->
                    <div class="tabletext">
                      <b>${uiLabelMap.EcommerceSearchNumber} ${searchOptions_index + 1}</b>
                    </div>
                    <div class="tabletext">
                      <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonSearch}</a>
                      <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine}</a>
                    </div>
                    <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator, locale)>
                    <#list constraintStrings as constraintString>
                      <div class="tabletext">&nbsp;-&nbsp;${constraintString}</div>
                    </#list>
                <#if searchOptions_has_next>
                    <div><hr class="sepbar"/></div>
                </#if>
            </#list>
        </div>
    </div>
</#if>
