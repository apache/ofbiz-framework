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

  <div id="partyVisits" class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.PartyVisits}</li>
        <li><a href="<@ofbizUrl>findVisits?partyId=${partyId}</@ofbizUrl>">${uiLabelMap.CommonListAll}</a></li>
      </ul>
      <br class="clear" />
    </div>
    <div class="screenlet-body">
      <#if visits?has_content>
        <table class="basic-table" cellspacing="0">
          <tr class="header-row">
            <td>${uiLabelMap.PartyVisitId}</td>
            <td>${uiLabelMap.PartyUserLogin}</td>
            <td>${uiLabelMap.PartyNewUser}</td>
            <td>${uiLabelMap.PartyWebApp}</td>
            <td>${uiLabelMap.PartyClientIP}</td>
            <td>${uiLabelMap.CommonFromDate}</td>
            <td>${uiLabelMap.CommonThruDate}</td>
          </tr>
          <#list visits as visitObj>
            <#if (visitObj_index > 4)><#break></#if>
              <tr>
                <td class="button-col">
                  <a href="<@ofbizUrl>visitdetail?visitId=${visitObj.visitId!}</@ofbizUrl>">${visitObj.visitId!}</a>
                </td>
                <td>${visitObj.userLoginId!}</td>
                <td>${visitObj.userCreated!}</td>
                <td>${visitObj.webappName!}</td>
                <td>${visitObj.clientIpAddress!}</td>
                <td>${(visitObj.fromDate.toString())!}</td>
                <td>${(visitObj.thruDate.toString())!}</td>
              </tr>
          </#list>
        </table>
      <#else>
        ${uiLabelMap.PartyNoVisitFound}
      </#if>
    </div>
  </div>