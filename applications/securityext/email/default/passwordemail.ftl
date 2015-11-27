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

<html>
<head>
</head>
<body>
  <div>${uiLabelMap.SecurityExtThisEmailIsInResponseToYourRequestToHave} <#if useEncryption>${uiLabelMap.SecurityExtANew}<#else>${uiLabelMap.SecurityExtYour}</#if> ${uiLabelMap.SecurityExtPasswordSentToYou}.</div>
  <br />
  <div>
      <form method="post" action="${baseEcommerceSecureUrl}/partymgr/control/passwordChange?USERNAME=${userLogin.userLoginId!}&password=${password!}&forgotPwdFlag=true&tenantId=${tenantId!}" name="loginform" id="loginform" target="_blank">
      <#--form method="post" action="${baseEcommerceSecureUrl}/partymgr/control/passwordChange" name="loginform" id="loginform" target="_blank">
        <input type="hidden" name="USERNAME" value="${userLogin.userLoginId!}" />
        <input type="hidden"  name="password" value="${password!}" />
        <input type="hidden"  name="tenantId" value="${tenantId!}" />
        <input type="hidden" name="forgotPwdFlag" value="true" /--><#-- see OFBIZ-4983 -->
        <input type="submit" name="submit" value="${uiLabelMap.ResetPassword}" />
      </form>
  </div>
</body>
</html>
