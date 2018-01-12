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
    <form name="advToKeyWordSearchForm" method="post" action="<@ofbizUrl>ContentSearchResults</@ofbizUrl>" style="margin: 0;">
      <input type="hidden" name="VIEW_SIZE" value="25"/>
      <table class="basic-table" cellspacing="0">
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.ContentKeywords}</td>
          <td valign="middle">
            <div>
              <input type="text" name="SEARCH_STRING" size="40" value="${requestParameters.SEARCH_STRING!}"/>&nbsp;
              <label>${uiLabelMap.CommonAny}<input type="radio" name="SEARCH_OPERATOR" value="OR" <#if "OR" == searchOperator>checked="checked"</#if>/></label>
              <label>${uiLabelMap.CommonAll}<input type="radio" name="SEARCH_OPERATOR" value="AND" <#if "AND" == searchOperator>checked="checked"</#if>/></label>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" class="label">${uiLabelMap.FormFieldTitle_contentId}</td>
          <td valign="middle">
            <div>
              <@htmlTemplate.lookupField value="${requestParameters.SEARCH_CONTENT_ID!}" formName="advToKeyWordSearchForm" name="SEARCH_CONTENT_ID" id="SEARCH_CONTENT_ID" fieldFormName="LookupContent"/>
            </div>
          </td>
        </tr>
        <tr>
          <td align="right" valign="middle" nowrap="nowrap" class="label">${uiLabelMap.FormFieldTitle_contentAssocTypeId}</td>
          <td valign="middle" nowrap="nowrap">
            <div>
              <select name="contentAssocTypeId">
                <option value="">- ${uiLabelMap.ContentAnyAssocType} -</option>
                  <#list contentAssocTypes as contentAssocType>
                      <option value="${contentAssocType.contentAssocTypeId}">${contentAssocType.description}</option>
                  </#list>
                  </select>
                  ${uiLabelMap.ContentIncludeAllSubContents}?
                  <label>${uiLabelMap.CommonYes}<input type="radio" name="SEARCH_SUB_CONTENTS" value="Y" checked="checked"/></label>
                  <label>${uiLabelMap.CommonNo}<input type="radio" name="SEARCH_SUB_CONTENTS" value="N"/></label>
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
          <td width='25%' align='right' class='label'>${uiLabelMap.ContentLastUpdatedDateFilter}</td>
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
                <option value="SortContentField:contentName">${uiLabelMap.FormFieldTitle_contentName}</option>
              </select>
              <label>${uiLabelMap.ProductLowToHigh}<input type="radio" name="sortAscending" value="Y" checked="checked"/></label>
              <label>${uiLabelMap.ProductHighToLow}<input type="radio" name="sortAscending" value="N"/></label>
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
                  <label>${uiLabelMap.ProductNewSearch}<input type="radio" name="clearSearch" value="Y" checked="checked"/></label>
                  <label>${uiLabelMap.CommonRefineSearch}<input type="radio" name="clearSearch" value="N"/></label>
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
