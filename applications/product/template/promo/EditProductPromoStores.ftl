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
<#if productPromoId?? && productPromo??>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.PageTitleEditProductPromoStores}</h3>
        </div>
        <div class="screenlet-body">
            <table cellspacing="0" class="basic-table">
                <tr class="header-row">
                    <td><b>${uiLabelMap.ProductStoreNameId}</b></td>
                    <td><b>${uiLabelMap.CommonFromDateTime}</b></td>
                    <td align="center"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></td>
                    <td><b>&nbsp;</b></td>
                </tr>
                <#assign line = 0>
                <#assign rowClass = "2">
                <#list productStorePromoAppls as productStorePromoAppl>
                <#assign line = line + 1>
                <#assign productStore = productStorePromoAppl.getRelatedOne("ProductStore", false)>
                <tr valign="middle"<#if "1" == rowClass> class="alternate-row"</#if>>
                    <td><a href="<@ofbizUrl>EditProductStore?productStoreId=${productStorePromoAppl.productStoreId}</@ofbizUrl>" class="buttontext"><#if productStore??>${(productStore.storeName)!}</#if>[${productStorePromoAppl.productStoreId}]</a></td>
                    <#assign hasntStarted = false>
                    <#if (productStorePromoAppl.getTimestamp("fromDate"))?? && nowTimestamp.before(productStorePromoAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
                    <td <#if hasntStarted>style="color: red;"</#if>>${productStorePromoAppl.fromDate!}</td>
                    <td align="center">
                        <#assign hasExpired = false>
                        <#if (productStorePromoAppl.getTimestamp("thruDate"))?? && nowTimestamp.after(productStorePromoAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                        <form method="post" action="<@ofbizUrl>promo_updateProductStorePromoAppl</@ofbizUrl>" name="lineForm${line}">
                            <input type="hidden" name="productStoreId" value="${productStorePromoAppl.productStoreId}" />
                            <input type="hidden" name="productPromoId" value="${productStorePromoAppl.productPromoId}" />
                            <input type="hidden" name="fromDate" value="${productStorePromoAppl.fromDate}" />
                            <#if hasExpired><#assign class="alert"><#else><#assign class=""></#if>
                            <@htmlTemplate.renderDateTimeField name="thruDate" event="" action="" className="${class!}" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="${(productStorePromoAppl.thruDate)!}" size="25" maxlength="30" id="thruDate_${productStorePromoAppl_index}" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                            <input type="text" size="5" name="sequenceNum" value="${(productStorePromoAppl.sequenceNum)!}" />
                            <input type="submit" value="${uiLabelMap.CommonUpdate}" />
                        </form>
                    </td>
                    <td align="center">
                       <form method="post" action="<@ofbizUrl>promo_deleteProductStorePromoAppl</@ofbizUrl>">
                           <input type="hidden" name="productStoreId" value="${productStorePromoAppl.productStoreId}" />
                           <input type="hidden" name="productPromoId" value="${productStorePromoAppl.productPromoId}" />
                           <input type="hidden" name="fromDate" value="${productStorePromoAppl.fromDate}" />
                           <input type="submit" value="${uiLabelMap.CommonDelete}" />
                       </form>
                    </td>
                </tr>
                <#-- toggle the row color -->
                <#if "2" == rowClass>
                    <#assign rowClass = "1">
                <#else>
                    <#assign rowClass = "2">
                </#if>
                </#list>
            </table>
        </div>
    </div>
    <div class="screenlet">
        <div class="screenlet-title-bar">
            <h3>${uiLabelMap.ProductAddStorePromo}</h3>
        </div>
        <div class="screenlet-body">
            <form method="post" action="<@ofbizUrl>promo_createProductStorePromoAppl</@ofbizUrl>" name="addProductPromoToCatalog">
                <input type="hidden" name="productPromoId" value="${productPromoId}"/>
                <input type="hidden" name="tryEntity" value="true"/>
                <select name="productStoreId">
                <#list productStores as productStore>
                    <option value="${(productStore.productStoreId)!}">${(productStore.storeName)!} [${(productStore.productStoreId)!}]</option>
                </#list>
                </select>
                <@htmlTemplate.renderDateTimeField name="fromDate" event="" action="" className="" alert="" title="Format: yyyy-MM-dd HH:mm:ss.SSS" value="" size="25" maxlength="30" id="fromDate1" dateType="date" shortDateInput=false timeDropdownParamName="" defaultDateTimeString="" localizedIconTitle="" timeDropdown="" timeHourName="" classString="" hour1="" hour2="" timeMinutesName="" minutes="" isTwelveHour="" ampmName="" amSelected="" pmSelected="" compositeType="" formName=""/>
                <input type="submit" value="${uiLabelMap.CommonAdd}"/>
            </form>
        </div>
    </div>
</#if>