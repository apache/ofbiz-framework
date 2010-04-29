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
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.PageTitleEditProductBom} <#if product?exists>${(product.internalName)?if_exists}</#if>&nbsp;[${uiLabelMap.CommonId}&nbsp;${productId?if_exists}]</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <#if product?has_content>
        <a href="<@ofbizUrl>BomSimulation</@ofbizUrl>?productId=${productId}&amp;bomType=${productAssocTypeId}" class="buttontext">${uiLabelMap.ManufacturingBomSimulation}</a>
    </#if>
    <br />
    <br />
    <form name="searchform" action="<@ofbizUrl>UpdateProductBom</@ofbizUrl>#topform" method="post">
    <input type="hidden" name="UPDATE_MODE" value=""/>

    <table class="basic-table" cellspacing="0">
        <tr>
            <td align="right">${uiLabelMap.ManufacturingBomType}</td>
            <td>&nbsp; <a name="topform"/></td>
            <td>
                <select name="productAssocTypeId" size="1">
                <#if productAssocTypeId?has_content>
                    <#assign curAssocType = delegator.findByPrimaryKey("ProductAssocType", Static["org.ofbiz.base.util.UtilMisc"].toMap("productAssocTypeId", productAssocTypeId))>
                    <#if curAssocType?exists>
                        <option selected="selected" value="${(curAssocType.productAssocTypeId)?if_exists}">${(curAssocType.get("description",locale))?if_exists}</option>
                        <option value="${(curAssocType.productAssocTypeId)?if_exists}"></option>
                    </#if>
                </#if>
                <#list assocTypes as assocType>
                    <option value="${(assocType.productAssocTypeId)?if_exists}">${(assocType.get("description",locale))?if_exists}</option>
                </#list>
                </select>
            </td>
            <td align="right">${uiLabelMap.ProductProductId}</td>
            <td>&nbsp;</td>
            <td>
                <@htmlTemplate.lookupField value="${productId?if_exists}" formName="searchform" name="productId" id="productId" fieldFormName="LookupProduct" presentation="window"/>
                <span><a href="javascript:document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingShowBOMAssocs}</a></span>
            </td>
        </tr>
        <tr>
            <td colspan='3'>
                &nbsp;
            </td>
            <td align="right">${uiLabelMap.ManufacturingCopyToProductId}</td>
            <td>&nbsp;</td>
            <td>
                <@htmlTemplate.lookupField formName="searchform" name="copyToProductId" id="copyToProductId" fieldFormName="LookupProduct" presentation="window"/>
                <span><a href="javascript:document.searchform.UPDATE_MODE.value='COPY';document.searchform.submit();" class="buttontext">${uiLabelMap.ManufacturingCopyBOMAssocs}</a></span>
            </td>
        </tr>
    </table>
    </form>
    <hr />
    <form action="<@ofbizUrl>UpdateProductBom</@ofbizUrl>" method="post" name="editProductAssocForm">
    <#if !(productAssoc?exists)>
        <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
        <table class="basic-table" cellspacing="0">
          <tr>
            <td align="right">${uiLabelMap.ManufacturingBomType}</td>
            <td>&nbsp;</td>
            <td>
                <select name="productAssocTypeId" size="1">
                <#if productAssocTypeId?has_content>
                    <#assign curAssocType = delegator.findByPrimaryKey("ProductAssocType", Static["org.ofbiz.base.util.UtilMisc"].toMap("productAssocTypeId", productAssocTypeId))>
                    <#if curAssocType?exists>
                        <option selected="selected" value="${(curAssocType.productAssocTypeId)?if_exists}">${(curAssocType.get("description",locale))?if_exists}</option>
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
            <td align="right">${uiLabelMap.ProductProductId}</td>
            <td>&nbsp;</td>
            <td>
                <@htmlTemplate.lookupField value="${productId?if_exists}" formName="editProductAssocForm" name="productId" id="productId2" fieldFormName="LookupProduct" presentation="window"/>
            </td>
          </tr>
          <tr>
            <td align="right">${uiLabelMap.ManufacturingProductIdTo}</td>
            <td>&nbsp;</td>
            <td>
                <@htmlTemplate.lookupField value="${productIdTo?if_exists}" formName="editProductAssocForm" name="productIdTo" id="productIdTo" fieldFormName="LookupProduct" presentation="window"/>
            </td>
          </tr>
          <tr>
            <td align="right">${uiLabelMap.CommonFromDate}</td>
            <td>&nbsp;</td>
            <td>
                <input type="text" name="fromDate" size="25" maxlength="40" value=""/>
                <a href="javascript:call_cal(document.editProductAssocForm.fromDate,'${nowTimestampString}');"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
                <span class="tooltip">(${uiLabelMap.ManufacturingWillBeSetToNow})</span>
            </td>
          </tr>
    <#else>
        <#assign curProductAssocType = productAssoc.getRelatedOneCache("ProductAssocType")>
        <input type="hidden" name="UPDATE_MODE" value="UPDATE"/>
        <input type="hidden" name="productId" value="${productId?if_exists}"/>
        <input type="hidden" name="productIdTo" value="${productIdTo?if_exists}"/>
        <input type="hidden" name="productAssocTypeId" value="${productAssocTypeId?if_exists}"/>
        <input type="hidden" name="fromDate" value="${fromDate?if_exists}"/>
        <table class="basic-table" cellspacing="0">
          <tr>
            <td align="right">${uiLabelMap.ProductProductId}</td>
            <td>&nbsp;</td>
            <td>${productId?if_exists}</td>
          </tr>
          <tr>
            <td align="right">${uiLabelMap.ManufacturingProductIdTo}</td>
            <td>&nbsp;</td>
            <td>${productIdTo?if_exists}</td>
          </tr>
          <tr>
            <td align="right">${uiLabelMap.ManufacturingBomType}</td>
            <td>&nbsp;</td>
            <td><#if curProductAssocType?exists>${(curProductAssocType.get("description",locale))?if_exists}<#else> ${productAssocTypeId?if_exists}</#if></td>
          </tr>
          <tr>
            <td align="right">${uiLabelMap.CommonFromDate}</td>
            <td>&nbsp;</td>
            <td>${fromDate?if_exists}</td>
          </tr>
    </#if>
    <tr>
        <td width="26%" align="right">${uiLabelMap.CommonThruDate}</td>
        <td>&nbsp;</td>
        <td width="74%">
            <input type="text" name="thruDate" <#if useValues> value="${productAssoc.thruDate?if_exists}"<#else>value="${(request.getParameter("thruDate"))?if_exists}"</#if> size="30" maxlength="30"/>
            <a href="javascript:call_cal(document.editProductAssocForm.thruDate,<#if useValues>'${productAssoc.thruDate?if_exists}'<#elseif (request.getParameter("thruDate"))?exists>'${request.getParameter("thruDate")}'<#else>'${nowTimestampString}'</#if>);"><img src="<@ofbizContentUrl>/images/cal.gif</@ofbizContentUrl>" width="16" height="16" border="0" alt="Calendar"/></a>
        </td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.CommonSequenceNum}</td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="sequenceNum" <#if useValues>value="${(productAssoc.sequenceNum)?if_exists}"<#else>value="${(request.getParameter("sequenceNum"))?if_exists}"</#if> size="5" maxlength="10"/></td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingReason}</td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="reason" <#if useValues>value="${(productAssoc.reason)?if_exists}"<#else>value="${(request.getParameter("reason"))?if_exists}"</#if> size="60" maxlength="255"/></td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingInstruction}</td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="instruction" <#if useValues>value="${(productAssoc.instruction)?if_exists}"<#else>value="${(request.getParameter("instruction"))?if_exists}"</#if> size="60" maxlength="255"/></td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingQuantity}</td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="quantity" <#if useValues>value="${(productAssoc.quantity)?if_exists}"<#else>value="${(request.getParameter("quantity"))?if_exists}"</#if> size="10" maxlength="15"/></td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingScrapFactor}</td>
        <td>&nbsp;</td>
        <td width="74%"><input type="text" name="scrapFactor" <#if useValues>value="${(productAssoc.scrapFactor)?if_exists}"<#else>value="${(request.getParameter("scrapFactor"))?if_exists}"</#if> size="10" maxlength="15"/></td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingFormula}</td>
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
                <option value="${formula.customMethodId}" <#if selectedFormula = formula.customMethodId>selected="selected"</#if>>${formula.get("description",locale)?if_exists}</option>
            </#list>
            </select>
        </td>
    </tr>
    <tr>
        <td width="26%" align="right">${uiLabelMap.ManufacturingRoutingTask}</td>
        <td>&nbsp;</td>
        <td width="74%">
          <#if useValues>
            <#assign value = productAssoc.routingWorkEffortId?if_exists>
          <#else>
            <#assign value = request.getParameter("routingWorkEffortId")?if_exists>
          </#if>
          <#if value?has_content>
            <@htmlTemplate.lookupField value="${value}" formName="editProductAssocForm" name="routingWorkEffortId" id="routingWorkEffortId" fieldFormName="LookupRoutingTask"/>
          <#else>
            <@htmlTemplate.lookupField formName="editProductAssocForm" name="routingWorkEffortId" id="routingWorkEffortId" fieldFormName="LookupRoutingTask"/>
          </#if>
        </td>
    </tr>
    <tr>
        <td colspan="2">&nbsp;</td>
        <td><input type="submit" <#if !(productAssoc?exists)>value="${uiLabelMap.CommonAdd}"<#else>value="${uiLabelMap.CommonEdit}"</#if>/></td>
    </tr>
    </table>
    </form>
  </div>
