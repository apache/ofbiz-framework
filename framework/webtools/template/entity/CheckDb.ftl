

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
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <td>
               <input type="hidden" name="option" value="checkupdatetables"/>
            </td>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}: </label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
            </td>
         </tr>
         <tr>
            <td class="label">
            </td>
            <td>
               <label>&nbsp;<input type="checkbox" name="checkPks" value="true" checked="checked"/>&nbsp;${uiLabelMap.WebtoolsPks}</label>
               <label>&nbsp;<input type="checkbox" name="checkFks" value="true"/>&nbsp;${uiLabelMap.WebtoolsFks}</label>
               <label>&nbsp;<input type="checkbox" name="checkFkIdx" value="true"/>&nbsp;${uiLabelMap.WebtoolsFkIdx}</label>
               <label>&nbsp;<input type="checkbox" name="addMissing" value="true"/>&nbsp;${uiLabelMap.WebtoolsAddMissing}</label>
               <label>&nbsp;<input type="checkbox" name="repair" value="true"/>&nbsp;${uiLabelMap.WebtoolsRepairColumnSizes}</label>
            </td>
         </tr>
         <tr>
            <td class="label">
            </td>
            <td>
               <input type="submit" value="${uiLabelMap.WebtoolsCheckUpdateDatabase}"/>
            </td>
         </tr>
      </tbody>
   </table>
</form>
<p>${uiLabelMap.WebtoolsNoteUseAtYourOwnRisk}</p>
<script type="application/javascript">
   function enableTablesRemove() {
       document.forms["TablesRemoveForm"].elements["TablesRemoveButton"].disabled=false;
   }
</script>
<h3>${uiLabelMap.WebtoolsRemoveAllTables}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>" name="TablesRemoveForm">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removetables"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
               <input type="submit" value="${uiLabelMap.CommonRemove}" name="TablesRemoveButton" disabled="disabled"/>
               <input type="button" value="${uiLabelMap.WebtoolsEnable}" onclick="enableTablesRemove();"/>
            </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>" name="TableRemoveForm">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removetable"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="20"/>
            </td>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsEntityName}:</label>
            </td>
            <td>
               <input type="text" name="entityName" value="${entityName}" size="20"/>
               <input type="submit" value="${uiLabelMap.CommonRemove}" name="TablesRemoveButton"/>
            </td>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsCreateRemoveAllPrimaryKeys}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="createpks"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
               <input type="submit" value="${uiLabelMap.CommonCreate}"/>
            </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removepks"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
               <input type="submit" value="${uiLabelMap.CommonRemove}"/>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsCreateRemovePrimaryKey}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="createpk"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
            </td>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsEntityName}:</label>
         </td>
         <td>
            <input type="text" name="entityName" value="${entityName}" size="20"/>
            <input type="submit" value="${uiLabelMap.CommonCreate}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <td>
               <input type="hidden" name="option" value="removepk"/>
            </td>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="20"/>
            </td>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsEntityName}:</label>
            </td>
            <td>
               <input type="text" name="entityName" value="${entityName}" size="20"/>
               <input type="submit" value="${uiLabelMap.CommonRemove}"/>
            </td>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsCreateRemoveAllDeclaredIndices}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="createidx"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonCreate}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removeidx"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonRemove}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsCreateRemoveAllForeignKeyIndices}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="createfkidxs"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonCreate}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removefkidxs"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonRemove}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsCreateRemoveAllForeignKeys}</h3>
<p>${uiLabelMap.WebtoolsNoteForeighKeysMayAlsoBeCreated}</p>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="createfks"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonCreate}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="removefks"/>
         </tr>
         <td class="label">
            <label>${uiLabelMap.WebtoolsGroupName}:</label>
         </td>
         <td>
            <input type="text" name="groupName" value="${groupName}" size="40"/>
            <input type="submit" value="${uiLabelMap.CommonRemove}"/>
         </td>
         </tr>
      </tbody>
   </table>
</form>
<h3>${uiLabelMap.WebtoolsUpdateCharacterSetAndCollate}</h3>
<form class="basic-form" method="post" action="<@ofbizUrl>${checkDbURL}</@ofbizUrl>">
   <table class="basic-table" cellspacing="0">
      <tbody>
         <tr>
            <input type="hidden" name="option" value="updateCharsetCollate"/>
         </tr>
         <tr>
            <td class="label">
               <label>${uiLabelMap.WebtoolsGroupName}:</label>
            </td>
            <td>
               <input type="text" name="groupName" value="${groupName}" size="40"/>
               <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
               </td
         </tr>
      </tbody>
   </table>
</form>
<#if miters?has_content>
<hr />
<ul>
   <#list miters as miter>
   <li>${miter}</li>
   </#list>
</ul>
</#if>

