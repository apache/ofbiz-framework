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
<STYLE>
  .topouter { overflow: hidden; border-style: none; height: 900px;}
  .topcontainer { POSITION: absolute; VISIBILITY: visible; width: 90%; border-style: none; }
  .topcontainerhidden { POSITION: absolute; VISIBILITY: hidden; }
</STYLE>
<script language="JavaScript" type="text/javascript">  
    var numTabs=${relSize};
    function ShowTab(lname) {
        for(inc=1; inc <= numTabs; inc++) {
            document.getElementById('area' + inc).className = (lname == 'tab' + inc) ? 'topcontainer' : 'topcontainerhidden';
        }
    }
</script>
<div class="topouter">
    <div class="head1">${uiLabelMap.WebtoolsViewValue}</div>
    <div class="head2">${uiLabelMap.WebtoolsForEntity}: ${entityName}</div>
    <div class="head2">${uiLabelMap.WebtoolsWithPk}: ${findByPk}</div>
    <div>&nbsp;</div>
    <div>
        <a href='<@ofbizUrl>FindGeneric?entityName=${entityName}&find=true&VIEW_SIZE=50&VIEW_INDEX=0</@ofbizUrl>' class="buttontext">${uiLabelMap.WebtoolsBackToFindScreen}</a>
    </div>
    <div>
        <#if hasCreatePermission>
            <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonCreateNew}</a>
            <a href='javascript:ShowTab("tab2")' class="buttontext">${uiLabelMap.CommonEdit}</a>
        </#if>    
        <#if value?has_content>
            <#if hasDeletePermission>
                <a href='<@ofbizUrl>UpdateGeneric?UPDATE_MODE=DELETE&${curFindString}</@ofbizUrl>' class="buttontext">${uiLabelMap.WebtoolsDeleteThisValue}</a>
            </#if>
        </#if>    
    </div>
    <div>&nbsp;</div>
    <#if value?has_content>
        <form name="relationForm">
            <table cellpadding='0' cellspacing='0'>
                <tr>
                    <td class="tableheadtext">${uiLabelMap.CommonView}:</td>
                </tr>
                <tr>
                    <td>
                        <select name="relations" onchange='javascript:ShowTab(this.options[this.selectedIndex].value)' class="selectBox">
                        <option value="tab1"><b>${entityName}</b></option>
                        <#list relations as relation>
                        <option value="${relation.tab}">${relation.title}${relation.relEntityName} (${relation.type})</option>
                        </#list>
                        </select>
                    </td>
                </tr>
            </table>
        </form>
    </#if> 
    <#assign rowClass = "viewOneTR1">
    <div id='area1' class='topcontainer' width="1%">
        <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
        <#if value?has_content>  
            <#assign rowClass = 'viewManyTR1'>
            <#list fields as field>                        
                <tr class="${rowClass}">
                    <td valign="top"><div class="tabletext"><b>${field.name}</b></div></td>
                    <td valign="top">
                        <div class="tabletext">
                        ${field.value}
                        &nbsp;
                        </div>
                    </td>
                </tr>
                <#if rowClass == 'viewManyTR1'>
                    <#assign rowClass = 'viewManyTR2'>
                <#else>
                    <#assign rowClass = 'viewManyTR1'>
                </#if>
            </#list>
        <#else>
            <tr class="${rowClass}">
              <td>
                <h3>${uiLabelMap.WebtoolsSpecifiedEntity1} ${entityName} ${uiLabelMap.WebtoolsSpecifiedEntity2}.</h3>
              </td>
            </tr>
        </#if>
        </table>
    </div>
    <#if hasUpdatePermission || hasCreatePermission>
        <div id='area2' class='topcontainerhidden' width="1%">                
        <#if pkNotFound>
            <div class="tabletext">${uiLabelMap.WebtoolsEntityName} ${entityName} ${uiLabelMap.WebtoolsWithPk} ${findByPk} ${uiLabelMap.WebtoolsSpecifiedEntity2}.</div>
        </#if>
        <form action='<@ofbizUrl>UpdateGeneric?entityName=${entityName}</@ofbizUrl>' method="POST" name="updateForm">
            <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
            <#assign showFields = true>
            <#assign rowClass = 'viewManyTR1'>
            <#if value?has_content>             
                <#if hasUpdatePermission>
                    <#if newFieldPkList?has_content>
                        <input type="hidden" name="UPDATE_MODE" value="UPDATE"/>
                        <#assign rowClass = 'viewManyTR1'>
                        <#list newFieldPkList as field> 
                            <tr class="${rowClass}">
                                <td valign="top"><div class="tabletext"><b>${field.name}</b></div></td>
                                <td valign="top">
                                    <div class="tabletext">                                    
                                        <input type="hidden" name="${field.name}" value="${field.value}">
                                        ${field.value}
                                        &nbsp;
                                    </div>
                                </td>
                            </tr>
                            <#if rowClass == 'viewManyTR1'>
                                <#assign rowClass = 'viewManyTR2'>
                            <#else>
                                <#assign rowClass = 'viewManyTR1'>
                            </#if>
                        </#list>
                    </#if>
                <#else>
                    ${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}
                    <#assign showFields = false>
                </#if>                            
            <#else>
                <#if hasCreatePermission>
                    <#if newFieldPkList?has_content>
                        <div class="tabletext">${uiLabelMap.WebtoolsMessage15} ${entityName} ${uiLabelMap.WebtoolsMessage16}.</div>
                        <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
                        <#list newFieldPkList as field> 
                            <tr class="${rowClass}">
                                <td valign="top"><div class="tabletext"><b>${field.name}</b></div></td>
                                <td valign="top">
                                    <div class="tabletext">                                    
                                        <#if field.fieldType == 'DateTime'>                                
                                            DateTime(YYYY-MM-DD HH:mm:SS.sss):<input class='editInputBox' type="text" name="${field.name}" size="24" value="${field.value}">
                                            <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                                        <#elseif field.fieldType == 'Date'>                                
                                            Date(YYYY-MM-DD):<input class='editInputBox' type="text" name="${field.name}" size="11" value="${field.value}">
                                            <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                                        <#elseif field.fieldType == 'Time'>                                
                                            Time(HH:mm:SS.sss):<input class='editInputBox' type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}">
                                        <#elseif field.fieldType == 'Integer'>                                
                                            <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}">
                                        <#elseif field.fieldType == 'Long'>                                
                                            <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}"> 
                                        <#elseif field.fieldType == 'Double'>                                
                                            <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}"> 
                                        <#elseif field.fieldType == 'Float'>                                
                                            <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}">
                                        <#elseif field.fieldType == 'StringOneRow'>                                
                                            <input class='editInputBox' type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                                        <#elseif field.fieldType == 'String'>                                
                                            <input class='editInputBox' type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                                        <#elseif field.fieldType == 'Textarea'>                                
                                            <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                                        </#if>
                                        &nbsp;
                                    </div>
                                </td>
                            </tr>
                            <#if rowClass == 'viewManyTR1'>
                                <#assign rowClass = 'viewManyTR2'>
                            <#else>
                                <#assign rowClass = 'viewManyTR1'>
                            </#if>
                        </#list>
                    </#if>
                <#else>
                    ${uiLabelMap.WebtoolsMesseage17} ${entityName} ${plainTableName} ${uiLabelMap.WebtoolsMesseage18}
                    <#assign showFields = false>
                </#if>            
            </#if>
            <#if showFields>
                <#if newFieldNoPkList?has_content>
                    <#list newFieldNoPkList as field> 
                        <tr class="${rowClass}">
                            <td valign="top"><div class="tabletext"><b>${field.name}</b></div></td>
                            <td valign="top">
                                <div class="tabletext">                                    
                                    <#if field.fieldType == 'DateTime'>                                
                                        DateTime(YYYY-MM-DD HH:mm:SS.sss):<input class='editInputBox' type="text" name="${field.name}" size="24" value="${field.value}">
                                        <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                                    <#elseif field.fieldType == 'Date'>                                
                                        Date(YYYY-MM-DD):<input class='editInputBox' type="text" name="${field.name}" size="11" value="${field.value}">
                                        <a href="javascript:call_cal(document.updateForm.${field.name}, '${field.value}');" onmouseover="window.status='Date Picker';return true;" onmouseout="window.status='';return true;"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Click here For Calendar'></a>
                                    <#elseif field.fieldType == 'Time'>                                
                                        Time(HH:mm:SS.sss):<input class='editInputBox' type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}">
                                    <#elseif field.fieldType == 'Integer'>                                
                                        <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}">
                                    <#elseif field.fieldType == 'Long'>                                
                                        <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}"> 
                                    <#elseif field.fieldType == 'Double'>                                
                                        <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}"> 
                                    <#elseif field.fieldType == 'Float'>                                
                                        <input class='editInputBox' type="text" size="20" name="${field.name}" value="${field.value}">
                                    <#elseif field.fieldType == 'StringOneRow'>                                
                                        <input class='editInputBox' type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                                    <#elseif field.fieldType == 'String'>                                
                                        <input class='editInputBox' type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}">
                                    <#elseif field.fieldType == 'Textarea'>                                
                                        <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                                    </#if>
                                    &nbsp;
                                </div>
                            </td>
                        </tr>
                        <#if rowClass == 'viewManyTR1'>
                            <#assign rowClass = 'viewManyTR2'>
                        <#else>
                            <#assign rowClass = 'viewManyTR1'>
                        </#if>
                    </#list>
                    <#if value?has_content>
                        <#assign button = "${uiLabelMap.CommonUpdate}">
                    <#else>
                        <#assign button = "${uiLabelMap.CommonCreate}">
                    </#if>
                    <tr class="${rowClass}" align="center">
                        <td colspan="2"><input type="submit" name="Update" value="${button}"></td>
                    </tr>
                </#if>
            </#if>
            </table>
        </form>
        </div>
    </#if>
    <#if relationFieldList?has_content>
        <#list relationFieldList as relation>
            <div id="area${relation.relIndex}" class='topcontainerhidden' width="100%">
                <div class='areaheader'>
                    <b>${relation.title}</b> ${uiLabelMap.WebtoolsRelatedEntity}: <b>${relation.relatedTable}</b> 
                    ${uiLabelMap.WebtoolsWithPk}: <#if relation.valueRelated?has_content>${relation.valueRelatedPk?if_exists}<#else>"${uiLabelMap.WebtoolsSpecifiedEntity1} ${uiLabelMap.WebtoolsSpecifiedEntity2}!"</#if>
                </div>
                <#if relation.valueRelated?has_content>
                    <a href='<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonView} ${relation.relatedTable}]</a>
                <#else>
                    <#if hasAllCreate || relCreate>
                        <a href='<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonCreate} ${relation.relatedTable}]</a>
                    </#if>
                </#if>    
                <div style='width: 100%; overflow: visible; border-style: none;'>
                    <table border="1" cellpadding="2" cellspacing="0" class="calendarTable">
                        <#assign rowClass = 'viewManyTR1'>
                        <#if relation.valueRelated?has_content>
                            <#if relation.relatedFieldsList?has_content>
                                <#list relation.relatedFieldsList as relatedField>
                                    <tr class="${rowClass}">
                                        <td valign="top"><div class="tabletext"><b>${relatedField.name}</b></div></td>
                                        <td valign="top">
                                            <div class="tabletext">
                                                ${relatedField.value}&nbsp;
                                            </div>
                                        </td>    
                                    </tr>  
                                    <#if rowClass == 'viewManyTR1'>
                                        <#assign rowClass = 'viewManyTR2'>
                                    <#else>
                                        <#assign rowClass = 'viewManyTR1'>
                                    </#if>      
                                </#list>
                            </#if>
                        <#else>
                            <tr class="${rowClass}"><td><b>${uiLabelMap.WebtoolsSpecifiedEntity1} ${relation.relatedTable} ${uiLabelMap.WebtoolsSpecifiedEntity2}.</b></td></tr>
                        </#if> 
                    </table>
                </div>
            </div>    
        </#list>
    </#if>
</div>
<#if (hasUpdatePermission || hasCreatePermission)  && !useValue> 
      <script language="JavaScript" type="text/javascript">  
        ShowTab("tab2");
      </script>
</#if>
