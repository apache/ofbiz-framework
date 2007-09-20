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
      <#if timesheet?exists>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleEditTimesheet} # ${timesheet.timesheetId?if_exists}</li>
      <#else>
        <li class="head3">&nbsp;${uiLabelMap.PageTitleAddTimesheet}</li>
      </#if>
    </ul>
    <br class="clear" />
  </div> 
  <div class="screenlet-body"> 
    <#if timesheet?exists>
      <form name="CreateTimeSheetForm" method="get" action="<@ofbizUrl>updateTimeSheet</@ofbizUrl>" >
    <#else>
      <form name="CreateTimeSheetForm" method="get" action="<@ofbizUrl>createTimesheets</@ofbizUrl>" >
    </#if>
    <table width="100%" cellpadding="2" cellspacing="0" border="0">
      <#if timesheet?exists>
        <td><input type="hidden" name="timesheetId" value="${timesheet.timesheetId?if_exists}"/></td>
        <td><input type="hidden" name="partyId" value="${timesheet.partyId?if_exists}"/></td>
      </#if>
      <#if timesheet?exists>
        <tr>
          <td class="label">${uiLabelMap.TimesheetTimesheetId}</td>
          <td>${timesheet.timesheetId?if_exists}<span class="tooltip">${uiLabelMap.CommonNotModifRecreat}</span></td>
        </tr>
      </#if>
      <tr>
        <td class="label">${uiLabelMap.PartyParty}</td>
        <#if timesheet?exists>
          <td>
            <#assign partyDetail = delegator.findByPrimaryKeyCache("PartyNameView", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId",timesheet.partyId?if_exists))>
            [${timesheet.partyId?if_exists}] ${partyDetail.firstName?if_exists} ${partyDetail.middleName?if_exists} ${partyDetail.lastName?if_exists}
          </td>
        <#else>
          <td>
            <select name="partyIdList" class="selectBox" size="6" multiple="multiple">
              <#assign partyList = delegator.findAll("Party", Static["org.ofbiz.base.util.UtilMisc"].toList("partyId"))>
              <#list partyList as party>
                <#assign partyDetail = delegator.findByPrimaryKeyCache("PartyNameView", Static["org.ofbiz.base.util.UtilMisc"].toMap("partyId", party.partyId?if_exists))>
                <option value="${party.partyId?if_exists}">[${partyDetail.partyId?if_exists}] ${partyDetail.firstName?if_exists} ${partyDetail.middleName?if_exists} ${partyDetail.lastName?if_exists}</option>
              </#list>
            </select>
          </td>  
        </#if>
      </tr>
      <tr>  
        <td class="label">${uiLabelMap.ClientPartyId}</td>
        <#if timesheet?exists>
          <td><input type="text" name="clientPartyId" value="${timesheet.clientPartyId?if_exists}"/>
            <a href="javascript:call_fieldlookup2(document.CreateTimeSheetForm.clientPartyId,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a></td>
          </td>
        <#else>        
          <td><input type="text" name="clientPartyId" value=""/>
            <a href="javascript:call_fieldlookup2(document.CreateTimeSheetForm.clientPartyId,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a></td>
          </td>
        </#if>              
      </tr>      
      <tr>
        <td class="label">${uiLabelMap.CommonFromDate}</td>
        <#if timesheet?exists>  
          <td>
            <input type="text" size="20" name="fromDate" value="${timesheet.fromDate?if_exists}" class="field text">
            <a href="javascript:call_cal(document.CreateTimeSheetForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
        <#else>  
          <td>
            <input type="text" size="20" name="fromDate" class="field text">
            <a href="javascript:call_cal(document.CreateTimeSheetForm.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
          <td>&nbsp;</td>
        </#if>  
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonThruDate}</td>
        <#if timesheet?exists>  
          <td>
            <input type="text" size="20" name="thruDate" value="${timesheet.thruDate?if_exists}" class="field text">
            <a href="javascript:call_cal(document.CreateTimeSheetForm.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
        <#else>
          <td>
            <input type="text" size="20" name="thruDate" class="field text">
            <a href="javascript:call_cal(document.CreateTimeSheetForm.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
        </#if>
        <td>&nbsp;</td>
      </tr> 
      <tr>    
        <td class="label">${uiLabelMap.CommonComments}</td>
        <#if timesheet?exists>  
          <td><input type="text" size="35" name="comments" value="${timesheet.comments?if_exists}"/></td>
        <#else>
          <td><input type="text" size="35" name="comments" value=""/></td>
        </#if>         
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>
          <a href="javascript:document.CreateTimeSheetForm.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
        </td>
      </tr>
    </table>
    </forms>
  </div>
</div>
