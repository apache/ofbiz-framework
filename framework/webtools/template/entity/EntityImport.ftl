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

  <form class="basic-form" method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <table class="basic-table" cellspacing="0">
        <tbody>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsAbsoluteFileNameOrUrl}</label>
                </td>
                <td>
                    <input type="text" size="60" name="filename" value="${filename!}"/>
                </td>
            </tr>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsAbsoluteFTLFilename}</label>
                </td>
                <td>
                    <input type="text" size="40" name="fmfilename" value="${fmfilename!}"/>
                </td>
            </tr>
            <tr>
                <td class="label">
                </td>
                <td>
                    <label><input type="checkbox" name="isUrl" <#if isUrl??>checked="checked"</#if>/>${uiLabelMap.WebtoolsIsURL}</label>
                    <label><input type="checkbox" name="onlyInserts" <#if onlyInserts??>checked="checked"</#if>/>${uiLabelMap.WebtoolsOnlyInserts}</label>
                    <label><input type="checkbox" name="maintainTimeStamps" <#if keepStamps??>checked="checked"</#if>/>${uiLabelMap.WebtoolsMaintainTimestamps}</label>
                    <label><input type="checkbox" name="createDummyFks" <#if createDummyFks??>checked="checked"</#if>/>${uiLabelMap.WebtoolsCreateDummyFks}</label>
                    <label><input type="checkbox" name="checkDataOnly" <#if checkDataOnly??>checked="checked"</#if>/>${uiLabelMap.WebtoolsCheckDataOnly}</label>
                </td>
            </tr>
            <tr>
                 <td class="label">
                    <label>${uiLabelMap.WebtoolsTimeoutSeconds}</label>
                </td>
                <td>
                    <input type="text" size="6" value="${txTimeoutStr?default("7200")}" name="txTimeout"/>
                </td>
           </tr>
           <tr>
                <td class="label">
                </td>
                <td colspan="4">
                    <input type="submit" value="${uiLabelMap.WebtoolsImportFile}"/>
                </td>
           </tr>
        </tbody>
    </table>
  </form>
  <form class="basic-form" method="post" action="<@ofbizUrl>entityImport</@ofbizUrl>">
    <table class="basic-table" cellspacing="0">
        <tbody>
            <tr>
                <td class="label">
                    <label>${uiLabelMap.WebtoolsCompleteXMLDocument}</label>
                </td>
                <td>
                    <textarea rows="20" cols="85" name="fulltext">${fulltext?default("<entity-engine-xml>\n</entity-engine-xml>")}</textarea>
                </td>
            </tr>
            <tr>
            </tr>
            <tr>
                <td class="label">
                </td>
                <td colspan="4">
                    <input type="submit" value="${uiLabelMap.WebtoolsImportText}"/>
                </td>
            </tr>
        </tbody>
    </table>
  </form>
  <#if messages??>
      <hr />
      <h3>${uiLabelMap.WebtoolsResults}:</h3>
      <#list messages as message>
          <p>${message}</p>
      </#list>
  </#if>
