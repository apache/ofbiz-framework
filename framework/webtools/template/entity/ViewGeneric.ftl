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
<script type="application/javascript">
var numTabs=${(entity.getRelationsSize()+1)};
function ShowTab(lname) {
  for(inc=1; inc <= numTabs; inc++) {
    // document.getElementById('area' + inc).className = (lname == 'tab' + inc) ? 'screenlet' : 'topcontainerhidden';
    // style of topcontainerhidden 
    // .topcontainerhidden { POSITION: absolute; VISIBILITY: hidden; }
    var elem = document.getElementById('area' + inc);
    if (lname == 'tab' + inc){
      elem.className = 'screenlet';
    }
    else {
      elem.className = 'topcontainerhidden';
      elem.style.position = 'absolute';
      elem.style.visibility = 'hidden';
    }
  }
}
</script>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.WebtoolsViewValue}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <h2>${uiLabelMap.WebtoolsForEntity}: ${entityName}</h2>
    <h2>${uiLabelMap.WebtoolsWithPk}: ${findByPk}</h2>
    <br />
    <div class="button-bar">
      <a href='<@ofbizUrl>FindGeneric?entityName=${entityName}&amp;noConditionFind=Y</@ofbizUrl>' class="buttontext">${uiLabelMap.WebtoolsBackToFindScreen}</a>
      <#if enableEdit = "false">
        <#if hasCreatePermission>
          <a href='<@ofbizUrl>ViewGeneric?entityName=${entityName}&amp;enableEdit=true</@ofbizUrl>' class="buttontext create">${uiLabelMap.CommonCreate}</a>
          <a href="<@ofbizUrl>ViewGeneric?${curFindString}&amp;enableEdit=true</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </#if>
        <#if value?has_content>
          <#if hasDeletePermission>
            <a href='<@ofbizUrl>UpdateGeneric?UPDATE_MODE=DELETE&amp;${curFindString}</@ofbizUrl>' class="buttontext delete">${uiLabelMap.WebtoolsDeleteThisValue}</a>
          </#if>
        </#if>
      </#if>
    </div>
    <#if value?has_content>
      <form name="relationForm">
        <p><b>${uiLabelMap.CommonView}</b></p>
        <select name="viewRelated" onchange="javascript:ShowTab(this.options[this.selectedIndex].value)">
          <option value="tab1">${entityName}</option>
          <#list relationFieldList as relation>
            <option value="tab${(relation_index+2)}">${relation.title}${relation.relEntityName} (${relation.type})</option>
          </#list>
        </select>
      </form>
      <br />
    </#if>
    <div id="area1" class="screenlet">
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
        <#assign alt_row = false>
        <div id="area2" class="screenlet">
          <div class="screenlet-title-bar">
            <h3>${uiLabelMap.WebtoolsEntityEditValue}</h3>
          </div>
          <#if pkNotFound>
            <p>${uiLabelMap.WebtoolsEntityName} ${entityName} ${uiLabelMap.WebtoolsWithPk} ${findByPk} ${uiLabelMap.WebtoolsSpecifiedEntity2}.</p>
          </#if>
          <form action='<@ofbizUrl>UpdateGeneric?entityName=${entityName}</@ofbizUrl>' method="post" name="updateForm">
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
                  ${uiLabelMap.WebtoolsEntityCreatePermissionError} ${entityName} ${plainTableName}
                  <#assign showFields = false>
                </#if>
              <#else>
                <#if hasCreatePermission>
                  <#if newFieldPkList?has_content>
                    <p>${uiLabelMap.WebtoolsYouMayCreateAnEntity}</p>
                    <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
                    <#list newFieldPkList as field>
                      <tr<#if alt_row> class="alternate-row"</#if>>
                        <td class="label">${field.name}</td>
                        <td>
                          <#if field.fieldType == 'DateTime'>
                            ${uiLabelMap.CommonFormatDateTime}<@htmlTemplate.renderDateTimeField name="${field.name}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${field.value}" size="25" maxlength="30" id="${field.name}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                          <#elseif field.fieldType == 'Date'>
                            ${uiLabelMap.CommonFormatDate}<@htmlTemplate.renderDateTimeField name="${field.name}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${field.value}" size="25" maxlength="30" id="${field.name}" dateType="date" shortDateInput=true timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                          <#elseif field.fieldType == 'Time'>
                            ${uiLabelMap.CommonFormatTime}<input type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'Integer'>
                            <input type="text" size="20" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'Long'>
                            <input type="text" size="20" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'Double'>
                            <input type="text" size="20" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'Float'>
                            <input type="text" size="20" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'StringOneRow'>
                            <input type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'String'>
                            <input type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}" />
                          <#elseif field.fieldType == 'Textarea'>
                            <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                          <#else>
                            <input type="text" size="20" name="${field.name}" value="${field.value}" />
                          </#if>
                        </td>
                      </tr>
                      <#assign alt_row = !alt_row>
                    </#list>
                  </#if>
                <#else>
                  <p>${uiLabelMap.WebtoolsEntityCreatePermissionError} ${entityName} ${plainTableName}</p>
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
                          ${uiLabelMap.CommonFormatDateTime}<@htmlTemplate.renderDateTimeField name="${field.name}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${field.value}" size="25" maxlength="30" id="${field.name}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <#elseif field.fieldType == 'Date'>
                          ${uiLabelMap.CommonFormatDate}<@htmlTemplate.renderDateTimeField name="${field.name}" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${field.value}" size="25" maxlength="30" id="${field.name}" dateType="date" shortDateInput=true timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                        <#elseif field.fieldType == 'Time'>
                          ${uiLabelMap.CommonFormatTime}<input type="text" size="6" maxlength="10" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'Integer'>
                          <input type="text" size="20" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'Long'>
                          <input type="text" size="20" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'Double'>
                          <input type="text" size="20" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'Float'>
                          <input type="text" size="20" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'StringOneRow'>
                          <input type="text" size="${field.stringLength}" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'String'>
                          <input type="text" size="80" maxlength="${field.stringLength}" name="${field.name}" value="${field.value}" />
                        <#elseif field.fieldType == 'Textarea'>
                          <textarea cols="60" rows="3" maxlength="${field.stringLength}" name="${field.name}">${field.value}</textarea>
                        <#else>
                          <input type="text" size="20" name="${field.name}" value="${field.value}" />
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
                      <input type="submit" name="Update" value="${button}" />
                      <a href="<@ofbizUrl>ViewGeneric?${curFindString}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonCancel}</a>
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
      <#list relationFieldList as relation>
          <div id="area${(relation_index + 2)}" class="topcontainerhidden">
            <div class="screenlet-title-bar">
              <ul>
                <li class="h3">${uiLabelMap.WebtoolsRelatedEntity}: ${relation.title}${relation.relatedTable} (${relation.type})</li>
                <li><a href="<@ofbizUrl>FindGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>">${uiLabelMap.CommonFind}</a></li>
                <#if relation.valueRelated?has_content>
                  <li><a href="<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>">${uiLabelMap.CommonView}</a></li>
                </#if>
                <#if hasAllCreate || relCreate>
                  <li><a href="<@ofbizUrl>ViewGeneric?${relation.encodeRelatedEntityFindString}&amp;enableEdit=true</@ofbizUrl>">${uiLabelMap.CommonCreate}</a></li>
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
              <#if "one" = relation.type>
              <b>${uiLabelMap.WebtoolsNoValueFoundFor}</b> ${relation.title}${relation.relatedTable}.
              <#else>
              <a href="<@ofbizUrl>FindGeneric?${relation.encodeRelatedEntityFindString}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonFind}</a>
              </#if>
            </#if>
          </div>
      </#list>
    </#if>
  </div>
  <div class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.WebtoolsEntityXMLRepresentation}</h3>
    </div>
    <div class="screenlet-body">
      <p>
      <#if value?has_content>
        <#assign valueXmlDoc = Static["org.apache.ofbiz.entity.GenericValue"].makeXmlDocument([value]) />
        ${Static["org.apache.ofbiz.base.util.UtilXml"].writeXmlDocument(valueXmlDoc)?replace("\n", "<br />")?replace("    ", "&nbsp;&nbsp;&nbsp;&nbsp;")}
      </#if>
      </p>
    </div>
  </div>
  <br class="clear"/>
</div>
