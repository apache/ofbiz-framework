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
    <#if activeOnly>
        <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&activeOnly=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveAndInactive}]</a>
    <#else>
        <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&activeOnly=true</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductActiveOnly}]</a>
    </#if>    
    <#if productCategoryId?exists && productCategory?exists>
        <#if (listSize > 0)>
            <table border="0" cellpadding="2">
                <tr>
                <td align="right">
                    <span class="tabletext">
                    <b>
                    <#if (viewIndex > 1)>
                    <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}&activeOnly=${activeOnly.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                    </#if>
                    ${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
                    <#if (listSize > highIndex)>
                    | <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}&activeOnly=${activeOnly.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                    </#if>
                    </b>
                    </span>
                </td>
                </tr>
            </table>
        </#if>
        
        <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductProductNameId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
            <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequenceQuantity}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
        </tr>
        <#if (listSize > 0)>
            <#assign line = 0>
            <#list productCategoryMembers as productCategoryMember>
            <#assign product = productCategoryMember.getRelatedOne("Product")>
            <#assign hasntStarted = false>
            <#if productCategoryMember.fromDate?exists && nowTimestamp.before(productCategoryMember.getTimestamp("fromDate"))><#assign hasntStarted = true></#if>
            <#assign hasExpired = false>
            <#if productCategoryMember.thruDate?exists && nowTimestamp.after(productCategoryMember.getTimestamp("thruDate"))><#assign hasExpired = true></#if>
            <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProduct?productId=${(productCategoryMember.productId)?if_exists}</@ofbizUrl>" class="buttontext"><#if product?exists>${(product.internalName)?if_exists}</#if> [${(productCategoryMember.productId)?if_exists}]</a></td>
                <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(productCategoryMember.fromDate)?if_exists}</div></td>
                <td align="center">
                    <form method="post" action="<@ofbizUrl>updateCategoryProductMember?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}</@ofbizUrl>" name="lineForm${line}">
                        <input type="hidden" name="activeOnly" value="${activeOnly.toString()}">
                        <input type="hidden" name="productId" value="${(productCategoryMember.productId)?if_exists}">
                        <input type="hidden" name="productCategoryId" value="${(productCategoryMember.productCategoryId)?if_exists}">
                        <input type="hidden" name="fromDate" value="${(productCategoryMember.fromDate)?if_exists}">
                        <input type="text" size="25" name="thruDate" value="${(productCategoryMember.thruDate)?if_exists}" class="inputBox" <#if hasExpired>style="color: red;"</#if>>
                        <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productCategoryMember.thruDate)?default(nowTimestamp?string)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
                        <input type="text" size="5" name="sequenceNum" value="${(productCategoryMember.sequenceNum)?if_exists}" class="inputBox">
                        <input type="text" size="5" name="quantity" value="${(productCategoryMember.quantity)?if_exists}" class="inputBox">
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                    </form>
                </td>
                <td align="center">
                <a href="<@ofbizUrl>removeCategoryProductMember?VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex}&productId=${(productCategoryMember.productId)?if_exists}&productCategoryId=${(productCategoryMember.productCategoryId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue((productCategoryMember.getTimestamp("fromDate").toString()))}&activeOnly=${activeOnly.toString()}</@ofbizUrl>" class="buttontext">
                [${uiLabelMap.CommonDelete}]</a>
                </td>
            </tr>
            <#assign line = line + 1>
            </#list>
        </#if>
        </table>
        
        <#if (listSize > 0)>
            <table border="0" cellpadding="2">
                <tr>
                <td align="right">
                    <span class="tabletext">
                    <b>
                    <#if (viewIndex > 1)>
                        <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}&activeOnly=${activeOnly.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                        </#if>
                        ${lowIndex} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}
                        <#if (listSize > highIndex)>
                        | <a href="<@ofbizUrl>EditCategoryProducts?productCategoryId=${productCategoryId?if_exists}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}&activeOnly=${activeOnly.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                    </#if>
                    </b>
                    </span>
                </td>
                </tr>
            </table>
        </#if>
        <br/>
        <form method="post" action="<@ofbizUrl>addCategoryProductMember</@ofbizUrl>" style="margin: 0;" name="addProductCategoryMemberForm">
        <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
        <input type="hidden" name="activeOnly" value="${activeOnly.toString()}">
        
        <div class="head2">${uiLabelMap.ProductAddProductCategoryMember}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductProductId}: <input type="text" size="20" name="productId" class="inputBox">
            <a href="javascript:call_fieldlookup2(document.addProductCategoryMemberForm.productId, 'LookupProduct');"><img src="/content/images/fieldlookup.gif" width="16" height="16" border="0" alt="Lookup"></a>
            ${uiLabelMap.CommonFromDate}: <input type="text" size="22" name="fromDate" class="inputBox">
            <a href="javascript:call_cal(document.addProductCategoryMemberForm.fromDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <input type="submit" value="${uiLabelMap.CommonAdd}">
        </div>
        </form>
        
        <br/>
        <form method="post" action="<@ofbizUrl>copyCategoryProductMembers</@ofbizUrl>" style="margin: 0;" name="copyCategoryProductMembersForm">
        <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
        <input type="hidden" name="activeOnly" value="${activeOnly.toString()}">
        
        <div class="head2">${uiLabelMap.ProductCopyProductCategoryMembersToAnotherCategory}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductTargetProductCategory}:
            <select name="productCategoryIdTo" class="selectBox">
            <option value=""></option>
            <#list productCategories as productCategoryTo>
                <option value="${(productCategoryTo.productCategoryId)?if_exists}">${(productCategoryTo.description)?if_exists}  [${(productCategoryTo.productCategoryId)?if_exists}]</option>
            </#list>
            </select>
            <br/>
            ${uiLabelMap.ProductOptionalFilterWithDate}: <input type="text" size="20" name="validDate" class="inputBox">
            <a href="javascript:call_cal(document.copyCategoryProductMembersForm.validDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            <br/>
            ${uiLabelMap.ProductIncludeSubCategories}?
            <select name="recurse" class="selectBox">
                <option>${uiLabelMap.CommonN}</option>
                <option>${uiLabelMap.CommonY}</option>
            </select>
            <input type="submit" value="${uiLabelMap.CommonCopy}">
        </div>
        </form>
        
        <br/>
        <form method="post" action="<@ofbizUrl>expireAllCategoryProductMembers</@ofbizUrl>" style="margin: 0;" name="expireAllCategoryProductMembersForm">
        <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
        <input type="hidden" name="activeOnly" value="${activeOnly.toString()}">
        
        <div class="head2">${uiLabelMap.ProductExpireAllProductMembers}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductOptionalExpirationDate}: <input type="text" size="20" name="thruDate" class="inputBox">
            <a href="javascript:call_cal(document.expireAllCategoryProductMembersForm.thruDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            &nbsp;&nbsp;<input type="submit" value="${uiLabelMap.CommonExpireAll}">
        </div>
        </form>
        <br/>
        <form method="post" action="<@ofbizUrl>removeExpiredCategoryProductMembers</@ofbizUrl>" style="margin: 0;" name="removeExpiredCategoryProductMembersForm">
        <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
        <input type="hidden" name="activeOnly" value="${activeOnly.toString()}">
        
        <div class="head2">${uiLabelMap.ProductRemoveExpiredProductMembers}:</div>
        <div class="tabletext">
            ${uiLabelMap.ProductOptionalExpiredBeforeDate}: <input type="text" size="20" name="validDate" class="inputBox">
            <a href="javascript:call_cal(document.removeExpiredCategoryProductMembersForm.validDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
            &nbsp;&nbsp;<input type="submit" value="${uiLabelMap.CommonRemoveExpired}">
        </div>
        </form>
    </#if>
