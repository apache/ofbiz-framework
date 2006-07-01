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
<a href="<@ofbizUrl>attachProductFeaturesToCategory?productCategoryId=${productCategoryId?if_exists}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductFeatureCategoryAttach}]</a>
<#if productCategoryId?exists && productCategory?exists>    
    <#-- Feature Groups -->
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFeatureGroup}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list productFeatureCatGrpAppls as productFeatureCatGrpAppl>
    <#assign line = line + 1>
    <#assign productFeatureGroup = (productFeatureCatGrpAppl.getRelatedOne("ProductFeatureGroup"))?default(null)>
    <tr valign="middle">
        <td><a href="<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}</@ofbizUrl>" class="buttontext"><#if productFeatureGroup?exists>${(productFeatureGroup.description)?if_exists}</#if> [${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}]</a></td>
        <#assign hasntStarted = false>
        <#if (productFeatureCatGrpAppl.getTimestamp("fromDate"))?exists && nowTimestamp.before(productFeatureCatGrpAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
        <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(productFeatureCatGrpAppl.fromDate)?if_exists}</div></td>
        <td align="center">
            <FORM method="post" action="<@ofbizUrl>updateProductFeatureCatGrpAppl</@ofbizUrl>" name="lineFormGrp${line}">
                <#assign hasExpired = false>
                <#if (productFeatureCatGrpAppl.getTimestamp("thruDate"))?exists && nowTimestamp.after(productFeatureCatGrpAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                <input type="hidden" name="productCategoryId" value="${(productFeatureCatGrpAppl.productCategoryId)?if_exists}">
                <input type="hidden" name="productFeatureGroupId" value="${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}">
                <input type="hidden" name="fromDate" value="${(productFeatureCatGrpAppl.fromDate)?if_exists}">
                <input type="text" size="25" name="thruDate" value="${(productFeatureCatGrpAppl.thruDate)?if_exists}" class="inputBox" <#if hasExpired>style="color: red;"</#if>>
                <a href="javascript:call_cal(document.lineFormGrp${line}.thruDate, '${(productFeatureCatGrpAppl.thruDate)?default(nowTimestamp?string)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
                <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
            </FORM>
        </td>
        <td align="center">
        <a href="<@ofbizUrl>removeProductFeatureCatGrpAppl?productFeatureGroupId=${(productFeatureCatGrpAppl.productFeatureGroupId)?if_exists}&productCategoryId=${(productFeatureCatGrpAppl.productCategoryId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productFeatureCatGrpAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    <form method="post" action="<@ofbizUrl>createProductFeatureCatGrpAppl</@ofbizUrl>" style="margin: 0;" name="addNewGroupForm">
    <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
    
    <div class="head2">${uiLabelMap.ProductApplyFeatureGroupFromCategory}:</div>
    <select name="productFeatureGroupId" class="selectBox">
    <#list productFeatureGroups as productFeatureGroup>
        <option value="${(productFeatureGroup.productFeatureGroupId)?if_exists}">${(productFeatureGroup.description)?if_exists} [${(productFeatureGroup.productFeatureGroupId)?if_exists}]</option>
    </#list>
    </select>
    <input type="text" size="25" name="fromDate" class="inputBox">
    <a href="javascript:call_cal(document.addNewGroupForm.fromDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
    </form>
    <br/>
    <br/>

    <#-- Feature Categories -->
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductFeatureCategory}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list productFeatureCategoryAppls as productFeatureCategoryAppl>
    <#assign line = line + 1>
    <#assign productFeatureCategory = (productFeatureCategoryAppl.getRelatedOne("ProductFeatureCategory"))?default(null)>
    <tr valign="middle">
        <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}</@ofbizUrl>" class="buttontext"><#if productFeatureCategory?exists>${(productFeatureCategory.description)?if_exists}</#if> [${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}]</a></td>
        <#assign hasntStarted = false>
        <#if (productFeatureCategoryAppl.getTimestamp("fromDate"))?exists && nowTimestamp.before(productFeatureCategoryAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
        <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(productFeatureCategoryAppl.fromDate)?if_exists}</div></td>
        <td align="center">
            <FORM method="post" action="<@ofbizUrl>updateProductFeatureCategoryAppl</@ofbizUrl>" name="lineForm${line}">
                <#assign hasExpired = false>
                <#if (productFeatureCategoryAppl.getTimestamp("thruDate"))?exists && nowTimestamp.after(productFeatureCategoryAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                <input type="hidden" name="productCategoryId" value="${(productFeatureCategoryAppl.productCategoryId)?if_exists}">
                <input type="hidden" name="productFeatureCategoryId" value="${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}">
                <input type="hidden" name="fromDate" value="${(productFeatureCategoryAppl.fromDate)?if_exists}">
                <input type="text" size="25" name="thruDate" value="${(productFeatureCategoryAppl.thruDate)?if_exists}" class="inputBox" <#if hasExpired>style="color: red;"</#if>>
                <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productFeatureCategoryAppl.thruDate)?default(nowTimestamp?string)}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
                <INPUT type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
            </FORM>
        </td>
        <td align="center">
        <a href="<@ofbizUrl>removeProductFeatureCategoryAppl?productFeatureCategoryId=${(productFeatureCategoryAppl.productFeatureCategoryId)?if_exists}&productCategoryId=${(productFeatureCategoryAppl.productCategoryId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productFeatureCategoryAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    <form method="post" action="<@ofbizUrl>createProductFeatureCategoryAppl</@ofbizUrl>" style="margin: 0;" name="addNewCategoryForm">
    <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}">
    
    <div class="head2">${uiLabelMap.ProductApplyFeatureGroupToCategory}:</div>
    <select name="productFeatureCategoryId" class="selectBox">
    <#list productFeatureCategories as productFeatureCategory>
        <option value="${(productFeatureCategory.productFeatureCategoryId)?if_exists}">${(productFeatureCategory.description)?if_exists} [${(productFeatureCategory.productFeatureCategoryId)?if_exists}]</option>
    </#list>
    </select>
    <input type="text" size="25" name="fromDate" class="inputBox">
    <a href="javascript:call_cal(document.addNewCategoryForm.fromDate, '${nowTimestamp?string}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}">
    </form>
</#if>    
