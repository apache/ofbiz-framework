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

<h1>${uiLabelMap.WebtoolsEntityDataMaintenance}</h1>
<br />
<p><b>${uiLabelMap.CommonNote}:</b></p>
<p>${uiLabelMap.WebtoolsCreate} :- ${uiLabelMap.CommonCreateNew}</p>
<p>${uiLabelMap.WebtoolsReln} :- ${uiLabelMap.WebtoolsViewRelations}</p>
<p>${uiLabelMap.WebtoolsFind} :- ${uiLabelMap.WebtoolsFindRecord}</p>
<p>${uiLabelMap.WebtoolsAll} :- ${uiLabelMap.WebtoolsFindAllRecords}</p>
<br />
<#assign firstChar = "*">
<#assign alt_row = false>
<#assign right_col = false>
<table class="basic-table light-grid hover-bar" cellspacing='0'>
  <tr class="header-row">
    <td>${uiLabelMap.WebtoolsEntityName}</td>
    <td>&nbsp;</td>
      <td>${uiLabelMap.WebtoolsEntityName}</td>
      <td>&nbsp;</td>
  </tr>
  <tr>
  <#list entitiesList as entity>
    <#if entity.entityName?substring(0, 1) != firstChar>
      <#if right_col>
        <td>&nbsp;</td><td>&nbsp;</td></tr>
        <#assign right_col = false>
      </#if>
      <#if firstChar != "*">
        <tr class="header-row"><td colspan="4">&nbsp;</td></tr>
        <tr<#if alt_row> class="alternate-row"</#if>>
      </#if>
      <#assign firstChar = entity.entityName?substring(0, 1)>
    </#if>
    <td>${entity.entityName}<#if entity.viewEntity == 'Y'>&nbsp;(${uiLabelMap.WebtoolsEntityView})</#if></td>
    <td class="button-col">
      <#if entity.viewEntity == 'Y'>
        <#if entity.entityPermissionView == 'Y'>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>'>${uiLabelMap.WebtoolsAll}</a>
        </#if>
      <#else>
        <#if entity.entityPermissionCreate == 'Y'>
          <a href='<@ofbizUrl>ViewGeneric?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsCreate}</a>
        </#if>
        <#if entity.entityPermissionView == 'Y'>
          <a href='<@ofbizUrl>ViewRelations?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsReln}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsFind}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>'>${uiLabelMap.WebtoolsAll}</a>
        </#if>
      </#if>
    </td>
    <#if right_col>
      </tr>
      <tr<#if alt_row> class="alternate-row"</#if>>
      <#if alt_row>
        <#assign alt_row = false>
      <#else>
        <#assign alt_row = true>
      </#if>
      <#assign right_col = false>
    <#else>
      <#assign right_col = true>
    </#if>
  </#list>
  <#if !right_col></tr></#if>
</table>
