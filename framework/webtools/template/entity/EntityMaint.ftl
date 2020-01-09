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
    <div>
       <form action="<@ofbizUrl>/entitymaint</@ofbizUrl>">
          <b>${uiLabelMap.CommonGroup}:</b>
          <select name="filterByGroupName">
             <option value="">${uiLabelMap.CommonAll}</option>
             <#list entityGroups as group>
                <option value="${group}" <#if filterByGroupName??><#if group = filterByGroupName>selected="selected"</#if></#if>>${group}</option>
             </#list>
          </select>
          <b>${uiLabelMap.WebtoolsEntityName}:</b>
          <input type= "text" name= "filterByEntityName" value="${parameters.filterByEntityName!}"/>
          <input type="submit" value="${uiLabelMap.CommonApply}"/>
       </form>
    </div>
    <#assign firstChar = "x">
    <#assign anchor="">
    <#assign alt_row = false>
    <#assign right_col = false>
    <div class="button-bar">
      <#list entitiesList as entity>
        <#if entity.entityName?substring(0, 1) != firstChar>
          <#assign firstChar = entity.entityName?substring(0, 1)>
          <a href="#Entity_${firstChar}">${firstChar}</a>&nbsp;
        </#if>
      </#list>
    </div>
    <div class="screenlet">
      <div class="screenlet-title-bar">
        <ul>
          <li class="h3">${uiLabelMap.WebtoolsEntitiesAlpha}</li>
        </ul>
        <br class="clear"/>
      </div>
      <div class="screenlet-body">
        <#assign firstChar = "*">
        <table class="basic-table hover-bar" cellspacing='0'>
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
              <#if entity.viewEntity != 'Y' && entity.entityPermissionCreate == 'Y'>
                <a href='<@ofbizUrl>entity/create/${entity.entityName}</@ofbizUrl>' title='${uiLabelMap.CommonCreate}'>${uiLabelMap.WebtoolsCreate}</a>
              </#if>
              <#if entity.entityPermissionView == 'Y'>
                <a href='<@ofbizUrl>entity/relations/${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsReln}</a>
                <a href='<@ofbizUrl>entity/find/${entity.entityName}</@ofbizUrl>'>${uiLabelMap.WebtoolsFind}</a>
                <a href='<@ofbizUrl>entity/find/${entity.entityName}?noConditionFind=Y</@ofbizUrl>'>${uiLabelMap.WebtoolsAll}</a>
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
      </div>
    </div>
