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
