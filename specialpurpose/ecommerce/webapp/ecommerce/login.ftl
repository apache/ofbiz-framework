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

<h1>${uiLabelMap.CommonLogin}</h1>
<div class="screenlet">
  <h3>${uiLabelMap.CommonRegistered}</h3>
  <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform">
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

<div class="screenlet">
  <h3>${uiLabelMap.CommonForgotYourPassword}?</h3>
  <form method="post" action="<@ofbizUrl>forgotpassword</@ofbizUrl>">
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
</div>
<div class="endcolumns">&nbsp;</div>
</div>

<script language="JavaScript" type="text/javascript">
  <#if autoUserLogin?has_content>document.loginform.PASSWORD.focus();</#if>
  <#if !autoUserLogin?has_content>document.loginform.USERNAME.focus();</#if>
</script>
