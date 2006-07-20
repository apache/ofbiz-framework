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

<div class="head1">${uiLabelMap.WebtoolsImportToDataSource}</div>
<div>${uiLabelMap.WebtoolsMessage5}.</div>
<hr>
  <div class="head2">${uiLabelMap.WebtoolsImport}:</div>

  <form method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsMessage6}:</div>
    <div><input type="text" class="inputBox" size="60" name="fmfilename" value="${fmfilename?if_exists}"/></div>
    <div class="tabletext">${uiLabelMap.WebtoolsMessage7}:</div>
    <div><input type="text" class="inputBox" size="60" name="filename" value="${filename?if_exists}"/></div>
    <div class="tabletext"><input type="checkbox" name="isUrl" <#if isUrl?exists>"checked"</#if>/>${uiLabelMap.WebtoolsIsURL}</div>
    <div class="tabletext"><input type="checkbox" name="mostlyInserts" <#if mostlyInserts?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMostlyInserts}</div>
    <div class="tabletext"><input type="checkbox" name="maintainTimeStamps" <#if keepStamps?exists>"checked"</#if>/>${uiLabelMap.WebtoolsMaintainTimestamps}</div>
    <div class="tabletext"><input type="checkbox" name="createDummyFks" <#if createDummyFks?exists>"checked"</#if>/>${uiLabelMap.WebtoolsCreateDummyFks}</div>
    <div class="tabletext">${uiLabelMap.WebtoolsTimeoutSeconds}:<input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/></div>
    <div><input type="submit" value="${uiLabelMap.WebtoolsImportFile}"/></div>
  </form>
  <form method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <div class="tabletext">${uiLabelMap.WebtoolsMessage4}:</div>
    <textarea class="textAreaBox" rows="20" cols="85" name="fulltext">${fulltext?if_exists}</textarea>
    <br/><input type="submit" value="${uiLabelMap.WebtoolsImportText}"/>
  </form>
  <hr>
  <#if messages?exists>
      <h3>${uiLabelMap.WebtoolsResults}:</h3>
      <#list messages as message>
          <div class="tabletext">${message}</div>
      </#list>
  </#if>
