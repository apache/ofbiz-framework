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
    <ul>
      <li class="h3">${uiLabelMap.WebtoolsRelations}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if hasViewPermission>
        <br />
        <h2>${uiLabelMap.WebtoolsForEntity}: ${entityName}</h2>
        <br />
        <div class="button-bar">
            <a href="<@ofbizUrl>entity/find/${entityName}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.WebtoolsBackToFindScreen}</a>
        </div>
        <br />
        <table class="basic-table hover-bar" cellspacing="0">
            <tr class="header-row">
                <td>${uiLabelMap.WebtoolsTitle}</td>
                <td>${uiLabelMap.WebtoolsRelatedEntity}</td>
                <td>${uiLabelMap.WebtoolsRelationType}</td>
                <td>${uiLabelMap.WebtoolsFKName}</td>
                <td>${uiLabelMap.WebtoolsFieldsList}</td>
            </tr>
            <#assign alt_row = false>
            <#list relations as relation>
                <tr<#if alt_row> class="alternate-row"</#if>>
                    <td>${relation.title}</td>
                    <td class="button-col"><a href='<@ofbizUrl>entity/find/${relation.relEntityName}</@ofbizUrl>'>${relation.relEntityName}</a></td>
                    <td>${relation.type}</td>
                    <td>${relation.fkName}</td>
                    <td>
                        <#list relation.relFields as field>
                            ${field.fieldName} -> ${field.relFieldName}<br />
                        </#list>
                    </td>
                </tr>
                <#assign alt_row = !alt_row>
            </#list>
        </table>
    <#else>
        <h3>${uiLabelMap.WebtoolsEntityCreatePermissionError} ${entityName} ${plainTableName}.</h3>
    </#if>
  </div>
</div>
