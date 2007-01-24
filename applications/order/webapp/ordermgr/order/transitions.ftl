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

<#if inProcess?exists>
  <table border='0' width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
          <tr>
            <td valign="middle" align="left">
              <div class="boxhead">&nbsp;${uiLabelMap.OrderProcessingStatus}</div>
            </td>         
          </tr>
        </table>
      </td>
    </tr>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
          <tr>
            <td>
              <!-- Suspended Processes -->
              <#if workEffortStatus == "WF_SUSPENDED">
                <form action="<@ofbizUrl>releasehold</@ofbizUrl>" method="post" name="activityForm">
                  <input type="hidden" name="workEffortId" value="${workEffortId}">                        
                  <table width="100%">
                    <tr>
                      <td>
                        <div class="tabletext">${uiLabelMap.OrderProcessingInHold}</div>
                        <div class="tabletext">&nbsp;${uiLabelMap.OrderProcessingInHoldNote}</div>                     
                      </td>
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
                  <input type="hidden" name="workEffortId" value="${workEffortId}">                        
                  <table width="100%">
                    <tr>
                      <td>
                        <div class="tabletext">${uiLabelMap.OrderProcessingInActive}</div>                    
                      </td>
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
      </td>
    </tr>
  </table>  
</#if>
<br/>
<#if wfTransitions?exists && wfTransitions?has_content>
  <table border='0' width='100%' cellspacing='0' cellpadding='0' class='boxoutside'>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxtop'>
          <tr>
            <td valign="middle" align="left">
              <div class="boxhead">&nbsp;${uiLabelMap.OrderProcessingTransitions}</div>
            </td>         
          </tr>
        </table>
      </td>
    </tr>
    <tr>
      <td width='100%'>
        <table width='100%' border='0' cellspacing='0' cellpadding='0' class='boxbottom'>
          <tr>
            <td>
              <form action="<@ofbizUrl>completeassignment</@ofbizUrl>" method="post" name="transitionForm">
                <input type="hidden" name="workEffortId" value="${workEffortId}">
                <input type="hidden" name="partyId" value="${assignPartyId}">
                <input type="hidden" name="roleTypeId" value="${assignRoleTypeId}">
                <input type="hidden" name="fromDate" value="${fromDate}">             
                <table>
                  <tr>
                    <td>
                      <select name="approvalCode" class="selectBox">
                        <#list wfTransitions as trans>
                          <#if trans.extendedAttributes?has_content>
                            <#assign attrs = Static["org.ofbiz.base.util.StringUtil"].strToMap(trans.extendedAttributes)>
                            <#if attrs.approvalCode?exists>
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
      </td>
    </tr>
  </table>
</#if>

