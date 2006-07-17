<#--

Copyright 2001-2006 The Apache Software Foundation

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

<#if security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>  

 <p class="head1">${uiLabelMap.OrderSendConfirmationEmail}</p>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>

  <form method="post" action="<@ofbizUrl>sendconfirmationmail/${donePage}</@ofbizUrl>" name="sendConfirmationForm">
    <input type="hidden" name="partyId" value="${partyId?if_exists}">
    <table width="90%" border="0" cellpadding="2" cellspacing="0">
     <tr>
        <td width="26%" align="right">
           <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailSendTo}</div>
        </td>
        <td width="54%">
          <input type="text" name="sendTo" value="${sendTo}"/>
        </td>
      </tr> 
       <tr>
        <td width="26%" align="right">
            <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailCCTo}</div>
        </td>
        <td width="54%">
          <input type="text" name="sendCc" value="" />
        </td>
      </tr>
      <tr>
        <td width="26%" align="right">
             <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailNote}</div>
        </td>
        <td width="54%">
          <textarea name="note" class="textAreaBox" rows="5" cols="70"></textarea>
        </td>
      </tr> 
    </table>
  </form>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
  
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
