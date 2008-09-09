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
        <li class="h3">&nbsp;${uiLabelMap.ProjectMgrAddTimeEntry}</li>
    </ul>
    <br class="clear" />
  </div> 
  <div class="screenlet-body">
    <form name="editTimeEntryForm" action="<@ofbizUrl>createTimeEntry</@ofbizUrl>">
      <table width="100%" cellpadding="2" cellspacing="0" border="1">
        <tr>
          <td><input type="hidden" name="workEffortId" value="${parameters.workEffortId?if_exists}"/></td>
        </tr>                           
        <tr>    
          <td class="label" >${uiLabelMap.PartyPartyId}</td>
          <td>
            <input type="text" name="partyId" value=""/>
            <a href="javascript:call_fieldlookup2(document.editTimeEntryForm.partyId,'LookupPerson');">
              <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
            </a>
          </td>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.CommonFromDate}</td>
          <td>
            <input type="text" size="20" name="fromDate"/>
            <a href="javascript:call_cal(document.editTimeEntryForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
        </tr>
        <tr>
          <td class="label" >${uiLabelMap.CommonThruDate}</td>
          <td>
            <input type="text" size="20" name="thruDate"/>
            <a href="javascript:call_cal(document.editTimeEntryForm.thruDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
        </tr>
        <tr>    
          <td class="label" >${uiLabelMap.TimesheetRateType}</td>
          <td>
            <#assign rateTypes = delegator.findList("RateType", null, null, null, null, false)>
            <select name="rateTypeId">
              <#list rateTypes as rateType>                    
                <option value="${rateType.rateTypeId}">${rateType.description}</option>
              </#list>
            </select>  
          </td>
        </tr>
        <tr>    
          <td class="label" >${uiLabelMap.TimesheetTimesheetId}</td>
          <td>
            <input type="text" name="timesheetId" value=""/>
            <a href="javascript:call_fieldlookup2(document.editTimeEntryForm.timesheetId,'LookupTimesheet');">
              <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/>
            </a>
          </td>
        </tr>
        <tr>    
          <td class="label" >${uiLabelMap.TimesheetHours}</td>
          <td><input type="text" name="hours" value=""/></td>
        </tr> 
        <tr>
          <td>&nbsp;</td>    
          <td>
            <input type="submit" name="submit" value="${uiLabelMap.CommonSave}"/>
          </td>
        </tr>    
      </table>    
    </form>     
  </div>
</div>
