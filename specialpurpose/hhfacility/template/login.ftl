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
<#assign useMultitenant = Static["org.ofbiz.base.util.UtilProperties"].getPropertyValue("general.properties", "multitenant")>
<#assign username = requestParameters.USERNAME?default((sessionAttributes.autoUserLogin.userLoginId)?default(""))>
<#if username != "">
  <#assign focusName = false>
<#else>
  <#assign focusName = true>
</#if>

<form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform" data-ajax="false">
  <div data-role="fieldcontainer">
    <label for="USERNAME">${uiLabelMap.CommonUsername}</label>
    <input type="text" id="USERNAME" name="USERNAME" value="${username}" size="20"/>
  </div>
  <div data-role="fieldcontainer">
    <label for="PASSWORD">${uiLabelMap.CommonPassword}</label>
    <input type="password" id="PASSWORD" name="PASSWORD" value="" size="20" />
  </div>
  <#if ("Y" == useMultitenant)>
    <div data-role="fieldcontainer">
      <label for="tenantId">${uiLabelMap.CommonTenantId}</label>
      <input type="text" id="tenantId" name="userTenantId" value="${parameters.userTenantId!}" size="20"/>
    </div>
  </#if>
  <input type="submit" value="${uiLabelMap.CommonLogin}" class="loginButton" />
</form>

<script language="JavaScript" type="text/javascript">
  document.loginform.JavaScriptEnabled.value = "Y";
  <#if focusName>
    document.loginform.USERNAME.focus();
  <#else>
    document.loginform.PASSWORD.focus();
  </#if>
</script>
