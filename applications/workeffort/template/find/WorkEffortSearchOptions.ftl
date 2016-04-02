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
    <h3>${uiLabelMap.CommonAdvancedSearch}</h3>
  </div>
  <div class="screenlet-body">
    <form name="advToKeyWordSearchForm" method="post" action="<@ofbizUrl>WorkEffortSearchResults</@ofbizUrl>" style="margin: 0;">
      <input type="hidden" name="VIEW_SIZE" value="25"/>
      <table class="basic-table" cellspacing="0">
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.WorkEffortKeywords}</td>
          <td valign="middle">
            <div>
              <input type="text" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING!}"/>&nbsp;
              ${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if searchOperator == "OR">checked="checked"</#if>/>
              ${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if searchOperator == "AND">checked="checked"</#if>/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.WorkEffortReviews}</td>
          <td valign="middle">
            <div>
              <input type="text" name="SEARCH_STRING_REVIEW_TEXT" size="40" value="${requestParameters.SEARCH_STRING_REVIEW_TEXT!}"/>&nbsp;
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.FormFieldTitle_workEffortId}</td>
          <td valign="middle">
            <div>
              <@htmlTemplate.lookupField value="${requestParameters.SEARCH_WORK_EFFORT_ID!}" formName="advToKeyWordSearchForm" name="SEARCH_WORK_EFFORT_ID" id="SEARCH_WORK_EFFORT_ID" fieldFormName="LookupWorkEffort"/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" nowrap="nowrap" class="label">${uiLabelMap.FormFieldTitle_workEffortAssocTypeId}</td>
          <td valign="middle" nowrap="nowrap">
            <div>
              <select name="workEffortAssocTypeId">
                <option value="">- ${uiLabelMap.WorkEffortAnyAssocType} -</option>
                  <#list workEffortAssocTypes as workEffortAssocType>
                      <option value="${workEffortAssocType.workEffortAssocTypeId}">${workEffortAssocType.description}</option>
                  </#list>
                  </select>
                  ${uiLabelMap.WorkEffortIncludeAllSubWorkEfforts}?
                  ${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_WORK_EFFORTS" value="Y" checked="checked"/>
                  ${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_WORK_EFFORTS" value="N"/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.PartyPartyId}</td>
          <td valign="middle">
            <div>
              <@htmlTemplate.lookupField value="${requestParameters.partyId!}" formName="advToKeyWordSearchForm" name="partyId" id="partyId" fieldFormName="LookupPartyName"/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.PartyRoleTypeId}</td>
          <td valign="middle">
            <div>
              <select name="partyRoleTypeId">
                <option value="">- ${uiLabelMap.CommonAnyRoleType} -</option>
                <#list roleTypes as roleType>
                   <option value="${roleType.roleTypeId}">${roleType.description}</option>
                 </#list>
              </select>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.WorkEffortProductId1}</td>
          <td valign="middle">
            <div>
              <@htmlTemplate.lookupField value="${requestParameters.productId_1!}" formName="advToKeyWordSearchForm" name="productId_1" id="productId_1" fieldFormName="LookupProduct"/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.WorkEffortProductId2}</td>
          <td valign="middle">
            <div>
              <@htmlTemplate.lookupField value="${requestParameters.productId_2!}" formName="advToKeyWordSearchForm" name="productId_2" id="productId_2" fieldFormName="LookupProduct"/>
            </div>
          </td>
        </tr>
        <tr>
          <td width='25%' align='right' class='label'>${uiLabelMap.WorkEffortLastUpdatedDateFilter}</td>
          <td>
            <table class="basic-table" cellspacing="0">
               <tr>
                  <td nowrap="nowrap">
                    <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${requestParameters.fromDate!}" size="25" maxlength="30" id="fromDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <span>${uiLabelMap.CommonFrom}</span>
                  </td>
               </tr>
               <tr>
                  <td nowrap="nowrap">
                    <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${requestParameters.thruDate!}" size="25" maxlength="30" id="thruDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                    <span>${uiLabelMap.CommonThru}</span>
                  </td>
               </tr>
            </table>
          </td>
          </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.CommonSortedBy}</td>
          <td valign="middle">
            <div>
              <select name="sortOrder">
                <option value="SortKeywordRelevancy">${uiLabelMap.ProductKeywordRelevancy}</option>
                <option value="SortWorkEffortField:workEffortName">${uiLabelMap.WorkEffortName}</option>
              </select>
              ${uiLabelMap.ProductLowToHigh}<input type="radio" name="sortAscending" value="Y" checked="checked"/>
              ${uiLabelMap.ProductHighToLow}<input type="radio" name="sortAscending" value="N"/>
            </div>
          </td>
        </tr>
        <#if searchConstraintStrings?has_content>
          <tr>
            <td align="right" valign="top" class="label">${uiLabelMap.ProductLastSearch}</td>
            <td valign="top">
                <#list searchConstraintStrings as searchConstraintString>
                    <div>&nbsp;-&nbsp;${searchConstraintString}</div>
                </#list>
                <div class="label">${uiLabelMap.CommonSortedBy} ${searchSortOrderString}</div>
                <div>
                  ${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked="checked"/>
                  ${uiLabelMap.CommonRefineSearch}<input type="radio" name="clearSearch" value="N"/>
                </div>
            </td>
          </tr>
        </#if>
        <tr>
          <td colspan="2" align="center">
            <div>
              <a href="javascript:document.advToKeyWordSearchForm.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
            </div>
          </td>
        </tr>
      </table>
        <input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onclick="javascript:document.advToKeyWordSearchForm.submit();"/>
    </form>
  </div>
</div>
