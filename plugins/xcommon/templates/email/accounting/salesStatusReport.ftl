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
       width="100%" bgcolor="#F7F8F9">
    <tr>
        <td align="center">
            <!-- main body table -->
            <table border="0" cellpadding="0" cellspacing="0" width="600">
                <tr>
                    <td style="padding: 10px 0px 20px 0px;">
                        <!--start preheader -->
                        <table border="0" cellspacing="0" cellpadding="0" width="600">
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
                                <td align="left" style="padding-left: 5px;">
                                <#if renderEmailBody?? && renderEmailBody == true>
                                    <img alt="" src="<@ofbizContentUrl>/ext/sellercentral/img/logo-group-164x33.png</@ofbizContentUrl>" height="30px"/>
                                <#else>
                                    <img src="cid:logoImageUrl3" height="30px" />
                                </#if>
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
                    <td style="border-left: 1px solid #dadbdd; border-right: 1px solid #dadbdd; background-color: white">
                        <!-- primary module wrapper -->
                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                            <tr>
                                <td align="left"
                                    colspan="2">
                                    <!-- primary module -->
                                    <table border="0" cellpadding="0" cellspacing="0" width="90%">
                                        <tr>
                                            <td align="left" style="padding-left: 30px;padding-right:30px;font-family: sans-serif, Arial, Verdana;">
                                                <div style="font-size: 20px;padding-bottom: 10px;">Sales Status Report as of ${today!}</div>
                                                <div style="font-size: 14px;padding-bottom: 10px;">Quotes Ordered but Payment not received</div>
                                            </td>
                                        </tr>
                                    </table>
                                    <!-- end primary module -->
                                </td>
                            </tr>
                        </table>

                        <table style="border-collapse: collapse;width: 100%;padding-left: 30px;padding-right: 30px;" border="0" cellpadding="0" cellspacing="0">
                            <tr style="background-color: white">
                                <td>
                                <#list salesInvoicesInfo as invoicesInfo>
                                    <div>
                                        <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;<#if (invoicesInfo.highlight)?matches('Y')>background-color: #ffff0021;<#else>background-color: #F7F8F9</#if>">
                                            <tr style="<#if (invoicesInfo.highlight)?matches('Y')>background-color: #FFFF00;</#if>">
                                                <td style="padding-top: 10px;padding-bottom: 10px;margin-bottom: 1em;margin-top: 1em; text-overflow: ellipsis;overflow: hidden;white-space: nowrap;">
                                                    <span align="left" style="padding-left: 30px;font-size: 1.17em;">${invoicesInfo.name!}</span>
                                                </td>
                                                <td style="padding-top: 10px;padding-bottom: 10px;margin-bottom: 1em;margin-top: 1em;padding-right: 30px;" align="right">
                                                    <img width="20px" src="https://cdn.fidelissd.com/img/icons/invoice.png">
                                                </td>
                                            </tr>
                                            <tr>
                                                <td align="left" style="padding-left: 30px;padding-top: 10px;">
                                                    <b>Unpaid Invoices</b>
                                                </td>
                                                <td></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of unpaid invoices</td>
                                                <td style="text-align: right; padding-right: 30px;color: red;">${invoicesInfo.numberOfUnpaidInvoices!}</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Outstanding amount</td>
                                                <td style="text-align: right; padding-right: 30px;color: red;"><@ofbizCurrency amount=invoicesInfo.totalAmountOutstanding isoCode=currencyUomId/></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Paid amount</td>
                                                <td style="text-align: right; padding-right: 30px;color: green;"><@ofbizCurrency amount=invoicesInfo.totalAmountPaid isoCode=currencyUomId/></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">
                                                    Total amount
                                                </td>
                                                <td style="text-align: right; padding-right: 30px;color: green;"><@ofbizCurrency amount=invoicesInfo.totalAmount isoCode=currencyUomId/></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of invoices partially paid</td>
                                                <td style="text-align: right; padding-right: 30px;color:orange;">${invoicesInfo.numberOfInvoicesPartiallyPaid!}/${invoicesInfo.numberOfUnpaidInvoices!}</td>
                                            </tr>
                                            <tr>
                                                <td align="left" style="padding-left: 30px;padding-top: 10px;">
                                                    <b>Shipments</b>
                                                </td>
                                                <td></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of shipments delivered</td>
                                                <td style="text-align: right; padding-right: 30px;color: green;">${invoicesInfo.numberOfShipmentsDelivered!}/${invoicesInfo.totalNumberOfShipments!}</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of shipments in transit</td>
                                                <td style="text-align: right; padding-right: 30px;color:orange;">${invoicesInfo.numberOfShipmentsInTransit!}/${invoicesInfo.totalNumberOfShipments!}</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of shipments not yet started</td>
                                                <td style="text-align: right; padding-right: 30px;color:red;">${invoicesInfo.numberOfShipmentsNotYetStarted!}/${invoicesInfo.totalNumberOfShipments!}</td>
                                            </tr>
                                            <tr>
                                                <td align="left" style="padding-left: 30px;padding-top: 10px;">
                                                    <b>Paid Invoices</b>
                                                </td>
                                                <td></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Number of invoices billed</td>
                                                <td style="text-align: right; padding-right: 30px;color: green;">${invoicesInfo.numberOfInvoicesBilled!}/${invoicesInfo.numberOfUnpaidInvoices!}</td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Total amount billed</td>
                                                <td style="text-align: right; padding-right: 30px;color: green;"><@ofbizCurrency amount=invoicesInfo.totalAmountBilled isoCode=currencyUomId/></td>
                                            </tr>
                                            <tr>
                                                <td style="padding-left: 30px;">Total amount paid</td>
                                                <td style="text-align: right; padding-right: 30px;color: green;"><@ofbizCurrency amount=invoicesInfo.totalAmountPaid isoCode=currencyUomId/></td>
                                            </tr>
                                            <tr>
                                                <td colspam="2" style="padding-left: 30px;">
                                                    <a href="${invoicesInfo.invoicesLink!}" style="text-decoration: none;">
                                                        View Records on Finance Page <img style="width: 12px;vertical-align: middle;" src="https://cdn.fidelissd.com/img/icons/icon-arrow.png">
                                                    </a>
                                                </td>
                                                <td>
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </#list>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td align="left"
                        style="background: #ffffff; border-left: 1px solid #dadbdd; border-right: 1px solid #dadbdd; border-bottom: 1px solid #dadbdd;padding-left: 30px;padding-top: 25px;">

                        <!-- secondary module 1 -->
                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                            <tr>
                                <td align="left"
                                    style="font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;line-height: 24px;">Thanks,</td>
                            </tr>
                            <tr>
                                <td align="left"
                                    style="font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">Team
                                    Fidelis</td>
                            </tr>
                            <tr>
                                <td align="left"
                                    style="font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px; padding-bottom: 25px;"><a
                                        href="http://fidelissd.com/">www.fidelissd.com</a></td>
                            </tr>
                        </table> <!-- end secondary module -->
                    </td>
                </tr>
                <tr>
                    <td>
                        <table border="0" cellspacing="0" cellpadding="0"
                               width="100%" bgcolor="#F7F8F9" align="center"
                               style="background-color:#F7F8F9;padding:0 24px;padding-top: 20px;color:#6a6c6d;text-align:center">
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
                                                    connections digest
                                                    emails.</p></td>
                                        </tr>
                                        <tr>
                                            <td align="center"
                                                style="padding:0 0 8px 0;text-align:center">
                                                <span style="color:#6a6c6d;white-space:normal;text-decoration:underline;display:inline-block"
                                                      target="_blank"
                                                >
                                                <#if renderEmailBody?? && renderEmailBody == true>
                                                    <img alt="" src="<@ofbizContentUrl>/ext/sellercentral/img/logo-group-164x33.png</@ofbizContentUrl>" height="30px"/>
                                                <#else>
                                                    <img src="cid:logoImageUrl3" height="30px" />
                                                </#if>
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







