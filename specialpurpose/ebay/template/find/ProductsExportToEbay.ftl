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
     function changeEbayCategory(categ) {
         document.forms["ProductsExportToEbay"].action = "<@ofbizUrl>ProductsExportToEbay?categoryCode="+categ+"</@ofbizUrl>";
         document.forms["ProductsExportToEbay"].submit();
     }

    function changeWebSite(Id) {
        var formId = Id ;
        formId.action="<@ofbizUrl>ProductsExportToEbay</@ofbizUrl>";
        formId.submit();
    }

     function activateSubmitButton() {
         categ = document.forms["ProductsExportToEbay"].ebayCategory.value;
         if (document.forms["ProductsExportToEbay"].submitButton) {
             if (categ != null && (categ.substring(0, 1) == 'Y' || categ == '')) {
                 document.forms["ProductsExportToEbay"].submitButton.disabled = false;
             } else {
                 document.forms["ProductsExportToEbay"].submitButton.disabled = true;
                 document.forms["ProductsExportToEbay"].submitButton.value = "Please select a category";
             }
         }
    }
</script>
<div>
    <form id="ProductsExportToEbay" method="post" action="<@ofbizUrl>PostProductsToEbay</@ofbizUrl>" name="ProductsExportToEbay">
        <input type="hidden" name="productStoreId" value="${productStoreId!}" />
        <table border="0" cellpadding="2" cellspacing="0">
             <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_ebayCategory}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="hidden" name="selectResult" value="${selectResult!}"/>
                    <select name="ebayCategory" onchange="changeEbayCategory(this.value)">
                        <option value=""> </option>
                        <#if categories??>
                            <#list categories as category>
                                <option value="${category.CategoryCode}" <#if categoryCode?? && categoryCode == category.CategoryCode>selected="selected"</#if>>${category.CategoryName}</option>
                            </#list>
                        </#if>
                    </select>
                </td>
            </tr>
            <#if hideExportOptions?has_content && hideExportOptions == "N">
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonCountry}</td>
                <td>&nbsp;</td>
                <td>
                    <select name="country">
                        <#if countries??>
                            <#list countries as country>
                                <option value="${country.geoCode}" <#if countryCode?? && countryCode == country.geoCode>selected="selected"</#if>>${country.get("geoName",locale)}</option>
                            </#list>
                        </#if>
                    </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_location}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="location" size="50" maxlength="50" value="${parameters.location!}" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_listingDuration}</td>
                <td>&nbsp;</td>
                <td>
                    <select name="listingDuration">
                        <option value="Days_1">1 ${uiLabelMap.CommonDay}</option>
                        <option value="Days_3">3 ${uiLabelMap.CommonDays}</option>
                        <option value="Days_7">7 ${uiLabelMap.CommonDays}</option>
                    </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_startPrice}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="startPrice" size="12" maxlength="12" value="${parameters.startPrice!}" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonQuantity}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="quantity" size="12" maxlength="12" value="<#if parameters.quantity??>${parameters.quantity!}<#else>1</#if>" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonWebsite}</td>
                <td>&nbsp;</td>
                <td>
                  <select name="webSiteId" onchange="javascript:changeWebSite(document.getElementById('ProductsExportToEbay'));">
                    <#list webSiteList as webSite>
                      <#assign displayDesc = webSite.siteName?default("${uiLabelMap.ProductNoDescription}")>
                      <#if (18 < displayDesc?length)>
                        <#assign displayDesc = displayDesc[0..15] + "...">
                      </#if>
                      <option value="${webSite.webSiteId}" <#if selectedWebSiteId! == webSite.webSiteId> selected="selected"</#if>>${displayDesc} [${webSite.webSiteId}]</option>
                    </#list>
                  </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_webSiteUrl}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="webSiteUrl" size="100" value="${webSiteUrl!}"/>
                </td>
            </tr>
            <tr>
                <td align="center" colspan="3"><b><u>${uiLabelMap.FormFieldTitle_paymentMethodsAccepted}</u></b></td>
            </tr>
            <tr>
                <td>&nbsp;</td>
            </tr>
            <tr>
                <td colspan="3">
                    <table class="basic-table" cellspacing="0">
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentPayPal}</td>
                            <td width="2%"><input type="checkbox" name="paymentPayPal" <#if parameters.paymentPayPal??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentVisaMC}</td>
                            <td width="2%"><input type="checkbox" name="paymentVisaMC" <#if parameters.paymentVisaMC??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentAmEx}</td>
                            <td width="2%"><input type="checkbox" name="paymentAmEx" <#if parameters.paymentAmEx??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentDiscover}</td>
                            <td width="2%"><input type="checkbox" name="paymentDiscover" <#if parameters.paymentDiscover??>checked="checked"</#if> /></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMOCC}</td>
                            <td width="2%"><input type="checkbox" name="paymentMOCC" <#if parameters.paymentMOCC??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentPersonalCheck}</td>
                            <td width="2%"><input type="checkbox" name="paymentPersonalCheck" <#if parameters.paymentPersonalCheck??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCCAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentCCAccepted" <#if parameters.paymentCCAccepted??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashInPerson}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashInPerson" <#if parameters.paymentCashInPerson??>checked="checked"</#if> /></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashOnPickup}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashOnPickup" <#if parameters.paymentCashOnPickup??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCOD}</td>
                            <td width="2%"><input type="checkbox" name="paymentCOD" <#if parameters.paymentCOD??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCODPrePayDelivery}</td>
                            <td width="2%"><input type="checkbox" name="paymentCODPrePayDelivery" <#if parameters.paymentCODPrePayDelivery??>checked="checked"</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMoneyXferAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentMoneyXferAccepted" <#if parameters.paymentMoneyXferAccepted??>checked="checked"</#if> /></td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_payPalEmail}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="payPalEmail" size="50" maxlength="50" value="${parameters.payPalEmail!}" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_customXml}</td>
                <td>&nbsp;</td>
                <td>
                    <textarea cols="60" rows="6" wrap="soft" name="customXml">${customXml!}</textarea>
                </td>
            </tr>
            <tr>
                <td colspan=2>&nbsp;</td>
                <td>
                    <input type="submit" value="${uiLabelMap.EbayExportToEbay}" name="submitButton" class="smallSubmit" />
                </td>
            </tr>
            </#if>
        </table>
    </form>
    <script language="JavaScript" type="text/javascript">
        activateSubmitButton();
    </script>
</div>
