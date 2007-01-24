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
<#if hasViewPermission>
    <div class="head1">${uiLabelMap.WebtoolsRelations}</div>
    <div class="head2">${uiLabelMap.WebtoolsForEntity}: ${entityName}</div>
    <div>&nbsp;</div>
    <div>
        <a href="<@ofbizUrl>FindGeneric?entityName=${entityName}&amp;find=true&amp;VIEW_SIZE=50&amp;VIEW_INDEX=0</@ofbizUrl>" class="buttontext">${uiLabelMap.WebtoolsBackToFindScreen}</a>
    </div>
    <div>&nbsp;</div>
    <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
        <tr>
            <td class="viewOneTR2"><b>${uiLabelMap.WebtoolsTitle}</b></td>
              <td class="viewOneTR2"><b>${uiLabelMap.WebtoolsRelatedEntity}</b></td>
              <td class="viewOneTR2"><b>${uiLabelMap.WebtoolsRelationType}</b></td>
              <td class="viewOneTR2"><b>${uiLabelMap.WebtoolsFKName}</b></td>
              <td class="viewOneTR2"><b>${uiLabelMap.WebtoolsFieldsList}</b></td>
           </tr>
           <#assign rowClass = 'viewManyTR1'>
        <#list relations as relation>
            <tr class="${rowClass}">
                <td>${relation.title}</td>
                   <td><a href='<@ofbizUrl>FindGeneric?entityName=${relation.relEntityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>' class="buttontext">${relation.relEntityName}</a></td>
                   <td>${relation.type}</td>
                   <td>${relation.fkName}</td>
                   <td>
                       <#list relation.relFields as field>
                           ${field.fieldName} -> ${field.relFieldName}<br/>
                       </#list>
                   </td>
            </tr>
            <#if rowClass == 'viewManyTR1'>
                <#assign rowClass = 'viewManyTR2'>
            <#else>
                <#assign rowClass = 'viewManyTR1'>
            </#if>
        </#list>
    </table>
<#else>
    <h3>${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}.</h3>
</#if>
