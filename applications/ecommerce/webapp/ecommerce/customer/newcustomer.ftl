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

<#if getUsername>
<script language="JavaScript" type="text/javascript">
     lastFocusedName = null;
     function setLastFocused(formElement) {
         lastFocusedName = formElement.name;
     }
     function clickUsername() {
         if (document.forms["newuserform"].elements["UNUSEEMAIL"].checked) {
             if (lastFocusedName == "UNUSEEMAIL") {
                 document.forms["newuserform"].elements["PASSWORD"].focus();
             } else if (lastFocusedName == "PASSWORD") {
                 document.forms["newuserform"].elements["UNUSEEMAIL"].focus();
             } else {
                 document.forms["newuserform"].elements["PASSWORD"].focus();
             }
         }
     }
     function changeEmail() {
         if (document.forms["newuserform"].elements["UNUSEEMAIL"].checked) {
             document.forms["newuserform"].elements["USERNAME"].value=document.forms["newuserform"].elements["CUSTOMER_EMAIL"].value;
         }
     }
     function setEmailUsername() {
         if (document.forms["newuserform"].elements["UNUSEEMAIL"].checked) {
             document.forms["newuserform"].elements["USERNAME"].value=document.forms["newuserform"].elements["CUSTOMER_EMAIL"].value;
             // don't disable, make the browser not submit the field: document.forms["newuserform"].elements["USERNAME"].disabled=true;
         } else {
             document.forms["newuserform"].elements["USERNAME"].value='';
             // document.forms["newuserform"].elements["USERNAME"].disabled=false;
         }
     }
     alert(document.getElementById("customerCountry").value);
        if ( document.getElementById("customerCountry").value == "USA" 
             || document.getElementById("customerCountry").value == "UMI" ) {
            document.getElementById("customerState").style.display = "block";
        }
        else {
            document.getElementById("customerState").style.display = "none";
        } 
     }     
</script>
</#if>

<p class="head1">${uiLabelMap.PartyRequestNewAccount}</p>
<p class='tabletext'>${uiLabelMap.PartyAlreadyHaveAccount}, <a href='<@ofbizUrl>checkLogin/main</@ofbizUrl>' class='buttontext'>${uiLabelMap.CommonLoginHere}</a>.</p>

<#macro fieldErrors fieldName>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>
<#macro fieldErrorsMulti fieldName1 fieldName2 fieldName3 fieldName4>
  <#if errorMessageList?has_content>
    <#assign fieldMessages = Static["org.ofbiz.base.util.MessageString"].getMessagesForField(fieldName1, fieldName2, fieldName3, fieldName4, true, errorMessageList)>
    <ul>
      <#list fieldMessages as errorMsg>
        <li class="errorMessage">${errorMsg}</li>
      </#list>
    </ul>
  </#if>
</#macro>

