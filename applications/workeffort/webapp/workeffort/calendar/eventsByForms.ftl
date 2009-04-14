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
      <h3>${uiLabelMap.WorkEffortCalendarFindEntries}</h3>
  </div>
  <div class="screenlet-body">
    <form style="display: inline;" action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" name="partyform" method="post">
      <input type="hidden" name="start" value="${start.time?string("#")}"/>
      <span class="label">${uiLabelMap.WorkEffortByPartyId}</span>
      <input type="text" name="partyId" value="${requestParameters.partyId?if_exists}"/>
      <a href="javascript:call_fieldlookup2(document.partyform.partyId,'<@ofbizUrl>LookupPartyName</@ofbizUrl>');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt="${uiLabelMap.CommonClickHereForFieldLookup}"></a>
      <input type="submit" value="${uiLabelMap.CommonView}"/>
    </form>
    &nbsp;|
    <form style="display: inline;" action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
      <input type="hidden" name="start" value="${start.time?string("#")}"/>
      <span class="label">${uiLabelMap.WorkEffortByFacility}</span>
      <select name="facilityId">
        <option value=""></option>
        <#list allFacilities as facility>
          <option value="${facility.facilityId}"<#if requestParameters.facilityId?has_content && requestParameters.facilityId == facility.facilityId>${uiLabelMap.WorkEffortSelected}</#if>>${facility.facilityName}</option>
        </#list>
      </select>
      <input type="submit" value="${uiLabelMap.CommonView}"/>
    </form>
    <br/>
    &nbsp;
    <form style="display: inline;" action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
      <input type="hidden" name="start" value="${start.time?string("#")}"/>
      <span class="label">${uiLabelMap.WorkEffortByFixedAsset}</span>
      <select name="fixedAssetId">
        <option value=""></option>
        <#list allFixedAssets as fixedAsset>
          <option value="${fixedAsset.fixedAssetId}"<#if requestParameters.fixedAssetId?has_content && requestParameters.fixedAssetId == fixedAsset.fixedAssetId>${uiLabelMap.WorkEffortSelected}</#if>>${fixedAsset.fixedAssetId}</option>
        </#list>
      </select>
      <input type="submit" value="${uiLabelMap.CommonView}"/>
    </form>
    &nbsp;|
    <form style="display: inline;" action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
      <input type="hidden" name="start" value="${start.time?string("#")}"/>
      <span class="label">${uiLabelMap.CommonType}</span>
      <select name="workEffortTypeId">
        <option value=""></option>
        <#list allWorkEffortTypes as WorkEffortType>
          <option value="${WorkEffortType.workEffortTypeId}"<#if requestParameters.workEffortTypeId?has_content && requestParameters.workEffortTypeId == WorkEffortType.workEffortTypeId>${uiLabelMap.WorkEffortSelected}</#if>>${WorkEffortType.description}</option>
        </#list>
      </select>
      <input type="submit" value="${uiLabelMap.CommonView}"/>
    </form>
  </div>
</div>