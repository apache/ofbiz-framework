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

<#assign hideFields = parameters.hideFields?default("N")>
<#assign delimchar = "?">
<#if parameters.targetRequestUri?contains("?")><#assign delimchar = "&"></#if>

<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <h3>${uiLabelMap.WorkEffortCalendarFindEntries}</h3>
      <#if hideFields = "Y">
        <li><a href="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>">${uiLabelMap.CommonShowLookupFields}</a></li>
      <#else>
        <li><a href="<@ofbizUrl>${parameters.targetRequestUri}${delimchar}hideFields=Y</@ofbizUrl>">${uiLabelMap.CommonHideFields}</a></li>
      </#if>
    </ul>
    <br class="clear"/>
  </div>
  <#if hideFields = "N">
    <div class="screenlet-body">
      <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" name="partyform" method="post">
        <input type="hidden" name="start" value="${start.time?string("#")}"/>
        ${uiLabelMap.WorkEffortByPartyId}: 
        <input type="text" name="partyId" value="${requestParameters.partyId?if_exists}"/>
        <a href="javascript:call_fieldlookup2(document.partyform.partyId,'<@ofbizUrl>LookupPartyName</@ofbizUrl>');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'></a>
        <input type="submit" value="${uiLabelMap.CommonView}"/>
      </form>
      <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
        <input type="hidden" name="start" value="${start.time?string("#")}"/>
        ${uiLabelMap.WorkEffortByFacility}: 
        <select name="facilityId">
          <option value=""></option>
          <#list allFacilities as facility>
            <option value="${facility.facilityId}"<#if requestParameters.facilityId?has_content && requestParameters.facilityId == facility.facilityId>${uiLabelMap.WorkEffortSelected}</#if>>${facility.facilityName}</option>
          </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonView}"/>
      </form>
      <form action="<@ofbizUrl>${parameters.targetRequestUri}</@ofbizUrl>" method="post">
        <input type="hidden" name="start" value="${start.time?string("#")}"/>
        ${uiLabelMap.WorkEffortByFixedAsset}: 
        <select name="fixedAssetId">
          <option value=""></option>
          <#list allFixedAssets as fixedAsset>
            <option value="${fixedAsset.fixedAssetId}"<#if requestParameters.fixedAssetId?has_content && requestParameters.fixedAssetId == fixedAsset.fixedAssetId>${uiLabelMap.WorkEffortSelected}</#if>>${fixedAsset.fixedAssetId}</option>
          </#list>
        </select>
        <input type="submit" value="${uiLabelMap.CommonView}"/>
      </form>
    </div>
  </#if>
</div>
