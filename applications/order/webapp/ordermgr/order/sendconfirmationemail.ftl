<#--
 *  Copyright (c) 2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Tim Chen (timchen_sh@hotmail.com)
 *@version    $Rev$
 *@since      1.0
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
