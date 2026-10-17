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

<form method="post" action="<@ofbizUrl>SearchInventoryItemsByLabels</@ofbizUrl>">
  <input type="hidden" name="facilityId" value="${facility.facilityId}"/>
  <table>
  <#assign index = 0>
  <#list labelTypes as labelType>
    <#assign index = index + 1>
    <#assign labels = labelType.getRelated("InventoryItemLabel", null, Static["org.apache.ofbiz.base.util.UtilMisc"].toList("inventoryItemLabelId"), false)>
    <tr>
      <td>
          <div>
          <span class="label">${labelType.description!} [${labelType.inventoryItemLabelTypeId}]</span>
          &nbsp;
          <select name="inventoryItemLabelId_${index}">
            <option></option>
            <#list labels as label>
            <option value="${label.inventoryItemLabelId}" <#if parameters["inventoryItemLabelId_" + index]?has_content && parameters["inventoryItemLabelId_" + index] == label.inventoryItemLabelId>selected="selected"</#if>>${label.description!} [${label.inventoryItemLabelId}]</option>
            </#list>
          </select>
          </div>
      </td>
    </tr>
  </#list>
  <tr>
    <td>
      <input type="submit" value="${uiLabelMap.CommonSubmit}"/>
    </td>
  </tr>
  </table>
  <input type="hidden" name="numberOfFields" value="${index}"/>
</form>
