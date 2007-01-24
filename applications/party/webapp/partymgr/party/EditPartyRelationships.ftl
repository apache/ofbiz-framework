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
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.PartyRelationships}</div>
    </div>
    <div class="screenlet-body">
        <#if partyRelationships?has_content>
        <table width="100%" border="1" cellpadding="1" cellspacing="0">
          <tr>
            <td><div class="tabletext"><b>&nbsp;${uiLabelMap.CommonDescription}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;${uiLabelMap.CommonFromDate}</b></div></td>
            <#if security.hasEntityPermission("PARTYMGR", "_REL_DELETE", session)>
            <td>&nbsp;</td>
            </#if>
          </tr>
          <#list partyRelationships as partyRelationship>
              <#assign partyRelationshipType = partyRelationship.getRelatedOneCache("PartyRelationshipType")?if_exists>
              <#assign roleTypeTo = partyRelationship.getRelatedOneCache("ToRoleType")>
              <#assign roleTypeFrom = partyRelationship.getRelatedOneCache("FromRoleType")>
              <tr>
                <td><div class="tabletext">
                    ${uiLabelMap.PartyParty} <b>${partyRelationship.partyIdTo}</b>
                    <#if "_NA_" != partyRelationship.roleTypeIdTo>
                        ${uiLabelMap.PartyRole} <b>${roleTypeTo.get("description",locale)}</b>
                    </#if>
                    ${uiLabelMap.CommonIsA} <b>${(partyRelationshipType.get("partyRelationshipName",locale))?default("${uiLabelMap.CommonNA}")}</b>
                    ${uiLabelMap.CommonOf} ${uiLabelMap.PartyParty} <b>${partyRelationship.partyIdFrom}</b>
                    <#if "_NA_" != partyRelationship.roleTypeIdFrom>
                        ${uiLabelMap.PartyRole} <b>${roleTypeFrom.get("description",locale)}</b>
                    </#if>
                    <#if partyRelationship.securityGroupId?exists>
                    ${uiLabelMap.CommonAnd} ${uiLabelMap.PartyRelationSecurity} <b>${partyRelationship.getRelatedOne("SecurityGroup").get("description",locale)}</b>
                    </#if>
                </div></td>
                <td><div class="tabletext">&nbsp;${partyRelationship.fromDate}</div></td>
                <#if security.hasEntityPermission("PARTYMGR", "_REL_DELETE", session)>
                <td align="right">                     
                    <a href="<@ofbizUrl>deletePartyRelationship?partyIdTo=${partyRelationship.partyIdTo}&amp;roleTypeIdTo=${partyRelationship.roleTypeIdTo}&amp;roleTypeIdFrom=${partyRelationship.roleTypeIdFrom}&amp;partyIdFrom=${partyRelationship.partyIdFrom}&amp;fromDate=${partyRelationship.fromDate}&amp;partyId=${partyId?if_exists}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonRemove}</a>&nbsp;
                </td>
                </#if>
              </tr>
              <#if security.hasEntityPermission("PARTYMGR", "_REL_UPDATE", session)>
              <tr>
                <td colspan="3" align="right">
                    <form method="post" name="updatePartyRel${partyRelationship_index}" action="<@ofbizUrl>updatePartyRelationship</@ofbizUrl>">
                        <input type="hidden" name="partyId" value="${partyId}"/>
                        <input type="hidden" name="partyIdFrom" value="${partyRelationship.partyIdFrom}"/>
                        <input type="hidden" name="roleTypeIdFrom" value="${partyRelationship.roleTypeIdFrom}"/>
                        <input type="hidden" name="partyIdTo" value="${partyRelationship.partyIdTo}"/>
                        <input type="hidden" name="roleTypeIdTo" value="${partyRelationship.roleTypeIdTo}"/>
                        <input type="hidden" name="fromDate" value="${partyRelationship.fromDate}"/>
                        <span class="tabletext"><b>${uiLabelMap.CommonThru}: </b></span><input type="text" size="24" class="inputBox" name="thruDate" value="${partyRelationship.thruDate?if_exists}"/>
                        <a href="javascript:call_cal(document.updatePartyRel${partyRelationship_index}.thruDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"/></a>
                        <#-- ${partyRelationship.statusId}-->
                        <span class="tabletext"><b>${uiLabelMap.CommonComments}: </b></span><input type="text" size="50" class="inputBox" name="comments" value="${partyRelationship.comments?if_exists}"/>
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
                    </form>
                </td>
              </tr>
              </#if>
          </#list>
        </table>
        <#else/>
          <div class="tabletext">${uiLabelMap.PartyNoPartyRelationshipsFound}</div>
        </#if>
    </div>

  <#if security.hasEntityPermission("PARTYMGR", "_REL_UPDATE", session)>
    <div><hr class="sepbar"></div>
    <div class="screenlet-body">
        <form name="addPartyRelationshipTo" method="post" action="<@ofbizUrl>createPartyRelationship</@ofbizUrl>">
          <input type="hidden" name="partyId" value="${partyId}"/>
          <input type="hidden" name="partyIdFrom" value="${partyId}"/>
          <div class="tabletext" style="font-weight: bold;">
            ${uiLabelMap.PartyPartyWithId}
            <input type="text" size="20" name="partyIdTo" class="inputBox"/>
            <a href="javascript:call_fieldlookup2(document.addPartyRelationshipTo.partyIdTo,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
            ${uiLabelMap.PartyPartyInTheRoleOf}
            <select name="roleTypeIdTo" class="selectBox">
              <#list roleTypes as roleType>
                <option <#if "_NA_" == roleType.roleTypeId>selected="selected"</#if> value="${roleType.roleTypeId}">${roleType.get("description",locale)}<#-- [${roleType.roleTypeId}]--></option>
              </#list>
            </select>
            ${uiLabelMap.CommonIsA}
            <select name="partyRelationshipTypeId" class="selectBox">
              <#list relateTypes as relateType>
                <option value="${relateType.partyRelationshipTypeId}">${relateType.get("partyRelationshipName",locale)}<#-- [${relateType.partyRelationshipTypeId}]--></option>
              </#list>
            </select>
            ${uiLabelMap.PartyPartyOfTheRoleParty}
            <select name="roleTypeIdFrom" class="selectBox">
              <#list roleTypesForCurrentParty as roleType>
                <option <#if "_NA_" == roleType.roleTypeId>selected="selected"</#if> value="${roleType.roleTypeId}">${roleType.get("description",locale)}<#-- [${roleType.roleTypeId}]--></option>
              </#list>
            </select>
            <#-- set security group specific to this party relationship -->
            <br/>${uiLabelMap.CommonAnd} ${uiLabelMap.PartyRelationSecurity} 
            <select name="securityGroupId" class="selectBox">
              <option value="">&nbsp;</option>
              <#list securityGroups as securityGroup>
                <option value="${securityGroup.groupId}">${securityGroup.get("description",locale)}</option>
              </#list>
            </select><br/>
            ${uiLabelMap.CommonFrom} <input type="text" size="24" name="fromDate" class="inputBox"/><a href="javascript:call_cal(document.addPartyRelationshipTo.fromDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"/></a>
            ${uiLabelMap.CommonThru} <input type="text" size="24" name="thruDate" class="inputBox"/><a href="javascript:call_cal(document.addPartyRelationshipTo.thruDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"/></a>
          </div>
          <div><span class="tabletext">${uiLabelMap.CommonComments}:&nbsp;&nbsp;</span><input type="text" size="60" name="comments" class="inputBox"/></div>
          <div><a href="javascript:document.addPartyRelationshipTo.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></div>
        </form>
    </div>
    <div><hr class="sepbar"></div>
    <div class="screenlet-body">
        <form name="addPartyRelationshipFrom" method="post" action="<@ofbizUrl>createPartyRelationship</@ofbizUrl>">
          <input type="hidden" name="partyId" value="${partyId}"/>
          <input type="hidden" name="partyIdTo" value="${partyId}"/>
          <div class="tabletext" style="font-weight: bold;">
              ${uiLabelMap.PartyPartyCurrentInTheRoleOf}
            <select name="roleTypeIdTo" class="selectBox">
              <#list roleTypesForCurrentParty as roleType>
                <option <#if "_NA_" == roleType.roleTypeId>selected="selected"</#if> value="${roleType.roleTypeId}">${roleType.get("description",locale)}<#-- [${roleType.roleTypeId}]--></option>
              </#list>
            </select>
            ${uiLabelMap.CommonIsA}
            <select name="partyRelationshipTypeId" class="selectBox">
              <#list relateTypes as relateType>
                <option value="${relateType.partyRelationshipTypeId}">${relateType.get("partyRelationshipName",locale)}<#-- [${relateType.partyRelationshipTypeId}]--></option>
              </#list>
            </select>
            ${uiLabelMap.PartyPartyWithId}
            <input type="text" size="20" name="partyIdFrom" class="inputBox"/>
            <a href="javascript:call_fieldlookup2(document.addPartyRelationshipFrom.partyIdFrom,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
            ${uiLabelMap.PartyPartyInTheRoleOf}
            <select name="roleTypeIdFrom" class="selectBox">
              <#list roleTypes as roleType>
                <option <#if "_NA_" == roleType.roleTypeId>selected="selected"</#if> value="${roleType.roleTypeId}">${roleType.get("description",locale)}<#-- [${roleType.roleTypeId}]--></option>
              </#list>
            </select>
            <br/>${uiLabelMap.CommonAnd} ${uiLabelMap.PartyRelationSecurity}
            <select name="securityGroupId" class="selectBox">
              <option value="">&nbsp;</option>
              <#list securityGroups as securityGroup>
                <option value="${securityGroup.groupId}">${securityGroup.get("description",locale)}</option>
              </#list>
            </select><br/>
            ${uiLabelMap.CommonFrom} <input type="text" size="24" name="fromDate" class="inputBox"/><a href="javascript:call_cal(document.addPartyRelationshipFrom.fromDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"/></a>
            ${uiLabelMap.CommonThru} <input type="text" size="24" name="thruDate" class="inputBox"/><a href="javascript:call_cal(document.addPartyRelationshipFrom.thruDate, null);"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"/></a>
          </div>
          <div><span class="tabletext">${uiLabelMap.CommonComments}:&nbsp;&nbsp;</span><input type="text" size="60" name="comments" class="inputBox"/></div>
          <div><a href="javascript:document.addPartyRelationshipFrom.submit()" class="buttontext">${uiLabelMap.CommonAdd}</a></div>
        </form>
    </div>
  </#if>
</div>
