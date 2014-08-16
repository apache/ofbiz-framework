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
<form  method="post" action="<@ofbizUrl>mergeContacts</@ofbizUrl>" class="basic-form">
  <table border="0" width="100%" >
    <tr>
      <td>
        <#if contactInfoList?has_content >
          <#assign contactInfo1 = contactInfoList[0]/>
          <#assign contactInfo2 = contactInfoList[1]/>
          <input type="hidden" name="partyIdTo" value="${contactInfo1.partyId!}" />
          <input type="hidden" name="partyId" value="${contactInfo2.partyId!}" />

          <input type="hidden" name="addrContactMechIdTo" value="${contactInfo1.addrContactMechId!}" />
          <input type="hidden" name="phoneContactMechIdTo" value="${contactInfo1.phoneContactMechId!}" />
          <input type="hidden" name="emailContactMechIdTo" value="${contactInfo1.emailContactMechId!}" />

          <input type="hidden" name="addrContactMechId" value="${contactInfo2.addrContactMechId!}" />
          <input type="hidden" name="phoneContactMechId" value="${contactInfo2.phoneContactMechId!}" />
          <input type="hidden" name="emailContactMechId" value="${contactInfo2.emailContactMechId!}" />

          <table  >
            <tr>
              <td width="20%" ></td>
              <td width="30%"><h2>${uiLabelMap.SfaFirstContact}</h2><br /></td>
              <td width="30%"><h2>${uiLabelMap.SfaSecondContact}</h2><br /></td>
              <td width="20%"><h2>${uiLabelMap.CommonSelect}</h2><br /></td>
            </tr>
            <tr>
              <td width="20%">${uiLabelMap.PartyFirstName}</td>
              <td width="30%"><h3>${contactInfo1.firstName!}</h3></td>
              <td width="30%"><h3>${contactInfo2.firstName!}</h3></td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%">${uiLabelMap.PartyLastName}</td>
              <td width="30%"><h3>${contactInfo1.lastName!}</h3></td>
              <td width="30%"><h3>${contactInfo2.lastName!}</h3></td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="30"><br /><h3>${uiLabelMap.PartyGeneralCorrespondenceAddress}</h3></td>
              <td width="30"></td>
            </tr>
            <tr>
              <td width="20%" >${uiLabelMap.PartyAddressLine1}</td>
              <td width="30%">${contactInfo1.address1!}</td>
              <td width="30%">${contactInfo2.address1!}</td>
              <td width="20%"><input type="checkbox" name="useAddress2" value="Y"/></td>
            </tr>

            <tr>
              <td width="20%" >${uiLabelMap.PartyAddressLine2}</td>
              <td width="30%">${contactInfo1.address2!}</td>
              <td width="30%">${contactInfo2.address2!}</td>
              <td width="20%"></td>
            </tr>

            <tr>
              <td width="20%" >${uiLabelMap.PartyCity}</td>
              <td width="30%">${contactInfo1.city!}</td>
              <td width="30%">${contactInfo2.city!}</td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%">${uiLabelMap.PartyState}</td>
              <td width="30%">${contactInfo1.state!}</td>
              <td width="30%">${contactInfo2.state!}</td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%">${uiLabelMap.PartyZipCode}</td>
              <td width="30%">${contactInfo1.postalCode!}</td>
              <td width="30%">${contactInfo2.postalCode!}</td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%" >${uiLabelMap.CommonCountry}</td>
              <td width="30%">${contactInfo1.country!}</td>
              <td width="30%">${contactInfo2.country!}</td>
              <td width="20%"></td>
            </tr>
            <tr><td><br /><h3>${uiLabelMap.PartyPrimaryPhone}</h3></td></tr>
            <tr>
              <td width="20%">${uiLabelMap.CommonCountryCode}</td>
              <td width="30%">${contactInfo1.countryCode!}</td>
              <td width="30%">${contactInfo2.countryCode!}</td>
              <td width="10%"><input type="checkbox" name="useContactNum2" value="Y"/></td>
            </tr>
            <tr>
              <td width="20%" >${uiLabelMap.PartyAreaCode}</td>
              <td width="30%">${contactInfo1.areaCode!}</td>
              <td width="30%">${contactInfo2.areaCode!}</td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%" >${uiLabelMap.PartyPhoneNumber}</td>
              <td width="30%">${contactInfo1.contactNumber!}</td>
              <td width="30%">${contactInfo2.contactNumber!}</td>
              <td width="20%"></td>
            </tr>
            <tr>
              <td width="20%" >${uiLabelMap.PartyEmailAddress}</td>
              <td width="30%">${contactInfo1.primaryEmail!}</td>
              <td width="30%">${contactInfo2.primaryEmail!}</td>
              <td width="10%"><input type="checkbox" name="useEmail2" value="Y"/></td>
            </tr>
            <tr>
              <td colspan="4" align="center"><br />
                <input type="submit" value="${uiLabelMap.CommonSubmit}"/>
              </td>
            </tr>
          </table>
        </#if>
      </td>
    </tr>
  </table>
</form>