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
          <#if workEffortStatus == "WF_SUSPENDED">
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
          <#if workEffortStatus == "WF_RUNNING">
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
<#if wfTransitions?? && wfTransitions?has_content>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.OrderProcessingTransitions}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <table class="basic-table" cellspacing='0'>
      <tr>
        <td>
          <form action="<@ofbizUrl>completeassignment</@ofbizUrl>" method="post" name="transitionForm">
            <input type="hidden" name="workEffortId" value="${workEffortId}" />
            <input type="hidden" name="partyId" value="${assignPartyId}" />
            <input type="hidden" name="roleTypeId" value="${assignRoleTypeId}" />
            <input type="hidden" name="fromDate" value="${fromDate}" />
            <table class="basic-table" cellspacing='0'>
              <tr>
                <td>
                  <select name="approvalCode">
                    <#list wfTransitions as trans>
                      <#if trans.extendedAttributes?has_content>
                        <#assign attrs = Static["org.apache.ofbiz.base.util.StringUtil"].strToMap(trans.extendedAttributes)>
                        <#if attrs.approvalCode??>
                          <option value="${attrs.approvalCode}">${trans.transitionName}</option>
                        </#if>
                      </#if>
                    </#list>
                  </select>
                </td>
                <td valign="center">
                  <a href="javascript:document.transitionForm.submit()" class="buttontext">${uiLabelMap.CommonContinue}</a>
                </td>
              </tr>
            </table>
          </form>
        </td>
      </tr>
    </table>
</#if>