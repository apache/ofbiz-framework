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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.Results}</li>
    </ul>
  </div>
  <div class="screenlet-body">
    <#if cycleCountMap?has_content>
      <form method="post" id="selectAllForm" action="<@ofbizUrl>createNewSessionFromRejectedSession</@ofbizUrl>" name="selectAllForm">
        <table class="basic-table data-table-find-cycle-count" data-height="550">
          <thead>
            <tr class="header-row">
              <th class="data-table-align-left" width="20%">${uiLabelMap.Session}</th>
              <th class="data-table-align-left" width="20%">${uiLabelMap.ProductFacility}</th>
              <th class="data-table-align-left" width="20%">${uiLabelMap.ProductLocation}s</th>
              <th class="data-table-align-left" width="20%">${uiLabelMap.CommonStatus}</th>
              <th class="data-table-align-left" width="18%">${uiLabelMap.CountedInventoryItems}</th>
            </tr>
          </thead>
          <tbody>
            <#assign rowClass = "2"/>
            <#assign rowCount = 0/>
            <#list cycleCountMap.keySet() as key>
              <#assign cycleCount = cycleCountMap.get(key)>
              <tr <#if rowClass == "1"> class="alternate-row"</#if>>
                <td width="20%">
                  <#if "INV_COUNT_CREATED" == cycleCount.statusId>
                    <a href="<@ofbizUrl>RecordCount?inventoryCountId=${cycleCount.inventoryCountId!}</@ofbizUrl>" class="smallSubmit">${cycleCount.inventoryCountId!}</a>
                  </#if>
                  <#if "INV_COUNT_CREATED" != cycleCount.statusId>
                    <a href="<@ofbizUrl>ReviewCountSession?inventoryCountId=${cycleCount.inventoryCountId!}</@ofbizUrl>" class="smallSubmit">${cycleCount.inventoryCountId!}</a>
                  </#if>
                </td>
                <td width="20%">${cycleCount.facilityName!}</td>
                <td width="20%" class="data-table-word-break"><#if cycleCount.locationSeqIds?has_content><#list cycleCount.locationSeqIds as locationSeqId>${locationSeqId!}<#if locationSeqId_has_next>, </#if></#list></#if></td>
                <td width="20%">${cycleCount.statusDescription!}</td>
                <td width="18%">${cycleCount.totalInventoryItems!}</td>
              </tr>
              <#assign rowCount = rowCount + 1>
              <#if rowClass == "2">
                <#assign rowClass = "1">
              <#else>
                <#assign rowClass = "2">
              </#if>
            </#list>
          </tbody>
        </table>
      </form>
    <#else>
      <p>${uiLabelMap.CommonNoRecordFound}.</p>
    </#if>
  </div>
</div>