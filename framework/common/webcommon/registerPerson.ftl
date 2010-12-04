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
<#assign useCaptcha = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", "login.newRegistration.useCaptcha")>

<script type="text/javascript" language="JavaScript"> <!--
  dojo.require("dojo.widget.*");
  dojo.require("dojo.event.*");
  dojo.require("dojo.io.*");
                         
  function reloadCaptcha(){
    var submitToUri = "<@ofbizUrl>reloadCaptchaImage</@ofbizUrl>";
    dojo.io.bind({url: submitToUri,
    load: function(type, data, evt){
      if(type == "load"){
        document.getElementById("captchaImage").innerHTML = data;
        reloadCaptchaCode();
      }
    },mimetype: "text/html"});
  }
  function reloadCaptchaCode(){
    var submitToUri = "<@ofbizUrl>reloadCaptchaCode</@ofbizUrl>";
    dojo.io.bind({url: submitToUri,
      load: function(type, data, evt){
        if(type == "load"){
          document.getElementById("captchaCode").innerHTML = data;
        }
      },mimetype: "text/html"});
    }
//--></script>

<form name="RegisterPerson" onsubmit="javascript:submitFormDisableSubmits(this)" class="basic-form" id="RegisterPerson" action="createRegister" method="post">
  <input type="hidden" value="${webSiteId}" name="webSiteId"/>
  <input type="hidden" name="reload"/>
  <div id="captchaCode"><input type="hidden" value="${parameters.ID_KEY}" name="captchaCode"/></div>
  <table cellspacing="0" class="basic-table">
    <tbody>
      <tr>
        <td class="label">${uiLabelMap.CommonWhyRegister}</td>
        <td>
          <textarea id="RegisterPerson_whyWouldYouLikeToRegister" rows="5" cols="60" class="class="no-required"" name="whyWouldYouLikeToRegister">${requestParameters.whyWouldYouLikeToRegister?if_exists}</textarea>
        </td>
      </tr>
      <tr>
        <td class="label">Salutation</td>
        <td class="no-required">
          <input type="text" id="RegisterPerson_salutation" maxlength="60" size="40" name="salutation" value="${requestParameters.salutation?if_exists}"/>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonFirstName}</td>
        <td>
          <input type="text" id="RegisterPerson_firstName" maxlength="60" size="40" class="required" name="firstName" value="${requestParameters.firstName?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonMiddleName}</td>
        <td class="no-required">
          <input type="text" id="RegisterPerson_middleName" maxlength="60" size="40" name="middleName" value="${requestParameters.middleName?if_exists}"/>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonLastName}</td>
        <td>
          <input type="text" id="RegisterPerson_lastName" maxlength="60" size="40" class="required" name="lastName" value="${requestParameters.lastName?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonEmail}</td>
        <td>
          <input type="text" id="RegisterPerson_USER_EMAIL" maxlength="250" size="60" class="required" name="USER_EMAIL" value="${requestParameters.USER_EMAIL?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonUsername}</td>
        <td>
          <input type="text" id="RegisterPerson_USERNAME" maxlength="250" size="30" class="required" name="USERNAME" value="${requestParameters.USERNAME?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonPassword}</td>
        <td>
          <input type="password" id="RegisterPerson_PASSWORD" maxlength="250" size="15" name="PASSWORD" class="required" value="${requestParameters.PASSWORD?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <tr>
        <td class="label">${uiLabelMap.CommonPasswordVerify}</td>
        <td>
          <input type="password" id="RegisterPerson_CONFIRM_PASSWORD" maxlength="250" size="15" name="CONFIRM_PASSWORD" class="required" value="${requestParameters.CONFIRM_PASSWORD?if_exists}"/>
          <span class="tooltip">${uiLabelMap.CommonRequired}</span>
        </td>
      </tr>
      <#if ("Y" == useCaptcha)>
        <tr class="captcha">
          <td class="label">${uiLabelMap.CommonVerifyCaptcha}</td>
          <td>
            <span id="captchaImage"><img src="${parameters.captchaFileName}" alt="" /></span>
            <input type="text" id="RegisterPerson_captcha" maxlength="30" size="23" class="required" name="captcha"/>
            <div><a href="javascript:reloadCaptcha();" class="buttontext refresh">${uiLabelMap.CommonReloadImage}</a></div>
          </td>
        </tr>
      </#if>  
      <tr>
        <td></td>
        <td>
          <input type="submit" value="Save" name="submitButton"/>
        </td>
      </tr>
    </tbody>
  </table>
</form>