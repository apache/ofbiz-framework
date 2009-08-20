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
  <h2>${uiLabelMap.PartyChangePassword}</h2>

    <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="button">[${uiLabelMap.CommonGoBack}]</a>
    <a href="javascript:document.changepasswordform.submit()" class="button">[${uiLabelMap.CommonSave}]</a>

  <form method="post" action="<@ofbizUrl>updatePassword/${donePage}</@ofbizUrl>" name="changepasswordform">
  <table width="90%" border="0" cellpadding="2" cellspacing="0" class="tabletext">
    <tr>
      <td align="right">${uiLabelMap.PartyOldPassword}</td>
      <td>
        <input type="password" class='inputBox' name="currentPassword" size="20" maxlength="20"/>
      *</td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyNewPassword}</td>
      <td>
        <input type="password" class='inputBox' name="newPassword" size="20" maxlength="20"/>
      *</td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyNewPasswordVerify}</td>
      <td>
        <input type="password" class='inputBox' name="newPasswordVerify" size="20" maxlength="20"/>
      *</td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyPasswordHint}</td>
      <td>
        <input type="text" class='inputBox' size="40" maxlength="100" name="passwordHint" value="${userLoginData.passwordHint?if_exists}"/>
      </td>
    </tr>
  </table>
  </form>
<div class="tabletext">${uiLabelMap.CommonFieldsMarkedAreRequired}</div>

    <a href="<@ofbizUrl>authview/${donePage}</@ofbizUrl>" class="button">[${uiLabelMap.CommonGoBack}]</a>
    <a href="javascript:document.changepasswordform.submit()" class="button">[${uiLabelMap.CommonSave}]</a>
