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
<div class="screenlet-body">
    <form method="post" action="<@ofbizUrl></@ofbizUrl>" name="ViewReferencesForm">
        <table class="basic-table" cellspacing="3">
            <tr>
                <td colspan="2"><b>${uiLabelMap.WebtoolsLabelManagerKey}</b> ${parameters.sourceKey?if_exists}</td>
            </tr>
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr class="header-row">
                <td>${uiLabelMap.WebtoolsLabelManagerRow}</td>
                <td>${uiLabelMap.WebtoolsLabelManagerFileName}</td>
                <td>${uiLabelMap.WebtoolsLabelManagerReferences}</td>
            </tr>
            <#if parameters.sourceKey?exists && parameters.sourceKey?has_content>
              <#assign rowNum = "2">
              <#assign rowNumber = 1>
              <#assign reference = references.get(parameters.sourceKey)?if_exists>
              <#if reference?exists &&  reference?has_content>
                <#assign entries = reference.entrySet()>
                <#list entries as entry>
                <tr>
                    <td>${rowNumber}</td>
                    <td><a href="<@ofbizUrl>ViewFile?fileName=${entry.getKey()}&sourceKey=${parameters.sourceKey?if_exists}</@ofbizUrl>">${entry.getKey()}</a></td>
                    <td>${entry.getValue()}</td>
                </tr>
                <#if rowNum == "2">
                  <#assign rowNum = "1">
                <#else>
                  <#assign rowNum = "2">
                </#if>
                <#assign rowNumber = rowNumber + 1>
                </#list>
              </#if>
            </#if>
        </table>
    </form>
</div>
