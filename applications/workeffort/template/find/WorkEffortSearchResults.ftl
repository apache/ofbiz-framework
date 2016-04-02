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
    <h3>${uiLabelMap.WorkEffortSearchWorkEfforts}, ${uiLabelMap.ProductSearchFor}:</h3>
  </div>
  <div class="screenlet-body">
    <#list searchConstraintStrings as searchConstraintString>
        <div>&nbsp;<a href="<@ofbizUrl>WorkEffortSearchResults?removeConstraint=${searchConstraintString_index}&amp;clearSearch=N</@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
    </#list>
    <div><span class="label">${uiLabelMap.CommonSortedBy}</span> ${searchSortOrderString}</div>
    <div><a href="<@ofbizUrl>WorkEffortSearchOptions</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefineSearch}</a></div>

    <#if !workEffortIds?has_content>
      <br /><h2>&nbsp;${uiLabelMap.ProductNoResultsFound}.</h2>
    </#if>

    <#if workEffortIds?has_content>
    <table class="basic-table" cellspacing="0">
        <tr>
          <td align="right" class="label">
            <#if 0 < viewIndex?int>
              <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a>
            </#if>
            <#if 0 < listSize?int>
              ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
            </#if>
            <#if highIndex?int < listSize?int>
              | <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
            </#if>
          </td>
        </tr>
    </table>
    </#if>

    <#if workEffortIds?has_content>

    <center>
      <table width="100%" cellpadding="0" cellspacing="0">
        <#assign listIndex = lowIndex>
        <#list workEffortIds as workEffortId><#-- note that there is no boundary range because that is being done before the list is put in the content -->
          <#assign workEffort = delegator.findOne("WorkEffort", Static["org.ofbiz.base.util.UtilMisc"].toMap("workEffortId", workEffortId), false)>
          <tr>
            <td>
              <a href="<@ofbizUrl>EditWorkEffort?workEffortId=${workEffortId}</@ofbizUrl>" class="buttontext">${workEffortId} ${(workEffort.workEffortName)!}</a>
            </td>
          </tr>
        </#list>
      </table>
    </center>
    </#if>

    <#if workEffortIds?has_content>
    <table class="basic-table" cellspacing="0">
        <tr>
          <td align="right" class="label">
            <#if 0 < viewIndex?int>
              <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonPrevious}</a> |
            </#if>
            <#if 0 < listSize?int>
              ${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
            </#if>
            <#if highIndex?int < listSize?int>
              | <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonNext}</a>
            </#if>
          </td>
        </tr>
    </table>
    </#if>
  </div>
</div>
