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
      <li class="head3">${uiLabelMap.PageTitleFindTask}&nbsp</li>
    </ul>
    <br class="clear" />
  </div>  
  <div class="screenlet-body">
    <form method="post" name="listTasksByDateRange" action="<@ofbizUrl>FindTaskByRange?workEffortTypeId=TASK&findAll=Y</@ofbizUrl>">
      <table width="">
        <tr>
          <td><b>${uiLabelMap.ProjectMgrToFindTask}</b></td>
        </tr>
        <tr>
          <td align="right">${uiLabelMap.CommonFromDate}</td>
          <td>
            <input type="text" size="20" name="fromDate" class="field text">
             <a href="javascript:call_cal(document.listTasksByDateRange.fromDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
          <td>&nbsp;</td>
        </tr>
        <tr>
          <td align="right">${uiLabelMap.CommonThruDate}</td>
          <td>
            <input type="text" size="20" name="thruDate" class="field text">
            <a href="javascript:call_cal(document.listTasksByDateRange.thruDate, null);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
          </td>
          <td>&nbsp;</td>
        </tr>
        <tr><td>&nbsp;</td></tr>
        <tr>
          <td>&nbsp;</td>
          <td align="left">
            <a href="javascript:document.listTasksByDateRange.submit()" class="buttontext">${uiLabelMap.CommonFind}</a>
          </td>
        </tr>
      </table>
    </form>
  </div>
</div>
