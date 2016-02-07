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
<#assign janrainEnabled = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("ecommerce.properties", "janrain.enabled")>
<#assign appName = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("ecommerce.properties", "janrain.appName")>
<#if janrainEnabled == "Y">
<script type="text/javascript">
(function() {
    if (typeof window.janrain !== 'object') window.janrain = {};
    window.janrain.settings = {};
    
    janrain.settings.tokenUrl = '<@ofbizUrl fullPath="true" secure="true">janrainCheckLogin</@ofbizUrl>';

    function isReady() { janrain.ready = true; };
    if (document.addEventListener) {
      document.addEventListener("DOMContentLoaded", isReady, false);
    } else {
      window.attachEvent('onload', isReady);
    }

    var e = document.createElement('script');
    e.type = 'text/javascript';
    e.id = 'janrainAuthWidget';

    if (document.location.protocol === 'https:') {
      e.src = 'https://rpxnow.com/js/lib/${appName}/engage.js';
    } else {
      e.src = 'http://widget-cdn.rpxnow.com/js/lib/${appName}/engage.js';
    }

    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(e, s);
})();
</script>
<h1>${uiLabelMap.CommonLogin}</h1>
<div class="screenlet">
  <div class="screenlet-title-bar"><h3>${uiLabelMap.CommonRegistered}</h3></div>
  <div class="screenlet-body">
  <table width="100%" class="Signlogin">
      <tr>
          <td>
          <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform" class="horizontal">
            <fieldset>
              <div>
                <label for="userName">${uiLabelMap.CommonUsername}</label>
                <input type="text" id="userName" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>"/>
              </div>
              <#if autoUserLogin?has_content>
                <p>(${uiLabelMap.CommonNot} ${autoUserLogin.userLoginId}? <a href="<@ofbizUrl>${autoLogoutUrl}</@ofbizUrl>">${uiLabelMap.CommonClickHere}</a>)</p>
              </#if>
              <div>
                <label for="password">${uiLabelMap.CommonPassword}:</label>
                <input type="password" id="password" name="PASSWORD" value=""/>
              </div>
              <div>
                <input type="submit" class="button" value="${uiLabelMap.CommonLogin}"/>
              </div>
              <div>
                <label for="newcustomer_submit">${uiLabelMap.CommonMayCreateNewAccountHere}:</label>
                <a href="<@ofbizUrl>newcustomer</@ofbizUrl>">${uiLabelMap.CommonMayCreate}</a>
              </div>
            </fieldset>
          </form>
          </td>
          <td><div id="janrainEngageEmbed"></div></td>
      </tr>
  </table>
  </div>
</div>
<#else>
<h1>${uiLabelMap.CommonLogin}</h1>
<div class="screenlet">
  <div class="screenlet-title-bar"><h3>${uiLabelMap.CommonRegistered}</h3></div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform" class="horizontal">
      <fieldset>
        <div>
          <label for="userName">${uiLabelMap.CommonUsername}</label>
          <input type="text" id="userName" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>"/>
        </div>
        <#if autoUserLogin?has_content>
          <p>(${uiLabelMap.CommonNot} ${autoUserLogin.userLoginId}? <a href="<@ofbizUrl>${autoLogoutUrl}</@ofbizUrl>">${uiLabelMap.CommonClickHere}</a>)</p>
        </#if>
        <div>
          <label for="password">${uiLabelMap.CommonPassword}:</label>
          <input type="password" id="password" name="PASSWORD" value=""/>
        </div>
        <div>
          <input type="submit" class="button" value="${uiLabelMap.CommonLogin}"/>
        </div>
        <div>
          <label for="newcustomer_submit">${uiLabelMap.CommonMayCreateNewAccountHere}:</label>
          <a href="<@ofbizUrl>newcustomer</@ofbizUrl>">${uiLabelMap.CommonMayCreate}</a>
        </div>
      </fieldset>
    </form>
  </div>
</div>
</#if>

<div class="screenlet">
  <div class="screenlet-title-bar"><h3>${uiLabelMap.CommonForgotYourPassword}</h3></div>
  <div class="screenlet-body">
  <form method="post" action="<@ofbizUrl>forgotpassword</@ofbizUrl>" class="horizontal">
    <div>
      <label for="forgotpassword_userName">${uiLabelMap.CommonUsername}</label>
      <input type="text" id="forgotpassword_userName" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>"/>
    </div>
    <div class="buttons">
      <input type="submit" class="button" name="GET_PASSWORD_HINT" value="${uiLabelMap.CommonGetPasswordHint}"/>
      <input type="submit" class="button" name="EMAIL_PASSWORD" value="${uiLabelMap.CommonEmailPassword}"/>
    </div>
  </form>
  </div>
</div>
<#--    
<div class="screenlet">
  <h3>${uiLabelMap.CommonNewUser}</h3>
  <form method="post" action="<@ofbizUrl>newcustomer</@ofbizUrl>">
    <div>
      <label for="newcustomer_submit">${uiLabelMap.CommonMayCreateNewAccountHere}:</p>
      <input type="submit" class="button" id="newcustomer_submit" value="${uiLabelMap.CommonMayCreate}"/>
    <div>
  </form>
</div>
-->
<div class="endcolumns">&nbsp;</div>

<script language="JavaScript" type="text/javascript">
  <#if autoUserLogin?has_content>document.loginform.PASSWORD.focus();</#if>
  <#if !autoUserLogin?has_content>document.loginform.USERNAME.focus();</#if>
</script>
