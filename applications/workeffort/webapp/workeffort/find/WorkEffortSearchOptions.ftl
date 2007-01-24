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
        <div class="tabletext">${uiLabelMap.FormFieldTitle_workEffortId}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="SEARCH_WORK_EFFORT_ID" size="40" value="${requestParameters.SEARCH_WORK_EFFORT_ID?if_exists}"/>&nbsp;
          <a href="javascript:call_fieldlookup2(document.advToKeyWordSearchForm.SEARCH_WORK_EFFORT_ID,'LookupWorkEffort');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle" nowrap>
          <div class="tabletext">${uiLabelMap.FormFieldTitle_workEffortAssocTypeId}:</div>
      </td>
      <td valign="middle" nowrap>
        <div class="tabletext">
          <select class="selectBox" name="workEffortAssocTypeId">
            <option value="">- ${uiLabelMap.WorkEffortAnyAssocType} -</option>
              <#list workEffortAssocTypes as workEffortAssocType>
                  <option value="${workEffortAssocType.workEffortAssocTypeId}">${workEffortAssocType.description}</option>
              </#list>
              </select>
              ${uiLabelMap.WorkEffortIncludeAllSubWorkEfforts}?
              ${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_WORK_EFFORTS" value="Y" checked/>
              ${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_WORK_EFFORTS" value="N"/>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.PartyPartyId}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="partyId" size="40" value="${requestParameters.partyId?if_exists}"/>&nbsp;
          <a href="javascript:call_fieldlookup2(document.advToKeyWordSearchForm.partyId,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
          <div class="tabletext">${uiLabelMap.PartyRoleTypeId}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <select class="selectBox" name="partyRoleTypeId">
            <option value="">- ${uiLabelMap.CommonAnyRoleType} -</option>
            <#list roleTypes as roleType>
               <option value="${roleType.roleTypeId}">${roleType.description}</option>
             </#list>
          </select>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.WorkEffortProductId1}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="productId_1" size="40" value="${requestParameters.productId_1?if_exists}"/>&nbsp;
          <a href="javascript:call_fieldlookup2(document.advToKeyWordSearchForm.productId_1,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </div>
      </td>
    </tr>
    <tr>
      <td align="right" valign="middle">
        <div class="tabletext">${uiLabelMap.WorkEffortProductId2}:</div>
      </td>
      <td valign="middle">
        <div class="tabletext">
          <input type="text" class="inputBox" name="productId_2" size="40" value="${requestParameters.productId_2?if_exists}"/>&nbsp;
          <a href="javascript:call_fieldlookup2(document.advToKeyWordSearchForm.productId_2,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </div>
      </td>
    </tr>
    <tr>
      <td width='25%' align='right'>
        <div class='tableheadtext'>Last Updated Date Filter</div>
      </td>
      <td>
        <table border='0' cellspacing='0' cellpadding='0'>
           <tr>
              <td nowrap>
                <input type='text' size='25' class='inputBox' name='fromDate' value='${requestParameters.fromDate?if_exists}'/>
                  <a href="javascript:call_cal(document.advToKeyWordSearchForm.fromDate,'${fromDateStr}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/></a>
                    <span class='tabletext'>${uiLabelMap.CommonFrom}</span>
              </td>
           </tr>
           <tr>
              <td nowrap>
                <input type='text' size='25' class='inputBox' name='thruDate' value='${requestParameters.thruDate?if_exists}'/>
                <a href="javascript:call_cal(document.advToKeyWordSearchForm.thruDate,'${thruDateStr}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'/></a>
                <span class='tabletext'>${uiLabelMap.CommonThru}</span>
              </td>
           </tr>
          </table>
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
