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

<script language="JavaScript" type="text/javascript">
<!-- //
function lookupBom() {
    document.searchbom.productId.value=document.editProductAssocForm.productId.value;
    document.searchbom.productAssocTypeId.value=document.editProductAssocForm.productAssocTypeId.options[document.editProductAssocForm.productAssocTypeId.selectedIndex].value;
    document.searchbom.submit();
}
// -->
</script>

    <h1>${uiLabelMap.ManufacturingBillOfMaterials} <#if product?exists>${(product.internalName)?if_exists}</#if>[${uiLabelMap.CommonId}:${productId?if_exists}]</h1>
    <#if product?has_content>
        <a href="<@ofbizUrl>BomSimulation</@ofbizUrl>?productId=${productId}&bomType=${productAssocTypeId}" class="buttontext">${uiLabelMap.ManufacturingBillOfMaterials}</a>
        <!--<a href="<@ofbizUrl>EditRoutingProductLink?byProduct=${productId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.ManufacturingProductRoutings}]</a></span>-->
    </#if>

    <br/>
    <br/>

    <form name="searchform" action="<@ofbizUrl>UpdateProductBom</@ofbizUrl>#topform" method="post">
    <input type="hidden" name="UPDATE_MODE" value=""/>
    <a name="topform"/>
    <table border="0" cellpadding="2" cellspacing="0">
        <tr>
            <th align="right">${uiLabelMap.ManufacturingBomType}:</th>
            <td>&nbsp;</td>
            <td>
            <select name="productAssocTypeId" size="1">
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
            <th align="right">${uiLabelMap.ProductProductId}:</th>
            <td>&nbsp;</td>
            <td>
            <input type="text" name="productId" size="20" maxlength="40" value="${productId?if_exists}"/>
            <a href="javascript:call_fieldlookup2(document.searchform.productId,'LookupProduct');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>
            <span><a href="javascript:document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingShowBOMAssocs}</a></span>
            </td>
        </tr>
        <tr>
            <td colspan='3' align="left">
                &nbsp;
            </td>
            <th align="right">${uiLabelMap.ManufacturingCopyToProductId}:</th>
            <td>&nbsp;</td>
            <td>
            <input type="text" name="copyToProductId" size="20" maxlength="40" value=""/>
            <a href="javascript:call_fieldlookup2(document.searchform.copyToProductId,'LookupProduct');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>
            <span><a href="javascript:document.searchform.UPDATE_MODE.value='COPY';document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingCopyBOMAssocs}</a></span>
            </td>
        </tr>
    </table>
    </form>

    <hr/>

    
    <form action="<@ofbizUrl>UpdateProductBom</@ofbizUrl>" method="post" name="editProductAssocForm">
    <#if !(productAssoc?exists)>
        <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
        <table border="0" cellpadding="2" cellspacing="0">
            <tr>
            <th align="right">${uiLabelMap.ManufacturingBomType}:</th>
            <td>&nbsp;</td>
            <td>
                <select name="productAssocTypeId" size="1">
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
            <th align="right">${uiLabelMap.ProductProductId}:</th>
            <td>&nbsp;</td>
            <td>
                <input type="text" name="productId" size="20" maxlength="40" value="${productId?if_exists}"/>
                <a href="javascript:call_fieldlookup2(document.editProductAssocForm.productId,'LookupProduct');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>
            </td>
            </tr>
            <tr>
            <th align="right">${uiLabelMap.ManufacturingProductIdTo}:</th>
            <td>&nbsp;</td>
            <td>
                <input type="text" name="productIdTo" size="20" maxlength="40" value="${productIdTo?if_exists}"/>
                <a href="javascript:call_fieldlookup2(document.editProductAssocForm.productIdTo,'LookupProduct');"><img src="<@ofbizContentUrl>/images/fieldlookup.gif"</@ofbizContentUrl>" width="16" height="16" border="0" alt="Lookup"/></a>
            </td>
            </tr>
            <tr>
            <th align="right">${uiLabelMap.CommonFromDate}:</th>
            <td>&nbsp;</td>
            <td>
                <input type="text" name="fromDate" size="25" maxlength="40" value=""/>
                <a href="javascript:call_cal(document.editProductAssocForm.fromDate,'${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
                (${uiLabelMap.ManufacturingWillBeSetToNow})
            </td>
            </tr>
    <#else>
        <#assign curProductAssocType = productAssoc.getRelatedOneCache("ProductAssocType")>
        <input type="hidden" name="UPDATE_MODE" value="UPDATE"/>
        <input type="hidden" name="productId" value="${productId?if_exists}"/>
        <input type="hidden" name="productIdTo" value="${productIdTo?if_exists}"/>
        <input type="hidden" name="productAssocTypeId" value="${productAssocTypeId?if_exists}"/>
        <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
        <table border="0" cellpadding="2" cellspacing="0">
        <tr>
            <th align="right">${uiLabelMap.ProductProductId}:</th>
            <td>&nbsp;</td>
            <td><b>${productId?if_exists}</b></td>
        </tr>
        <tr>
            <th align="right">${uiLabelMap.ManufacturingProductIdTo}:</th>
            <td>&nbsp;</td>
            <td><b>${productIdTo?if_exists}</b></td>
        </tr>
        <tr>
            <th align="right">${uiLabelMap.ManufacturingBomType}:</th>
            <td>&nbsp;</td>
            <td><b><#if curProductAssocType?exists>${(curProductAssocType.get("description",locale))?if_exists}<#else> ${productAssocTypeId?if_exists}</#if></b></td>
        </tr>
        <tr>
            <th align="right">${uiLabelMap.CommonFromDate}:</th>
            <td>&nbsp;</td>
            <td><b>${fromDate?if_exists}</b></td>
        </tr>
    </#if>
    <tr>
        <th width="26%" align="right">${uiLabelMap.CommonThruDate}:</th>
        <td>&nbsp;</td>
        <td width="74%">
            <input type="text" name="thruDate" <#if useValues> value="${productAssoc.thruDate?if_exists}"<#else>value="${(request.getParameter("thruDate"))?if_exists}"</#if> size="30" maxlength="30"/> 
            <a href="javascript:call_cal(document.editProductAssocForm.thruDate,<#if useValues>'${productAssoc.thruDate?if_exists}'<#elseif (request.getParameter("thruDate"))?exists>'${request.getParameter("thruDate")}'<#else>'${nowTimestampString}'</#if>);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
        </td>
    </tr>
    <tr>
        <th width="26%" align="right">${uiLabelMap.CommonSequenceNum}:</th>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="sequenceNum" <#if useValues>value="${(productAssoc.sequenceNum)?if_exists}"<#else>value="${(request.getParameter("sequenceNum"))?if_exists}"</#if> size="5" maxlength="10"/></td>
    </tr>
    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingReason}:</th>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="reason" <#if useValues>value="${(productAssoc.reason)?if_exists}"<#else>value="${(request.getParameter("reason"))?if_exists}"</#if> size="60" maxlength="255"/></td>
    </tr>
    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingInstruction}:</th>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="instruction" <#if useValues>value="${(productAssoc.instruction)?if_exists}"<#else>value="${(request.getParameter("instruction"))?if_exists}"</#if> size="60" maxlength="255"/></td>
    </tr>
    
    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingQuantity}:</th>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="quantity" <#if useValues>value="${(productAssoc.quantity)?if_exists}"<#else>value="${(request.getParameter("quantity"))?if_exists}"</#if> size="10" maxlength="15"/></td>
    </tr>

    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingScrapFactor}:</th>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="scrapFactor" <#if useValues>value="${(productAssoc.scrapFactor)?if_exists}"<#else>value="${(request.getParameter("scrapFactor"))?if_exists}"</#if> size="10" maxlength="15"/></td>
    </tr>

    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingFormula}:</th>
        <td>&nbsp;</td>
        <td width="74%">
            <select name="estimateCalcMethod">
            <option value="">&nbsp;</option>
            <#assign selectedFormula = "">
            <#if useValues>
                <#assign selectedFormula = (productAssoc.estimateCalcMethod)?if_exists>
            <#else>
                <#assign selectedFormula = (request.getParameter("estimateCalcMethod"))?if_exists>
            </#if>
            <#list formulae as formula>
                <option value="${formula.customMethodId}" <#if selectedFormula = formula.customMethodId>selected</#if>>${formula.get("description",locale)?if_exists}</option>
            </#list>
            </select>
        </td>
    </tr>

    <tr>
        <th width="26%" align="right">${uiLabelMap.ManufacturingRoutingTask}:</th>
        <td>&nbsp;</td>
        <td width="74%">
            <input type="text" name="routingWorkEffortId" <#if useValues>value="${(productAssoc.routingWorkEffortId)?if_exists}"<#else>value="${(request.getParameter("routingWorkEffortId"))?if_exists}"</#if> size="10" maxlength="15"/>
            <a href="javascript:call_fieldlookup(document.editProductAssocForm.routingWorkEffortId,'<@ofbizUrl>LookupRoutingTask</@ofbizUrl>','none',640,460);"><img src='/images/fieldlookup.gif' width='15' height='14' border='0' alt='Click here For Field Lookup'/></a>
        </td>
    </tr>
    
    <tr>
        <td colspan="2">&nbsp;</td>
        <td align="left"><input type="submit" <#if !(productAssoc?exists)>value="${uiLabelMap.CommonAdd}"<#else>value="${uiLabelMap.CommonEdit}"</#if>/></td>
    </tr>
    </table>
    </form>
    <br/>
    <#if productId?exists && product?exists>
        <hr/>
        <a name="components"></a>
        <h2>${uiLabelMap.ManufacturingProductComponents}</h2>
        
        <table border="1" cellpadding="2" cellspacing="0">
            <tr>
            <td><b>${uiLabelMap.ProductProductId}</b></td>
            <td><b>${uiLabelMap.ProductProductName}</b></td>
            <td><b>${uiLabelMap.CommonFromDate}</b></td>
            <td><b>${uiLabelMap.CommonThruDate}</b></td>
            <td><b>${uiLabelMap.CommonSequenceNum}</b></td>
            <td><b>${uiLabelMap.CommonQuantity}</b></td>
            <td><b>${uiLabelMap.ManufacturingScrapFactor}</b></td>
            <td><b>${uiLabelMap.ManufacturingFormula}</b></td>
            <td><b>${uiLabelMap.ManufacturingRoutingTask}</b></td>
            <td><b>&nbsp;</b></td>
            <td><b>&nbsp;</b></td>
            </tr>
            <#list assocFromProducts as assocFromProduct>
            <#assign listToProduct = assocFromProduct.getRelatedOneCache("AssocProduct")>
            <#assign curProductAssocType = assocFromProduct.getRelatedOneCache("ProductAssocType")>
            <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProductBom?productId=${(assocFromProduct.productIdTo)?if_exists}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(assocFromProduct.productIdTo)?if_exists}</a></td>
                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProductBom?productId=${(assocFromProduct.productIdTo)?if_exists}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if>&nbsp;</td>
                <td<#if (assocFromProduct.getTimestamp("fromDate"))?exists && nowDate.before(assocFromProduct.getTimestamp("fromDate"))> class="alert"</#if>>
                ${(assocFromProduct.fromDate)?if_exists}&nbsp;</td>
                <td<#if (assocFromProduct.getTimestamp("thruDate"))?exists && nowDate.after(assocFromProduct.getTimestamp("thruDate"))> class="alert"</#if>>
                ${(assocFromProduct.thruDate)?if_exists}&nbsp;</td>
                <td>&nbsp;${(assocFromProduct.sequenceNum)?if_exists}</td>
                <td>&nbsp;${(assocFromProduct.quantity)?if_exists}</td>
                <td>&nbsp;${(assocFromProduct.scrapFactor)?if_exists}</td>
                <td>&nbsp;${(assocFromProduct.estimateCalcMethod)?if_exists}</td>
                <td>&nbsp;${(assocFromProduct.routingWorkEffortId)?if_exists}</td>
                <td>
                <a href="<@ofbizUrl>UpdateProductBom?UPDATE_MODE=DELETE&productId=${productId}&productIdTo=${(assocFromProduct.productIdTo)?if_exists}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
                </td>
                <td>
                <a href="<@ofbizUrl>EditProductBom?productId=${productId}&productIdTo=${(assocFromProduct.productIdTo)?if_exists}&productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocFromProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonEdit}</a>
                </td>
            </tr>
            </#list>
        </table>
        
        <hr/>
        <h2>${uiLabelMap.ManufacturingProductComponentOf}</h2>
        <table border="1" cellpadding="2" cellspacing="0">
            <tr>
            <td><b>${uiLabelMap.ProductProductId}</b></td>
            <td><b>${uiLabelMap.ProductProductName}</b></td>
            <td><b>${uiLabelMap.CommonFromDate}</b></td>
            <td><b>${uiLabelMap.CommonThruDate}</b></td>
            <td><b>${uiLabelMap.CommonQuantity}</b></td>
            <td><b>&nbsp;</b></td>
            </tr>
            <#list assocToProducts as assocToProduct>
            <#assign listToProduct = assocToProduct.getRelatedOneCache("MainProduct")>
            <#assign curProductAssocType = assocToProduct.getRelatedOneCache("ProductAssocType")>
            <tr valign="middle">
                <td><a href="<@ofbizUrl>EditProductBom?productId=${(assocToProduct.productId)?if_exists}&productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(assocToProduct.productId)?if_exists}</a></td>
<!--                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProduct?productId=${(assocToProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if></td> -->
                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProductBom?productId=${(assocToProduct.productId)?if_exists}&productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if></td>
                <td>${(assocToProduct.getTimestamp("fromDate"))?if_exists}&nbsp;</td>
                <td>${(assocToProduct.getTimestamp("thruDate"))?if_exists}&nbsp;</td>
                <td>${(assocToProduct.quantity)?if_exists}&nbsp;</td>
                <td>
                <a href="<@ofbizUrl>UpdateProductBom?UPDATE_MODE=DELETE&productId=${(assocToProduct.productId)?if_exists}&productIdTo=${(assocToProduct.productIdTo)?if_exists}&productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}&fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocToProduct.getTimestamp("fromDate").toString())}&useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
                </td>
            </tr>
            </#list>
        </table>

        <br/>
        NOTE: <b class="alert">Red</b> date/time entries denote that the current time is before the From Date or after the Thru Date. If the From Date is <b style="color: red;">red</b>, association has not started yet; if Thru Date is <b style="color: red;">red</b>, association has expired (<u>and should probably be deleted</u>).
    </#if>
