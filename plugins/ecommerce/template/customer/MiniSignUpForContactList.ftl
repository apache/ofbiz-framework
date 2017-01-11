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
      <#assign publicContactMechType = publicEmailContactList.contactList.getRelatedOne("ContactMechType", true)!>
      <option value="${publicEmailContactList.contactList.contactListId}">
        ${publicEmailContactList.contactListType.description!}- ${publicEmailContactList.contactList.contactListName!}
      </option>
    </#list>
  </select>
</#macro>

<script type="text/javascript" language="JavaScript">
  function unsubscribe() {
    var form = document.getElementById("signUpForContactListForm");
    form.action = "<@ofbizUrl>unsubscribeContactListParty</@ofbizUrl>"
    document.getElementById("statusId").value = "CLPT_UNSUBS_PENDING";
    form.submit();
  }
  function unsubscribeByContactMech() {
    var form = document.getElementById("signUpForContactListForm");
    form.action = "<@ofbizUrl>unsubscribeContactListPartyContachMech</@ofbizUrl>"
    document.getElementById("statusId").value = "CLPT_UNSUBS_PENDING";
    form.submit();
  }
</script>

<div id="miniSignUpForContactList" class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.EcommerceSignUpForContactList}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
  <#if sessionAttributes.autoName?has_content>
  <#-- The visitor potentially has an account and party id -->
    <#if userLogin?has_content && userLogin.userLoginId != "anonymous">
    <#-- They are logged in so lets present the form to sign up with their email address -->
      <form method="post" action="<@ofbizUrl>createContactListParty</@ofbizUrl>" name="signUpForContactListForm"
          id="signUpForContactListForm">
        <fieldset>
          <#assign contextPath = request.getContextPath()>
          <input type="hidden" name="baseLocation" value="${contextPath}"/>
          <input type="hidden" name="partyId" value="${partyId}"/>
          <input type="hidden" id="statusId" name="statusId" value="CLPT_PENDING"/>
          <p>${uiLabelMap.EcommerceSignUpForContactListComments}</p>
          <div>
            <@contactList publicEmailContactLists=publicEmailContactLists/>
          </div>
          <div>
            <label for="preferredContactMechId">${uiLabelMap.CommonEmail} *</label>
            <select id="preferredContactMechId" name="preferredContactMechId" class="selectBox">
              <#list partyAndContactMechList as partyAndContactMech>
                <option value="${partyAndContactMech.contactMechId}">
                  <#if partyAndContactMech.infoString?has_content>
                    ${partyAndContactMech.infoString}
                  <#elseif partyAndContactMech.tnContactNumber?has_content>
                    ${partyAndContactMech.tnCountryCode!}-${partyAndContactMech.tnAreaCode!}
                    -${partyAndContactMech.tnContactNumber}
                  <#elseif partyAndContactMech.paAddress1?has_content>
                  ${partyAndContactMech.paAddress1}, ${partyAndContactMech.paAddress2!}, ${partyAndContactMech.paCity!}
                  , ${partyAndContactMech.paStateProvinceGeoId!}, ${partyAndContactMech.paPostalCode!}
                  , ${partyAndContactMech.paPostalCodeExt!} ${partyAndContactMech.paCountryGeoId!}
                  </#if>
                </option>
              </#list>
            </select>
          </div>
          <div>
            <input type="submit" value="${uiLabelMap.EcommerceSubscribe}"/>
            <input type="button" value="${uiLabelMap.EcommerceUnsubscribe}"
                onclick="javascript:unsubscribeByContactMech();"/>
          </div>
        </fieldset>
      </form>
    <#else>
    <#-- Not logged in so ask them to log in and then sign up or clear the user association -->
      <p>${uiLabelMap.EcommerceSignUpForContactListLogIn}</p>
      <p>
        <a href="<@ofbizUrl>${checkLoginUrl}</@ofbizUrl>">${uiLabelMap.CommonLogin}</a> ${sessionAttributes.autoName}
      </p>
      <p>(${uiLabelMap.CommonNotYou}? <a href="<@ofbizUrl>autoLogout</@ofbizUrl>">${uiLabelMap.CommonClickHere}</a>)</p>
    </#if>
  <#else>
  <#-- There is no party info so just offer an anonymous (non-partyId) related newsletter sign up -->
    <form method="post" action="<@ofbizUrl>signUpForContactList</@ofbizUrl>" name="signUpForContactListForm"
        id="signUpForContactListForm">
      <fieldset>
        <#assign contextPath = request.getContextPath()>
        <input type="hidden" name="baseLocation" value="${contextPath}"/>
        <input type="hidden" id="statusId" name="statusId"/>
        <div>
          <label>${uiLabelMap.EcommerceSignUpForContactListComments}</label>
          <@contactList publicEmailContactLists=publicEmailContactLists/>
        </div>
        <div>
          <label for="email">${uiLabelMap.CommonEmail} *</label>
          <input id="email" name="email" class="required" type="text"/>
        </div>
        <div>
          <input type="submit" value="${uiLabelMap.EcommerceSubscribe}"/>
          <input type="button" value="${uiLabelMap.EcommerceUnsubscribe}" onclick="javascript:unsubscribe();"/>
        </div>
      </fieldset>
    </form>
  </#if>
  </div>
</div>
