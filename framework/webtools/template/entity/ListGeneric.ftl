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
        <#if (arraySize > 0)>
            <#assign commonUrl="FindGeneric?${curFindString}&amp;searchOptions_collapsed=${(parameters.searchOptions_collapsed)?default(\"false\")}&amp;"/>
            <@htmlTemplate.nextPrev commonUrl=commonUrl listSize=arraySize viewSize=viewSize viewIndex=viewIndex highIndex=highIndex commonDisplaying=commonDisplaying/>
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
                            <a onclick="return confirm ('${uiLabelMap.WebtoolsConfirmDelete}')" href='<@ofbizUrl>UpdateGeneric?${record.findString}&amp;UPDATE_MODE=DELETE</@ofbizUrl>'>${uiLabelMap.CommonDelete}</a>
                        </#if>
                        </td>
                        <#list fieldList as field>
                            <td>${Static["org.apache.ofbiz.base.util.UtilFormatOut"].makeString(record.fields.get(field.name))}</td>
                            <#-- <td>${record.fields.get(field.name)!?string}</td> -->
                        </#list>
                    </tr>
                    <#assign alt_row = !alt_row>
                </#list>
            <#else>
                <tr>
                    <td colspan="${columnCount}">
                        <h2>${uiLabelMap.WebtoolsNoEntityRecordsFound} ${entityName}.</h2>
                    </td>
                </tr>
            </#if>
        </table>
        <#if (arraySize > 0)>
            <@htmlTemplate.nextPrev commonUrl=commonUrl listSize=arraySize viewSize=viewSize viewIndex=viewIndex  highIndex=highIndex />
        </#if>
