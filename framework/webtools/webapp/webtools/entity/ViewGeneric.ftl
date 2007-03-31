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

<#assign enableEdit = parameters.enableEdit?default("false")>

<h1>${uiLabelMap.WebtoolsViewValue}</h1>
<br />
<h2>${uiLabelMap.WebtoolsForEntity}: ${entityName}</h2>
<h2>${uiLabelMap.WebtoolsWithPk}: ${findByPk}</h2>
<br />

<div class="button-bar">
  <a href='<@ofbizUrl>FindGeneric?entityName=${entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>' class="smallSubmit">${uiLabelMap.WebtoolsBackToFindScreen}</a>
  <#if enableEdit = "false">
    <#if hasCreatePermission>
      <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}&enableEdit=true</@ofbizUrl>' class="smallSubmit">${uiLabelMap.CommonCreateNew}</a>
      <a href=<@ofbizUrl>ViewGeneric?${curFindString}&enableEdit=true</@ofbizUrl> class="smallSubmit">${uiLabelMap.CommonEdit}</a>
    </#if>    
    <#if value?has_content>
      <#if hasDeletePermission>
        <a href='<@ofbizUrl>UpdateGeneric?UPDATE_MODE=DELETE&${curFindString}</@ofbizUrl>' class="smallSubmit">${uiLabelMap.WebtoolsDeleteThisValue}</a>
      </#if>
    </#if>    
  </#if>    
</div>
<br />

<#if value?has_content>
  <form name="relationForm" action='<@ofbizUrl>ViewGeneric?entityName=${entityName}&contactMechId=${parameters.contactMechId?if_exists}</@ofbizUrl>' method="POST">
    <p>${uiLabelMap.CommonView}:</p>
    <select name="viewRelated" onchange='javascript:document.relationForm.submit();'>
      <option value="void">${entityName}</option>
      <#list relations as relation>
        <option value="${relation.tab}"<#if relation.tab = parameters.viewRelated?default("void")> selected="selected"</#if>>${relation.title}${relation.relEntityName} (${relation.type})</option>
      </#list>
    </select>
  </form>
  <br />
</#if> 

<div class="screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.WebtoolsEntityCurrentValue}</h3>
  </div>
  <#if value?has_content>  
    <#assign alt_row = false>
    <table class="basic-table" cellspacing="0">
      <#list fields as field>                        
        <tr<#if alt_row> class="alternate-row"</#if>>
          <td class="label">${field.name}</td>
          <td>${field.value}</td>
        </tr>
        <#assign alt_row = !alt_row>
      </#list>
    </table>
  <#else>
    ${uiLabelMap.WebtoolsSpecifiedEntity1} ${entityName} ${uiLabelMap.WebtoolsSpecifiedEntity2}.
  </#if>
</div>

