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
  <#if activeOnly>
    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&userEntered=${userEntered?string}&activeOnly=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonActiveInactive}]</a>
  <#else>
    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&userEntered=${userEntered?string}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonActiveOnly}]</a>
  </#if>
  <#if userEntered>
    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&activeOnly=${activeOnly?string}&userEntered=false</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonUserAutoEntered}]</a>
  <#else>
    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&activeOnly=${activeOnly?string}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonUserEnteredOnly}]</a>
  </#if>
  <br/>
  <div class="head3">${uiLabelMap.CommonShow}
    <#if activeOnly>${uiLabelMap.CommonActiveOnly}<#else>${uiLabelMap.CommonActiveInactive}</#if>
    ${uiLabelMap.CommonAnd}
    <#if userEntered>${uiLabelMap.CommonUserEnteredOnly}<#else>${uiLabelMap.CommonUserAutoEntered}</#if>
  </div>

    <#if productStoreId?exists && productStore?exists>
        <#if (listSize > 0)>
            <table border="0" cellpadding="2">
                <tr>
                <td align="right">
                    <b>
                    <#if (viewIndex > 0)>
                    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}&activeOnly=${activeOnly.toString()}&userEntered=${userEntered.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                    </#if>
                    <#if (listSize > 0)>
                        <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
                    </#if>
                    <#if (listSize > highIndex)>
                    | <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}&activeOnly=${activeOnly.toString()}&userEntered=${userEntered.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                    </#if>
                    </b>
                </td>
                </tr>
            </table>
        </#if>
        
        <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductPromoNameId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
            <td align="center"><div class="tabletext"><b>${uiLabelMap.ProductThruDateTimeSequence}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
        </tr>
        <#if (listSize > 0)>
            <#assign line = 0>
            <#list productStorePromoAndAppls[lowIndex..highIndex-1] as productStorePromoAndAppl>
                <#assign line = line+1>
                <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProductPromo?productPromoId=${(productStorePromoAndAppl.productPromoId)?if_exists}</@ofbizUrl>" class="buttontext">${(productStorePromoAndAppl.promoName)?if_exists} [${(productStorePromoAndAppl.productPromoId)?if_exists}]</a></td>
                <#assign hasntStarted = false>
                <#if productStorePromoAndAppl.getTimestamp("fromDate")?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productStorePromoAndAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true> </#if>
                <td><div class="tabletext" <#if hasntStarted> style="color: red;"</#if> >${productStorePromoAndAppl.getTimestamp("fromDate").toString()}</div></td>
                <td align="center">
                    <#assign hasExpired = false>
                    <#if productStorePromoAndAppl.getTimestamp("thruDate")?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productStorePromoAndAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                    <form method="post" action="<@ofbizUrl>updateProductStorePromoAppl</@ofbizUrl>" name="lineForm${line}">
                        <input type="hidden" name="productStoreId" value="${(productStorePromoAndAppl.productStoreId)?if_exists}">
                        <input type="hidden" name="productPromoId" value="${(productStorePromoAndAppl.productPromoId)?if_exists}">
                        <input type="hidden" name="fromDate" value="${(productStorePromoAndAppl.fromDate)?if_exists}">
                        <input type="text" size="25" name="thruDate" value="${(productStorePromoAndAppl.thruDate)?if_exists}" class="inputBox" style="<#if (hasExpired) >color: red;</#if>">
                        <a href="javascript:call_cal(document.lineForm${line}.thruDate, '${(productStorePromoAndAppl.thruDate)?default(nowTimestampString)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                        <input type="text" size="5" name="sequenceNum" value="${(productStorePromoAndAppl.sequenceNum)?if_exists}" class="inputBox">
                        <input type="submit" value="${uiLabelMap.CommonUpdate}" style="font-size: x-small;">
                    </form>
                </td>
                <td align="center">
                    <a href="<@ofbizUrl>deleteProductStorePromoAppl?productStoreId=${(productStorePromoAndAppl.productStoreId)?if_exists}&productPromoId=${(productStorePromoAndAppl.productPromoId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productStorePromoAndAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>" class="buttontext">
                    [${uiLabelMap.CommonDelete}]</a>
                </td>
                </tr>
            </#list>
        </#if>
        </table>
        <#if (listSize > 0)>
            <table border="0" cellpadding="2">
                <tr>
                <td align="right">
                    <b>
                    <#if (viewIndex > 0)>
                    <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex-1}&activeOnly=${activeOnly.toString()}&userEntered=${userEntered.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonPrevious}]</a> |
                    </#if>
                    <#if (listSize > 0)>
                        <span class="tabletext">${lowIndex+1} - ${highIndex} ${uiLabelMap.CommonOf} ${listSize}</span>
                    </#if>
                    <#if (listSize > highIndex)>
                    | <a href="<@ofbizUrl>EditProductStorePromos?productStoreId=${productStoreId}&VIEW_SIZE=${viewSize}&VIEW_INDEX=${viewIndex+1}&activeOnly=${activeOnly.toString()}&userEntered=${userEntered.toString()}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNext}]</a>
                    </#if>
                    </b>
                </td>
                </tr>
            </table>
        </#if>
        
        <br/>
        <form method="post" action="<@ofbizUrl>createProductStorePromoAppl</@ofbizUrl>" style="margin: 0;" name="addNewForm">
        <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}"/>
        <input type="hidden" name="tryEntity" value="true"/>
        
        <div class="head2">${uiLabelMap.ProductAddStorePromoOptionalDate}:</div>
        <br/>
        <select name="productPromoId" class="selectBox">
        <#list productPromos as productPromo>
            <option value="${productPromo.productPromoId?if_exists}">${productPromo.promoName?if_exists} [${productPromo.productPromoId?if_exists}]</option>
        </#list>
        </select> <span class="tabletext">${uiLabelMap.ProductNoteUserPromotionEntered}</span> 
        <br/>
        <input type="text" size="25" name="fromDate" class="inputBox"/>
        <a href="javascript:call_cal(document.addNewForm.fromDate, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
        </form>
    </#if>
