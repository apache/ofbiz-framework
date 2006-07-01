<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      2.1
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
          <form method="post" action="<@ofbizUrl>finalizeOrder</@ofbizUrl>" style="margin: 0;">
            <div class="tabletext" align="center">${uiLabelMap.CommonCheckoutAnonymousMsg}:</div>
            <div align="center"><input type="submit" class="smallSubmit" value="Checkout"/></div>
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
