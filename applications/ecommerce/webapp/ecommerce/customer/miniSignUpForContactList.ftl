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

<#-- A simple macro that builds the contact list -->
<#macro contactList publicEmailContactLists>
    <select name="contactListId" class="selectBox" style="width:134px">
        <#list publicEmailContactLists as publicEmailContactList>
            <#assign publicContactMechType = publicEmailContactList.getRelatedOneCache("ContactMechType")?if_exists>
            <option value="${publicEmailContactList.contactListId}">${publicEmailContactList.contactListName?if_exists}</option>
        </#list>
    </select>
</#macro>

<div id="miniSignUpForContactList" class="screenlet">
    <div class="screenlet-header">
        <div class="boxhead">${uiLabelMap.EcommerceSignUpForContactList}</div>
    </div>
    <div class="screenlet-body" style="text-align: center;">
        <#if sessionAttributes.autoName?has_content>
            <#-- The visitor potentially has an account and party id -->
            <#if userLogin?has_content && userLogin.userLoginId != "anonymous">
                <#-- They are logged in so lets present the form to sign up with their email address -->
                <form method="post" action="<@ofbizUrl>createContactListParty</@ofbizUrl>" name="signUpForContactListForm">
                    <input type="hidden" name="partyId" value="${partyId}"/>
                    <input type="hidden" name="statusId" value="CLPT_PENDING"/>
                    <span class="tabletext">${uiLabelMap.EcommerceSignUpForContactListComments}</span>
                    <@contactList publicEmailContactLists=publicEmailContactLists/>

                    <select name="preferredContactMechId" class="selectBox" style="width:134px">
                        <#list partyAndContactMechList as partyAndContactMech>
                            <option value="${partyAndContactMech.contactMechId}"><#if partyAndContactMech.infoString?has_content>${partyAndContactMech.infoString}<#elseif partyAndContactMech.tnContactNumber?has_content>${partyAndContactMech.tnCountryCode?if_exists}-${partyAndContactMech.tnAreaCode?if_exists}-${partyAndContactMech.tnContactNumber}<#elseif partyAndContactMech.paAddress1?has_content>${partyAndContactMech.paAddress1}, ${partyAndContactMech.paAddress2?if_exists}, ${partyAndContactMech.paCity?if_exists}, ${partyAndContactMech.paStateProvinceGeoId?if_exists}, ${partyAndContactMech.paPostalCode?if_exists}, ${partyAndContactMech.paPostalCodeExt?if_exists} ${partyAndContactMech.paCountryGeoId?if_exists}</#if></option>
                        </#list>
                    </select>

                    <input type="submit" value="${uiLabelMap.CommonSubscribe}" class="smallSubmit"/>
                </form>
            <#else>
                <#-- Not logged in so ask them to log in and then sign up or clear the user association -->
                <div class="tabletext">${uiLabelMap.EcommerceSignUpForContactListLogIn}</div>
                <div class="tabletext"><a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>" class="linktext">${uiLabelMap.CommonLogin}</a> ${sessionAttributes.autoName}</div>
                <div class="tabletext">(${uiLabelMap.CommonNotYou}?&nbsp;<a href="<@ofbizUrl>autoLogout</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonClickHere}</a>)</div>
            </#if>
        <#else>
            <#-- There is no party info so just offer an anonymous (non-partyId) related newsletter sign up -->
            <form method="post" action="<@ofbizUrl>signUpForContactList</@ofbizUrl>" name="signUpForContactListForm">
                <span class="tabletext">${uiLabelMap.EcommerceSignUpForContactListComments}</span>
                <@contactList publicEmailContactLists=publicEmailContactLists/>
                <input size="20" maxlength="255" name="email" class="inputBox" value="" type="text">
                <input type="submit" value="${uiLabelMap.CommonSubscribe}" class="smallSubmit"/>
            </form>
        </#if>
    </div>
</div>

