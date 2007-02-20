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

<div class="head1">${uiLabelMap.WebtoolsImportToDataSource}</div>
<div>${uiLabelMap.WebtoolsMessage5}.</div>
<hr/>
  <div class="head2">${uiLabelMap.WebtoolsImport}:</div>

  <form method="post" action="<@ofbizUrl>entityImportReaders</@ofbizUrl>">
    <div class="tabletext">Enter Readers (comma separated, no spaces; from entityengine.xml and ofbiz-component.xml files; common ones include seed,ext,demo):</div>
    <div><input type="text" class="inputBox" size="60" name="readers" value="${readers?default("seed")}"/></div>
    <div class="tabletext"><input type="checkbox" name="mostlyInserts" <#if mostlyInserts?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMostlyInserts}</div>
    <div class="tabletext"><input type="checkbox" name="maintainTimeStamps" <#if keepStamps?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMaintainTimestamps}</div>
    <div class="tabletext"><input type="checkbox" name="createDummyFks" <#if createDummyFks?exists>"checked"</#if>/>${uiLabelMap.WebtoolsCreateDummyFks}</div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}:<input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/></div>
    <div><input type="submit" value="${uiLabelMap.WebtoolsImport}"/></div>
  </form>
  <hr/>
  <#if messages?exists>
      <h3>${uiLabelMap.WebtoolsResults}:</h3>
      <#list messages as message>
          <div class="tabletext">${message}</div>
      </#list>
  </#if>
