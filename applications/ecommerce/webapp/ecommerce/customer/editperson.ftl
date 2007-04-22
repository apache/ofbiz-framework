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
<#if person?exists>
  <p class="head1">${uiLabelMap.PartyEditPersonalInformation}</p>
    <form method="post" action="<@ofbizUrl>updatePerson/${donePage}</@ofbizUrl>" name="editpersonform">
<#else>
  <p class="head1">${uiLabelMap.PartyAddNewPersonalInformation}</p>
    <form method="post" action="<@ofbizUrl>createPerson/${donePage}</@ofbizUrl>" name="editpersonform">
</#if>

&nbsp;<a href='<@ofbizUrl>authview/${donePage}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonGoBack}</a>
&nbsp;<a href="javascript:document.editpersonform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>

<table width="90%" border="0" cellpadding="2" cellspacing="0">
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonTitle}</div></td>
      <td width="74%" align="left">
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
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyFirstName}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="30" name="firstName" value="${personData.firstName?if_exists}"/>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMiddleInitial}</div></td>
      <td width="74%" align="left">
          <input type="text" class='inputBox' size="4" maxlength="4" name="middleName" value="${personData.middleName?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyLastName} </div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="30" name="lastName" value="${personData.lastName?if_exists}"/>
      *</td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartySuffix}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="10" maxlength="30" name="suffix" value="${personData.suffix?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyNickName}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="nickname" value="${personData.nickname?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyGender}</div></td>
      <td width="74%" align="left">
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
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyBirthDate}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="11" maxlength="20" name="birthDate" value="${personData.birthDate?if_exists}"/>
        <div class="tabletext">${uiLabelMap.CommonFormatDate}</div>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyHeight}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="height" value="${personData.height?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyWeight}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="weight" value="${personData.weight?if_exists}"/>
      </td>
    </tr>

    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMaidenName}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="mothersMaidenName" value="${personData.mothersMaidenName?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyMaritalStatus}</div></td>
      <td width="74%" align="left">
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
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartySocialSecurityNumber}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="socialSecurityNumber" value="${personData.socialSecurityNumber?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyPassportNumber}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="passportNumber" value="${personData.passportNumber?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyPassportExpireDate}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="11" maxlength="20" name="passportExpireDate" value="${personData.passportExpireDate?if_exists}"/>
        <div class="tabletext">${uiLabelMap.CommonFormatDate}</div>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyTotalYearsWorkExperience}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="totalYearsWorkExperience" value="${personData.totalYearsWorkExperience?if_exists}"/>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonComment}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="30" maxlength="60" name="comments" value="${personData.comments?if_exists}"/>
      </td>
    </tr>
</table>
</form>

&nbsp;<a href='<@ofbizUrl>authview/${donePage}</@ofbizUrl>' class="buttontext">${uiLabelMap.CommonGoBack}</a>
&nbsp;<a href="javascript:document.editpersonform.submit()" class="buttontext">${uiLabelMap.CommonSave}</a>
