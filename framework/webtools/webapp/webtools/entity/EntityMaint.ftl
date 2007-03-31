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
<#assign firstChar = "x">
<#assign anchor="">
<#assign alt_row = false>
<#assign right_col = false>

<#list entitiesList as entity>
    <#if entity.entityName?substring(0, 1) != firstChar>
        <#assign firstChar = entity.entityName?substring(0, 1)>
        <a href="#Entity_${firstChar}">${firstChar}</a>&nbsp;
    </#if>
</#list>
<br/><br/>

<#assign firstChar = "*">
<table class="basic-table light-grid hover-bar" cellspacing='0'>
  <tr class="header-row">
    <td>${uiLabelMap.WebtoolsEntityName}</td>
    <td>&nbsp;</td>
    <td>${uiLabelMap.WebtoolsEntityName}</td>
    <td>&nbsp;</td>
  </tr>
  <#list entitiesList as entity>
    <#if entity.entityName?substring(0, 1) != firstChar>
      <#if right_col>
        <td>&nbsp;</td><td>&nbsp;</td></tr>
        <#assign right_col = false>
        <#assign alt_row = !alt_row>
      </#if>
      <#if firstChar != "*">
        <tr<#if alt_row> class="alternate-row"</#if>><td colspan="4">&nbsp;</td></tr>
        <#assign alt_row = !alt_row>
      </#if>
      <#assign firstChar = entity.entityName?substring(0, 1)>
      <#assign anchor="id=\"Entity_${firstChar}\"">
    </#if>
    <#if !right_col>
      <tr<#if alt_row> class="alternate-row"</#if>>
    </#if>

    <td<#if anchor?has_content> ${anchor}</#if>>${entity.entityName}<#if entity.viewEntity == 'Y'>&nbsp;(${uiLabelMap.WebtoolsEntityView})</#if></td>
    <#assign anchor="">
    <td class="button-col">
      <#if entity.viewEntity == 'Y'>
        <#if entity.entityPermissionView == 'Y'>
          <a href='<@ofbizUrl>ViewRelations?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsReln}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsFind}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</@ofbizUrl>'>${uiLabelMap.WebtoolsAll}</a>
        </#if>
      <#else>
        <#if entity.entityPermissionCreate == 'Y'>
          <a href='<@ofbizUrl>ViewGeneric?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsCreate}</a>
        </#if>
        <#if entity.entityPermissionView == 'Y'>
          <a href='<@ofbizUrl>ViewRelations?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsReln}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsFind}</a>
          <a href='<@ofbizUrl>FindGeneric?entityName=${entity.entityName}&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</@ofbizUrl>'>${uiLabelMap.WebtoolsAll}</a>
        </#if>
      </#if>
    </td>
    <#if right_col>
      </tr>
      <#assign alt_row = !alt_row>
    </#if>
    <#assign right_col = !right_col>
  </#list>
  <#if right_col>
    <td>&nbsp;</td><td>&nbsp;</td></tr>
  </#if>
</table>
