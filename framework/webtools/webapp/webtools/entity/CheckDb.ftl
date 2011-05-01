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
    <h3>${uiLabelMap.WebtoolsCheckUpdateDatabase}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="checkupdatetables"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        &nbsp;<input type="checkbox" name="checkPks" value="true" checked="checked"/>&nbsp;${uiLabelMap.WebtoolsPks}
        &nbsp;<input type="checkbox" name="checkFks" value="true"/>&nbsp;${uiLabelMap.WebtoolsFks}
        &nbsp;<input type="checkbox" name="checkFkIdx" value="true"/>&nbsp;${uiLabelMap.WebtoolsFkIdx}
        &nbsp;<input type="checkbox" name="addMissing" value="true"/>&nbsp;${uiLabelMap.WebtoolsAddMissing}
        &nbsp;<input type="checkbox" name="repair" value="true"/>&nbsp;${uiLabelMap.WebtoolsRepairColumnSizes}
        <input type="submit" value="${uiLabelMap.WebtoolsCheckUpdateDatabase}"/>
    </form>
    <p>${uiLabelMap.WebtoolsNoteUseAtYourOwnRisk}</p>
    <script language="JavaScript" type="text/javascript">
         function enableTablesRemove() {
             document.forms["TablesRemoveForm"].elements["TablesRemoveButton"].disabled=false;
         }
    </script>
    <h3>${uiLabelMap.WebtoolsRemoveAllTables}</h3>
    <form method="post" action="${encodeURLCheckDb}" name="TablesRemoveForm">
        <input type="hidden" name="option" value="removetables"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}" name="TablesRemoveButton" disabled="disabled"/>
        <input type="button" value="${uiLabelMap.WebtoolsEnable}" onclick="enableTablesRemove();"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}" name="TableRemoveForm">
        <input type="hidden" name="option" value="removetable"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="20"/>
        ${uiLabelMap.WebtoolsEntityName}: <input type="text" name="entityName" value="${entityName}" size="20"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}" name="TablesRemoveButton"/>
    </form>
    <h3>${uiLabelMap.WebtoolsCreateRemoveAllPrimaryKeys}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="createpks"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonCreate}"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="removepks"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}"/>
    </form>
    <h3>${uiLabelMap.WebtoolsCreateRemovePrimaryKey}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="createpk"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="20"/>
        ${uiLabelMap.WebtoolsEntityName}: <input type="text" name="entityName" value="${entityName}" size="20"/>
        <input type="submit" value="${uiLabelMap.CommonCreate}"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="removepk"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="20"/>
        ${uiLabelMap.WebtoolsEntityName}: <input type="text" name="entityName" value="${entityName}" size="20"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}"/>
    </form>
    <h3>${uiLabelMap.WebtoolsCreateRemoveAllDeclaredIndices}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="createidx"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonCreate}"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="removeidx"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}"/>
    </form>
    <h3>${uiLabelMap.WebtoolsCreateRemoveAllForeignKeyIndices}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="createfkidxs"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonCreate}"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="removefkidxs"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}"/>
    </form>
    <h3>${uiLabelMap.WebtoolsCreateRemoveAllForeignKeys}</h3>
    <p>${uiLabelMap.WebtoolsNoteForeighKeysMayAlsoBeCreated}</p>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="createfks"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonCreate}"/>
    </form>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="removefks"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonRemove}"/>
    </form>
    <h3>${uiLabelMap.WebtoolsUpdateCharacterSetAndCollate}</h3>
    <form method="post" action="${encodeURLCheckDb}">
        <input type="hidden" name="option" value="updateCharsetCollate"/>
        ${uiLabelMap.WebtoolsGroupName}: <input type="text" name="groupName" value="${groupName}" size="40"/>
        <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
    </form>
<#if miters?has_content>
    <hr />
    <ul>
        <#list miters as miter>
            <li>${miter}</li>
        </#list>
    </ul>
</#if>