</div>
<#if productId?exists && product?exists>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ManufacturingProductComponents}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
    <a name="components"></a>
    <table class="basic-table" cellspacing="0">
      <tr class="header-row">
        <td>${uiLabelMap.ProductProductId}</td>
        <td>${uiLabelMap.ProductProductName}</td>
        <td>${uiLabelMap.CommonFromDate}</td>
        <td>${uiLabelMap.CommonThruDate}</td>
        <td>${uiLabelMap.CommonSequenceNum}</td>
        <td>${uiLabelMap.CommonQuantity}</td>
        <td>${uiLabelMap.ManufacturingScrapFactor}</td>
        <td>${uiLabelMap.ManufacturingFormula}</td>
        <td>${uiLabelMap.ManufacturingRoutingTask}</td>
        <td>&nbsp;</td>
        <td>&nbsp;</td>
      </tr>
    <#assign alt_row = false>
    <#list assocFromProducts?if_exists as assocFromProduct>
    <#assign listToProduct = assocFromProduct.getRelatedOneCache("AssocProduct")>
    <#assign curProductAssocType = assocFromProduct.getRelatedOneCache("ProductAssocType")>
      <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
        <td><a href="<@ofbizUrl>EditProductBom?productId=${(assocFromProduct.productIdTo)?if_exists}&amp;productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(assocFromProduct.productIdTo)?if_exists}</a></td>
        <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProductBom?productId=${(assocFromProduct.productIdTo)?if_exists}&amp;productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if>&nbsp;</td>
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
        <a href="<@ofbizUrl>UpdateProductBom?UPDATE_MODE=DELETE&amp;productId=${productId}&amp;productIdTo=${(assocFromProduct.productIdTo)?if_exists}&amp;productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}&amp;fromDate=${assocFromProduct.getString("fromDate")}&amp;useValues=true</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonDelete}</a>
        </td>
        <td>
        <a href="<@ofbizUrl>EditProductBom?productId=${productId}&amp;productIdTo=${(assocFromProduct.productIdTo)?if_exists}&amp;productAssocTypeId=${(assocFromProduct.productAssocTypeId)?if_exists}&amp;fromDate=${assocFromProduct.getString("fromDate")}&amp;useValues=true</@ofbizUrl>" class="buttontext">${uiLabelMap.CommonEdit}</a>
        </td>
      </tr>
      <#-- toggle the row color -->
      <#assign alt_row = !alt_row>
    </#list>
    </table>
  </div>
