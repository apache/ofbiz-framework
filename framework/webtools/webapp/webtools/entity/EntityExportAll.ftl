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

<h1>${uiLabelMap.WebtoolsExportFromDataSource}</h1>
<br />
<p>
    ${uiLabelMap.WebtoolsMessage1}. 
    ${uiLabelMap.WebtoolsMessage2}.
    ${uiLabelMap.WebtoolsMessage3}.
</p>
<#if results?has_content>
    <hr/>
    <h2>${uiLabelMap.WebtoolsResults}:</h2>
    <#list results as result>
        <p>${result}</p>
    </#list>
</#if>

<hr/>

<h2>${uiLabelMap.WebtoolsExport}:</h2>
<form method="post" action="<@ofbizUrl>entityExportAll</@ofbizUrl>">
    ${uiLabelMap.WebtoolsOutputDirectory}: <input type="text" size="60" name="outpath" value="${outpath?if_exists}"><br />
    ${uiLabelMap.WebtoolsTimeoutSeconds}: <input type="text" size="6" value="${txTimeout?default('7200')}" name="txTimeout"/><br />
    <br />
    <input type="submit" value="${uiLabelMap.WebtoolsExport}">
</form>
