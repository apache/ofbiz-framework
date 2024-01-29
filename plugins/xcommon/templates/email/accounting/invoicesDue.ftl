<html xmlns:collapse="http://docbook.org/ns/docbook" xmlns:er="http://www.w3.org/1999/xhtml" xmlns:6.0pt xmlns:6.0pt
      xmlns:o="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <meta property="og:title" content="Fidelis" />
    <title>Fidelis</title>
</head>
<body style="margin: 0;">
<!-- outer frame -->
<table border="0" cellpadding="0" cellspacing="0"
       width="100%">
    <tr>
        <td align="center" style="border: 1px solid #dddddd;">
            <!-- main body table -->
            <table border="0" cellpadding="0" cellspacing="0" width="800">
                <tr>
                    <td style="padding: 10px 0px 20px 0px;">
                        <!--start preheader -->
                        <table border="0" cellspacing="0" cellpadding="0" width="800">
                            <tr>
                                <td align="left"
                                    style="font: 11px arial, verdana, sans-serif; color: #505050; padding-bottom: 20px; padding-left: 2px;">&nbsp;</td>
                                <td align="right"
                                    style="font: 11px arial, verdana, sans-serif; color: #505050; padding-bottom: 20px; padding-right: 2px;">&nbsp;</td>
                            </tr>
                        </table> <!--end preheader -->

                    </td>
                </tr>
                <tr>
                    <td style="background: #ffffff; border-top: 1px solid #dadbdd; border-left: 1px solid #dadbdd; border-right: 1px solid #dadbdd;">

                        <!-- header -->
                        <table border="0" cellspacing="0" cellpadding="0" width="100%">
                            <tr>
                                <td height="20"></td>
                            </tr>
                            <tr>
                                <td width="25"></td>
                                <td align="left">
                                    <img src="cid:logoImageUrl3" height="30px" />
                                </td>
                                <td width="25"></td>
                            </tr>
                            <tr>
                                <td height="20"></td>
                            </tr>
                        </table> <!-- end header -->
                    </td>
                </tr>
                <tr>
                    <td align="center"
                        style="border-left: 1px solid #dadbdd; border-right: 1px solid #dadbdd; background-color: white">
                        <!-- primary module wrapper -->
                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                            <tr>
                                <td align="left"
                                    style="background: #fafafa; border-bottom: 1px solid #f4f3f3;"
                                    colspan="2">
                                    <!-- primary module -->
                                    <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                        <tr>
                                            <td align="center"
                                                style="color: #333333; padding: 20px 15px; background-color: #fafafa;">
                                                <span style="font: 20px arial, verdana, sans-serif;">Purchase Invoices Approaching Their Due Dates</span>
                                                <div style="font: 16px arial, verdana, sans-serif;">As of ${today!}</div>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <table border="0" cellspacing="0" cellpadding="0"
                           width="100%">
                        <tbody>
                        <tr>
                            <td>
                                <table border="0" cellspacing="0"
                                       cellpadding="0" width="100%"
                                       style="border-bottom:4px solid #edf0f3">
                                    <tbody>
                                    <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                        <tr style="">
                                            <td colspan="5" style="font: 20px arial, verdana, sans-serif;border: 1px solid #dddddd;text-align: center;background-color: #FAFAFA;">Vendor Snapshot</td>
                                        </tr>
                                        <tr style="">
                                            <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;background-color: #dddddd; font-weight: bold"><div>Vendor Name</div><div>&nbsp;</div></td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #FFFF00; font-weight: bold"><div>On Hold</div><div>($ / Count)</div></td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #dddddd; font-weight: bold"><div>Past Due</div><div>($ / Count)</div></td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #dddddd; font-weight: bold"><div>Due in 7 Days</div><div>($ / Count)</div></td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #dddddd; font-weight: bold"><div>Due in 15 Days</div><div>($ / Count)</div></td>
                                            <#--<td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;background-color: #dddddd; font-weight: bold"><div>Total Due</div><div>($ / Count)</div></td>-->
                                        </tr>

                                    <#if suppliersInvoiceList?? && (suppliersInvoiceList?size>0)>
                                        <#list suppliersInvoiceList as supplierInvoice>
                                            <tr style="" bgcolor="#EEF5FB">
                                                <#assign supplierName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, supplierInvoice.supplierPartyId) >
                                                 <td style="border: 1px solid #dddddd;text-align: left; padding:10px 25px 10px 25px; font-weight: bold;color: #0078d7;">${supplierName!}</td>
                                                 <td style="border: 1px solid #dddddd;text-align: right; padding-right: 5px;background-color:#FFFF00;">
                                                    <#if !supplierInvoice.shippingHoldPo?matches("0") && supplierInvoice.shippingHoldPo?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.shippingHoldPo isoCode=currencyUomId/>
                                                        <#if supplierInvoice.shippingHoldPoCount??> / ${supplierInvoice.shippingHoldPoCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-right: 5px;">
                                                    <#if !supplierInvoice.pastDue?matches("0") && supplierInvoice.pastDue?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.pastDue isoCode=currencyUomId/>
                                                        <#if supplierInvoice.pastDueCount??> / ${supplierInvoice.pastDueCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-right: 5px;">
                                                    <#if !supplierInvoice.sevenDays?matches("0") && supplierInvoice.sevenDays?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.sevenDays isoCode=currencyUomId/>
                                                        <#if supplierInvoice.sevenDaysCount??> / ${supplierInvoice.sevenDaysCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-right: 25px;">
                                                    <#if !supplierInvoice.fifteenDays?matches("0") && supplierInvoice.fifteenDays?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.fifteenDays isoCode=currencyUomId/>
                                                        <#if supplierInvoice.fifteenDaysCount??> / ${supplierInvoice.fifteenDaysCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                            <#--<td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;"><@ofbizCurrency amount= supplierInvoice.totalDue isoCode=currencyUomId/><#if supplierInvoice.totalDueCount??> / ${supplierInvoice.totalDueCount!}</#if></td>-->

                                            </tr>
                                            <tr style="">
                                            <#list supplierInvoice.categorizedShippingHoldPoList as l>
                                                <tr style="border: 1px solid #dddddd;padding-left: 25px;padding-right: 25px;">
                                                    <td style="padding: 5px;padding-left: 25px;">
                                                        <div>
                                                            <img width="20px" src="https://cdn.fidelissd.com/img/icons/icon-arrow-01.png">
                                                            <span style="font-weight: normal;">${l.category!}</span>
                                                        </div>
                                                    </td>
                                                    <td style="padding: 5px;text-align: right;background-color: #ffff0021;padding-right: 5px;">
                                                      <#if l.value?has_content && !l.value?matches("0")>
                                                          <a href="${l.link!}" title="Click to view awards" style="text-decoration: none;">
                                                              <@ofbizCurrency amount= l.value isoCode=currencyUomId/> / ${l.count!}
                                                              <div style="font-size: 10px;color: grey;"><b>${l.quantity!} Unit(s)</b></div>
                                                          </a>
                                                      <#else>
                                                          <div>
                                                              --
                                                          </div>
                                                      </#if>
                                                    </td>
                                                    <td style="padding: 5px;text-align: right;padding-right: 5px;">
                                                        <#if supplierInvoice.categorizedPastDueList?exists>
                                                            <#list supplierInvoice.categorizedPastDueList as pastDue>
                                                                <#if pastDue.category?matches('${l.category!}') >
                                                                    <#if !pastDue.value?matches("0") && pastDue.value?has_content>
                                                                        <a href="${pastDue.link!}" title="Click to view awards" style="text-decoration: none;">
                                                                            <div><@ofbizCurrency amount= pastDue.value isoCode=currencyUomId/>  / ${pastDue.count!}</div>
                                                                            <div style="font-size: 10px;color: grey;"><b>${pastDue.quantity!} Unit(s)</b></div>
                                                                        </a>
                                                                     <#else>
                                                                        <div>
                                                                            --
                                                                        </div>
                                                                    </#if>
                                                                </#if>
                                                            </#list>
                                                        </#if>
                                                    </td>
                                                    <td style="padding: 5px;text-align: right;padding-right: 5px;">
                                                        <#if supplierInvoice.categorizedDueIn7DaysList?exists>
                                                            <#list supplierInvoice.categorizedDueIn7DaysList as dueIn7>
                                                                <#if dueIn7.category?matches('${l.category!}')>
                                                                    <#if !dueIn7.value?matches("0") && dueIn7.value?has_content>
                                                                        <a href="${dueIn7.link!}" title="Click to view awards" style="text-decoration: none;">
                                                                            <div><@ofbizCurrency amount= dueIn7.value isoCode=currencyUomId/> / ${dueIn7.count!} </div>
                                                                            <div style="font-size: 10px;color: grey;"><b>${dueIn7.quantity!} Unit(s)</b></div>
                                                                        </a>
                                                                    <#else>
                                                                        <div>
                                                                            --
                                                                        </div>
                                                                    </#if>
                                                                </#if>
                                                            </#list>
                                                        </#if>
                                                    </td>
                                                    <td style="padding: 5px;text-align: right;padding-right: 25px;">
                                                        <#if supplierInvoice.categorizedDueIn15DaysList?exists>
                                                            <#list supplierInvoice.categorizedDueIn15DaysList as dueIn15>
                                                                <#if dueIn15.category?matches('${l.category!}')>
                                                                    <#if !dueIn15.value?matches("0") && dueIn15.value?has_content>
                                                                        <a href="${dueIn15.link!}" title="Click to view awards" style="text-decoration: none;">
                                                                            <div><@ofbizCurrency amount= dueIn15.value isoCode=currencyUomId/> / ${dueIn15.count!} </div>
                                                                            <div style="font-size: 10px;color: grey;"><b>${dueIn15.quantity!} Unit(s)</b></div>-
                                                                        </a>
                                                                    <#else>
                                                                        <div>
                                                                            --
                                                                        </div>
                                                                    </#if>
                                                                </#if>
                                                            </#list>
                                                        </#if>
                                                    </td>
                                                </tr>
                                            </#list>
                                            <tr style="" bgcolor="#F2F2F2">
                                                <td style="border: 1px solid #dddddd;text-align: left; padding: 10px 25px 10px 25px;"><b>Total</b></td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 5px;background-color:#FFFF00;">
                                                    <#if !supplierInvoice.shippingHoldPo?matches("0") && supplierInvoice.shippingHoldPo?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.shippingHoldPo isoCode=currencyUomId/>
                                                        <#if supplierInvoice.shippingHoldPoCount??> / ${supplierInvoice.shippingHoldPoCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right;padding-right: 5px;">
                                                    <#if !supplierInvoice.pastDue?matches("0") && supplierInvoice.pastDue?has_content>
                                                    <b><@ofbizCurrency amount= supplierInvoice.pastDue isoCode=currencyUomId/>
                                                        <#if supplierInvoice.pastDueCount??> / ${supplierInvoice.pastDueCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-right: 5px;">
                                                    <#if !supplierInvoice.sevenDays?matches("0") && supplierInvoice.sevenDays?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.sevenDays isoCode=currencyUomId/>
                                                        <#if supplierInvoice.sevenDaysCount??> / ${supplierInvoice.sevenDaysCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-right: 25px;">
                                                    <#if !supplierInvoice.fifteenDays?matches("0") && supplierInvoice.fifteenDays?has_content>
                                                        <b><@ofbizCurrency amount= supplierInvoice.fifteenDays isoCode=currencyUomId/>
                                                        <#if supplierInvoice.fifteenDaysCount??> / ${supplierInvoice.fifteenDaysCount!}</#if></b>
                                                    <#else>
                                                        <div>
                                                            --
                                                        </div>
                                                    </#if>
                                                </td>
                                            <#--<td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;"><@ofbizCurrency amount= supplierInvoice.totalDue isoCode=currencyUomId/><#if supplierInvoice.totalDueCount??> / ${supplierInvoice.totalDueCount!}</#if></td>-->

                                            </tr>

                                                <tr style="border: 1px solid #dddddd;background-color: #EFEFEF;">
                                                    <td style="padding-left: 25px;padding-right: 25px;">
                                                        <span style="text-align: left;"><strong>Total Due($ / Count)</strong></span>
                                                    </td>
                                                    <td colspan="4" style="text-align: right;padding: 12px;padding-left: 25px;padding-right: 25px;">
                                                        <strong>
                                                            <@ofbizCurrency amount= supplierInvoice.totalDue isoCode=currencyUomId/><#if supplierInvoice.totalDueCount??> / ${supplierInvoice.totalDueCount!}</#if>
                                                        </strong>
                                                    </td>
                                                </tr>
                                            </tr>
                                        </#list>
                                    </#if>
                                        <tr style="">
                                            <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;padding-top: 10px;padding-bottom: 10px;background-color: #dddddd; font-weight: bold">Total</td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 5px;background-color: #FFFF00; font-weight: bold">
                                                <#if !amountOfShippingHoldPoInvoices?matches("0") && amountOfShippingHoldPoInvoices?has_content>
                                                    <div><@ofbizCurrency amount= amountOfShippingHoldPoInvoices isoCode=currencyUomId/>
                                                        / ${numberOfShippingHoldPoInvoices!("0")}
                                                    </div>
                                                <#else>
                                                    <div>
                                                        --
                                                    </div>
                                                </#if>
                                            </td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 5px;background-color: #dddddd; font-weight: bold">
                                            <#if !amountPastDue?matches("0") && amountPastDue?has_content>
                                                <div> <@ofbizCurrency amount= amountPastDue isoCode=currencyUomId/>
                                                    / ${numberOfInvoicesPastDue!("0")}
                                                </div>
                                            <#else>
                                                <div>
                                                    --
                                                </div>
                                            </#if>
                                            </td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 5px;background-color: #dddddd; font-weight: bold">
                                            <#if !amountDueIn7Days?matches("0") && amountDueIn7Days?has_content>
                                                <div> <@ofbizCurrency amount= amountDueIn7Days isoCode=currencyUomId/>
                                                    / ${numberOfInvoicesDueIn7Days!("0")}
                                                </div>
                                            <#else>
                                                <div>
                                                    --
                                                </div>
                                            </#if>
                                            </td>
                                            <td style="border: 1px solid #dddddd;text-align: right; padding-right: 25px;background-color: #dddddd; font-weight: bold">
                                                <#if !amountDueIn15Days?matches("0") && amountDueIn15Days?has_content>
                                                    <div><@ofbizCurrency amount= amountDueIn15Days isoCode=currencyUomId/>
                                                        / ${numberOfInvoicesDueIn15Days!("0")}
                                                    </div>
                                                <#else>
                                                    <div>
                                                        --
                                                    </div>
                                                </#if>
                                            </td>
                                            <#--<td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;background-color: #dddddd; font-weight: bold"><@ofbizCurrency amount= grandTotalDue isoCode=currencyUomId/> / ${totalNumberOfDueInvoices!("0")}</td>-->
                                        </tr>
                                        <tr style="background-color: #C3C3C3;">
                                            <td style="padding-left: 25px;padding-right: 25px;">
                                                <span style="text-align: left;"><strong>Grand Total($ / Count)</strong></span>
                                            </td>
                                            <td colspan="4" style="text-align: right;padding: 12px;padding-left: 25px;padding-right: 25px;">
                                                <strong>
                                                <@ofbizCurrency amount= grandTotalDue isoCode=currencyUomId/> / ${totalNumberOfDueInvoices!("0")}
                                                </strong>
                                            </td>
                                        </tr>
                                    </table>
                                    </tbody>
                                </table>
                            </td>
                        </tr>

                        </tbody>
                    </table>
                </tr>
                <tr>
                    <td style="border: 1px solid #dddddd;">
                        <table border="0" cellspacing="0" cellpadding="0"
                               width="100%">
                            <tbody>
                                <tr>
                                    <td>
                                        <table border="0" cellspacing="0"
                                               cellpadding="0" width="100%"
                                               style="border-bottom:4px solid #edf0f3; margin-top: 10px">
                                            <tbody>
                                                <tr>
                                                    <td>
                                                        <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                                            <tr style="">
                                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;background-color: #dddddd;width: 40%;padding-right: 25px;">Invoices</th>
                                                                <th style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;background-color: #dddddd;padding-right: 25px;">Number of Invoices</th>
                                                                <th style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;background-color: #dddddd;padding-right: 25px;">Dollar Amount</th>
                                                            </tr>
                                                            <tr style="">
                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;width: 40%;background-color: #FFFF00;"><div>On Hold</div></td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #ffff0021;"><#if numberOfShippingHoldPoInvoices?has_content && !numberOfShippingHoldPoInvoices?matches("0")>${numberOfShippingHoldPoInvoices!}<#else>--</#if></td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #FFFF00;">
                                                                <#if amountOfShippingHoldPoInvoices?has_content && !amountOfShippingHoldPoInvoices?matches("0")>
                                                                    <@ofbizCurrency amount= amountOfShippingHoldPoInvoices isoCode=currencyUomId/>
                                                                <#else>
                                                                    --
                                                                </#if>
                                                                </td>
                                                            </tr>
                                                            <tr style="">
                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;width: 40%;">Past Due</td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;"><#if numberOfInvoicesPastDue?has_content && !numberOfInvoicesPastDue?matches("0")>${numberOfInvoicesPastDue!}<#else>--</#if></td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;">
                                                                    <#if amountPastDue?has_content && !amountPastDue?matches("0")>
                                                                        <@ofbizCurrency amount= amountPastDue isoCode=currencyUomId/>
                                                                    <#else>
                                                                        --
                                                                    </#if>
                                                                </td>
                                                            </tr>
                                                            <tr style="">
                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;width: 40%;">Due in 7 Days</td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;"><#if numberOfInvoicesDueIn7Days?has_content && !numberOfInvoicesDueIn7Days?matches("0")>
                                                                  ${numberOfInvoicesDueIn7Days!}<#else>--</#if>
                                                                </td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;">
                                                                    <#if amountDueIn7Days?has_content && !amountDueIn7Days?matches("0")>
                                                                        <@ofbizCurrency amount= amountDueIn7Days isoCode=currencyUomId/>
                                                                    <#else>
                                                                        --
                                                                    </#if>
                                                                </td>
                                                            </tr>
                                                            <tr style="">
                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;width: 40%;">Due in 15 Days</td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;">
                                                                 <#if numberOfInvoicesDueIn15Days?has_content && !numberOfInvoicesDueIn15Days?matches("0")>
                                                                       ${numberOfInvoicesDueIn15Days!}
                                                                 <#else>--</#if>
                                                                 </td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;">
                                                                <#if amountDueIn15Days?has_content && !amountDueIn15Days?matches("0")>
                                                                    <@ofbizCurrency amount= amountDueIn15Days isoCode=currencyUomId/>
                                                                <#else>
                                                                    --
                                                                </#if>
                                                                </td>
                                                            </tr>
                                                            <tr style="">
                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 25px;padding-right: 25px;width: 40%;background-color: #dddddd;font-weight: bold">Total</td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #dddddd;font-weight: bold">${totalNumberOfDueInvoices!("--")}</td>
                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 25px;padding-right: 25px;background-color: #dddddd;font-weight: bold"><#if amountDueIn15Days??><@ofbizCurrency amount= grandTotalDue isoCode=currencyUomId/><#else>--</#if></td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td>
                        <table border="0" cellspacing="0" cellpadding="0"
                               width="100%" align="center"
                               style="padding:0 24px;padding-top: 20px;color:#6a6c6d;text-align:center">
                            <tbody>
                            <tr>
                                <td>
                                    <table border="0" cellspacing="0"
                                           cellpadding="0" width="100%">
                                        <tbody>
                                        <tr>
                                            <td align="center"
                                                style="padding:0 0 12px 0;text-align:center">
                                                <p style="padding:0;margin:0;color:#6a6c6d;font-weight:400;font-size:12px;line-height:1.333">
                                                    You are receiving
                                                    Messages from
                                                    Zeus notifications digest
                                                    emails.</p></td>
                                        </tr>
                                        <tr>
                                            <td align="center"
                                                style="padding:0 0 8px 0;text-align:center">
                                                    <span style="color:#6a6c6d;white-space:normal;text-decoration:underline;display:inline-block"
                                                          target="_blank"
                                                    ><img
                                                            border="0"
                                                            height="14"
                                                            src="cid:logoImageUrl3"
                                                            width="58"
                                                            style="outline:none;color:#ffffff;display:block;text-decoration:none">
                                                    </span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td align="center"
                                                style="padding:0 0 12px 0;text-align:center">
                                                <p style="padding:0;margin:0;color:#6a6c6d;font-weight:400;font-size:12px;line-height:1.333">
                                                    Copyright &copy; 2013-${nowTimestamp?string("yyyy")} Fidelis Sustainability Distribution.
                                        </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
            </table> <!-- end Main body table -->
        </td>
    </tr>
</table>
<!-- end outer frame -->
</body>
</html>