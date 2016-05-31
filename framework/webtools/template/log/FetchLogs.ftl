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

<div class="page-title"><span>${uiLabelMap.WebtoolsFetchLogs}</span></div>
<div>
  <form name="fetchLogs" method="post" action="<@ofbizUrl>FetchLogs</@ofbizUrl>">
    <fieldset>
      <span>
        <label for="logFileName">${uiLabelMap.CommonSelectFile}: </label>
        <select name="logFileName">
          <option value="">${uiLabelMap.CommonSelectFile}</option>
          <#if parameters.logFileName?has_content>
            <option selected value="${parameters.logFileName}">${parameters.logFileName}</option>
          </#if>
          <#if listLogFileNames?has_content>
            <#list listLogFileNames as logFileName>
              <option value="${logFileName}">${logFileName}</option>
            </#list>
          </#if>
        </select>
      </span>
      <span>
        <label for="searchString">${uiLabelMap.WebtoolsSearchString}: </label>
        <input name="searchString" class="required" type="text" value="${parameters.searchString!}" />
      </span>
    </fieldset>
    <input type="submit" value="${uiLabelMap.CommonSubmit}" />
  </form>
</div>
<#if logLines?has_content>
  <#list logLines as logLine>
    <div class="${logLine.type}">${logLine.line}</div>
  </#list>
</#if>