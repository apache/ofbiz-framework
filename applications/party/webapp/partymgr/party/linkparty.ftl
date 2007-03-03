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

<#-- Party IDs -->
<#assign partyId = requestParameters.partyId?if_exists>
<#assign partyIdTo = requestParameters.partyIdTo?if_exists>

<!-- begin linkparty.ftl -->
<br/>
<#if hasUpdatePermission>
  <div id="linkParty" class="screenlet">
    <div class="screenlet-title-bar">
      <h3>${uiLabelMap.PartyLink}</h3>
    </div>
    <div class="screenlet-body">
      <center>
        <#if partyTo?has_content && partyFrom?has_content>
          <form name="linkparty" method="post" action="<@ofbizUrl>setPartyLink</@ofbizUrl>">
            <div class="head1 alert">
              ${uiLabelMap.PartyLinkMessage1}
            </div>
            <br />
            <div>
              <span class="label">${uiLabelMap.PartyLink}:</span>
              <input type="hidden" name="partyId" value="${partyFrom.partyId}"/>
              <#if personFrom?has_content>
                ${personFrom.lastName}, ${personFrom.firstName}
              <#elseif groupFrom?has_content>
                ${groupFrom.groupName}
              <#else>
                [${uiLabelMap.PartyUnknown}]
              </#if>
              &nbsp;[${partyFrom.partyId}]
            </div>
            <div>
              <span class="label">${uiLabelMap.CommonTo}:</span>
              <input type="hidden" name="partyIdTo" value="${partyTo.partyId}"/>
              <#if personTo?has_content>
                ${personTo.lastName}, ${personTo.firstName}
              <#elseif groupTo?has_content>
                ${groupTo.groupName}
              <#else>
                [${uiLabelMap.PartyUnknown}]
              </#if>
              &nbsp;[${partyTo.partyId}]
            </div>
            <br />
            <div>
              <a href="javascript:document.linkparty.submit()" class="smallSubmit">${uiLabelMap.CommonConfirm}</a>
            </div>
            <br />
         </form>
        <#else>
          <form name="linkpartycnf" method="post" action="<@ofbizUrl>linkparty</@ofbizUrl>">
            <div>
              <span class="label">${uiLabelMap.PartyLink}</span>
              <input type="text" name="partyId" value="${partyId?if_exists}"/>
              &nbsp;
              <span class="label">${uiLabelMap.CommonTo}</span>
              <input type="text" name="partyIdTo" value="${partyIdTo?if_exists}"/>
              <a href="javascript:document.linkpartycnf.submit()" class="smallSubmit">${uiLabelMap.CommonLink}</a>
            </div>
          </form>
        </#if>
      </center>
    </div>
  </div>
</#if>
<!-- end linkparty.ftl -->
