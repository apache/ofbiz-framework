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
     function changeEbayCategory(categ, cat_desc) {
         document.forms["ProductsExportToEbay"].action = "<@ofbizUrl>ProductsExportToEbay?categoryCode="+categ+"</@ofbizUrl>";
         document.forms["ProductsExportToEbay"].submit();
     }
     
     function activateSubmitButton() {
         categ = document.forms["ProductsExportToEbay"].ebayCategory.value;
         if (categ != null && categ.substring(0, 1) == 'Y') {
             document.forms["ProductsExportToEbay"].submitButton.disabled = false;
         } else {
             document.forms["ProductsExportToEbay"].submitButton.disabled = true;
         }
    } 
</script>
<div>
    <form method="post" action="<@ofbizUrl>PostProductsToEbay</@ofbizUrl>" name="ProductsExportToEbay">
        <table border="0" cellpadding="2" cellspacing="0">
             <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_ebayCategory}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="hidden" name="selectResult" value="${selectResult}"/>
                    <select name="ebayCategory" onchange="changeEbayCategory(this.value, this.options[this.selectedIndex].text)">
                        <#if categories?exists>
                            <#list categories as category>
                                <option value="${category.CategoryCode}">${category.CategoryName}</option>
                            </#list>
                        </#if>
                    </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonCountry}</td>
                <td>&nbsp;</td>
                <td>
                    <select name="country">
                        <#if countries?exists>
                            <#list countries as country>
                                <option <#if country.geoCode?has_content && country.geoCode == "US">selected="selected"</#if> value="${country.geoCode}">${country.get("geoName",locale)}</option>
                            </#list>
                        </#if>
                    </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_location}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="location" size="50" maxlength="50"/>
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
                    <input type="text" name="startPrice" size="12" maxlength="12" value="1.0"/>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonQuantity}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="qnt" size="12" maxlength="12" value="1"/>
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
                            <td width="2%"><input type="checkbox" name="paymentPayPal" checked="checked"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentVisaMC}</td>
                            <td width="2%"><input type="checkbox" name="paymentVisaMC" checked="checked"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentAmEx}</td>
                            <td width="2%"><input type="checkbox" name="paymentAmEx" checked="checked"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentDiscover}</td>
                            <td width="2%"><input type="checkbox" name="paymentDiscover" checked="checked"/></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMOCC}</td>
                            <td width="2%"><input type="checkbox" name="paymentMOCC" checked="checked"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentPersonalCheck}</td>
                            <td width="2%"><input type="checkbox" name="paymentPersonalCheck" checked="checked"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCCAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentCCAccepted"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashInPerson}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashInPerson"/></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashOnPickup}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashOnPickup"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCOD}</td>
                            <td width="2%"><input type="checkbox" name="paymentCOD"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCODPrePayDelivery}</td>
                            <td width="2%"><input type="checkbox" name="paymentCODPrePayDelivery"/></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMoneyXferAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentMoneyXferAccepted"/></td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_payPalEmail}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="quantity" size="50" maxlength="50"/>
                </td>
            </tr>
            <tr>
                <td colspan=2>&nbsp;</td>
                <td>
                    <input type="submit" value="${uiLabelMap.EbayExportToEbay}" name="submitButton" class="smallSubmit">
                </td>
            </tr>
        </table>
    </form>
    <script language="JavaScript" type="text/javascript">
        activateSubmitButton();
    </script>
</div>