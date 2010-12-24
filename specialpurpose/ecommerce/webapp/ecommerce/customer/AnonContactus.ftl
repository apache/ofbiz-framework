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

<div class="screenlet">
    <div class="screenlet-title-bar">
        <h3>${uiLabelMap.CommonContactUs}</h3>
    </div>
<#if parameters.person?has_content>
    <#assign person = parameters.person/>
        <div class="screenlet-body">
        <form name="contactForm" method="post" action="<@ofbizUrl>submitAnonContact</@ofbizUrl>">
            <input type="hidden" name="partyIdFrom" value="${(userLogin.partyId)?if_exists}" />
            <input type="hidden" name="partyIdTo" value="${productStore.payToPartyId?if_exists}"/>
            <input type="hidden" name="contactMechTypeId" value="WEB_ADDRESS" />
            <input type="hidden" name="communicationEventTypeId" value="WEB_SITE_COMMUNICATI" />
            <input type="hidden" name="productStoreId" value="${productStore.productStoreId}" />
            <input type="hidden" name="emailType" value="CONT_NOTI_EMAIL" />
            <input type="hidden" name="captchaCode" value="${requestParameters.captchaCode?if_exists}"/>
            <input type="hidden" name="captcha" value="${requestParameters.captcha?if_exists}"/>
            <input type="hidden" name="partyId" value="${person.partyId?if_exists}"/>
            <input type="hidden" name="emailAddress" value="${requestParameters.emailAddress?if_exists}"/>
            <table class="basic-table" cellspacing="0">
                <tbody>
                    <tr>
                       <td class="label">${uiLabelMap.EcommerceSubject}</td>
                       <td><input type="text" name="subject" id="subject" class="required" value="${requestParameters.subject?if_exists}"/>*</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.CommonMessage}</td>
                       <td><textarea name="content" id="message" class="required" cols="50" rows="5">${requestParameters.content?if_exists}</textarea>*</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.FormFieldTitle_emailAddress}</td>
                       <td>${requestParameters.emailAddress?if_exists} (${uiLabelMap.CommonEmailAlreadyExist})</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.CommonFrom}</td>
                       <td>${person.firstName?if_exists} ${person.lastName?if_exists} (${uiLabelMap.FormFieldTitle_existingCustomer})</td>
                    </tr>
                    <tr>
                       <td class="label"></td>
                       <td><a class="smallsubmit" href="javascript: void(0)" onclick="document.contactForm.submit();">${uiLabelMap.CommonConfirm}</a><a href="<@ofbizUrl>AnonContactus</@ofbizUrl>" class="smallsubmit">${uiLabelMap.CommonCancel}</a></td>
                    </tr>
                </tbody>
            </table>
        </form>
    </div>
<#else>
<script type="text/javascript" language="JavaScript">
<!--
    function reloadCaptcha(){
        var submitToUri = "<@ofbizUrl>reloadCaptchaImage</@ofbizUrl>";
        $.post(submitToUri, null,
        function(data){
            document.getElementById("captchaImage").innerHTML = data;
        });
        reloadCaptchaCode();
    }
    function reloadCaptchaCode(){
        var submitToUri = "<@ofbizUrl>reloadCaptchaCode</@ofbizUrl>";
        $.post(submitToUri, null,
        function(data){
            document.getElementById("captchaCode").innerHTML = data;
        });
    }
    //-->
</script>
    <div class="screenlet-body">
        <form id="contactForm" method="post" action="<@ofbizUrl>submitAnonContact</@ofbizUrl>">
            <input type="hidden" name="partyIdFrom" value="${(userLogin.partyId)?if_exists}" />
            <input type="hidden" name="partyIdTo" value="${productStore.payToPartyId?if_exists}"/>
            <input type="hidden" name="contactMechTypeId" value="WEB_ADDRESS" />
            <input type="hidden" name="communicationEventTypeId" value="WEB_SITE_COMMUNICATI" />
            <input type="hidden" name="productStoreId" value="${productStore.productStoreId}" />
            <input type="hidden" name="emailType" value="CONT_NOTI_EMAIL" />
            <table class="basic-table" cellspacing="0">
                <tbody>
                    <tr>
                       <td></td>
                       <td><div id="captchaCode"><input type="hidden" value="${parameters.ID_KEY}" name="captchaCode"/></div></td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.EcommerceSubject}</td>
                       <td><input type="text" name="subject" id="subject" class="required" value="${requestParameters.subject?if_exists}"/>*</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.CommonMessage}</td>
                       <td><textarea name="content" id="message" class="required" cols="50" rows="5">${requestParameters.content?if_exists}</textarea>*</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.FormFieldTitle_emailAddress}</td>
                       <td><input type="text" name="emailAddress" id="emailAddress" class="required" value="${requestParameters.emailAddress?if_exists}"/>*</td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.PartyFirstName}</td>
                       <td><input type="text" name="firstName" id="firstName" class="required" value="${requestParameters.firstName?if_exists}"/></td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.PartyLastName}</td>
                       <td><input type="text" name="lastName" id="lastName" class="required" value="${requestParameters.lastName?if_exists}"/></td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.CommonCaptchaCode}</td>
                       <td><div id="captchaImage"><img src="${parameters.captchaFileName}" alt="" /></div><a href="javascript:reloadCaptcha();">${uiLabelMap.CommonReloadCaptchaCode}</a></td>
                    </tr>
                    <tr>
                       <td class="label">${uiLabelMap.CommonVerifyCaptchaCode}</td>
                       <td><input type="text" autocomplete="off" maxlength="30" size="23" name="captcha"/>*</td>
                    </tr>
                    <tr>
                       <td class="label"></td>
                       <td><input type="submit" value="${uiLabelMap.CommonSubmit}" /></td>
                    </tr>
                </tbody>
            </table>
        </form>
    </div>
</#if>
</div>