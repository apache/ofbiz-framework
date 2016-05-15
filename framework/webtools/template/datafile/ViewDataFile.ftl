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
    <p>${uiLabelMap.WebtoolsDataFileMessage1}.</p>
    <br />
    <#if security.hasPermission("DATAFILE_MAINT", session)>
      <form method="post" action="<@ofbizUrl>viewdatafile</@ofbizUrl>">
        <table class="basic-table" cellspacing="0">
          <tr>
            <td class="label">${uiLabelMap.WebtoolsDataDefinitionFileName}</td>
            <td><input name="DEFINITION_LOCATION" type="text" size="60" value="${parameters.DEFINITION_LOCATION!}" /></td>
            <td><span class="label">${uiLabelMap.WebtoolsDataIsUrl}</span><input type="checkbox" name="DEFINITION_IS_URL"<#if parameters.DEFINITION_IS_URL?has_content> checked="checked"</#if> /></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsDataDefinitionName}</td>
            <td>
              <#if definitionNames?has_content>
                <select name="DEFINITION_NAME">
                  <option value=""></option>
                  <#list definitionNames as oneDefinitionName>
                    boolean isSelected = definitionName?? && definitionName.equals(oneDefinitionName);
                    <option value="${oneDefinitionName}" <#if parameters.DEFINITION_NAME?? && parameters.DEFINITION_NAME == oneDefinitionName> selected="selected" </#if>>${oneDefinitionName}</option>
                  </#list>
                </select>
              <#else>
                <input name="DEFINITION_NAME" type="text" size="30" value="${definitionName!}" />
              </#if>
            </td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsDataFileName}</td>
            <td><input name="DATAFILE_LOCATION" type="text" size="60" value="${parameters.DATAFILE_LOCATION!}" /></td>
            <td><span class="label">${uiLabelMap.WebtoolsDataIsUrl}</span><input type="checkbox" name="DATAFILE_IS_URL"<#if parameters.DATAFILE_IS_URL?has_content> checked="checked"</#if> /></td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsDataSaveToFile}</td>
            <td><input name="DATAFILE_SAVE" type="text" size="60" value="${parameters.DATAFILE_SAVE!}"/></td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td class="label">${uiLabelMap.WebtoolsDataSaveToXml}</td>
            <td><input name="ENTITYXML_FILE_SAVE" type="text" size="60" value="${parameters.ENTITYXML_FILE_SAVE!}" /></td>
            <td>&nbsp;</td>
          </tr>
          <tr>
            <td>&nbsp;</td>
            <td><input type="submit" value="${uiLabelMap.CommonRun}" /></td>
            <td>&nbsp;</td>
          </tr>
        </table>
      </form>

      <#if messages?has_content>
        <hr />
        <h1>${uiLabelMap.CommonFollowingOccurred}:</h1>
        <div>
          <#list messages as message>
            <p>${message}</p>
          </#list>
        </div>
      </#if>

    <#macro displayrecords records>
        <#assign lastRecordName = null>
        <#list records as record>
          <#assign modelRecord = record.getModelRecord()>
          <#-- if record is different than the last displayed, make a new table and header row -->
          <#if !modelRecord.name.equals(lastRecordName)>
            <#if lastRecordName??>
              </table><br />
            </#if>
            <table class="basic-table hover-bar" cellspacing="0">
              <tr>
                <td><b>Record: ${modelRecord.name}</b></td>
                <#if (modelRecord.parentName)?has_content>
                  <td><b>Parent: ${modelRecord.parentName}</b></td>
                </#if>
                 <td>${modelRecord.description}</td>
               </tr>
            </table>
            <table class="dark-grid" cellspacing='0'>
              <tr>
                <#list modelRecord.fields as modelField>
                  <td><b>${modelField.name}</b></td>
                </#list>
              </tr>
            <#assign lastRecordName = modelRecord.name>
          </#if>

          <tr>
            <#list modelRecord.fields as modelField>
              <#assign value = record.get(modelField.name)>
              <#if value?has_content>
                <td>${value}</td>
              <#else>
                <td>${modelField.defaultValue}</td>
              </#if>
            </#list>
          </tr>
          <#if (record.getChildRecords())?has_content>
            <@displayrecords records = record.getChildRecords()/>
          </#if>
        </#list>
        </table>
    </#macro>

      <#if dataFile?has_content && modelDataFile?has_content && (!parameters.ENTITYXML_FILE_SAVE?has_content || parameters.ENTITYXML_FILE_SAVE.length() == 0) && (parameters.DATAFILE_SAVE == null || parameters.DATAFILE_SAVE.length() == 0)>
        <hr />
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>Name</td>
            <td>Type-Code</td>
            <td>Sender</td>
            <td>Receiver</td>
            <td>Record Length</td>
            <td>Separator Style</td>
          </tr>
          <tr>
            <td>${modelDataFile.name}</td>
            <td>${modelDataFile.typeCode}</td>
            <td>${modelDataFile.sender}</td>
            <td>${modelDataFile.receiver}</td>
            <td>${modelDataFile.recordLength}</td>
            <td>${modelDataFile.separatorStyle}</td>
          </tr>
          <tr>
            <td class="label">Description</td>
            <td colspan="">${modelDataFile.description}</td>
          </tr>
        </table>
        <br />
        <@displayrecords records = dataFile.getRecords()/>
      </#if>
    <#else>
      <h3>You do not have permission to use this page (DATAFILE_MAINT needed)</h3>
    </#if>