<form method="post" action="<@ofbizUrl>createcustomer${previousParams}</@ofbizUrl>" name="newuserform" style="margin: 0;">
<input type="hidden" name="emailProductStoreId" value="${productStoreId}"/>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.PartyNameAndShippingAddress}</div>
    </div>
    <div class="screenlet-body">
        <div class="form-row">
            <div class="form-label">${uiLabelMap.CommonTitle}</div>
            <div class="form-field">
                <@fieldErrors fieldName="USER_TITLE"/>
                <select name="USER_TITLE" class="selectBox">
                  <#if requestParameters.USER_TITLE?has_content >
                      <option>${requestParameters.USER_TITLE}</option>
                      <option value="${requestParameters.USER_TITLE}"> -- </option>
                  <#else>
                      <option value="">${uiLabelMap.CommonSelectOne}</option>
                  </#if>
                      <option>${uiLabelMap.CommonTitleMr}</option>
                      <option>${uiLabelMap.CommonTitleMrs}</option>
                      <option>${uiLabelMap.CommonTitleMs}</option>
                      <option>${uiLabelMap.CommonTitleDr}</option>
                </select>                
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyFirstName}</div>
            <div class="form-field">
                <@fieldErrors fieldName="USER_FIRST_NAME"/>
                <input type="text" class='inputBox' name="USER_FIRST_NAME" value="${requestParameters.USER_FIRST_NAME?if_exists}" size="30" maxlength="30"> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyMiddleInitial}</div>
            <div class="form-field">
                <@fieldErrors fieldName="USER_MIDDLE_NAME"/>
                <input type="text" class='inputBox' name="USER_MIDDLE_NAME" value="${requestParameters.USER_MIDDLE_NAME?if_exists}" size="4" maxlength="4">
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyLastName}</div>
            <div class="form-field">
                <@fieldErrors fieldName="USER_LAST_NAME"/>
                <input type="text" class='inputBox' name="USER_LAST_NAME" value="${requestParameters.USER_LAST_NAME?if_exists}" size="30" maxlength="30"> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartySuffix}</div>
            <div class="form-field">
                <@fieldErrors fieldName="USER_SUFFIX"/>
                <input type="text" class='inputBox' name="USER_SUFFIX" value="${requestParameters.USER_SUFFIX?if_exists}" size="10" maxlength="30">
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyAddressLine1}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_ADDRESS1"/>
                <input type="text" class='inputBox' name="CUSTOMER_ADDRESS1" value="${requestParameters.CUSTOMER_ADDRESS1?if_exists}" size="30" maxlength="30"> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyAddressLine2}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_ADDRESS2"/>
                <input type="text" class='inputBox' name="CUSTOMER_ADDRESS2" value="${requestParameters.CUSTOMER_ADDRESS2?if_exists}" size="30" maxlength="30">
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyCity}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_CITY"/>
                <input type="text" class='inputBox' name="CUSTOMER_CITY" value="${requestParameters.CUSTOMER_CITY?if_exists}" size="30" maxlength="30"> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyZipCode}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_POSTAL_CODE"/>
                <input type="text" class='inputBox' name="CUSTOMER_POSTAL_CODE" value="${requestParameters.CUSTOMER_POSTAL_CODE?if_exists}" size="12" maxlength="10"> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyCountry}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_COUNTRY"/>
                <select name="CUSTOMER_COUNTRY" class='selectBox' onClick="javascript:hideShowUsaStates()" id="customerCountry">
                    <#if requestParameters.CUSTOMER_COUNTRY?exists><option value='${requestParameters.CUSTOMER_COUNTRY}'>${selectedCountryName?default(requestParameters.CUSTOMER_COUNTRY)}</option></#if>
                    ${screens.render("component://common/widget/CommonScreens.xml#countries")}
                </select> *
            </div>
        </div>
        <div class="form-row" id="customerState">
            <div class="form-label">${uiLabelMap.PartyState}</div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_STATE"/>
                <select name="CUSTOMER_STATE" class='selectBox'>
                    <#if requestParameters.CUSTOMER_STATE?exists><option value='${requestParameters.CUSTOMER_STATE}'>${selectedStateName?default(requestParameters.CUSTOMER_STATE)}</option></#if>
                    <option value="">${uiLabelMap.PartyNoState}</option>
                    ${screens.render("component://common/widget/CommonScreens.xml#states")}
                </select> *
            </div>
        </div>
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyAllowAddressSolicitation}</div>
            <div class="form-field">
                <select name="CUSTOMER_ADDRESS_ALLOW_SOL" class='selectBox'>
                    <option>${requestParameters.CUSTOMER_ADDRESS_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                    <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                </select>
            </div>
        </div>
        <div class="endcolumns"><span></span></div>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.PartyPhoneNumbers}</div>
    </div>
    <div class="screenlet-body">
        <div class="form-row">
            <div class="form-label">${uiLabelMap.PartyAllPhoneNumbers}</div>
            <div class="form-field">
                [${uiLabelMap.PartyCountry}] [${uiLabelMap.PartyAreaCode}] [${uiLabelMap.PartyContactNumber}] [${uiLabelMap.PartyExtension}]
            </div>
        </div>
        <div class="form-row">
            <div class="form-label"><div>${uiLabelMap.PartyHomePhone}</div><div>(${uiLabelMap.PartyAllowSolicitation}?)</div></div>
            <div class="form-field">
                <@fieldErrorsMulti fieldName1="CUSTOMER_HOME_COUNTRY" fieldName2="CUSTOMER_HOME_AREA" fieldName3="CUSTOMER_HOME_CONTACT" fieldName4="CUSTOMER_HOME_EXT"/>
                <input type="text" class='inputBox' name="CUSTOMER_HOME_COUNTRY" value="${requestParameters.CUSTOMER_HOME_COUNTRY?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_HOME_AREA" value="${requestParameters.CUSTOMER_HOME_AREA?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_HOME_CONTACT" value="${requestParameters.CUSTOMER_HOME_CONTACT?if_exists}" size="15" maxlength="15">
                &nbsp;ext&nbsp;<input type="text" class='inputBox' name="CUSTOMER_HOME_EXT" value="${requestParameters.CUSTOMER_HOME_EXT?if_exists}" size="6" maxlength="10">
                <br/>
                <select name="CUSTOMER_HOME_ALLOW_SOL" class='selectBox'>
                    <option>${requestParameters.CUSTOMER_HOME_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                    <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                </select>
            </div>
        </div>
        <div class="form-row">
            <div class="form-label"><div>${uiLabelMap.PartyBusinessPhone}</div><div>(${uiLabelMap.PartyAllowSolicitation}?)</div></div>
            <div class="form-field">
                <@fieldErrorsMulti fieldName1="CUSTOMER_WORK_COUNTRY" fieldName2="CUSTOMER_WORK_AREA" fieldName3="CUSTOMER_WORK_CONTACT" fieldName4="CUSTOMER_WORK_EXT"/>
                <input type="text" class='inputBox' name="CUSTOMER_WORK_COUNTRY" value="${requestParameters.CUSTOMER_WORK_COUNTRY?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_WORK_AREA" value="${requestParameters.CUSTOMER_WORK_AREA?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_WORK_CONTACT" value="${requestParameters.CUSTOMER_WORK_CONTACT?if_exists}" size="15" maxlength="15">
                &nbsp;ext&nbsp;<input type="text" class='inputBox' name="CUSTOMER_WORK_EXT" value="${requestParameters.CUSTOMER_WORK_EXT?if_exists}" size="6" maxlength="10">
                <br/>
                <select name="CUSTOMER_WORK_ALLOW_SOL" class='selectBox'>
                    <option>${requestParameters.CUSTOMER_WORK_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                    <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                </select>
            </div>
        </div>
        <div class="form-row">
            <div class="form-label"><div>${uiLabelMap.PartyFaxNumber}</div><div>(${uiLabelMap.PartyAllowSolicitation}?)</div></div>
            <div class="form-field">
                <@fieldErrorsMulti fieldName1="CUSTOMER_FAX_COUNTRY" fieldName2="CUSTOMER_FAX_AREA" fieldName3="CUSTOMER_FAX_CONTACT" fieldName4=""/>
                <input type="text" class='inputBox' name="CUSTOMER_FAX_COUNTRY" value="${requestParameters.CUSTOMER_FAX_COUNTRY?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_FAX_AREA" value="${requestParameters.CUSTOMER_FAX_AREA?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_FAX_CONTACT" value="${requestParameters.CUSTOMER_FAX_CONTACT?if_exists}" size="15" maxlength="15">
                <br/>
                <select name="CUSTOMER_FAX_ALLOW_SOL" class='selectBox'>
                    <option>${requestParameters.CUSTOMER_FAX_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                    <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                </select>
            </div>
        </div>
        <div class="form-row">
            <div class="form-label"><div>${uiLabelMap.PartyMobilePhone}</div><div>(${uiLabelMap.PartyAllowSolicitation}?)</div></div>
            <div class="form-field">
                <@fieldErrorsMulti fieldName1="CUSTOMER_MOBILE_COUNTRY" fieldName2="CUSTOMER_MOBILE_AREA" fieldName3="CUSTOMER_MOBILE_CONTACT" fieldName4=""/>
                <input type="text" class='inputBox' name="CUSTOMER_MOBILE_COUNTRY" value="${requestParameters.CUSTOMER_MOBILE_COUNTRY?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_MOBILE_AREA" value="${requestParameters.CUSTOMER_MOBILE_AREA?if_exists}" size="4" maxlength="10">
                -&nbsp;<input type="text" class='inputBox' name="CUSTOMER_MOBILE_CONTACT" value="${requestParameters.CUSTOMER_MOBILE_CONTACT?if_exists}" size="15" maxlength="15">
                <br/>
                <select name="CUSTOMER_MOBILE_ALLOW_SOL" class='selectBox'>
                    <option>${requestParameters.CUSTOMER_MOBILE_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                    <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                </select>
            </div>
        </div>
        <div class="endcolumns"><span></span></div>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;${uiLabelMap.PartyEmailAddress}</div>
    </div>
    <div class="screenlet-body">
        <div class="form-row">
            <div class="form-label"><div>${uiLabelMap.PartyEmailAddress}</div><div>(${uiLabelMap.PartyAllowSolicitation}?)</div></div>
            <div class="form-field">
                <@fieldErrors fieldName="CUSTOMER_EMAIL"/>
                <div><input type="text" class='inputBox' name="CUSTOMER_EMAIL" value="${requestParameters.CUSTOMER_EMAIL?if_exists}" size="40" maxlength="255" onchange="changeEmail()" onkeyup="changeEmail()"> *</div>
                <div>
                    <select name="CUSTOMER_EMAIL_ALLOW_SOL" class='selectBox'>
                        <option>${requestParameters.CUSTOMER_EMAIL_ALLOW_SOL?default("${uiLabelMap.CommonY}")}</option>
                        <option></option><option>${uiLabelMap.CommonY}</option><option>${uiLabelMap.CommonN}</option>
                    </select>
                </div>
            </div>
        </div>
<#--
        <div>Order Email addresses (comma separated)</div>
        <input type="text" name="CUSTOMER_ORDER_EMAIL" value="${requestParameters.CUSTOMER_ORDER_EMAIL?if_exists}" size="40" maxlength="80">
-->
        <div class="endcolumns"><span></span></div>
    </div>
</div>

<div class="screenlet">
    <div class="screenlet-header">
        <div class='boxhead'>&nbsp;<#if getUsername>${uiLabelMap.CommonUsername} & </#if>${uiLabelMap.CommonPassword}</div>
    </div>
    <div class="screenlet-body">
        <#if getUsername>
            <div class="form-row">
                <div class="form-label"><span class="tabletext">${uiLabelMap.CommonUsername}</span></div>
                <div class="form-field">
                    <@fieldErrors fieldName="USERNAME"/>
                    <div>${uiLabelMap.EcommerceUseEmailAddress}: <input type="checkbox" name="UNUSEEMAIL" value="on" onClick="setEmailUsername();" onFocus="setLastFocused(this);"/></div>
                    <div><input type="text" class='inputBox' name="USERNAME" value="${requestParameters.USERNAME?if_exists}" size="20" maxlength="50" onFocus="clickUsername();" onchange="changeEmail();"/> *</div>
                </div>
            </div>
        </#if>
        <#if createAllowPassword>
            <div class="form-row">
                <div class="form-label">${uiLabelMap.CommonPassword}</div>
                <div class="form-field">
                    <@fieldErrors fieldName="PASSWORD"/>
                    <input type="password" class='inputBox' name="PASSWORD" value="" size="20" maxlength="50" onFocus="setLastFocused(this);"/> *
                </div>
            </div>
            <div class="form-row">
                <div class="form-label">${uiLabelMap.PartyRepeatPassword}</div>
                <div class="form-field">
                    <@fieldErrors fieldName="CONFIRM_PASSWORD"/>
                    <input type="password" class='inputBox' name="CONFIRM_PASSWORD" value="" size="20" maxlength="50"/> *
                </div>
            </div>
            <div class="form-row">
                <div class="form-label"><span class="tabletext">${uiLabelMap.PartyPasswordHint}</span></div>
                <div class="form-field">
                    <@fieldErrors fieldName="PASSWORD_HINT"/>
                    <input type="text" class='inputBox' name="PASSWORD_HINT" value="${requestParameters.PASSWORD_HINT?if_exists}" size="30" maxlength="100"/>
                </div>
            </div>
        <#else/>
            <div class="form-row">
                <div class="form-label">${uiLabelMap.CommonPassword}</div>
                <div class="form-field">
                    ${uiLabelMap.PartyRecievePasswordByEmail}.
                </div>
            </div>
        </#if>
        <div class="endcolumns"><span></span></div>
    </div>
</div>

<input type="image" src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" onClick="javascript:document.newuserform.submit();">
</form>

<div class="tabletext">${uiLabelMap.CommonFieldsMarkedAreRequired}</div>

<div>
&nbsp;&nbsp;<a href="<@ofbizUrl>checkLogin/main</@ofbizUrl>" class="buttontextbig">${uiLabelMap.CommonBack}</a>
&nbsp;&nbsp;<a href="javascript:document.newuserform.submit()" class="buttontextbig">${uiLabelMap.CommonSave}</a>
</div>

<br/>

<script language="JavaScript" type="text/javascript">
    hideShowUsaStates();
</script>
