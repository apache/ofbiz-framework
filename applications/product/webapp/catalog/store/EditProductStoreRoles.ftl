<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
  <#if !requestParameters.showAll?exists>
    <a href="<@ofbizUrl>EditProductStoreRoles?productStoreId=${productStoreId}&showAll=Y</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductShowAll}]</a>
  <#else>
    <a href="<@ofbizUrl>EditProductStoreRoles?productStoreId=${productStoreId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductShowActive}]</a>
  </#if>
  <br/>
  
  <table border="1" cellpadding="2" cellspacing="0">
    <tr>
      <td><span class="tableheadtext">${uiLabelMap.PartyParty}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.PartyRole}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonFromDate}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.CommonThruDate}</span></td>
      <td>&nbsp;</td>
    </tr>
    <#if productStoreRoles?has_content>
      <#list productStoreRoles as role>
        <#assign roleType = role.getRelatedOne("RoleType")>
        <tr> 
          <td><a href="/partymgr/control/viewprofile?partyId=${role.partyId}&externalLoginKey=${requestAttributes.externalLoginKey}" class="buttontext">${role.partyId}</a></td>
          <td><span class="tabletext">${roleType.get("description",locale)}</span></td>
          <td><span class="tabletext">${role.fromDate?string}</span></td>
          <td><span class="tabletext">${role.thruDate?default("${uiLabelMap.CommonNA}")?string?if_exists}</span></td>
          <#if role.thruDate?exists>
            <td>&nbsp;</td>
          <#else>
            <td align="center">
              <a href="<@ofbizUrl>storeRemoveRole?productStoreId=${productStoreId}&partyId=${role.partyId}&roleTypeId=${role.roleTypeId}&fromDate=${role.fromDate}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a>
            </td>
          </#if>
        </tr>
      </#list>
    </#if>
  </table>
  
  <br/>
  <div class="head2">${uiLabelMap.ProductCreateProductStoreRole}:</div>
  <form name="addProductStoreRole" action="<@ofbizUrl>storeCreateRole</@ofbizUrl>" method="post">
    <input type="hidden" name="productStoreId" value="${productStoreId}">
    <table cellspacing="2" cellpadding="2">
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.PartyRoleType}</span></td>
        <td>
          <select class="selectBox" name="roleTypeId">
            <#list roleTypes as roleType>
              <option value="${roleType.roleTypeId}">${roleType.get("description",locale)}</option>
            </#list>
          </select>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.PartyParty}</span></td>
        <td>
          <input type="text" class="inputBox" name="partyId" size="20">
          <a href="javascript:call_fieldlookup2(document.addProductStoreRole.partyId,'LookupPartyName');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </td>
      </tr>
      <tr>
        <td><span class="tableheadtext">${uiLabelMap.CommonFromDate}</span></td>
        <td>
          <input type="text" class="inputBox" name="fromDate" size="25">
          <a href="javascript:call_cal(document.addProductStoreRole.fromDate, null);"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>                   
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td><input type="submit" class="smallSubmit" value="${uiLabelMap.CommonAdd}"></td>
      </tr>
    </table>
  </form>
