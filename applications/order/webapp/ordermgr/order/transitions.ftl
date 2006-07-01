<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.2
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

