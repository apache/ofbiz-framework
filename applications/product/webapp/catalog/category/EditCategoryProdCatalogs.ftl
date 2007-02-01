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

<#if productCategoryId?exists && productCategory?exists>    
    <table border="1" cellpadding="2" cellspacing="0">
    <tr>
        <td><div class="tabletext"><b>${uiLabelMap.ProductCatalogNameId}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonType}</b></div></td>
        <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
        <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
        <td><div class="tabletext"><b>&nbsp;</b></div></td>
    </tr>
    <#assign line = 0>
    <#list prodCatalogCategories as prodCatalogCategory>
    <#assign line = line + 1>
    <#assign prodCatalog = prodCatalogCategory.getRelatedOne("ProdCatalog")>
    <#assign curProdCatalogCategoryType = prodCatalogCategory.getRelatedOneCache("ProdCatalogCategoryType")>
    <tr valign="middle">
        <td><a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${(prodCatalogCategory.prodCatalogId)?if_exists}</@ofbizUrl>" class="buttontext"><#if prodCatalog?exists>${(prodCatalog.catalogName)?if_exists}</#if> [${(prodCatalogCategory.prodCatalogId)?if_exists}]</a></td>
        <td>
            <div class="tabletext">${(curProdCatalogCategoryType.get("description",locale))?default(prodCatalogCategory.prodCatalogCategoryTypeId)}</div>
        </td>
        <#assign hasntStarted = false>
        <#if (prodCatalogCategory.getTimestamp("fromDate"))?exists && nowTimestamp.before(prodCatalogCategory.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
        <td><div class="tabletext"<#if hasntStarted> style="color: red;"</#if>>${(prodCatalogCategory.fromDate)?if_exists}</div></td>
        <td align="center">
            <form method="post" action="<@ofbizUrl>category_updateProductCategoryToProdCatalog</@ofbizUrl>" name="lineForm${line}">
                <#assign hasExpired = false>
                <#if (prodCatalogCategory.getTimestamp("thruDate"))?exists && nowTimestamp.after(prodCatalogCategory.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                <input type="hidden" name="prodCatalogId" value="${(prodCatalogCategory.prodCatalogId)?if_exists}"/>
                <input type="hidden" name="productCategoryId" value="${(prodCatalogCategory.productCategoryId)?if_exists}"/>
                <input type="hidden" name="prodCatalogCategoryTypeId" value="${prodCatalogCategory.prodCatalogCategoryTypeId}"/>
                <input type="hidden" name="fromDate" value="${(prodCatalogCategory.fromDate)?if_exists}"/>
                <input type="text" size="25" name="thruDate" value="${(prodCatalogCategory.thruDate)?if_exists}" class="inputBox" style="<#if (hasExpired) >color: red;</#if>"/>
                <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(prodCatalogCategory.thruDate)?default(nowTimestamp?string)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
                <input type="text" size="5" name="sequenceNum" value="${(prodCatalogCategory.sequenceNum)?if_exists}" class="inputBox"/>
                <#-- the prodCatalogCategoryTypeId field is now part of the PK, so it can't be changed, must be re-created
                <select name="prodCatalogCategoryTypeId" size="1" class="selectBox">
                    <#if (prodCatalogCategory.prodCatalogCategoryTypeId)?exists>
                        <option value="${prodCatalogCategory.prodCatalogCategoryTypeId}"><#if curProdCatalogCategoryType?exists>${(curProdCatalogCategoryType.description)?if_exists}<#else> [${(prodCatalogCategory.prodCatalogCategoryTypeId)}]</#if></option>
                        <option value="${prodCatalogCategory.prodCatalogCategoryTypeId}"></option>
                    <#else>
                        <option value="">&nbsp;</option>
                    </#if>
                    <#list prodCatalogCategoryTypes as prodCatalogCategoryType>
                    <option value="${(prodCatalogCategoryType.prodCatalogCategoryTypeId)?if_exists}">${(prodCatalogCategoryType.get("description",locale))?if_exists}</option>
                    </#list>
                </select> -->
                <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;"/>
            </form>
        </td>
        <td align="center">
        <a href="<@ofbizUrl>category_removeProductCategoryFromProdCatalog?prodCatalogId=${(prodCatalogCategory.prodCatalogId)?if_exists}&productCategoryId=${(prodCatalogCategory.productCategoryId)?if_exists}&prodCatalogCategoryTypeId=${(prodCatalogCategory.prodCatalogCategoryTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(prodCatalogCategory.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
        [${uiLabelMap.CommonDelete}]</a>
        </td>
    </tr>
    </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>category_addProductCategoryToProdCatalog</@ofbizUrl>" style="margin: 0;" name="addNewForm">
    <input type="hidden" name="productCategoryId" value="${productCategoryId?if_exists}"/>
    
    <div class="head2">${uiLabelMap.ProductAddCatalogProductCategory}:</div>
    <br/>
    <select name="prodCatalogId" class="selectBox">
    <#list prodCatalogs as prodCatalog>
        <option value="${(prodCatalog.prodCatalogId)?if_exists}">${(prodCatalog.catalogName)?if_exists} [${(prodCatalog.prodCatalogId)?if_exists}]</option>
    </#list>
    </select>
        <select name="prodCatalogCategoryTypeId" size="1" class="selectBox">
            <#list prodCatalogCategoryTypes as prodCatalogCategoryType>
            <option value="${(prodCatalogCategoryType.prodCatalogCategoryTypeId)?if_exists}">${(prodCatalogCategoryType.get("description",locale))?if_exists}</option>
            </#list>
        </select>
    <input type="text" size="25" name="fromDate" class="inputBox"/>
    <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
    <input type="submit" value="${uiLabelMap.CommonAdd}"/>
    </form>
</#if>
