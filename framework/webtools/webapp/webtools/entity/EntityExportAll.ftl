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

<div class="head1">${uiLabelMap.WebtoolsExportFromDataSource}</div>
<div class="tabletext">
    ${uiLabelMap.WebtoolsMessage1}. 
    ${uiLabelMap.WebtoolsMessage2}.
    ${uiLabelMap.WebtoolsMessage3}.
</div>
<hr/>
    
<div class="head2">${uiLabelMap.WebtoolsResults}:</div>

<#if results?has_content>
    <#list results as result>
        <div class="tabletext">${result}</div>
    </#list>
</#if>

<hr/>

<div class="head2">${uiLabelMap.WebtoolsExport}:</div>
<form method="post" action="<@ofbizUrl>entityExportAll</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsOutputDirectory}: <input type="text" class="inputBox" size="60" name="outpath" value="${outpath?if_exists}"></div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}: <input type="text" size="6" value="${txTimeout?default('7200')}" name="txTimeout"/></div>
    <br/>
    <input type="submit" value="${uiLabelMap.WebtoolsExport}">
</form>
