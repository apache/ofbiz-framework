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

<#assign shoppingCart = sessionAttributes.shoppingCart?if_exists>
<#if shoppingCart?has_content>
    <#assign shoppingCartSize = shoppingCart.size()>
<#else>
    <#assign shoppingCartSize = 0>
</#if>

<div class="head1">${uiLabelMap.CommonLogin}</div>
<br/>

<div>
  <div style="float: left; width: 49%; margin-right: 5px; text-align: center;">
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">${uiLabelMap.CommonRegistered}</div>
        </div>
        <div class="screenlet-body" style="text-align: center;">
          <form method="post" action="<@ofbizUrl>login${previousParams}</@ofbizUrl>" name="loginform">
              <div class="tabletext">
                  ${uiLabelMap.CommonUsername}:&nbsp;
                  <input type="text" class="inputBox" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>" size="20"/>
              </div>
              <#if autoUserLogin?has_content>
                  <div class="tabletext">
                      (${uiLabelMap.CommonNot}&nbsp;${autoUserLogin.userLoginId}?&nbsp;<a href="<@ofbizUrl>${autoLogoutUrl}</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClickHere}</a>)
                  </div>
              </#if>
              <div class="tabletext">
                  ${uiLabelMap.CommonPassword}:&nbsp;
                  <input type="password" class="inputBox" name="PASSWORD" value="" size="20"/>
              </div>
              <div class="tabletext">
                  <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonLogin}"/>
              </div>
          </form>
        </div>
    </div>

    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">${uiLabelMap.CommonForgotYourPassword}?</div>
        </div>
        <div class="screenlet-body" style="text-align: center;">
          <form method="post" action="<@ofbizUrl>forgotpassword${previousParams}</@ofbizUrl>" name="forgotpassword" style="margin: 0;">
            <span class="tabletext">${uiLabelMap.CommonUsername}:&nbsp;</span><input type="text" size="20" class="inputBox" name="USERNAME" value="<#if requestParameters.USERNAME?has_content>${requestParameters.USERNAME}<#elseif autoUserLogin?has_content>${autoUserLogin.userLoginId}</#if>"/>
            <div><input type="submit" name="GET_PASSWORD_HINT" class="smallSubmit" value="${uiLabelMap.CommonGetPasswordHint}"/>&nbsp;<input type="submit" name="EMAIL_PASSWORD" class="smallSubmit" value="${uiLabelMap.CommonEmailPassword}"/></div>
          </form>
        </div>
    </div>

  </div>
  <div style="float: right; width: 49%; margin-left: 5px; text-align: center;">
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">${uiLabelMap.CommonNewUser}</div>
        </div>
        <div class="screenlet-body" style="text-align: center;">
          <form method="post" action="<@ofbizUrl>newcustomer${previousParams}</@ofbizUrl>" style="margin: 0;">
            <div class="tabletext">${uiLabelMap.CommonMayCreateNewAccountHere}:</div>
            <div><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonMayCreate}"/></div>
          </form>
        </div>
    </div>

    <#if (shoppingCartSize > 0)>
    <div class="screenlet">
        <div class="screenlet-header">
            <div class="boxhead">${uiLabelMap.CommonCheckoutAnonymous}</div>
        </div>
        <div class="screenlet-body">
          <div class="tabletext" align="center">${uiLabelMap.CommonCheckoutAnonymousMsg}:</div>
          <form method="post" action="<@ofbizUrl>setCustomer</@ofbizUrl>" style="margin: 0;">
            <div align="center"><input type="submit" class="smallSubmit" value="Checkout"/></div>
          </form>
          <form method="post" action="<@ofbizUrl>quickAnonSetCustomer</@ofbizUrl>" style="margin: 0;">
            <div align="center"><input type="submit" class="smallSubmit" value="Quick Checkout"/></div>
          </form>
        </div>
    </div>
    </#if>
  </div>
  <div class="endcolumns">&nbsp;</div>
</div>

<script language="JavaScript" type="text/javascript">
  <#if autoUserLogin?has_content>document.loginform.PASSWORD.focus();</#if>
  <#if !autoUserLogin?has_content>document.loginform.USERNAME.focus();</#if>
</script>
