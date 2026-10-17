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

  <div id="partyAVS" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.PartyAvsOver}</li>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <span class="label">${uiLabelMap.PartyAvsString}</span>${(avsOverride.avsDeclineString)?default("${uiLabelMap.CommonGlobal}")}
      <#if security.hasEntityPermission("PARTYMGR", "_UPDATE", session)>
        <a href="<@ofbizUrl>editAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonEdit}</a>
        <#if avsOverride??>
          <a href="<@ofbizUrl>resetAvsOverride?partyId=${party.partyId}</@ofbizUrl>" class="smallSubmit">${uiLabelMap.CommonReset}</a>
        </#if>
      </#if>
    </div>
  </div>