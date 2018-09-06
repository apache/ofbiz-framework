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
<div class="screenlet">
    <div class="screenlet-title-bar">
      <ul>
        <li class="h3">${uiLabelMap.OrderSendConfirmationEmail}</li>
      </ul>
      <br class="clear"/>
    </div>
    <div class="screenlet-body">
      <a href="<@ofbizUrl>authview/${donePage}?orderId=${orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSend}</a>      
      <br />
      <form method="post" action="<@ofbizUrl>sendconfirmationmail/${donePage}</@ofbizUrl>" name="sendConfirmationForm">
        <input type="hidden" name="orderId" value="${orderId!}" />
        <#if ! productStoreEmailSetting??>
            <#assign productStoreEmailSetting = {} />
        </#if>
        <input type="hidden" name="partyId" value="${partyId!}" />
        <input type="hidden" name="contentType" value="${productStoreEmailSetting.contentType?default("")}" />
        <table class="basic-table" cellspacing='0'>
            <tr>
                <td width="2%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailSubject}&nbsp;</td>
                <td width="54%">
                    <input type="text" size="40" name="subject" value="${productStoreEmailSetting.subject?default(uiLabelMap.OrderOrderConfirmation + " " + uiLabelMap.OrderNbr + orderId)?replace("\\$\\{orderId\\}",orderId,"r")}" />
                </td>
            </tr>
            </tr>
                <td width="26%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailSendTo}&nbsp;</td>
                <td width="54%">
                    <input type="text" size="40" name="sendTo" value="${sendTo}"/>
                </td>
            <tr>
            </tr>
            <tr>
                <td width="26%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailCCTo}&nbsp;</td>
                <td width="54%">
                    <input type="text" size="40" name="sendCc" value="${productStoreEmailSetting.ccAddress?default("")}" />
                </td>
            </tr>
            <tr>
                <td width="26%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailBCCTo}&nbsp;</td>
                <td width="54%">
                    <input type="text" size="40" name="sendBcc" value="${productStoreEmailSetting.bccAddress?default("")}" />
                </td>
            </tr>
            <tr>
                <td width="26%" align="right" class="label">${uiLabelMap.CommonFrom}&nbsp;</td>
                <td width="54%">
                    <#if productStoreEmailSetting.fromAddress??>
                        <input type="hidden" name="sendFrom" value="${productStoreEmailSetting.fromAddress}" />
                        ${productStoreEmailSetting.fromAddress}
                    <#else>
                        <input type="text" size="40" name="sendFrom" value="" />
                    </#if>
                </td>
            <tr>
            <tr>
                <td width="26%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailContentType}&nbsp;</td>
                <td width="54%">${productStoreEmailSetting.contentType?default("text/html")}</td>
            </tr>
            <tr>
                <td width="26%" align="right" class="label">${uiLabelMap.OrderSendConfirmationEmailBody}&nbsp;</td>
                <td width="54%">
                    <textarea name="body" rows="30" cols="80">${screens.render(productStoreEmailSetting.bodyScreenLocation?default(""))}</textarea>
                </td>
            </tr>
        </table>
      </form>
      <br />
      <a href="<@ofbizUrl>authview/${donePage}?orderId=${orderId}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonGoBack}</a>
      <a href="javascript:document.sendConfirmationForm.submit()" class="buttontext">${uiLabelMap.CommonSend}</a>
    </div>
</div>
<#else>
  <h3>${uiLabelMap.OrderViewPermissionError}</h3>
</#if>