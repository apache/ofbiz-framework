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
<div class="head1">${uiLabelMap.WebtoolsFindValues}</div>
<div class="head2">${uiLabelMap.WebtoolsForEntity}: ${entityName}</div>
<div>&nbsp;</div>
<div>
    <a href="<@ofbizUrl>entitymaint</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToEntityList}</a>
</div>
<div>
    <a href="<@ofbizUrl>ViewRelations?entityName=${entityName}</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsViewRelations}</a>
    <a href="<@ofbizUrl>FindGeneric?entityName=${entityName}&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonFind} ${uiLabelMap.CommonAll}</a>
    <#if hasCreatePermission == 'Y'>
        <a href="<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonCreateNew} ${entityName}</a>
    </#if>
</div>
<div>&nbsp;</div>
<div class="tabletext">${uiLabelMap.WebtoolsToFindAll} ${entityName}, ${uiLabelMap.WebtoolsLeaveAllEntriesBlank}.</div>
<form method="post" action="<@ofbizUrl>FindGeneric?entityName=${entityName}</@ofbizUrl>">
    <input type="hidden" name="find" value="true">
    <table border="1" cellpadding="2" cellspacing="0">
        <tr class="viewOneTR1">
            <td valign="top">
                <span class="tableheadtext">${uiLabelMap.WebtoolsFieldName}</span>
            </td>
            <td valign="top">
                <span class="tableheadtext">${uiLabelMap.WebtoolsPk}</span>
            </td>
            <td valign="top">
                <span class="tableheadtext">${uiLabelMap.WebtoolsFieldType}</span>
            </td>
            <td valign="top">
                <span class="tableheadtext"><input type="submit" value="${uiLabelMap.CommonFind}" class="smallSubmit"></span>
            </td>
        </tr>
        <#assign rowClass = 'viewManyTR1'>
        <#list fieldList as field>
            <tr class='${rowClass}'>
                <td valign="top">
                    <span class="tableheadtext">${field.name}</span>
                </td>
                <td valign="top">
                    <span class="tabletext"><#if field.isPk == 'Y'>*</#if></span>
                </td>
                <td valign="top">
                    <span class="tabletext">${field.javaType},${field.sqlType}</span>
                </td>
                <td valign="top">
                    <input type="text" name="${field.name}" value="${field.param}" size="40" class="inputBox">
                </td>
            </tr>
            <#if rowClass == 'viewManyTR1'>
                <#assign rowClass = 'viewManyTR2'>
            <#else>
                <#assign rowClass = 'viewManyTR1'>
            </#if>
        </#list>
        <tr>
            <td valign="top" align="center" colspan="4"><input type="submit" value="${uiLabelMap.CommonFind}" class="smallSubmit"></td>
        </tr>
    </table>
</form>
<div>&nbsp;</div>
<#if hasCreatePermission == 'Y'>
    <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonCreateNew} ${entityName}</a>
</#if>
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
