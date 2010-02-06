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
    function changeEbayDuration(value) {
    var listingDuration = document.ProductsExportToEbay.listingDuration;
    listingDuration.options.length = 0;
     for (i = listingDuration.options.length; i >= 0; i--) {
        listingDuration.options[i] = null;
     }
        if (value == "FixedPriceItem"){
            listingDuration.options[0] =  new Option("3 ${uiLabelMap.CommonDays}","Days_3");
            listingDuration.options[1] =  new Option("5 ${uiLabelMap.CommonDays}","Days_5");
            listingDuration.options[2] =  new Option("7 ${uiLabelMap.CommonDays}","Days_7");
            listingDuration.options[3] =  new Option("10 ${uiLabelMap.CommonDays}","Days_10");
            listingDuration.options[4] =  new Option("30 ${uiLabelMap.CommonDays}","Days_30");
            listingDuration.options[5] =  new Option("Good 'Til Cancelled","2147483647");
        }else{
            listingDuration.options[0] =  new Option("1 ${uiLabelMap.CommonDay}","Days_1");
            listingDuration.options[1] =  new Option("3 ${uiLabelMap.CommonDays}","Days_3");
            listingDuration.options[2] =  new Option("5 ${uiLabelMap.CommonDays}","Days_5");
            listingDuration.options[3] =  new Option("7 ${uiLabelMap.CommonDays}","Days_7");
            listingDuration.options[4] =  new Option("10 ${uiLabelMap.CommonDays} ($0.40)","Days_10");
        }
     }
