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
                                                        <div style="font-size: 20px;padding-bottom: 10px;">Finance Status Report as of ${today!}</div>
                                                    </td>
                                                </tr>
                                            </table> <!-- end primary module -->
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>

                        <tr>
                            <td style="padding-left:0;padding-right:0;">
                                <table border="0" cellpadding="0" cellspacing="0" width="100%">
                                    <tr>
                                        <td  style="text-align: center;padding:2px;background-color: #f4f4f4;border-right:1px solid #fff; font-size: 15px; font-weight: 600;"
                                             colspan="2">Accounts Receivable <#if dateRangeFromUsFormatted?? && dateRangeToUsFormatted??>(${dateRangeFromUsFormatted!} to ${dateRangeToUsFormatted!})</#if>
                                        </td>

                                        <td  style="text-align: center;padding:2px;background-color: #f4f4f4;border-right:1px solid #fff; font-size: 15px; font-weight: 600;"
                                             colspan="2">Accounts Payable <#if dateRangeFromUsFormatted?? && dateRangeToUsFormatted??>(${dateRangeFromUsFormatted!} to ${dateRangeToUsFormatted!})</#if>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td  style="border-right:1px solid #fff;background-color: #107C10" width="25%">
                                            <div style="color: white; text-align: center; ">${paidReceivableAwards!} of ${totalReceivableAwards!} Awards (Received)</div>
                                            <div style="color: white; text-align: center; "><@ofbizCurrency amount=paidAwardTotalReceivableAmount isoCode=currencyUomId/> of <@ofbizCurrency amount=totalReceivableAwardsAmount isoCode=currencyUomId/></div>
                                        </td>
                                        <td style="border-right:1px solid #fff;background-color: #d83b01;" width="25%">
                                            <div style="color: white; text-align: center; ">${unpaidReceivableAwards!} of ${totalReceivableAwards!} Awards (Outstanding)</div>
                                            <div style="color: white; text-align: center; "><@ofbizCurrency amount=totalOutstandingAmount isoCode=currencyUomId/>  of <@ofbizCurrency amount=totalReceivableAwardsAmount isoCode=currencyUomId/></div>
                                        </td>

                                        <td  style="border-right:1px solid #fff; border-left:1px solid #fff; background-color: #107C10" width="25%">
                                            <div style="color: white; text-align: center; ">${paidPayableAwardsRecd!} of ${totalPayableAwardsRecd!} Awards (Paid Out)</div>
                                            <div style="color: white; text-align: center; "><@ofbizCurrency amount=paidAwardTotalPayableAmountRecd isoCode=currencyUomId/> of <@ofbizCurrency amount=totalPayableAwardsAmountRecd isoCode=currencyUomId/></div>
                                        </td>
                                        <td style="border-right:1px solid #fff;background-color: #d83b01;" width="25%">
                                            <div style="color: white; text-align: center; ">${unpaidPayableAwardsRecd!} of ${totalPayableAwardsRecd!} Awards (Outstanding)</div>
                                            <div style="color: white; text-align: center; "><@ofbizCurrency amount=pendingUnpaidAwardTotalPayableAmountRecd isoCode=currencyUomId/> of <@ofbizCurrency amount=totalPayableAwardsAmountRecd isoCode=currencyUomId/></div>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="border-right:1px solid #fff; border-top:1px solid #fff; background-color: #ff8c00;" width="50%" colspan="2">
                                            <div style="color: white; text-align: center; text-transform: uppercase;">Payment partially received for <strong>${partiallyPaidAwards!("0")} of ${totalReceivableAwards!}</strong> awards for the amount</div>
                                            <div style="color: white; text-align: center; text-transform: uppercase;"><@ofbizCurrency amount=partiallyPaidAwardTotalAmount isoCode=currencyUomId/> OF <@ofbizCurrency amount=partialReceivableQuoteTotal isoCode=currencyUomId/></div>
                                        </td>
                                        <td style="border-right:1px solid #fff; border-left:1px solid #fff; border-top:1px solid #fff; background-color: #ff8c00;" width="50%" colspan="2">
                                            <div style="color: white; text-align: center; text-transform: uppercase;">Payment partially paid for <strong>${partialPayableAwards!("0")} of ${totalPayableAwardsRecd!}</strong> awards for the amount</div>
                                            <div style="color: white; text-align: center; text-transform: uppercase;"><@ofbizCurrency amount=partialPayableAwardTotalAmount isoCode=currencyUomId/> OF <@ofbizCurrency amount=partialPayableQuoteTotal isoCode=currencyUomId/></div>
                                        </td>
                                    </tr>

                                    <tr>
                                        <td  style="border-right:1px solid #fff; border-top:1px solid #fff; background-color: #FFFF00;" width="50%" colspan="2">
                                            <div style="color: #e81123; text-align: center; text-transform: uppercase;">Shipping Hold Receivable outstanding for <strong>${totalOutstandingShippingHoldAwards!} of ${shippingTotalReceivableAwards!}</strong> awards for the amount</div>
                                            <div style="color: #e81123; text-align: center; "><@ofbizCurrency amount=totalShippingOutstandingAmount isoCode=currencyUomId/></div>
                                        </td>
                                        <td style="border-right:1px solid #fff; border-left:1px solid #fff; background-color: #FFFF00;" width="50%" colspan="2">
                                            <div style="color: #e81123; text-align: center; text-transform: uppercase;">Shipping Hold Payable outstanding for <strong>${totalPayableOutstandingShippingHoldAwards!} of ${shippingTotalPayableAwardsRecd!}</strong> awards for the amount</div>
                                            <div style="color: #e81123; text-align: center; "><@ofbizCurrency amount=totalPayableShippingOutstandingAmount isoCode=currencyUomId/></div>
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







