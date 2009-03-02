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

<div id="panel">
    <form method="post" action="<@ofbizUrl>PayCreditCard</@ofbizUrl>" name="PayCreditCardForm">
        <table border="0">
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosPayCreditCardFirstName}</b><input type="text" name="firstName" id="firstName" size="20" maxlength="60" value="${requestParameters.firstName?if_exists}"/></td>
                <td><b>${uiLabelMap.WebPosPayCreditCardLastName}</b><input type="text" name="lastName" id="lastName" size="20" maxlength="60" value="${requestParameters.lastName?if_exists}"/></td>
            </tr>
            <tr>
                <td><b>${uiLabelMap.WebPosPayCreditCardNum}</b><input type="text" name="cardNum" id="cardNum" size="20" maxlength="30" value="${requestParameters.cardNum?if_exists}"/></td>
                <td>
                  <b>${uiLabelMap.WebPosPayCreditCardExp}</b>
                  <#assign expMonth = "">
                  <#assign expYear = "">
                  <select name="expMonth">
                    <#assign ccExprMonth = requestParameters.expMonth?if_exists>
                    <#if ccExprMonth?has_content>
                      <option value="${ccExprMonth?if_exists}">${ccExprMonth?if_exists}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccmonths")}
                  </select>
                  <select name="expYear">
                    <#assign ccExprYear = requestParameters.expYear?if_exists>
                    <#if ccExprYear?has_content>
                      <option value="${ccExprYear?if_exists}">${ccExprYear?if_exists}</option>
                    </#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#ccyears")}
                  </select>
                </td>
            </tr>
            <tr>
                <td>
                    <b>${uiLabelMap.WebPosPayCreditCardSecurityCode}</b>
                    <input type="text" name="securityCode" id="securityCode" size="5" maxlength="10" value="${requestParameters.securityCode?if_exists}"/>
                    <input type="hidden" name="postalCode" id="postalCode"/>
                    <input type="hidden" name="track2" id="track2"/>
                </td>
                <td>
                    <b>${uiLabelMap.WebPosPayCreditCardRefNum}</b>
                    <input type="text" name="refNum" id="refNum" value="${requestParameters.refNum?if_exists}"/>
                </td>
            </tr>
            <tr>
                <td>
                    <b>${uiLabelMap.WebPosPayCreditCard}</b>
                    <input type="text" name="amount" id="amount" value="${requestParameters.amount?if_exists}"/>
                </td>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td colspan="2" align="center">
                    <input type="submit" value="${uiLabelMap.CommonConfirm}" name="confirm"/>
                    <input type="submit" value="${uiLabelMap.CommonCancel}"/>
                </td>
            </tr>
        </table>
    </form>
</div>
<script language="javascript" type="text/javascript">
    document.PayCreditCardForm.firstName.focus();
</script>