</script>
<div>
    <form id="ProductsExportToEbay" method="post" action="<@ofbizUrl>exportProductsFromEbayStore</@ofbizUrl>" name="ProductsExportToEbay">
        <input type="hidden" name="productStoreId" value="${productStoreId?if_exists}">
        <input type="hidden" name="prodCatalogId" value="${parameters.SEARCH_CATALOG_ID?if_exists}">
        <input type="hidden" name="productCategoryId" value="${parameters.SEARCH_CATEGORY_ID?if_exists}">
        <#if !productIds?has_content>
            <div><h2>${uiLabelMap.ProductNoResultsFound}.</h2></div>
        </#if>

        <#if productIds?has_content>
        <table border="0" cellpadding="2" cellspacing="0">
            <tr>
                <td align="right" class="label">${uiLabelMap.CommonCountry}</td>
                <td>&nbsp;</td>
                <td>
                    <select name="country">
                        <#if countries?exists>
                            <#list countries as country>
                                <option value="${country.geoCode}" <#if countryCode?exists && countryCode == country.geoCode>selected</#if>>${country.get("geoName",locale)}</option>
                            </#list>
                        </#if>
                    </select>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_location}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="location" size="50" maxlength="50" value="${parameters.location?if_exists}" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_webSiteUrl}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="webSiteUrl" size="100" value="${webSiteUrl?if_exists}"/>
                </td>
            </tr>
            <tr>
                <td align="center" colspan="3"><b><u>${uiLabelMap.FormFieldTitle_paymentMethodsAccepted}</u></b></td>
            </tr>
            <tr>
                <td colspan="3">
                    <table class="basic-table" cellspacing="0">
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentPayPal}</td>
                            <td width="2%"><input type="checkbox" name="paymentPayPal" <#if parameters.paymentPayPal?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentVisaMC}</td>
                            <td width="2%"><input type="checkbox" name="paymentVisaMC" <#if parameters.paymentVisaMC?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentAmEx}</td>
                            <td width="2%"><input type="checkbox" name="paymentAmEx" <#if parameters.paymentAmEx?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentDiscover}</td>
                            <td width="2%"><input type="checkbox" name="paymentDiscover" <#if parameters.paymentDiscover?exists>checked</#if> /></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMOCC}</td>
                            <td width="2%"><input type="checkbox" name="paymentMOCC" <#if parameters.paymentMOCC?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentPersonalCheck}</td>
                            <td width="2%"><input type="checkbox" name="paymentPersonalCheck" <#if parameters.paymentPersonalCheck?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCCAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentCCAccepted" <#if parameters.paymentCCAccepted?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashInPerson}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashInPerson" <#if parameters.paymentCashInPerson?exists>checked</#if> /></td>
                        </tr>
                        <tr>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCashOnPickup}</td>
                            <td width="2%"><input type="checkbox" name="paymentCashOnPickup" <#if parameters.paymentCashOnPickup?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCOD}</td>
                            <td width="2%"><input type="checkbox" name="paymentCOD" <#if parameters.paymentCOD?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentCODPrePayDelivery}</td>
                            <td width="2%"><input type="checkbox" name="paymentCODPrePayDelivery" <#if parameters.paymentCODPrePayDelivery?exists>checked</#if> /></td>
                            <td align="right" width="23%" class="label">${uiLabelMap.FormFieldTitle_paymentMoneyXferAccepted}</td>
                            <td width="2%"><input type="checkbox" name="paymentMoneyXferAccepted" <#if parameters.paymentMoneyXferAccepted?exists>checked</#if> /></td>
                        </tr>
                    </table>
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_payPalEmail}</td>
                <td>&nbsp;</td>
                <td>
                    <input type="text" name="payPalEmail" size="50" maxlength="50" value="${parameters.payPalEmail?if_exists}" />
                </td>
            </tr>
            <tr>
                <td align="right" class="label">${uiLabelMap.FormFieldTitle_customXml}</td>
                <td>&nbsp;</td>
                <td>
                    <textarea cols="60" rows="6" wrap="soft" name="customXml"> ${(ebayConfig.customXml)?if_exists} </textarea>
                </td>
            </tr>
            <#assign rowCount = 0 />
            <#list productIds as productId>
            <tr>
                <td colspan="3">
                <hr/>
                <b>${productId}</b>
                <input type="hidden" name="productId_o_${rowCount}" value="${productId}"/>
                    <table class="basic-table" cellspacing="0" width="50%">
                        <tr>
                            <td colspan="2">
                                <input type="checkbox" name="requireEbayInventory_o_${rowCount}" value="on"/>
                            </td>
                            <td colspan="2">${uiLabelMap.requireEbayInventory}</td>
                        </tr>
                        <tr>
                            <td>
                                <input type="checkbox" name="listingTypeAuc_o_${rowCount}" value="on"/>
                            </td>
                            <td>${uiLabelMap.eBayOnlineAuction}</td>
                            <td>${uiLabelMap.FormFieldTitle_listingDuration}</td>
                            <td>
                                    <select name="listingDurationAuc_o_${rowCount}">
                                        <option value="Days_1">1 ${uiLabelMap.CommonDay}</option>
                                        <option value="Days_3">3 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_5">5 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_7">7 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_10">10 ${uiLabelMap.CommonDays}</option>
                                    </select>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <input type="checkbox" name="listingTypeFixed_o_${rowCount}" value="on"/>
                            </td>
                            <td>${uiLabelMap.eBayFixedPrice}</td>
                            <td>${uiLabelMap.FormFieldTitle_listingDuration}</td>
                            <td>
                                    <select name="listingDurationFixed_o_${rowCount}">
                                        <option value="Days_3">3 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_5">5 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_7">7 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_10">10 ${uiLabelMap.CommonDays}</option>
                                        <option value="Days_30">30 ${uiLabelMap.CommonDays}</option>
                                        <option value="2147483647">Good 'Til Cancelled</option>
                                    </select>
                            </td>
                        </tr>
                        <tr>
                            <td colspan="2">${uiLabelMap.CommonQuantity}</td>
                            <td colspan="2">
                                <input type="text" name="quantity_o_${rowCount}" size="12" maxlength="12" value="<#if parameters.quantity?exists>${parameters.quantity?if_exists}<#else>1</#if>" />
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            <#assign rowCount=rowCount + 1/>
            </#list>
            <tr>
                <td colspan=2>&nbsp;</td>
                <td>
                    <input type="submit" value="${uiLabelMap.EbayExportToEbay}" name="submitButton" class="smallSubmit">
                </td>
            </tr>
        </table>
        </#if>
    </form>
</div>