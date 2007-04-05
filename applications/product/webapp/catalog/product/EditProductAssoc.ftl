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
    <form action="<@ofbizUrl>UpdateProductAssoc</@ofbizUrl>" method="post" style="margin: 0;" name="editProductAssocForm">
    <input type="hidden" name="productId" value="${productId?if_exists}">
    <table border="0" cellpadding="2" cellspacing="0">
    
    <#if !(productAssoc?exists)>
        <#if productId?exists && productIdTo?exists && productAssocTypeId?exists && fromDate?exists>
<#--            <div class="tabletext"><b>Association not found: Product Id=${productId?if_exists}, Product Id To=${productIdTo?if_exists}, Association Type Id=${productAssocTypeId?if_exists}, From Date=${fromDate?if_exists}.</b></div> -->
            <div class="tabletext"><b><#assign uiLabelWithVar=uiLabelMap.ProductAssociationNotFound?interpret><@uiLabelWithVar/></b></div>
            <input type="hidden" name="UPDATE_MODE" value="CREATE">
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
            <td>&nbsp;</td>
            <td><input type="text" class="inputBox" name="PRODUCT_ID" size="20" maxlength="40" value="${productId?if_exists}"></td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductIdTo}</div></td>
            <td>&nbsp;</td>
            <td><input type="text" class="inputBox" name="PRODUCT_ID_TO" size="20" maxlength="40" value="${productIdTo?if_exists}"></td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductAssociationTypeId}</div></td>
            <td>&nbsp;</td>
            <td>
                <select class="selectBox" name="PRODUCT_ASSOC_TYPE_ID" size="1">
                <#if productAssocTypeId?has_content>
                    <#assign curAssocType = delegator.findByPrimaryKey("ProductAssocType", Static["org.ofbiz.base.util.UtilMisc"].toMap("productAssocTypeId", productAssocTypeId))>
                    <#if curAssocType?exists>
                        <option selected value="${(curAssocType.productAssocTypeId)?if_exists}">${(curAssocType.get("description",locale))?if_exists}</option>
                        <option value="${(curAssocType.productAssocTypeId)?if_exists}"></option>
                    </#if>
                </#if>
                <#list assocTypes as assocType>
                    <option value="${(assocType.productAssocTypeId)?if_exists}">${(assocType.get("description",locale))?if_exists}</option>
                </#list>
                </select>
            </td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.CommonFromDate}</div></td>
            <td>&nbsp;</td>
            <td>
                <div class="tabletext">
                    <input type="text" class="inputBox" name="FROM_DATE" size="25" maxlength="40" value="${fromDate?if_exists}">
                    <a href="javascript:call_cal(document.editProductAssocForm.FROM_DATE, '${fromDate?default(nowTimestampString)}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    ${uiLabelMap.CommonSetNowEmpty}
                </div>
            </td>
            </tr>
        <#else>
            <input type="hidden" name="UPDATE_MODE" value="CREATE">
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
            <td>&nbsp;</td>
            <td><input type="text" class="inputBox" name="PRODUCT_ID" size="20" maxlength="40" value="${productId?if_exists}"></td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductIdTo}</div></td>
            <td>&nbsp;</td>
            <td><input type="text" class="inputBox" name="PRODUCT_ID_TO" size="20" maxlength="40" value="${productIdTo?if_exists}">
            <a href="javascript:call_fieldlookup2(document.editProductAssocForm.PRODUCT_ID_TO,'LookupProduct');"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a></td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductAssociationTypeId}</div></td>
            <td>&nbsp;</td>
            <td>
                <select class="selectBox" name="PRODUCT_ASSOC_TYPE_ID" size="1">
                <-- <option value="">&nbsp;</option> -->
                <#list assocTypes as assocType>
                    <option value="${(assocType.productAssocTypeId)?if_exists}">${(assocType.get("description",locale))?if_exists}</option>
                </#list>
                </select>
            </td>
            </tr>
            <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.CommonFromDate}</div></td>
            <td>&nbsp;</td>
            <td>
                <div class="tabletext">
                    <input type="text" class="inputBox" name="FROM_DATE" size="25" maxlength="40" value="">
                    <a href="javascript:call_cal(document.editProductAssocForm.FROM_DATE, '${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
                    ${uiLabelMap.CommonSetNowEmpty}
                </div>
            </td>
            </tr>
        </#if>
    <#else>
        <#assign isCreate = false>
        <#assign curProductAssocType = productAssoc.getRelatedOneCache("ProductAssocType")>
        <input type="hidden" name="UPDATE_MODE" value="UPDATE">
        <input type="hidden" name="PRODUCT_ID" value="${productId?if_exists}">
        <input type="hidden" name="PRODUCT_ID_TO" value="${productIdTo?if_exists}">
        <input type="hidden" name="PRODUCT_ASSOC_TYPE_ID" value="${productAssocTypeId?if_exists}">
        <input type="hidden" name="FROM_DATE" value="${fromDate?if_exists}">
        <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductId}</div></td>
            <td>&nbsp;</td>
            <td><b>${productId?if_exists}</b> ${uiLabelMap.ProductRecreateAssociation}</td>
        </tr>
        <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductProductIdTo}</div></td>
            <td>&nbsp;</td>
            <td><b>${productIdTo?if_exists}</b> ${uiLabelMap.ProductRecreateAssociation}</td>
        </tr>
        <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.ProductAssociationType}</div></td>
            <td>&nbsp;</td>
            <td><b><#if curProductAssocType?exists>${(curProductAssocType.get("description",locale))?if_exists}<#else> ${productAssocTypeId?if_exists}</#if></b> ${uiLabelMap.ProductRecreateAssociation}</td>
        </tr>
        <tr>
            <td align="right"><div class="tabletext">${uiLabelMap.CommonFromDate}</div></td>
            <td>&nbsp;</td>
            <td><b>${fromDate?if_exists}</b> ${uiLabelMap.ProductRecreateAssociation}</td>
        </tr>
    </#if>
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.CommonThruDate}</div></td>
        <td>&nbsp;</td>
        <td width="74%">
        <div class="tabletext">
            <input type="text" class="inputBox" name="THRU_DATE" <#if useValues> value="${productAssoc.thruDate?if_exists}"<#else>value="${(request.getParameter("THRU_DATE"))?if_exists}"</#if> size="30" maxlength="30"> 
            <a href="javascript:call_cal(document.editProductAssocForm.THRU_DATE, <#if useValues>'${productAssoc.thruDate?if_exists}'<#elseif (request.getParameter("THRU_DATE"))?exists>'${request.getParameter("THRU_DATE")}'<#else>'${nowTimestampString}'</#if>);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"></a>
        </div>
        </td>
    </tr>
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductSequenceNum}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="SEQUENCE_NUM" <#if useValues>value="${(productAssoc.sequenceNum)?if_exists}"<#else>value="${(request.getParameter("SEQUENCE_NUM"))?if_exists}"</#if> size="5" maxlength="10"></td>
    </tr>
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductReason}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="REASON" <#if useValues>value="${(productAssoc.reason)?if_exists}"<#else>value="${(request.getParameter("REASON"))?if_exists}"</#if> size="60" maxlength="255"></td>
    </tr>
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductInstruction}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="INSTRUCTION" <#if useValues>value="${(productAssoc.instruction)?if_exists}"<#else>value="${(request.getParameter("INSTRUCTION"))?if_exists}"</#if> size="60" maxlength="255"></td>
    </tr>
    
    <tr>
        <td width="26%" align="right"><div class="tabletext">${uiLabelMap.ProductQuantity}</div></td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" class="inputBox" name="QUANTITY" <#if useValues>value="${(productAssoc.quantity)?if_exists}"<#else>value="${(request.getParameter("QUANTITY"))?if_exists}"</#if> size="10" maxlength="15"></td>
    </tr>
    
    <tr>
        <td colspan="2">&nbsp;</td>
        <td align="left"><input type="submit" <#if isCreate>value="${uiLabelMap.CommonCreate}"<#else>value="${uiLabelMap.CommonUpdate}"</#if>></td>
    </tr>
    </table>
    </form>
    <br/>
    <#if productId?exists && product?exists>
        <hr class="sepbar">
        <div class="head2">${uiLabelMap.ProductAssociationsFromProduct}...</div>
        
        <table border="1" cellpadding="2" cellspacing="0">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductProductId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductName}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductSeqNum}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonQuantity}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductAssociationType}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
            </tr>
            <#list assocFromProducts as assocFromProduct>
            <#assign listToProduct = assocFromProduct.getRelatedOneCache("AssocProduct")>
            <#assign curProductAssocType = assocFromProduct.getRelatedOneCache("ProductAssocType")>
            <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProduct?productId=${(assocFromProduct.productIdTo)?if_exists}</@ofbizUrl>" class="buttontext">${(assocFromProduct.productIdTo)?if_exists}</a></td>
                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProduct?productId=${(assocFromProduct.productIdTo)?if_exists}</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if>&nbsp;</td>
                <td><div class="tabletext" <#if (assocFromProduct.getTimestamp("fromDate"))?exists && nowDate.before(assocFromProduct.getTimestamp("fromDate"))> style="color: red;"</#if>>
                ${(assocFromProduct.fromDate)?if_exists}&nbsp;</div></td>
                <td><div class="tabletext" <#if (assocFromProduct.getTimestamp("thruDate"))?exists && nowDate.after(assocFromProduct.getTimestamp("thruDate"))> style="color: red;"</#if>>
                ${(assocFromProduct.thruDate)?if_exists}&nbsp;</div></td>
                <td><div class="tabletext">&nbsp;${(assocFromProduct.sequenceNum)?if_exists}</div></td>
                <td><div class="tabletext">&nbsp;${(assocFromProduct.quantity)?if_exists}</div></td>
                <td><div class="tabletext"><#if curProductAssocType?exists> ${(curProductAssocType.get("description",locale))?if_exists}<#else>${(assocFromProduct.productAssocTypeId)?if_exists}</#if></div></td>
                <td>
                <a href="<@ofbizUrl>UpdateProductAssoc?UPDATE_MODE=DELETE&productId=${productId}&PRODUCT_ID=${productId}&PRODUCT_ID_TO=${(assocFromProduct.productIdTo)?if_exists}&PRODUCT_ASSOC_TYPE_ID=${(assocFromProduct.productAssocTypeId)?if_exists}&FROM_DATE=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
                </td>
                <td>
                <a href="<@ofbizUrl>EditProductAssoc?productId=${productId}&PRODUCT_ID=${productId}&PRODUCT_ID_TO=${(assocFromProduct.productIdTo)?if_exists}&PRODUCT_ASSOC_TYPE_ID=${(assocFromProduct.productAssocTypeId)?if_exists}&FROM_DATE=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonEdit}</a>
                </td>
            </tr>
            </#list>
        </table>
        
        <hr class="sepbar">
        <div class="head2">${uiLabelMap.ProductAssociationsToProduct}...</div>
        <table border="1" cellpadding="2" cellspacing="0">
            <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductProductId}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductName}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonFromDateTime}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.CommonThruDateTime}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductAssociationType}</b></div></td>
            <td><div class="tabletext"><b>&nbsp;</b></div></td>
            </tr>
            <#list assocToProducts as assocToProduct>
            <#assign listToProduct = assocToProduct.getRelatedOneCache("MainProduct")>
            <#assign curProductAssocType = assocToProduct.getRelatedOneCache("ProductAssocType")>
            <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProduct?productId=${(assocToProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">${(assocToProduct.productId)?if_exists}</a></td>
                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProduct?productId=${(assocToProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if></td>
                <td><div class="tabletext">${(assocToProduct.getTimestamp("fromDate"))?if_exists}&nbsp;</div></td>
                <td><div class="tabletext">${(assocToProduct.getTimestamp("thruDate"))?if_exists}&nbsp;</div></td>
                <td><div class="tabletext"><#if curProductAssocType?exists> ${(curProductAssocType.get("description",locale))?if_exists}<#else> ${(assocToProduct.productAssocTypeId)?if_exists}</#if></div></td>
                <td>
                <a href="<@ofbizUrl>UpdateProductAssoc?UPDATE_MODE=DELETE&productId=${(assocToProduct.productIdTo)?if_exists}&PRODUCT_ID=${(assocToProduct.productId)?if_exists}&PRODUCT_ID_TO=${(assocToProduct.productIdTo)?if_exists}&PRODUCT_ASSOC_TYPE_ID=${(assocToProduct.productAssocTypeId)?if_exists}&FROM_DATE=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocToProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
                </td>
            </tr>
            </#list>
        </table>

        <br/>
        <div class="tabletext">${uiLabelMap.CommonNote} : ${uiLabelMap.ProductRedExplanation}</div>
    </#if>
