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

<div class="page-title"><span>${uiLabelMap.WebtoolsImportToDataSource}</span></div>
<p>${uiLabelMap.WebtoolsXMLImportInfo}</p>
<hr />
  <form method="post" action="<@ofbizUrl>entityImportReaders</@ofbizUrl>">
    Enter Readers (comma separated, no spaces; from entityengine.xml and ofbiz-component.xml files; common ones include seed,ext,demo):<br />
    <input type="text" size="60" name="readers" value="${readers?default("seed")}"/><br />
    <label><input type="checkbox" name="mostlyInserts" <#if mostlyInserts??>checked="checked"</#if> value="true"/>${uiLabelMap.WebtoolsMostlyInserts}</label><br />
    <label><input type="checkbox" name="maintainTimeStamps" <#if keepStamps??>checked="checked"</#if> value="true"/>${uiLabelMap.WebtoolsMaintainTimestamps}</label><br />
    <label><input type="checkbox" name="createDummyFks" <#if createDummyFks??>checked="checked"</#if> value="true"/>${uiLabelMap.WebtoolsCreateDummyFks}</label><br />
    <label><input type="checkbox" name="checkDataOnly" <#if checkDataOnly??>checked="checked"</#if> value="true"/>${uiLabelMap.WebtoolsCheckDataOnly}</label>R<br />
    ${uiLabelMap.WebtoolsTimeoutSeconds}:<input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/><br />
    <div class="button-bar"><input type="submit" value="${uiLabelMap.WebtoolsImport}"/></div>
  </form>
  <#if messages??>
      <hr />
      <h3>${uiLabelMap.WebtoolsResults}:</h3>
      <#list messages as message>
          <p>${message}</p>
      </#list>
  </#if>