</div>
<div class="screenlet">
  <div class="screenlet-title-bar">
    <ul>
      <li class="h3">${uiLabelMap.ManufacturingProductComponentOf}</li>
    </ul>
    <br class="clear"/>
  </div>
  <div class="screenlet-body">
      <table class="basic-table" cellspacing="0">
        <tr class="header-row">
            <td>${uiLabelMap.ProductProductId}</td>
            <td>${uiLabelMap.ProductProductName}</td>
            <td>${uiLabelMap.CommonFromDate}</td>
            <td>${uiLabelMap.CommonThruDate}</td>
            <td>${uiLabelMap.CommonQuantity}</td>
            <td>&nbsp;</td>
        </tr>
        <#assign alt_row = false>
        <#list assocToProducts?if_exists as assocToProduct>
        <#assign listToProduct = assocToProduct.getRelatedOneCache("MainProduct")>
        <#assign curProductAssocType = assocToProduct.getRelatedOneCache("ProductAssocType")>
        <tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
            <td><a href="<@ofbizUrl>EditProductBom?productId=${(assocToProduct.productId)?if_exists}&amp;productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(assocToProduct.productId)?if_exists}</a></td>
<!--                <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProduct?productId=${(assocToProduct.productId)?if_exists}</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if></td> -->
            <td><#if listToProduct?exists><a href="<@ofbizUrl>EditProductBom?productId=${(assocToProduct.productId)?if_exists}&amp;productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}#components</@ofbizUrl>" class="buttontext">${(listToProduct.internalName)?if_exists}</a></#if></td>
            <td>${(assocToProduct.getTimestamp("fromDate"))?if_exists}&nbsp;</td>
            <td>${(assocToProduct.getTimestamp("thruDate"))?if_exists}&nbsp;</td>
            <td>${(assocToProduct.quantity)?if_exists}&nbsp;</td>
            <td>
                <a href="<@ofbizUrl>UpdateProductBom?UPDATE_MODE=DELETE&amp;productId=${(assocToProduct.productId)?if_exists}&amp;productIdTo=${(assocToProduct.productIdTo)?if_exists}&amp;productAssocTypeId=${(assocToProduct.productAssocTypeId)?if_exists}&amp;fromDate=${Static["org.ofbiz.base.util.UtilFormatOut"].encodeQueryValue(assocToProduct.getTimestamp("fromDate").toString())}&amp;useValues=true</@ofbizUrl>" class="buttontext">
                ${uiLabelMap.CommonDelete}</a>
            </td>
        </tr>
        <#-- toggle the row color -->
        <#assign alt_row = !alt_row>
        </#list>
      </table>
      <br />
      ${uiLabelMap.CommonNote}: <b class="alert">${uiLabelMap.CommonRed}</b> ${uiLabelMap.ManufacturingNote1} <b style="color: red;">${uiLabelMap.CommonRed}</b>${uiLabelMap.ManufacturingNote2} <b style="color: red;">${uiLabelMap.CommonRed}</b>${uiLabelMap.ManufacturingNote3}
  </div>
</div>
</#if>