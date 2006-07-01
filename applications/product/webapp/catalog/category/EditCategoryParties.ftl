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
 *@author     Catherine Heintz (catherine.heintz@nereide.biz)
 *@version    $Rev$
 *@since      2.1
-->

<#if productCategoryId?exists && productCategory?exists>    
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
    <td><div class="tabletext"><b>${uiLabelMap.PartyPartyId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.PartyRole}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
    <td align="center"><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
    <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list productCategoryRoles as productCategoryRole>
    <#assign line = line + 1>
    <#assign curRoleType = productCategoryRole.getRelatedOneCache("RoleType")>
    <tr valign="middle">
    <td><a href="/partymgr/control/viewprofile?party_id=${(productCategoryRole.partyId)?if_exists}" target="_blank" class="buttontext">[${(productCategoryRole.partyId)?if_exists}]</a></td>
    <td><div class="tabletext">${(curRoleType.get("description",locale))?if_exists}</div></td>
    <#assign hasntStarted = false>
    <#if (productCategoryRole.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productCategoryRole.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
    <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(productCategoryRole.fromDate)?if_exists}</div></td>
    <td align="center">
        <FORM method="post" action="<@ofbizUrl>updatePartyToCategory</@ofbizUrl>" name="lineForm${line}">
            <#assign hasExpired = false>
            <#if (productCategoryRole.getTimestamp("thruDate"))?exists && (Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productCategoryRole.getTimestamp("thruDate")))> <#assign hasExpired = true></#if>
            <input type="hidden" name="productCategoryId" value="${(productCategoryRole.productCategoryId)?if_exists}">
            <input type="hidden" name="partyId" value="${(productCategoryRole.partyId)?if_exists}">
            <input type="hidden" name="roleTypeId" value="${(productCategoryRole.roleTypeId)?if_exists}">
            <input type="hidden" name="fromDate" value="${(productCategoryRole.getTimestamp("fromDate"))?if_exists}">
            <input type="text" size="25" name="thruDate" value="${(productCategoryRole. getTimestamp("thruDate"))?if_exists}" class="inputBox" <#if hasExpired> style="color: red;"</#if>>
            <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productCategoryRole.getTimestamp("thruDate"))?default(nowTimestamp?string)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
        </FORM>
    </td>
    <td align="center">
        <a href="<@ofbizUrl>removePartyFromCategory?productCategoryId=${(productCategoryRole.productCategoryId)?if_exists}&partyId=${(productCategoryRole.partyId)?if_exists}&roleTypeId=${(productCategoryRole.roleTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productCategoryRole.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
    </td>
    </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>addPartyToCategory</@ofbizUrl>" style="margin: 0;" name="addNewForm">
    <input type="hidden" name="productCategoryId" value="${productCategoryId}">
    
    <div class="head2">${uiLabelMap.ProductAssociatePartyToCategory}:</div>
    <br/>
    <input type="text" class="inputBox" size="20" maxlength="20" name="partyId" value="">
    <select name="roleTypeId" size="1" class="selectBox">
    <#list roleTypes as roleType>
        <option value="${(roleType.roleTypeId)?if_exists}" <#if roleType.roleTypeId.equals("_NA_")> ${uiLabelMap.ProductSelected}</#if>>${(roleType.get("description",locale))?if_exists}</option>
    </#list>
    </select>
    <input type="text" size="25" name="fromDate" class="inputBox">
    <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
    </form>
</#if>
