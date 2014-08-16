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
<#if person??>
  <h2>${uiLabelMap.PartyEditPersonalInformation}</h2>
    &nbsp;<form id="editpersonform1" method="post" action="<@ofbizUrl>updatePerson</@ofbizUrl>" name="editpersonform">    
<#else>
  <h2>${uiLabelMap.PartyAddNewPersonalInformation}</h2>
    &nbsp;<form id="editpersonform2" method="post" action="<@ofbizUrl>createPerson/${donePage}</@ofbizUrl>" name="editpersonform">
</#if>
<div>
  &nbsp;<a href='<@ofbizUrl>${donePage}</@ofbizUrl>' class="button">${uiLabelMap.CommonGoBack}</a>
  &nbsp;<a href="javascript:document.editpersonform.submit()" class="button">${uiLabelMap.CommonSave}</a>
  <p/>    
  <input type="hidden" name="partyId" value="${person.partyId!}" />
  <table width="90%" border="0" cellpadding="2" cellspacing="0">
  <tr>
    <td align="right">${uiLabelMap.CommonTitle}</td>
    <td>
      <select name="personalTitle" class="selectBox">
        <#if personData.personalTitle?has_content >
          <option>${personData.personalTitle}</option>
          <option value="${personData.personalTitle}"> -- </option>
        <#else>
          <option value="">${uiLabelMap.CommonSelectOne}</option>
        </#if>
        <option>${uiLabelMap.CommonTitleMr}</option>
        <option>${uiLabelMap.CommonTitleMrs}</option>
        <option>${uiLabelMap.CommonTitleMs}</option>
        <option>${uiLabelMap.CommonTitleDr}</option>
      </select>
    </td>
  </tr>
  <tr>
    <td align="right">${uiLabelMap.PartyFirstName}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="30" name="firstName" value="${personData.firstName!}"/>
      *</td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyMiddleInitial}</td>
      <td>
        <input type="text" class='inputBox' size="4" maxlength="4" name="middleName" value="${personData.middleName!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyLastName}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="30" name="lastName" value="${personData.lastName!}"/>
      *</td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartySuffix}</td>
      <td>
        <input type="text" class='inputBox' size="10" maxlength="30" name="suffix" value="${personData.suffix!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyNickName}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="nickname" value="${personData.nickname!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyGender}</td>
      <td>
        <select name="gender" class='selectBox'>
          <#if personData.gender?has_content >
            <option value="${personData.gender}">
                <#if personData.gender == "M" >${uiLabelMap.CommonMale}</#if>
                <#if personData.gender == "F" >${uiLabelMap.CommonFemale}</#if>
            </option>
            <option value="${personData.gender}"> -- </option>
          <#else>
            <option value="">${uiLabelMap.CommonSelectOne}</option>
          </#if>
          <option value="M">${uiLabelMap.CommonMale}</option>
          <option value="F">${uiLabelMap.CommonFemale}</option>
        </select>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyBirthDate}</td>
      <td>
        <input type="text" class='inputBox' size="11" maxlength="20" name="birthDate" value="${(personData.birthDate.toString())!}"/>
        <div>${uiLabelMap.CommonFormatDate}</div>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyHeight}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="height" value="${personData.height!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyWeight}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="weight" value="${personData.weight!}"/>
      </td>
    </tr>

    <tr>
      <td align="right">${uiLabelMap.PartyMaidenName}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="mothersMaidenName" value="${personData.mothersMaidenName!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyMaritalStatus}</td>
      <td>
        <select name="maritalStatus" class='selectBox'>
          <#if personData.maritalStatus?has_content>
             <option value="${personData.maritalStatus}">
               <#if personData.maritalStatus == "S">${uiLabelMap.PartySingle}</#if>
               <#if personData.maritalStatus == "M">${uiLabelMap.PartyMarried}</#if>
               <#if personData.maritalStatus == "D">${uiLabelMap.PartyDivorced}</#if>
             </option>
          <option value="${personData.maritalStatus}"> -- </option>
          <#else>
          <option></option>
          </#if>
          <option value="S">${uiLabelMap.PartySingle}</option>
          <option value="M">${uiLabelMap.PartyMarried}</option>
          <option value="D">${uiLabelMap.PartyDivorced}</option>
        </select>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartySocialSecurityNumber}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="socialSecurityNumber" value="${personData.socialSecurityNumber!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyPassportNumber}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="passportNumber" value="${personData.passportNumber!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyPassportExpireDate}</td>
      <td>
        <input type="text" class='inputBox' size="11" maxlength="20" name="passportExpireDate" value="${personData.passportExpireDate!}"/>
        <div>${uiLabelMap.CommonFormatDate}</div>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.PartyTotalYearsWorkExperience}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="totalYearsWorkExperience" value="${personData.totalYearsWorkExperience!}"/>
      </td>
    </tr>
    <tr>
      <td align="right">${uiLabelMap.CommonComment}</td>
      <td>
        <input type="text" class='inputBox' size="30" maxlength="60" name="comments" value="${personData.comments!}"/>
      </td>
    </tr>
</table>
</div>
</form>
&nbsp;<a href='<@ofbizUrl>${donePage}</@ofbizUrl>' class="button">${uiLabelMap.CommonGoBack}</a>
&nbsp;<a id="editpersonform3" href="javascript:document.editpersonform.submit()" class="button">${uiLabelMap.CommonSave}</a>