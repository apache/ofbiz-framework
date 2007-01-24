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

<#if security.hasEntityPermission("ORDERMGR", "_SEND_CONFIRMATION", session)>  

 <p class="head1">${uiLabelMap.OrderSendConfirmationEmail}</p>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSend}</a>

  <form method="post" action="<@ofbizUrl>sendconfirmationmail/${donePage}</@ofbizUrl>" name="sendConfirmationForm">
    <#if ! productStoreEmailSetting?exists>
        <#assign productStoreEmailSetting = {} />
    </#if>
    <input type="hidden" name="partyId" value="${partyId?if_exists}">
    <input type="hidden" name="contentType" value="${productStoreEmailSetting.contentType?default("")}" />
    <table width="90%" border="0" cellpadding="2" cellspacing="0">
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailSubject}</div>
            </td>
            <td width="54%">
                <input type="text" size="40" name="subject" value="${productStoreEmailSetting.subject?default(uiLabelMap.OrderConfirmation + " " + uiLabelMap.OrderNbr + orderId)?replace("\\$\\{orderId\\}",orderId,"r")}" />
            </td>
        </tr>
        </tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailSendTo}</div>
            </td>
            <td width="54%">
                <input type="text" size="40" name="sendTo" value="${sendTo}"/>
            </td>
        <tr>
        </tr> 
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailCCTo}</div>
            </td>
            <td width="54%">
                <input type="text" size="40" name="sendCc" value="${productStoreEmailSetting.ccAddress?default("")}" />
            </td>
        </tr>
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailBCCTo}</div>
            </td>
            <td width="54%">
                <input type="text" size="40" name="sendBcc" value="${productStoreEmailSetting.bccAddress?default("")}" />
            </td>
        </tr>
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.CommonFrom}</div>
            </td>
            <td width="54%">
                <#if productStoreEmailSetting.fromAddress?exists>
                    <input type="hidden" name="sendFrom" value="${productStoreEmailSetting.fromAddress}" />
                <#else>
                    <input type="text" size="40" name="sendFrom" value="" />
                </#if>
            </td>
        <tr>
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailContentType}</div>
            </td>
            <td width="54%">
                <div class="tabletext">${productStoreEmailSetting.contentType?default("text/html")}</div>
            </td>
        </tr>
        <tr>
            <td width="26%" align="right">
                <div class="tabletext">${uiLabelMap.OrderSendConfirmationEmailBody}</div>
            </td>
            <td width="54%">
                <textarea name="body" class="textAreaBox" rows="30" cols="80">${screens.render(productStoreEmailSetting.bodyScreenLocation?default(""))}</textarea>
            </td>
        </tr> 
    </table>
</form>

  &nbsp;<a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSend}</a>
  
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>
