<!--
  ~ /******************************************************************************************
  ~  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
  ~  * Unauthorized copying of this file, via any medium is strictly prohibited               *
  ~  * Proprietary and confidential                                                           *
  ~  * Written Mandeep Sidhu <mandeep.sidhu@fidelissd.com>, January 2019
  ~  ******************************************************************************************/
  -->

<html xmlns:collapse="http://docbook.org/ns/docbook" xmlns:er="http://www.w3.org/1999/xhtml" xmlns:6.0pt xmlns:6.0pt
      xmlns:o="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <meta property="og:title" content="Fidelis" />
    <title>Fidelis</title>
    <style>
        @media only screen and (min-device-width: 375px) and (max-device-width: 667px) {
            table { max-width: 370px;}
        }

        @media only screen and (min-device-width: 414px) and (max-device-width: 736px) {
            table { max-width: 410px;}
        }
    </style>
</head>
<body style="margin: 0;">
<div style="overflow: hidden;">
    <div style="padding:0;margin:0 auto;width:100%!important;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif">
        <div style="overflow:hidden;color:transparent;width:0;font-size:0;opacity:0;height:0"></div>
        <table align="center" border="0" cellspacing="0" cellpadding="0" width="100%"
               bgcolor="#EDF0F3" style="background-color:#edf0f3;table-layout:fixed">
            <tbody>
            <tr>
                <td align="center">
                    <center style="width:100%">
                        <table border="0"
                               class="m_6878300320588779569phoenix-email-container"
                               cellspacing="0" cellpadding="0" width="512"
                               bgcolor="#FFFFFF"
                               style="background-color:#ffffff;margin:0 auto;max-width:512px;width:inherit">
                            <tbody>
                            <tr>
                                <td bgcolor="#F6F8FA"
                                    style="background-color:#f6f8fa;padding:10px 16px 13px;border-bottom:1px solid #ececec">
                                    <table border="0" cellspacing="0" cellpadding="0"
                                           width="100%"
                                           style="width:100%!important;min-width:100%!important">
                                        <tbody>
                                        <tr>
                                            <td align="left" valign="middle" style="padding-top:7px">
                                                <img src="cid:logoImageUrl3" height="30" border="0" style="outline:none;color:#ffffff;text-decoration:none;display:block"/>
                                            </td>
                                            <td valign="middle" width="100%"
                                                align="right"
                                                style="padding:0 0 0 10px;padding-top:7px">
                                                <img style="display:block;padding-right:10px;" width="30" height="30" border="0" alt="" role="presentation" src="cid:partyLogo">
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td bgcolor="#fff"
                                    style="background-color:#fff;padding:10px 16px 13px;border-bottom:1px solid #ececec">
                                    <table border="0" cellspacing="0" cellpadding="0"
                                           width="100%" style="width:100%!important;min-width:100%!important">
                                        <tbody>
                                        <tr>
                                            <td width="auto" align="left" valign="top" rowspan="1" colspan="1" style="padding-top:10px">
                                                Hello ${salesRepName!},<br/>

                                                You have been issued ${bulkQuotes?size} new quotes. Please find below the summary of quotes issued to you.
                                                <div style="padding-top: 10px;">
                                                    <#if grandTotal??>
                                                        Amount per Quote: <strong><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></strong>
                                                    </#if>
                                                </div>
                                                <div style="padding-top: 10px;">
                                                    <strong>Quote Items are listed below:</strong>
                                                    <div>
                                                        <table border="0" cellspacing="0"
                                                               cellpadding="0" width="100%"
                                                               style="border-bottom:4px solid #edf0f3">
                                                            <tbody>
                                                                <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                                                    <tr style="">
                                                                        <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>#</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>Item Description</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: center; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>Price</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: center; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>QTY</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div> Amount</div></td>
                                                                    </tr>

                                                                    <#if orderEntityItems?? && (orderEntityItems?size>0)>
                                                                        <#list orderEntityItems as orderEntityItem>
                                                                            <tr style="">
                                                                                <#assign lineItemSequenceNumber = (orderEntityItem_index + 1) />
                                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;">
                                                                                    ${lineItemSequenceNumber!}
                                                                                </td>
                                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;">${orderEntityItem.getItemsProductName()}
                                                                                    <#assign itemComment = Static["org.ofbiz.entity.util.EntityUtil"].getFirst(delegator.findByAnd("QuoteItem", {"quoteId" : orderEntityItem.getOrderEntityItem().quoteId, "quoteItemSeqId" : orderEntityItem.getOrderEntityItem().quoteItemSeqId }, null, false))/>
                                                                                    <div>${itemComment.comments!}</div>
                                                                                </td>
                                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;">
                                                                                    <@ofbizCurrency amount=orderEntityItem.getUnitPrice() isoCode=orderEntityItem.getCurrencyUomId()/>
                                                                                </td>
                                                                                <td style="border: 1px solid #dddddd;text-align: center; padding-left: 12px;padding-right: 12px;">${orderEntityItem.getQuantity()}</td>
                                                                                <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;">
                                                                                    <strong><@ofbizCurrency amount=orderEntityItem.calculateOrderEntityItemPriceSubTotal() isoCode=orderEntityItem.getCurrencyUomId()/></strong>

                                                                                </td>
                                                                            </tr>
                                                                        </#list>
                                                                    </#if>
                                                                </table>
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>

                                                <div style="padding-top: 10px;">
                                                        <strong style="color: #212121;">Please find below the list of quotes created:</strong>
                                                    <div>
                                                        <table border="0" cellspacing="0"
                                                               cellpadding="0" width="100%"
                                                               style="border-bottom:4px solid #edf0f3">
                                                            <tbody>
                                                                <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                                                    <tr style="">
                                                                        <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>#</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>Quote Id</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: center; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div>End User Customer</div></td>
                                                                        <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;background-color: #F5F7F9; font-weight: bold"><div> Amount</div></td>
                                                                    </tr>

                                                                    <#if bulkQuotes?? && (bulkQuotes?size>0)>
                                                                        <#list bulkQuotes as newQuote>
                                                                            <tr style="">
                                                                                <#assign itemIndex = newQuote_index+1>
                                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;"> ${itemIndex!}</td>
                                                                                <td style="border: 1px solid #dddddd;text-align: left; padding-left: 12px;padding-right: 12px;">${newQuote.quoteId!}</td>
                                                                                <td style="border: 1px solid #dddddd;text-align: center; padding-left: 12px;padding-right: 12px;">${newQuote.customerAccountName!}</td>
                                                                                <#if grandTotal??>
                                                                                    <td style="border: 1px solid #dddddd;text-align: right; padding-left: 12px;padding-right: 12px;"> <strong><@ofbizCurrency amount=grandTotal isoCode=currencyUomId/></strong></td>
                                                                                </#if>
                                                                            </tr>
                                                                        </#list>
                                                                    </#if>
                                                                </table>
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>
                                                <div style="padding-top: 10px;">
                                                    <table border="0" cellspacing="0"
                                                           cellpadding="0" width="100%"
                                                           style="border-bottom:4px solid #edf0f3;">
                                                        <tbody>
                                                            <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                                                <tr style="">
                                                                    <td style="padding-left: 5px;padding-right: 5px;">
                                                                        <a href="https://${serverRootUrl!}/sellercentral/c/stream?contentId=${newQuotesContentId!}" target="_blank">
                                                                            <div style="padding:10px;text-align:center;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" colspan="2">
                                                                                <input type="submit" class="btn btn-primary btn-xs fsd-font-mi" name="submit"
                                                                                       style="font-size:11px;background-color:#0078d7;border-radius:5px;-webkit-border-radius:5px;border-top-width:5px;border-top-style:solid;border-top-color:#0078d7;border-bottom-width:5px;border-bottom-style:solid;border-bottom-color:#0078d7;border-left-width:10px;border-left-style:solid;border-left-color:#0078d7;border-right-width:10px;border-right-style:solid;border-right-color:#0078d7;color:#ffffff;display:inline-block;text-decoration:none;"
                                                                                       value="Download Quote Documents in Zip File"/>
                                                                            </div>
                                                                        </a>
                                                                    </td>
                                                                    <td style="padding-left: 5px;padding-right: 5px;">
                                                                        <a href="https://${serverRootUrl!}/sellercentral/c/stream?contentId=${contentId!}" target="_blank">
                                                                            <div style="padding:10px;text-align:center;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" colspan="2">
                                                                                <input type="submit" class="btn btn-primary btn-xs fsd-font-mi" name="submit"
                                                                                       style="font-size:11px;background-color:#0078d7;border-radius:5px;-webkit-border-radius:5px;border-top-width:5px;border-top-style:solid;border-top-color:#0078d7;border-bottom-width:5px;border-bottom-style:solid;border-bottom-color:#0078d7;border-left-width:5px;border-left-style:solid;border-left-color:#0078d7;border-right-width:5px;border-right-style:solid;border-right-color:#0078d7;color:#ffffff;display:inline-block;text-decoration:none;"
                                                                                       value="Download Supporting Documents in Zip File"/>
                                                                            </div>
                                                                        </a>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                            <table style="font-family:arial, sans-serif; border-collapse: collapse;width: 100%;">
                                                                <tr style="">
                                                                    <td style="padding-left: 5px;padding-right: 5px;">
                                                                      <span class="fsd-font-mi fsd-font-color-neutralTertiary" style="padding: 13px;">
                                                                        Please Note, the zip file will be available for download for next 10 days only.
                                                                      </span>
                                                                    </td>
                                                                </tr>
                                                            </table>
                                                        </tbody>
                                                    </table>
                                                </div>
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
        <img src="https://ci5.googleusercontent.com/proxy/1RgqIG8a1cVLrmRPU3wqoEv8UUsD6UWENQzJQdhzWWG_mXmVNQHjnFrCoqXU_9zKdCjKY89V9or85YLWhbeqG-NIS1aAUf9INUhK8U5y8-DKI0w8xz_wKtk7s0vJzwuF1Yw22audylPia2XNZm5JvxpgR2_kjWyrSlzIk3xtDJ2ozg_AMb2Z69DUGPxJ_6fF=s0-d-e1-ft#https://www.linkedin.com/emimp/ip_Ylc5MWVUUXRhbTQxTkhkallqa3Raekk9OlpXMWhhV3hmZEhsd1pWOXRaWE56WVdkcGJtZGZaR2xuWlhOMDo=.gif"
             style="outline:none;color:#ffffff;text-decoration:none;width:1px;height:1px">
    </div>
</div>

<!-- end outer frame -->
</body>
</html>
