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
    
    <br/>
    <#if productPromoId?exists && productPromo?exists>   
        <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductStoreNameId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
            <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
        </tr>
        <#assign line = 0>        
        <#list productStorePromoAppls as productStorePromoAppl>
        <#assign line = line + 1>
        <#assign productStore = productStorePromoAppl.getRelatedOne("ProductStore")>
        <tr valign="middle">
            <td><a href="<@ofbizUrl>EditProductStore?productStoreId=${productStorePromoAppl.productStoreId}</@ofbizUrl>" class="buttontext"><#if productStore?exists>${(productStore.storeName)?if_exists}</#if>[${productStorePromoAppl.productStoreId}]</a></td>
            <#assign hasntStarted = false>
            <#if (productStorePromoAppl.getTimestamp("fromDate"))?exists && nowTimestamp.before(productStorePromoAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
            <td><div class="tabletext" <#if hasntStarted>style="color: red;"</#if>>${productStorePromoAppl.fromDate?if_exists}</div></td>
            <td align="center">
                <#assign hasExpired = false>
                <#if (productStorePromoAppl.getTimestamp("thruDate"))?exists && nowTimestamp.after(productStorePromoAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                <form method="post" action="<@ofbizUrl>promo_updateProductStorePromoAppl</@ofbizUrl>" name="lineForm${line}">
                    <input type="hidden" name="productStoreId" value="${productStorePromoAppl.productStoreId}">
                    <input type="hidden" name="productPromoId" value="${productStorePromoAppl.productPromoId}">
                    <input type="hidden" name="fromDate" value="${productStorePromoAppl.fromDate}">
                    <input type="text" size="20" name="thruDate" value="${(productStorePromoAppl.thruDate.toString())?if_exists}" class="inputBox" <#if hasExpired>style="color: red;"</#if>>
                    <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${nowTimestamp.toString()}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
                    <input type="text" size="5" name="sequenceNum" value="${(productStorePromoAppl.sequenceNum)?if_exists}" class="inputBox">
                    <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                </form>
            </td>
            <td align="center">
            <a href="<@ofbizUrl>promo_deleteProductStorePromoAppl?productStoreId=${(productStorePromoAppl.productStoreId)?if_exists}&productPromoId=${(productStorePromoAppl.productPromoId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productStorePromoAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
            [${uiLabelMap.CommonDelete}]</a>
            </td>
        </tr>
        </#list>
        </table>
        <br/>
        <form method="post" action="<@ofbizUrl>promo_createProductStorePromoAppl</@ofbizUrl>" name="addProductPromoToCatalog" style="margin: 0;">
        <input type="hidden" name="productPromoId" value="${productPromoId}"/>
        <input type="hidden" name="tryEntity" value="true"/>
        
        <div class="head2">${uiLabelMap.ProductAddStorePromo} :</div>
        <br/>
        <select name="productStoreId" class="selectBox">
        <#list productStores as productStore>
            <option value="${(productStore.productStoreId)?if_exists}">${(productStore.storeName)?if_exists} [${(productStore.productStoreId)?if_exists}]</option>
        </#list>
        </select>
        <input type="text" size="20" name="fromDate" class="inputBox"/>
        <a href="javascript:call_cal(document.addProductPromoToCatalog.fromDate, '${nowTimestamp.toString()}');"><img src="/images/cal.gif" width="16" height="16" border="0" alt="Calendar"></a>
        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
        </form>
   </#if>
