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
        <#assign alt_row = !alt_row>
    </#list>
        <tr>
            <td colspan="3">&nbsp;</td>
            <td><input type="submit" value="${uiLabelMap.CommonFind}"></td>
        </tr>
    </table>
</form>
<br />

<#if hasCreatePermission == 'Y'>
    <div class="button-bar">
    <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="smallSubmit">${uiLabelMap.CommonCreateNew} ${entityName}</a>
    </div>
</#if>

<#macro tableNav>
    <div class="button-bar">
        <ul>
            <#if (viewIndex > 0)> 
                <li><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexPrevious}</@ofbizUrl>' class="nav-previous">${uiLabelMap.CommonPrevious}</a></li>
                <li>|</li>
            </#if>
            <#if (arraySize > 0)>
                <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${arraySize}</li>
            </#if>
            <#if (arraySize > highIndex)>
                <li>|</li>
                <li><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexNext}</@ofbizUrl>' class="nav-next">${uiLabelMap.CommonNext}</a></li>
            </#if>
        </ul>
        <br class="clear"/>
    </div>
</#macro>
<#if (arraySize > 0)>
    <@tableNav/>
</#if>

<table class="basic-table light-grid hover-bar" cellspacing="0">
    <tr class="header-row">
        <td>&nbsp;</td>
        <#list fieldList as field>
            <td>${field.name}</td>
        </#list>
    </tr>
    <#if resultPartialList?has_content>
        <#assign alt_row = false>
        <#list records as record>
            <tr<#if alt_row> class="alternate-row"</#if>>
                <td class="button-col">
                    <a href='<@ofbizUrl>ViewGeneric?${record.findString}</@ofbizUrl>'>${uiLabelMap.CommonView}</a>
                <#if hasDeletePermission == 'Y'>
                    <a href='<@ofbizUrl>UpdateGeneric?${record.findString}&amp;UPDATE_MODE=DELETE&amp;${curFindString}</@ofbizUrl>'>${uiLabelMap.CommonDelete}</a>
                </#if>
                </td>
                <#list record.fields as field>
                    <td>
                        ${field}
                    </td>
                </#list>
            </tr>
            <#assign alt_row = !alt_row>
        </#list>
    <#else>
        <tr>
            <td colspan="${columnCount}">
                <div class="head2">${uiLabelMap.WebtoolsNoRecordsFound} ${entityName}.</div>
            </td>
        </tr>
    </#if>
</table>

<#if (arraySize > 0)>
    <@tableNav/>
</#if>

<#if hasCreatePermission == 'Y'>
    <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="smallSubmit">${uiLabelMap.CommonCreateNew} ${entityName}</a>
<#else>
    <h2>${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}.</h2>
</#if>
