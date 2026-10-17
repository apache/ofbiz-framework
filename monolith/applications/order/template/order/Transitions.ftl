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

<#if inProcess??>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderProcessingStatus}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <table class="basic-table" cellspacing='0'>
      <tr>
        <td>
          <!-- Suspended Processes -->
          <#if "WF_SUSPENDED" == workEffortStatus>
            <form action="<@ofbizUrl>releasehold</@ofbizUrl>" method="post" name="activityForm">
              <input type="hidden" name="workEffortId" value="${workEffortId}" />
              <table class="basic-table" cellspacing='0'>
                <tr>
                  <td>${uiLabelMap.OrderProcessingInHold}&nbsp;${uiLabelMap.OrderProcessingInHoldNote}</td>
                  <td align="right" valign="center">
                    <a href="javascript:document.activityForm.submit()" class="buttontext">${uiLabelMap.OrderRelease}</a>
                  </td>
                </tr>
              </table>
            </form>
          </#if>
          <!-- Active Processes -->
          <#if "WF_RUNNING" == workEffortStatus>
            <form action="<@ofbizUrl>holdorder</@ofbizUrl>" method="post" name="activityForm">
              <input type="hidden" name="workEffortId" value="${workEffortId}" />
              <table class="basic-table" cellspacing='0'>
                <tr>
                  <td>${uiLabelMap.OrderProcessingInActive}</td>
                  <td align="right" valign="center">
                    <a href="javascript:document.activityForm.submit()" class="buttontext">${uiLabelMap.OrderHold}</a>
                  </td>
                </tr>
              </table>
            </form>
          </#if>
        </td>
      </tr>
    </table>
  </div>
</div>
</#if>
<br />
