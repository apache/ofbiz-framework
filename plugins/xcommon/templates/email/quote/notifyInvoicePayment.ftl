<html xmlns:collapse="http://docbook.org/ns/docbook" xmlns:er="http://www.w3.org/1999/xhtml" xmlns:6.0pt xmlns:6.0pt
      xmlns:o="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Fidelis</title>
    <style type=“text/css”>
        @media only screen and (min-device-width: 375px) and (max-device-width: 667px) {
            table { max-width: 320px;}
        }

        @media only screen and (min-device-width: 414px) and (max-device-width: 780px) {
            table { max-width: 410px;}
        }
    </style>

</head>

<body style="margin: 0;">

<table align="center" border="0" cellspacing="0" cellpadding="0" width="100%"
       bgcolor="#EDF0F3" style="background-color:#edf0f3;table-layout:fixed">
    <tbody>
    <tr>
        <td align="center">
            <center style="width:100%">
                <table border="0"
                       cellspacing="0" cellpadding="0" width="650"
                       bgcolor="#FFFFFF"
                       style="background-color:#ffffff;margin:0 auto;max-width:682px;width:inherit">
                    <tbody>
                    <tr>
                        <td bgcolor="#F6F8FA"
                            style="background-color:#f6f8fa;padding:5px 16px 13px;border-bottom:1px solid #ececec">
                            <table border="0" cellspacing="0" cellpadding="0"
                                   width="100%"
                                   style="width:100%!important;min-width:100%!important">
                                <tbody>
                                <tr>
                                    <td align="left" valign="middle">
                                        <img src="cid:logoImageUrl3" height="30" border="0" style="outline:none;color:#ffffff;text-decoration:none;display:block"/>
                                    </td>
                                    <td width="1">&nbsp;</td>
                                </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <table border="0" cellspacing="0" cellpadding="0"
                                   width="100%">
                                <tbody>
                                <tr>
                                    <td>
                                        <table border="0" cellpadding="0" cellspacing="0" width="100%" style="padding-left: 20px;">
                                            <tr style="background-color: white">
                                                <td>
                                                    <h3>Hello,</h3>
                                                    <p>Payment for below invoices that had an effective date as of today, has been applied and invoices have been marked as paid:</p>
                                                </td>
                                            </tr>
                                        </table>

                                        <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                            <tr style="">
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Awarded PO #</th>
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Invoice Type</th>
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Invoice Id</th>
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Contract Supplier Name</th>
                                                <th style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;background-color: #dddddd;padding-right: 12px;">Payment Amount</th>
                                            </tr>
                                            <tr style="">
                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;">${purchaseOrderNumber!('N/A')}</td>
                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;">${invoiceType!('N/A')}</td>
                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;">${invoiceId!('N/A')}</td>
                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;">${contractSupplierName!('N/A')}</td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;">
                                                    <strong><@ofbizCurrency amount= billedAmount isoCode=currencyUomId/></strong>
                                                </td>
                                            </tr>
                                        </table>
                                        &nbsp;
                                        <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                            <tr style="">

                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Awarded Date</th>
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Payment Scheduled On</th>
                                                <th style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;background-color: #dddddd;">Contract Customer Name</th>
                                                <th style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;background-color: #dddddd;padding-right: 12px;">Contract Amount</th>
                                            </tr>
                                            <tr style="">

                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;"><#if awardedDate?has_content>${awardedDate}<#else>N/A</#if></td>

                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;"><#if effectivePaymentDate?has_content>${effectivePaymentDate}<#else>N/A</#if>
                                                </td>
                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;">${contractCustomerName!('N/A')}</td>
                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;">
                                                    <strong><@ofbizCurrency amount= contractAmount isoCode=currencyUomId/></strong>
                                                </td>
                                            </tr>
                                        </table>

                                        <tr>
                                            <td>
                                                <table width="1" border="0"
                                                       cellspacing="0"
                                                       cellpadding="0">
                                                    <tbody>
                                                    <tr>
                                                        <td>
                                                            <div style="height:18px;font-size:18px;line-height:18px">
                                                                &nbsp; </div>
                                                        </td>
                                                    </tr>
                                                    </tbody>
                                                </table>
                                            </td>
                                        </tr>

                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </td>
                    </tr>
                    <tr>
                        <td>
                            <table border="0" cellspacing="0" cellpadding="0"
                                   width="100%" bgcolor="#EDF0F3" align="center"
                                   style="background-color:#edf0f3;padding:0 24px;padding-top: 20px;color:#6a6c6d;text-align:center">
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
                                                        ><img
                                                                border="0"
                                                                height="14"
                                                                src="cid:logoImageUrl"
                                                                width="58"
                                                                style="outline:none;color:#ffffff;display:block;text-decoration:none"></span>
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
                    </tbody>
                </table>
            </center>
        </td>
    </tr>
    </tbody>
</table>

<!-- end outer frame -->
</body>
</html>

