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
    <#if productId?exists>    
        <table border="1" cellpadding="2" cellspacing="0">
        <form method="post" action="<@ofbizUrl>UpdateFeatureToProductApplication</@ofbizUrl>" name="selectAllForm">
        <input type="hidden" name="_useRowSubmit" value="Y">
        <input type="hidden" name="_checkGlobalScope" value="Y">
        <input type="hidden" name="productId" value="${productId}">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.CommonId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductType}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductCategory}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDate}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductThruDateAmountSequenceApplicationType}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonAll}<input type="checkbox" name="selectAll" value="${uiLabelMap.CommonY}" onclick="javascript:toggleAll(this, 'selectAllForm');"></div></td>
           <!--<td><div class="tabletext">&nbsp;</div></td>-->
        </tr>
        <#assign rowCount = 0>
        <#list productFeatureAndAppls as productFeatureAndAppl>
            <#assign curProductFeatureType = productFeatureAndAppl.getRelatedOneCache("ProductFeatureType")>
            <#assign curProductFeatureApplType = productFeatureAndAppl.getRelatedOneCache("ProductFeatureApplType")>
            <#assign curProductFeatureCategory = (productFeatureAndAppl.getRelatedOneCache("ProductFeatureCategory")?if_exists)>
<!--            <#if curProductFeatureCategory?exists> pageContext.setAttribute("curProductFeatureCategory", curProductFeatureCategory)</#if> -->
            <tr valign="middle">
                <input type="hidden" name="productId_o_${rowCount}" value="${(productFeatureAndAppl.productId)?if_exists}">
                <input type="hidden" name="productFeatureId_o_${rowCount}" value="${(productFeatureAndAppl.productFeatureId)?if_exists}">
                <input type="hidden" name="fromDate_o_${rowCount}" value="${(productFeatureAndAppl.fromDate)?if_exists}">
                <td><div class="tabletext">${(productFeatureAndAppl.productFeatureId)?if_exists}</div></td>
                <td><div class="tabletext">${(productFeatureAndAppl.get("description",locale))?if_exists}</div></td>
                <td><div class="tabletext">${(curProductFeatureType.get("description",locale))?default((productFeatureAndAppl.productFeatureTypeId)?if_exists)}</div></td>
                <td><a href="<@ofbizUrl>EditFeatureCategoryFeatures?productFeatureCategoryId=${(productFeatureAndAppl.productFeatureCategoryId)?if_exists}&productId=${(productFeatureAndAppl.productId)?if_exists}</@ofbizUrl>" class="buttontext">
                    ${(curProductFeatureCategory.description)?if_exists}
                    [${(productFeatureAndAppl.productFeatureCategoryId)?if_exists}]</a></td>
                <#assign hasntStarted = false>
                <#if (productFeatureAndAppl.getTimestamp("fromDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().before(productFeatureAndAppl.getTimestamp("fromDate"))> <#assign hasntStarted = true></#if>
                <td><div class='tabletext'<#if hasntStarted> style='color: red;'</#if>>${(productFeatureAndAppl.fromDate)?if_exists}</div></td>
                <td>
                    <#assign hasExpired = false>
                    <#if (productFeatureAndAppl.getTimestamp("thruDate"))?exists && Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().after(productFeatureAndAppl.getTimestamp("thruDate"))> <#assign hasExpired = true></#if>
                    <input type='text' size='25' name='thruDate_o_${rowCount}' value='${(productFeatureAndAppl.thruDate)?if_exists}' class='inputBox' <#if hasExpired> style='color: red;'</#if>>
                    <a href="javascript:call_cal(document.selectAllForm.thruDate_o_${rowCount}, '${(productFeatureAndAppl.thruDate)?default(nowTimestamp?string)}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
                    <input type="text" size='6' name='amount_o_${rowCount}' value='${(productFeatureAndAppl.amount)?if_exists}'  class='inputBox'>
                    <input type="text" size='5' name='sequenceNum_o_${rowCount}' value='${(productFeatureAndAppl.sequenceNum)?if_exists}' class='inputBox'>
                <select class='selectBox' name='productFeatureApplTypeId_o_${rowCount}' size="1">
                    <#if (productFeatureAndAppl.productFeatureApplTypeId)?exists>
                        <option value='${(productFeatureAndAppl.productFeatureApplTypeId)?if_exists}'><#if curProductFeatureApplType?exists> ${(curProductFeatureApplType.get("description",locale))?if_exists} <#else> [${productFeatureAndAppl.productFeatureApplTypeId}]</#if></option>
                        <option value='${productFeatureAndAppl.productFeatureApplTypeId}'> </option>
                    </#if>
                    <#list productFeatureApplTypes as productFeatureApplType>
                        <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}'>${(productFeatureApplType.get("description",locale))?if_exists} </option>
                    </#list>
                </select>
                </td>
                <td align="right">              
                    <input type="checkbox" name="_rowSubmit_o_${rowCount}" value="Y" onclick="javascript:checkToggle(this, 'selectAllForm');">
                </td>
                <td>
                <a href='<@ofbizUrl>RemoveFeatureFromProduct?productId=${(productFeatureAndAppl.productId)?if_exists}&productFeatureId=${(productFeatureAndAppl.productFeatureId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(productFeatureAndAppl.getTimestamp("fromDate").toString())}</@ofbizUrl>' class="buttontext">
                 [${uiLabelMap.CommonDelete}]</a>
                </td>
            </tr>
            <#assign rowCount = rowCount + 1>
        </#list>
        <input type="hidden" name="_rowCount" value="${rowCount}">
        <tr><td colspan="8" align="center"><input type="submit" value='${uiLabelMap.CommonUpdate}' style='font-size: x-small;'/></td></tr>
        </form>
        </table>

        <br/>
        <form method="post" action="<@ofbizUrl>ApplyFeaturesFromCategory</@ofbizUrl>" style='margin: 0;'>
        <input type="hidden" name="productId" value="${productId}">
        <div class='head2'>${uiLabelMap.ProductAddProductFeatureFromCategory}:</div>
        <br/>
        <select class='selectBox' name='productFeatureCategoryId' size="1">
            <option value='' selected>${uiLabelMap.ProductChooseFeatureCategory}</option>
            <#list productFeatureCategories as productFeatureCategory>
                <option value='${(productFeatureCategory.productFeatureCategoryId)?if_exists}'>${(productFeatureCategory.description)?if_exists} [${(productFeatureCategory.productFeatureCategoryId)?if_exists}]</option>
            </#list>
        </select>
        <select class='selectBox' name='productFeatureGroupId' size="1">
            <option value='' selected>${uiLabelMap.ProductChooseFeatureGroup}</option>
            <#list productFeatureGroups as productFeatureGroup>
                <option value='${(productFeatureGroup.productFeatureGroupId)?if_exists}'>${(productFeatureGroup.description)?if_exists} [${(productFeatureGroup.productFeatureGroupId)?if_exists}]</option>
            </#list>
        </select>
        <span class='tabletext'>${uiLabelMap.ProductFeatureApplicationType}: </span>
        <select class='selectBox' name='productFeatureApplTypeId' size="1">
            <#list productFeatureApplTypes as productFeatureApplType>
               <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}' 
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected</#if>
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId?if_exists =="STANDARD_FEATURE")>selected</#if>
               >${(productFeatureApplType.get("description",locale))?if_exists} </option>           
            </#list>
        </select>
        <input type="submit" value='${uiLabelMap.CommonAdd}' style='font-size: x-small;'>
        </form>

        <br/>

        <form method="post" action="<@ofbizUrl>ApplyFeatureToProductFromTypeAndCode</@ofbizUrl>" style='margin: 0;' name='addFeatureByTypeIdCode'>
        <input type="hidden" name="productId" value="${productId}">
        <div class='head2'>${uiLabelMap.ProductAddProductFeatureTypeId}:</div>
        <br/>
        <span class='tabletext'>${uiLabelMap.ProductFeatureType}: </span><select class='selectBox' name='productFeatureTypeId' size="1">
            <#list productFeatureTypes as productFeatureType>
            <option value='${(productFeatureType.productFeatureTypeId)?if_exists}'>${(productFeatureType.get("description",locale))?if_exists} </option>
            </#list>
        </select>
        <span class='tabletext'>${uiLabelMap.CommonIdCode}: </span><input type="text" size='10' name='idCode' value='' class='inputBox'>
        <br/>
        <span class='tabletext'>${uiLabelMap.ProductFeatureApplicationType}: </span>
        <select class='selectBox' name='productFeatureApplTypeId' size="1">
            <#list productFeatureApplTypes as productFeatureApplType>
               <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}' 
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected</#if>
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId =="STANDARD_FEATURE")>selected</#if>
               >${(productFeatureApplType.get("description",locale))?if_exists} </option>           
            </#list>
        </select>
        <br/>
        <span class='tabletext'>${uiLabelMap.CommonFrom} : </span><input type="text" size='25' name='fromDate' class='inputBox'>
        <a href="javascript:call_cal(document.addFeatureByTypeIdCode.fromDate, '${nowTimestamp?string}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
        <span class='tabletext'>${uiLabelMap.CommonThru} : </span><input type="text" size='25' name='thruDate' class='inputBox'>
        <a href="javascript:call_cal(document.addFeatureByTypeIdCode.thruDate, '${nowTimestamp?string}');"><img src='/images/cal.gif' width='16' height='16' border='0' alt='Calendar'></a>
        <span class='tabletext'>${uiLabelMap.CommonSequence} : </span><input type="text" size='5' name='sequenceNum' class='inputBox'>
        <input type="submit" value="${uiLabelMap.CommonAdd}" style='font-size: x-small;'>
        </form>

        <br/>

        <form method="post" action="<@ofbizUrl>ApplyFeatureToProduct</@ofbizUrl>" style="margin: 0;" name="addFeatureById">
        <input type="hidden" name="productId" value="${productId}">
        <div class="head2">${uiLabelMap.ProductAddProductFeatureID}:</div>
        <br/>
        <span class="tabletext">${uiLabelMap.CommonId}: </span>
        <input type="text" size="10" name="productFeatureId" value="" class="inputBox">
        <span class='tabletext'>
            <a href="javascript:call_fieldlookup2(document.addFeatureById.productFeatureId,'LookupProductFeature');">
                <img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'>
            </a> 
        </span>
        <span class="tabletext">${uiLabelMap.ProductFeatureApplicationType}: </span>
        <select class="selectBox" name="productFeatureApplTypeId" size="1">
            <#list productFeatureApplTypes as productFeatureApplType>
               <option value='${(productFeatureApplType.productFeatureApplTypeId)?if_exists}' 
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'Y' && productFeatureApplType.productFeatureApplTypeId =="SELECTABLE_FEATURE")>selected</#if>
               <#if (productFeatureApplType.productFeatureApplTypeId?exists && product?exists && product.isVirtual == 'N' && productFeatureApplType.productFeatureApplTypeId =="STANDARD_FEATURE")>selected</#if>
               >${(productFeatureApplType.get("description",locale))?if_exists} </option>           
            </#list>
        </select>
        <br/>
        <span class="tabletext">${uiLabelMap.CommonFrom} : </span><input type="text" size="25" name="fromDate" class="inputBox">
        <a href="javascript:call_cal(document.addFeatureById.fromDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
        <span class="tabletext">${uiLabelMap.CommonThru} : </span><input type="text" size="25" name="thruDate" class="inputBox">
        <a href="javascript:call_cal(document.addFeatureById.thruDate, '${nowTimestamp?string}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
        <span class="tabletext">${uiLabelMap.CommonSequence} : </span><input type="text" size="5" name="sequenceNum" class="inputBox">
        <input type="submit" value="${uiLabelMap.CommonAdd}" style="font-size: x-small;">
        </form>
    </#if>
    <br/>
