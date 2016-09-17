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
    <h3>${uiLabelMap.CommonContactUs}</h3>
  </div>
  <script type="text/javascript" language="JavaScript">
    <!--
    function reloadCaptcha(fieldName) {
      var captchaUri = "<@ofbizUrl>captcha.jpg?captchaCodeId=" + fieldName + "&amp;unique=_PLACEHOLDER_</@ofbizUrl>";
      var unique = Date.now();
      captchaUri = captchaUri.replace("_PLACEHOLDER_", unique);
      document.getElementById(fieldName).src = captchaUri;
    }
    //-->
  </script>
  <div class="screenlet-body">
    <form id="contactForm" method="post" action="<@ofbizUrl>submitAnonContact</@ofbizUrl>">
      <input type="hidden" name="partyIdFrom" value="${(userLogin.partyId)!}"/>
      <input type="hidden" name="partyIdTo" value="${productStore.payToPartyId!}"/>
      <input type="hidden" name="contactMechTypeId" value="WEB_ADDRESS"/>
      <input type="hidden" name="communicationEventTypeId" value="WEB_SITE_COMMUNICATI"/>
      <input type="hidden" name="productStoreId" value="${productStore.productStoreId}"/>
      <input type="hidden" name="emailType" value="CONT_NOTI_EMAIL"/>
      <table class="basic-table" cellspacing="0">
        <tbody>
        <tr>
          <td class="label">${uiLabelMap.EcommerceSubject}</td>
          <td>
            <input type="text" name="subject" id="subject" class="required" value="${requestParameters.subject!}"/>*
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonMessage}</td>
          <td>
            <textarea name="content" id="message" class="required" cols="50" rows="5">
              ${requestParameters.content!}
            </textarea>*
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.FormFieldTitle_emailAddress}</td>
          <td>
            <input type="text" name="emailAddress" id="emailAddress" class="required"
                value="${requestParameters.emailAddress!}"/>*
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyFirstName}</td>
          <td>
            <input type="text" name="firstName" id="firstName" class="required" value="${requestParameters.firstName!}"/>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.PartyLastName}</td>
          <td>
            <input type="text" name="lastName" id="lastName" class="required" value="${requestParameters.lastName!}"/>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonCaptchaCode}</td>
          <td>
            <div>
              <img id="captchaImage"
                  src="<@ofbizUrl>captcha.jpg?captchaCodeId=captchaImage&amp;unique=${nowTimestamp.getTime()}</@ofbizUrl>"
                  alt=""/>
            </div>
            <a href="javascript:reloadCaptcha('captchaImage');">${uiLabelMap.CommonReloadCaptchaCode}</a>
          </td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonVerifyCaptchaCode}</td>
          <td><input type="text" autocomplete="off" maxlength="30" size="23" name="captcha"/>*</td>
        </tr>
        <tr>
          <td class="label"></td>
          <td><input type="submit" value="${uiLabelMap.CommonSubmit}"/></td>
        </tr>
        </tbody>
      </table>
    </form>
  </div>
</div>
