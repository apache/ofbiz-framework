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

<div class="head1">${uiLabelMap.WorkEffortSearchWorkEfforts}, <span class="head2">${uiLabelMap.ProductSearchFor}:</span></div>
<#list searchConstraintStrings as searchConstraintString>
    <div class="tabletext">&nbsp;<a href="<@ofbizUrl>WorkEffortSearchResults?removeConstraint=${searchConstraintString_index}&clearSearch=N</@ofbizUrl>" class="buttontext">X</a>&nbsp;${searchConstraintString}</div>
</#list>
<div class="tabletext">${uiLabelMap.CommonSortedBy}: ${searchSortOrderString}</div>
<div class="tabletext"><a href="<@ofbizUrl>WorkEffortSearchOptions</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRefine} ${uiLabelMap.CommonSearch}</a></div>

<#if !workEffortIds?has_content>
  <br/><div class="head2">&nbsp;${uiLabelMap.ProductNoResultsFound}.</div>
</#if>

<#if workEffortIds?has_content>
<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
        </#if>
        </b>
      </td>
    </tr>
</table>
</#if>

<#if workEffortIds?has_content>

<center>
  <table width="100%" cellpadding="0" cellspacing="0">
    <#assign listIndex = lowIndex>
    <#list workEffortIds as workEffortId><#-- note that there is no boundary range because that is being done before the list is put in the content -->
      <#assign workEffort = delegator.findByPrimaryKey("WorkEffort", Static["org.ofbiz.base.util.UtilMisc"].toMap("workEffortId", workEffortId))>
      <tr>
        <td>
          <a href="<@ofbizUrl>EditWorkEffort?workEffortId=${workEffortId}</@ofbizUrl>" class="buttontext">${workEffortId} ${(workEffort.workEffortName)?if_exists}</a>
        </td>
      </tr>
    </#list>
  </table>
</center>
</#if>

<#if workEffortIds?has_content>
<table border="0" width="100%" cellpadding="2">
    <tr>
      <td align="right">
        <b>
        <#if 0 < viewIndex?int>
          <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex-1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
        </#if>
        <#if 0 < listSize?int>
          <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
        </#if>
        <#if highIndex?int < listSize?int>
          | <a href="<@ofbizUrl>WorkEffortSearchResults/~VIEW_INDEX=${viewIndex+1}/~VIEW_SIZE=${viewSize}/~clearSearch=N</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
        </#if>
        </b>
      </td>
    </tr>
</table>

<hr class="sepbar"/>
</#if>
