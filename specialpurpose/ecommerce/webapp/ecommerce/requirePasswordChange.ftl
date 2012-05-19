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

<#assign username = ""/>
<#if requestParameters.USERNAME?has_content>
  <#assign username = requestParameters.USERNAME/>
<#elseif autoUserLogin?has_content>
  <#assign username = autoUserLogin.userLoginId/>
</#if>

<h1>${uiLabelMap.CommonLogin}</h1>
<br />

<div style="float: center; width: 49%; margin-right: 5px; text-align: center;" class="screenlet">
    <div class="screenlet-title-bar">
        <div class="h3">${uiLabelMap.CommonPasswordChange}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
      <form method="post" action="<@ofbizUrl>login${previousParams}</@ofbizUrl>" name="loginform">
          <input type="hidden" name="requirePasswordChange" value="Y"/>
          <input type="hidden" name="USERNAME" value="${username}"/>
          <div>
              ${uiLabelMap.CommonUsername}:&nbsp;${username}
          </div>
          <#if autoUserLogin?has_content>
              <div>
                  (${uiLabelMap.CommonNot}&nbsp;${autoUserLogin.userLoginId}?&nbsp;<a href="<@ofbizUrl>${autoLogoutUrl}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClickHere}</a>)
              </div>
          </#if>
          <div>
              ${uiLabelMap.CommonPassword}:&nbsp;
              <input type="password" class="inputBox" name="PASSWORD" value="" size="20"/>
          </div>
          <div>
              ${uiLabelMap.CommonNewPassword}:&nbsp;
              <input type="password" class="inputBox" name="newPassword" value="" size="20"/>
          </div>
          <div>
              ${uiLabelMap.CommonNewPasswordVerify}:&nbsp;
              <input type="password" class="inputBox" name="newPasswordVerify" value="" size="20"/>
          </div>
          <div>
              <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonLogin}"/>
          </div>
      </form>
    </div>
</div>

<script language="JavaScript" type="text/javascript">
  <#if autoUserLogin?has_content>document.loginform.PASSWORD.focus();</#if>
  <#if !autoUserLogin?has_content>document.loginform.USERNAME.focus();</#if>
</script>
