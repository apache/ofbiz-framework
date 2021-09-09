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

<#if requestAttributes.uiLabelMap??><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign useMultitenant = Static["org.apache.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", "multitenant")>

<#assign username = requestParameters.USERNAME?default((sessionAttributes.autoUserLogin.userLoginId)?default(""))>
<#if username != "">
  <#assign focusName = false>
<#else>
  <#assign focusName = true>
</#if>
  <div id="loginBar">
    <div id="company-logo"></div>
  </div>

  <div class="screenlet login-screenlet">
    <h3>${uiLabelMap.CommonBeLogged}</h3>
    <div class="screenlet-body">
      <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform">
        <label>
          ${uiLabelMap.CommonUsername}
          <input type="text" name="USERNAME" value="${username}"/>
        </label>
        
        <label>
          <span>
            ${uiLabelMap.CommonPassword}
            <a href="<@ofbizUrl>forgotPassword</@ofbizUrl>">${uiLabelMap.CommonForgotYourPassword}</a>
          </span>
          <input type="password" name="PASSWORD" autocomplete="off" value=""/>
        </label>

        <#if ("Y" == useMultitenant) >
          <#if !requestAttributes.userTenantId??>
          <label>
            ${uiLabelMap.CommonTenantId}
            <input type="text" name="userTenantId" value="${parameters.userTenantId!}"/>
          </label>
          <#else>
          <input type="hidden" name="userTenantId" value="${requestAttributes.userTenantId!}"/>
          </#if>
        </#if>

        <input type="submit" value="${uiLabelMap.CommonLogin}"/>
        <input type="hidden" name="JavaScriptEnabled" value="N"/>
        
      </form>
    </div>
  </div>

<script type="application/javascript">
  document.loginform.JavaScriptEnabled.value = "Y";
  <#if focusName>
    document.loginform.USERNAME.focus();
  <#else>
    document.loginform.PASSWORD.focus();
  </#if>
</script>
