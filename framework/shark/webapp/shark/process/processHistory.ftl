<#--
$Id: processHistory.ftl 7426 2006-04-26 23:35:58Z jonesde $

Copyright 2004-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->

<div class="head2">Process History</div>
<#if historyList?has_content>
  <div>&nbsp;</div>
  <div class="tabletext"><b>Process :</b> ${process.name()} - ${process.description()?default("N/A")} [${process.key()}]</div>
  <table cellpadding="2" cellspacing="0" border="1">
    <tr>
      <td><div class="tableheadtext">Time</div></td>
      <td><div class="tableheadtext">Event</div></td>
    </tr>
    <#list historyList as history>
      <#assign time = history.time_stamp().getTime()>
      <tr>
        <td align="left"><div class="tabletext">${Static["org.ofbiz.base.util.UtilDateTime"].getTimestamp(time)}</div></td>
        <td align="left">
          <div class="tabletext">
            <#assign eventType = history.event_type()>
            ${eventType}
            <#if eventType == "processStateChanged">
              [${history.old_state()} -> ${history.new_state()}]
            </#if>
          </div>
        </td>
      </tr>
    </#list>
  </table>
<#else>
  <div class="tabletext">No history available.</div>
</#if>