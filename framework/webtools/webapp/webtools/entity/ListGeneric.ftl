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
        <#macro tableNav>
            <div class="nav-pager">
                <ul>
                    <#if (viewIndex > 0)>
                        <li class="nav-first"><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexFirst}&amp;searchOptions_collapsed=${(parameters.searchOptions_collapsed)?default("false")}</@ofbizUrl>'>${uiLabelMap.CommonFirst}</a></li>
                        <li class="nav-previous"><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexPrevious}&amp;searchOptions_collapsed=${(parameters.searchOptions_collapsed)?default("false")}</@ofbizUrl>'>${uiLabelMap.CommonPrevious}</a></li>
                    <#else>
                        <li class="nav-first-disabled"><span>${uiLabelMap.CommonFirst}<span></li>
                        <li class="nav-previous-disabled"><span>${uiLabelMap.CommonPrevious}<span></li>
                    </#if>
                    <#if (arraySize > 0)>
                        <li>${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${arraySize}</li>
                    </#if>
                    <#if (arraySize > highIndex)>
                        <li class="nav-next"><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexNext}&amp;searchOptions_collapsed=${(parameters.searchOptions_collapsed)?default("false")}</@ofbizUrl>'>${uiLabelMap.CommonNext}</a></li>
                        <li class="nav-last"><a href='<@ofbizUrl>FindGeneric?${curFindString}&amp;VIEW_SIZE=${viewSize}&amp;VIEW_INDEX=${viewIndexLast}&amp;searchOptions_collapsed=${(parameters.searchOptions_collapsed)?default("false")}</@ofbizUrl>'>${uiLabelMap.CommonLast}</a></li>
                    <#else>
                        <li class="nav-next-disabled"><span>${uiLabelMap.CommonNext}<span></li>
                        <li class="nav-last-disabled"><span>${uiLabelMap.CommonLast}<span></li>
                    </#if>
                </ul>
                <br class="clear"/>
            </div>
        </#macro>
        <#if (arraySize > 0)>
            <@tableNav/>
        </#if>
          <table class="basic-table hover-bar" cellspacing="0">
            <tr class="header-row-2">
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
                            <a href='<@ofbizUrl>UpdateGeneric?${record.findString}&amp;UPDATE_MODE=DELETE</@ofbizUrl>'>${uiLabelMap.CommonDelete}</a>
                        </#if>
                        </td>
                        <#list fieldList as field>
                            <td>${record.fields.get(field.name)?if_exists?string}</td>
                        </#list>
                    </tr>
                    <#assign alt_row = !alt_row>
                </#list>
            <#else>
                <tr>
                    <td colspan="${columnCount}">
                        <h2>${uiLabelMap.WebtoolsNoRecordsFound} ${entityName}.</h2>
                    </td>
                </tr>
            </#if>
        </table>
        <#if (arraySize > 0)>
            <@tableNav/>
        </#if>