<#if enableEdit = "true">
  <#if hasUpdatePermission || hasCreatePermission>
    <br />
    <#assign alt_row = false>
    <div id="area2" class="screenlet">
      <div class="screenlet-title-bar">
        <h3>${uiLabelMap.WebtoolsEntityEditValue}</h3>
      </div>
      <#if pkNotFound>
        <p>${uiLabelMap.WebtoolsEntityName} ${entityName} ${uiLabelMap.WebtoolsWithPk} ${findByPk} ${uiLabelMap.WebtoolsSpecifiedEntity2}.</p>
      </#if>
      <form action='<@ofbizUrl>UpdateGeneric?entityName=${entityName}</@ofbizUrl>' method="POST" name="updateForm">
        <#assign showFields = true>
        <#assign alt_row = false>
        <table class="basic-table" cellspacing="0">
          <#if value?has_content>             
            <#if hasUpdatePermission>
              <#if newFieldPkList?has_content>
                <input type="hidden" name="UPDATE_MODE" value="UPDATE"/>
                <#list newFieldPkList as field> 
                  <tr<#if alt_row> class="alternate-row"</#if>>
                    <td class="label">${field.name}</td>
                    <td>
                      <input type="hidden" name="${field.name}" value="${field.value}"/>
                      ${field.value}
                    </td>
                  </tr>
                  <#assign alt_row = !alt_row>
                </#list>
              </#if>
            <#else>
              ${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}
              <#assign showFields = false>
            </#if>                            
          <#else>
            <#if hasCreatePermission>
              <#if newFieldPkList?has_content>
                <p>${uiLabelMap.WebtoolsMessage15} ${entityName} ${uiLabelMap.WebtoolsMessage16}.</p>
                <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
                <#list newFieldPkList as field> 
                  <tr<#if alt_row> class="alternate-row"</#if>>
                    <td class="label">${field.name}</td>
                    <td>
                      <#if field.fieldType == 'DateTime'>                                
                        DateTime(YYYY-MM-DD HH:mm:SS.sss):<input type="text" name="${field.name}" size="24" value="${field.value}">
                        <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                      <#elseif field.fieldType == 'Date'>                                
                        Date(YYYY-MM-DD):<input type="text" name="${field.name}" size="11" value="${field.value}">
                        <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                      <#elseif field.fieldType == 'Time'>                                
                        Time(HH:mm:SS.sss):<input type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}">
                      <#elseif field.fieldType == 'Integer'>                                
                        <input type="text" size="20" name="${field.name}" value="${field.value}">
                      <#elseif field.fieldType == 'Long'>                                
                        <input type="text" size="20" name="${field.name}" value="${field.value}"> 
                      <#elseif field.fieldType == 'Double'>                                
                        <input type="text" size="20" name="${field.name}" value="${field.value}"> 
                      <#elseif field.fieldType == 'Float'>                                
                        <input type="text" size="20" name="${field.name}" value="${field.value}">
                      <#elseif field.fieldType == 'StringOneRow'>                                
                        <input type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                      <#elseif field.fieldType == 'String'>                                
                        <input type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                      <#elseif field.fieldType == 'Textarea'>                                
                        <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                      </#if>
                    </td>
                  </tr>
                  <#assign alt_row = !alt_row>
                </#list>
              </#if>
            <#else>
              <p>${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}</p>
              <#assign showFields = false>
            </#if>            
          </#if>
          <#if showFields>
            <#if newFieldNoPkList?has_content>
              <#assign alt_row = false>
              <#list newFieldNoPkList as field> 
                <tr<#if alt_row> class="alternate-row"</#if>>
                  <td class="label">${field.name}</td>
                  <td>
                    <#if field.fieldType == 'DateTime'>                                
                      DateTime(YYYY-MM-DD HH:mm:SS.sss):<input type="text" name="${field.name}" size="24" value="${field.value}">
                      <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                    <#elseif field.fieldType == 'Date'>                                
                      Date(YYYY-MM-DD):<input type="text" name="${field.name}" size="11" value="${field.value}">
                      <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                    <#elseif field.fieldType == 'Time'>                                
                      Time(HH:mm:SS.sss):<input type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}">
                    <#elseif field.fieldType == 'Integer'>                                
                      <input type="text" size="20" name="${field.name}" value="${field.value}">
                    <#elseif field.fieldType == 'Long'>                                
                      <input type="text" size="20" name="${field.name}" value="${field.value}"> 
                    <#elseif field.fieldType == 'Double'>                                
                      <input type="text" size="20" name="${field.name}" value="${field.value}"> 
                    <#elseif field.fieldType == 'Float'>                                
                      <input type="text" size="20" name="${field.name}" value="${field.value}">
                    <#elseif field.fieldType == 'StringOneRow'>                                
                      <input type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                    <#elseif field.fieldType == 'String'>                                
                      <input type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                    <#elseif field.fieldType == 'Textarea'>                                
                      <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                    </#if>
                  </td>
                </tr>
                <#assign alt_row = !alt_row>
              </#list>
              <#if value?has_content>
                <#assign button = "${uiLabelMap.CommonUpdate}">
              <#else>
                <#assign button = "${uiLabelMap.CommonCreate}">
              </#if>
              <tr<#if alt_row> class="alternate-row"</#if>>
                <td>&nbsp;</td>
                <td>
                  <input type="submit" name="Update" value="${button}">
                  <a href=<@ofbizUrl>ViewGeneric?${curFindString}</@ofbizUrl> class="smallSubmit">${uiLabelMap.CommonCancel}</a>
                </td>
              </tr>
            </#if>
          </#if>
        </table>
      </form>
    </div>
  </#if>
</#if>

<#if relationFieldList?has_content>
  <#assign viewRelated = parameters.viewRelated?default("void")>
  <#list relationFieldList as relation>
    <#assign tabName = "tab" + relation.relIndex>
    <#if tabName = viewRelated>
      <br />
      <div class="screenlet">
        <div class="screenlet-title-bar">
          <ul>
            <h3>${relation.title} ${uiLabelMap.WebtoolsRelatedEntity}: ${relation.relatedTable}</h3>
            <#if relation.valueRelated?has_content>
              <li><a href='<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>'>${uiLabelMap.CommonView}</a></li>
            <#else>
              <#if hasAllCreate || relCreate>
                <li><a href='<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}&enableEdit=true</@ofbizUrl>'>${uiLabelMap.CommonCreate}</a></li>
              </#if>
            </#if>    
          </ul>
          <br class="clear"/>
        </div>
        <#if relation.valueRelated?has_content>
          <table class="basic-table" cellspacing="0">
            <#assign alt_row = false>
            <tr<#if alt_row> class="alternate-row"</#if>>
              <td class="label">${uiLabelMap.WebtoolsPk}</td>
              <td>${relation.valueRelatedPk}</td>
            </tr>
            <#list relation.relatedFieldsList as relatedField>
              <tr<#if alt_row> class="alternate-row"</#if>>
                <td class="label">${relatedField.name}</td>
                <td>${relatedField.value}</td>
              </tr>  
              <#assign alt_row = !alt_row>
            </#list>
          </table>
        <#else>
          ${uiLabelMap.WebtoolsSpecifiedEntity1} ${relation.relatedTable} ${uiLabelMap.WebtoolsSpecifiedEntity2}.
        </#if> 
      </div>    
    </#if>
  </#list>
</#if>
