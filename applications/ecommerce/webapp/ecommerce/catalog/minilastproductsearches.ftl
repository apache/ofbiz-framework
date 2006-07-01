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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      3.1
-->

<#assign maxToShow = 4/>
<#assign searchOptionsHistoryList = Static["org.ofbiz.product.product.ProductSearchSession"].getSearchOptionsHistoryList(session)?if_exists/>
<#if searchOptionsHistoryList?has_content>
    <#if (searchOptionsHistoryList?size > maxToShow)><#assign limit=maxToShow/><#else><#assign limit=(searchOptionsHistoryList?size-1)/></#if>
    <div class="screenlet">
        <div class="screenlet-header">
            <div style="float: right;">
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
                      <a href="<@ofbizUrl>setCurrentSearchFromHistoryAndSearch?searchHistoryIndex=${searchOptions_index}&clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonSearch}]</a>
                      <a href="<@ofbizUrl>setCurrentSearchFromHistory?searchHistoryIndex=${searchOptions_index}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRefine}]</a>
                    </div>
                    <#assign constraintStrings = searchOptions.searchGetConstraintStrings(false, delegator)>
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
