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
 *@version    $Rev$
 *@since      2.1
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
        <input type="text" class='inputBox' size="10" maxlength="30" name="personalTitle" value="${personData.personalTitle?if_exists}"/>
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
          <option>${personData.gender?if_exists}</option>
          <option></option>
          <option>M</option>
          <option>F</option>
        </select>
      </td>
    </tr>
    <tr>
      <td width="26%" align="right"><div class="tabletext">${uiLabelMap.PartyBirthDate}</div></td>
      <td width="74%" align="left">
        <input type="text" class='inputBox' size="11" maxlength="20" name="birthDate" value="${personData.birthDate?if_exists}"/>
        (yyyy-MM-dd)
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
          <option>${personData.maritalStatus?if_exists}</option>
          <option></option>
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
        (yyyy-MM-dd)
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
