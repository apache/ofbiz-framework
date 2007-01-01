<#--

Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<div class="head1">${uiLabelMap.ProductAdvancedSearch}</div>
<br/>
<form name="advToKeyWordSearchForm" method="post" action="<@ofbizUrl>WorkEffortSearchResults</@ofbizUrl>" style="margin: 0;">
  <input type="hidden" name="VIEW_SIZE" value="25"/>
  <table border="0" wdith="100%">
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.WorkEffortKeywords}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING?if_exists}"/>&nbsp;
          ${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked</#if>/>
          ${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked</#if>/>
        </div>
      </td>
    </tr>

    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.WorkEffortReviews}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_STRING_REVIEW_TEXT" size="40" value="${requestParameters.SEARCH_STRING_REVIEW_TEXT?if_exists}"/>&nbsp;
        </div>
      </td>
    </tr>

    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.CommonSortedBy}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <select name="sortOrder" class="selectBox">
            <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevency}</option>
            <option value="SortWorkEffortField:workEffortName">${uiLabelMap.WorkEffortName}</option>
          </select>
          ${uiLabelMap.ProductLowToHigh}<input type="radio" name="sortAscending" value="Y" checked/>
          ${uiLabelMap.ProductHighToLow}<input type="radio" name="sortAscending" value="N"/>
        </div>
      </td>
    </tr>
    <#if searchConstraintStrings?has_content>
      <tr>
        <td align="right" valign="top">
          <div class="tabletext">${uiLabelMap.ProductLastSearch}</div>
        </td>
        <td valign="top">
            <#list searchConstraintStrings as searchConstraintString>
                <div class="tabletext">&nbsp;-&nbsp;${searchConstraintString}</div>
            </#list>
            <div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
            <div class="tabletext">
              ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked/>
              ${uiLabelMap.ProductRefineSearch}<input type="radio" name="clearSearch" value="N"/>
            </div>
        </td>
      </tr>
    </#if>
    <tr>
      <td>
        <div class="tabletext">
          <a href="javascript:document.advToKeyWordSearchForm.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
        </div>
      </td>
    </tr>
    <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.advToKeyWordSearchForm.submit();"/>
  </table>
</form>
