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
 *@author     Brad Steiner (bsteiner@thehungersite.com)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
<#if productId?exists && product?exists>
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tableheadtext">${uiLabelMap.ProductAccountType}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.ProductOrganization}</div></td>
        <td align="center"><div class="tableheadtext">${uiLabelMap.ProductGlAccount}</div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list productGlAccounts as productGlAccount>
    <#assign line = line + 1>
    <#assign productGlAccountType = productGlAccount.getRelatedOneCache("GlAccountType")>
    <#assign curGlAccount = productGlAccount.getRelatedOneCache("GlAccount")>
    <tr valign="middle">
        <td><div class="tabletext"><#if productGlAccountType?exists>${(productGlAccountType.get("description",locale))?if_exists}<#else>[${(productGlAccount.productGlAccountTypeId)?if_exists}]</#if></div></td>
        <td><div class="tabletext">
           ${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, productGlAccount.getString("organizationPartyId"), true)} [${productGlAccount.organizationPartyId}]
        </div></td>
        <td align="center">
            <form method="post" action="<@ofbizUrl>updateProductGlAccount</@ofbizUrl>" name="lineForm${line}">
                <input type="hidden" name="productId" value="${(productGlAccount.productId)?if_exists}"/>
                <input type="hidden" name="glAccountTypeId" value="${(productGlAccount.glAccountTypeId)?if_exists}"/>
                <input type="hidden" name="organizationPartyId" value="${productGlAccount.organizationPartyId?if_exists}"/>
                <select class="selectBox" name="glAccountId">
                    <#if curGlAccount?exists>
                        <option value="${(curGlAccount.glAccountId)?if_exists}">${(curGlAccount.accountCode)?if_exists} ${(curGlAccount.accountName)?if_exists}</option>
                        <option value="${(curGlAccount.glAccountId)?if_exists}"></option>
                    </#if>
                    <#list glAccounts as glAccount>
                        <option value="${(glAccount.glAccountId)?if_exists}">${(glAccount.accountCode)?if_exists} ${(glAccount.accountName)?if_exists}</option>
                    </#list>
                </select>
                <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
            </form>
        </td>
        <td align="center">
        <a href="<@ofbizUrl>deleteProductGlAccount?productId=${(productGlAccount.productId)?if_exists}&glAccountTypeId=${(productGlAccount.glAccountTypeId)?if_exists}&organizationPartyId=${productGlAccount.organizationPartyId?if_exists}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>createProductGlAccount</@ofbizUrl>" style="margin: 0;" name="createProductGlAccountForm">
        <input type="hidden" name="productId" value="${productId}"/>
        <input type="hidden" name="useValues" value="true"/>
    
        <div class="head2">${uiLabelMap.ProductAddGlAccount} :</div>
        <div class="tabletext">
            ${uiLabelMap.ProductAccountType} :
            <select name="glAccountTypeId" class="selectBox">
                <#list productGlAccountTypes as productGlAccountType>
                    <option value="${(productGlAccountType.glAccountTypeId)?if_exists}">${(productGlAccountType.get("description",locale))?if_exists}</option>
                </#list>
            </select><br/>
            ${uiLabelMap.ProductGlAccount} : 
            <select name="glAccountId" class="inputBox">
                <#list glAccounts as glAccount>
                    <option value="${(glAccount.glAccountId)?if_exists}">${(glAccount.accountCode)?if_exists} ${(glAccount.accountName)?if_exists}</option>
                </#list>
            </select><br/>
            ${uiLabelMap.ProductOrganization} :
            <select class="selectBox" name="organizationPartyId">
                 <#list organizations as organization>
                     <option value="${organization.partyId?if_exists}">${Static["org.ofbiz.party.party.PartyHelper"].getPartyName(delegator, organization.getString("partyId"), true)} [${organization.partyId}]</option>
                 </#list>
            </select>
            <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;"/>
        </div>        
    </form>
</#if>
