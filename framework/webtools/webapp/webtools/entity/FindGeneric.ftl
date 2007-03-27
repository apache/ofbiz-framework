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

<h1>${uiLabelMap.WebtoolsFindValues}</h1>
<br />
<h2>${uiLabelMap.WebtoolsForEntity}: ${entityName}</h2>
<br />
<div class="button-bar">
  <a href="<@ofbizUrl>entitymaint</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsBackToEntityList}</a>
  <a href="<@ofbizUrl>ViewRelations?entityName=${entityName}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsViewRelations}</a>
  <a href="<@ofbizUrl>FindGeneric?entityName=${entityName}&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonFind} ${uiLabelMap.CommonAll}</a>
  <#if hasCreatePermission == 'Y'>
    <a href="<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCreateNew} ${entityName}</a>
  </#if>
</div>
<br />
<p>${uiLabelMap.WebtoolsToFindAll} ${entityName}, ${uiLabelMap.WebtoolsLeaveAllEntriesBlank}.</p>
<form method="post" action="<@ofbizUrl>FindGeneric?entityName=${entityName}</@ofbizUrl>">
  <input type="hidden" name="find" value="true">
  <table class="basic-table light-grid" cellspacing="0">
    <tr class="header-row">
      <td>${uiLabelMap.WebtoolsFieldName}</td>
      <td>${uiLabelMap.WebtoolsPk}</td>
      <td>${uiLabelMap.WebtoolsFieldType}</td>
      <td><input type="submit" value="${uiLabelMap.CommonFind}"></td>
    </tr>
    <#assign alt_row = false>
    <#list fieldList as field>
        <tr<#if alt_row> class="alternate-row"</#if>>
            <td>${field.name}</td>
            <td><#if field.isPk == 'Y'>*</#if></td>
            <td>${field.javaType},&nbsp;${field.sqlType}</td>
            <td><input type="text" name="${field.name}" value="${field.param}" size="40"></td>
        </tr>
        <#if alt_row>
            <#assign alt_row = false>
        <#else>
            <#assign alt_row = true>
        </#if>
    </#list>
        <tr>
            <td align="center" colspan="4"><input type="submit" value="${uiLabelMap.CommonFind}"></td>
        </tr>
    </table>
</form>
<br />
<div class="button-bar">
<#if hasCreatePermission == 'Y'>
    <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonCreateNew} ${entityName}</a>
</#if>
</div>
<table border="0" width="100%" cellpadding="2">
    <#if (arraySize > 0)>
        <tr>
            <td align="left">
                <span class="tableheadtext">
                <#if (viewIndex > 0)> 
                    <a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexPrevious}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonPrevious}</a> | 
                </#if>
                <#if (arraySize > 0)>
                    ${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${arraySize}
                </#if>
                <#if (arraySize > highIndex)>
                    | <a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexNext}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonNext}</a>
                </#if>
                </span>
            </td>
        </tr>
    </#if>
</table>
<table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
    <tr class="viewOneTR1">
        <td>&nbsp;</td>
        <#if hasDeletePermission == 'Y'>
            <td>&nbsp;</td>
        </#if>
        <#list fieldList as field>
            <td nowrap><span class="tableheadtext">${field.name}</span></td>
        </#list>
    </tr>
    <#if resultPartialList?has_content>
        <#assign rowClass = 'viewManyTR1'>
        <#list records as record>
            <tr class='${rowClass}'>
                <td>
                    <a href='<@ofbizUrl>ViewGeneric?${record.findString}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonView}</a>
                </td>
                <#if hasDeletePermission == 'Y'>
                    <td>
                        <a href='<@ofbizUrl>UpdateGeneric?${record.findString}&amp;UPDATE_MODE=DELETE&amp;${curFindString}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonDelete}</a>
                    </td>
                </#if>
                <#list record.fields as field>
                    <td>
                        <div class="tabletext">
                            ${field}&nbsp;
                        </div>
                    </td>
                </#list>
            </tr>
            <#if rowClass == 'viewManyTR1'>
                <#assign rowClass = 'viewManyTR2'>
            <#else>
                <#assign rowClass = 'viewManyTR1'>
            </#if>
        </#list>
    <#else>
        <tr>
            <td colspan="${columnCount}">
                <div class="head2">${uiLabelMap.WebtoolsNoRecordsFound} ${entityName}.</div>
            </td>
        </tr>
    </#if>
</table>
<table border="0" width="100%" cellpadding="2">
    <#if (arraySize > 0)>
        <tr>
            <td align="left">
                <span class="tableheadtext">
                    <#if (viewIndex > 0)> 
                        <a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexPrevious}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonPrevious}</a> | 
                    </#if>
                    <#if (arraySize > 0)>
                        ${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${arraySize}
                    </#if>
                    <#if (arraySize > highIndex)>
                        | <a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexNext}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonNext}</a>
                    </#if>
                </span>
            </td>
        </tr>
    </#if>
</table>
<#if hasCreatePermission == 'Y'>
    <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonCreateNew} ${entityName}</a>
<#else>
    <div class="head2">${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}.</div>
</#if>
