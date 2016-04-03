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


    <!-- Begin  Form Widget - Form Element  component://myportal/widget/MyPortalForms.xml#RegisterPerson -->
    <form name="RegisterPerson" onsubmit="javascript:submitFormDisableSubmits(this)" class="basic-form" id="RegisterPerson" action="/myportal/control/createRegister" method="post">
        <input type="hidden" value="${webSiteId!}" name="webSiteId"/>
        <input type="hidden" name="reload"/>
        <div id="_G0_" class="fieldgroup"><div class="fieldgroup-title-bar"><table><tbody><tr><td class="collapse"> </td><td> </td></tr></tbody></table></div><div class="fieldgroup-body" id="_G0__body">
            <table cellspacing="0" class="basic-table">
                <tbody>
                    <tr>
                        <td class="label">    Why Would You Like To Register    
                        </td>
                        <td><textarea id="RegisterPerson_whyWouldYouLikeToRegister" rows="5" cols="60" class="required false" name="whyWouldYouLikeToRegister"></textarea>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    Salutation    </td>
                        <td class="no-required">
                            <input type="text" autocomplete="off" id="RegisterPerson_salutation" maxlength="60" size="40" name="salutation" value="${requestParameters.salutation!}"/>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    First name    </td>
                        <td>
                            <input type="text" autocomplete="off" id="RegisterPerson_firstName" maxlength="60" size="40" class="required false" name="firstName" value="${requestParameters.firstName!}"/>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="label">        Middle Name    </td>
                        <td class="no-required">
                            <input type="text" autocomplete="off" id="RegisterPerson_middleName" maxlength="60" size="40" name="middleName" value="${requestParameters.middleName!}"/>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">        Last name    </td>
                        <td>
                            <input type="text" autocomplete="off" id="RegisterPerson_lastName" maxlength="60" size="40" class="required false" name="lastName" value="${requestParameters.lastName!}"/>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    Email    </td>
                        <td>
                            <input type="text" autocomplete="off" id="RegisterPerson_USER_EMAIL" maxlength="250" size="60" class="required false" name="USER_EMAIL" value="${requestParameters.USER_EMAIL!}"/>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="group-label">    User Login    </td>
                        <td> </td>
                    </tr>
                    <tr>
                        <td class="label">    Username    </td>
                        <td>
                            <input type="text" autocomplete="off" id="RegisterPerson_USERNAME" maxlength="250" size="30" class="required false" name="USERNAME" value="${requestParameters.USERNAME!}"/>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    Password    </td>
                        <td>
                            <input type="password" id="RegisterPerson_PASSWORD" maxlength="250" size="15" name="PASSWORD" class="required false" value="${requestParameters.PASSWORD!}"/>
                            <span class="tooltip">Required</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    Password    </td>
                        <td>
                            <input type="password" id="RegisterPerson_CONFIRM_PASSWORD" maxlength="250" size="15" name="CONFIRM_PASSWORD" class="required false" value="${requestParameters.CONFIRM_PASSWORD!}"/>
                            <span class="tooltip">* Confirm</span>    
                        </td>
                    </tr>
                    <tr>
                        <td class="group-label">    Verify captcha code    </td>
                        <td> </td>
                    </tr>
                    <tr>
                        <td class="label"> Code Captcha </td>
                        <td><div><img id="captchaImage" src="<@ofbizUrl>captcha.jpg?captchaCodeId=captchaImage&amp;unique=${nowTimestamp.getTime()}</@ofbizUrl>" alt="" /></div></td>
                    </tr>
                    <script type="text/javascript" language="JavaScript">
                    <!--
                        function reloadCaptcha(fieldName) {
                            var captchaUri = "<@ofbizUrl>captcha.jpg?captchaCodeId=" + fieldName + "&amp;unique=_PLACEHOLDER_</@ofbizUrl>";
                            var unique = Date.now();
                            captchaUri = captchaUri.replace("_PLACEHOLDER_", unique);
                            document.getElementById(fieldName).src = captchaUri;
                        }
                    //-->
                    </script>
                    <tr>
                        <td class="label"> </td>
                        <td>
                            <a href="javascript:reloadCaptcha('captchaImage');">${uiLabelMap.CommonReloadCaptchaCode}</a>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">    Verify captcha code    </td>
                        <td>
                            <input type="text" autocomplete="off" id="RegisterPerson_captcha" maxlength="30" size="23" class="required false" name="captcha"/>
                            <span class="tooltip">Required</span>
                        </td>
                    </tr>
                    <tr>
                        <td class="group-label"> </td>
                        <td>
                            <input type="submit" value="Save" name="submitButton"/>
                        </td>
                    </tr>
                </tbody>
             </table>
        </div>
    <!-- End  Form Widget - Form Element  component://myportal/widget/MyPortalForms.xml#RegisterPerson --><!-- End Section Widget  -->
    </form>

