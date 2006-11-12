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

<div class="head1">${uiLabelMap.WebtoolsEntityDataMaintenance}</div>
<div>&nbsp;</div>
<#assign rowOneClass = 'viewOneTR1'>
<#assign rowClass = 'viewManyTR1'>
<table cellpadding='1' cellspacing='1' border='0'>
    <tr>
        <td valign="top">
            <table cellpadding="2" cellspacing="0" border="1" width="100%" class="boxoutside">
                <tr class='${rowOneClass}'>
                    <td colspan='5' class="tableheadtext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsEntityName}</td>
                </tr>
                <#list entitiesList as entity>
                    <tr class='${rowClass}'>
                        <td>
                            ${entity.entityName}
                        </td>
                        <#if entity.viewEntity == 'Y'>
                            <#if entity.entityPermissionView == 'Y'>
                                <td colspan='3' align="center"><div class='tabletext' style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsEntityView}</div></td>
                                <td><a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>' class="buttontext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsAll}</a></td>
                            <#else>
                                <td colspan='3' align="center"><div class='tabletext' style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsEntityView}</div></td>
                        </#if>
                        <#else>
                            <#if entity.entityPermissionCreate == 'Y'>
                                <td><a href='<@ofbizUrl>ViewGeneric?entityName=${entity.entityName}</@ofbizUrl>' class="buttontext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsCreate}</a></td>
                            <#else>
                                <td><div class='tabletext' style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsNotPresent}</div></td>
                            </#if>
                            <#if entity.entityPermissionView == 'Y'>
                                <td><a href='<@ofbizUrl>ViewRelations?entityName=${entity.entityName}</@ofbizUrl>' class="buttontext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsReln}</a></td>
                                <td><a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}</@ofbizUrl>' class="buttontext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsFind}</a></td>
                                <td><a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>' class="buttontext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsAll}</a></td>
                            <#else>
                                <td><div class='tabletext' style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsNotPresent}</div></td>
                                <td><div class='tabletext' style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsNotPresent}</div></td>
                            </#if>
                        </#if>
                    </tr>
                    <#if rowClass == 'viewManyTR1'>
                        <#assign rowClass = 'viewManyTR2'>
                    <#else>
                        <#assign rowClass = 'viewManyTR1'>
                    </#if>
                    <#if entity.changeColumn == 'Y'>
                        </table>
                        </td>
                        <td valign="top">
                        <table cellpadding="2" cellspacing="0" border="1" width="100%" class="boxoutside">
                            <tr class='${rowOneClass}'>
                                <td colspan='5' class="tableheadtext" style='FONT-SIZE: xx-small;'>${uiLabelMap.WebtoolsEntityName}</td>
                            </tr>
                        <#assign rowClass = 'viewManyTR1'>
                    </#if>
                </#list>
            </table>
        </td>
    </tr>
    <tr>
        <td>&nbsp;</td>
    </tr>
    <tr>
        <td class="tableheadtext">${uiLabelMap.CommonNote} : </td>
    </tr>
    <tr>
        <td class="tabletext">${uiLabelMap.WebtoolsCreate} :- ${uiLabelMap.CommonCreateNew}</td>
    </tr>
    <tr>
        <td class="tabletext">${uiLabelMap.WebtoolsReln} :- ${uiLabelMap.WebtoolsViewRelations}</td>
    </tr>
    <tr>
        <td class="tabletext">${uiLabelMap.WebtoolsFind} :- ${uiLabelMap.WebtoolsFindRecord}</td>
    </tr>
    <tr>
        <td class="tabletext">${uiLabelMap.WebtoolsAll} :- ${uiLabelMap.WebtoolsFindAllRecords}</td>
    </tr>   
</table>
