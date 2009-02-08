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

<#assign username = requestParameters.USERNAME?default((sessionAttributes.autoUserLogin.userLoginId)?default(""))>

<center>
<div class="screenlet login-screenlet">
  <div class="screenlet-title-bar">
    <h3>${uiLabelMap.CommonPasswordChange}</h3>
  </div>
  <div class="screenlet-body">
    <form method="post" action="<@ofbizUrl>login</@ofbizUrl>" name="loginform">
      <input type="hidden" name="requirePasswordChange" value="Y"/>
      <input type="hidden" name="USERNAME" value="${username}"/>
      <table cellspacing="0">
        <tr>
          <td class="label">${uiLabelMap.CommonUsername}</td>
          <td>${username}</td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonCurrentPassword}</td>
          <td><input type="password" name="PASSWORD" value="" size="20"/></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonNewPassword}</td>
          <td><input type="password" name="newPassword" value="" size="20"/></td>
        </tr>
        <tr>
          <td class="label">${uiLabelMap.CommonNewPasswordVerify}</td>
          <td><input type="password" name="newPasswordVerify" value="" size="20"/></td>
        </tr>
        <tr>
          <td colspan="2" align="center">
            <input type="submit" value="${uiLabelMap.CommonSubmit}"/>
          </td>
        </tr>
      </table>
    </form>
  </div>
</div>
</center>

<script language="JavaScript" type="text/javascript">
  document.loginform.PASSWORD.focus();
</script>
