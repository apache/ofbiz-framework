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
                    <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
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
        <a href="javascript:call_cal(document.addProductPromoToCatalog.fromDate, '${nowTimestamp.toString()}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
        </form>
   </#if>
