<!--
  ~ /******************************************************************************************
  ~  * Copyright (c) Fidelis Sustainability Distribution, LLC 2015. - All Rights Reserved     *
  ~  * Unauthorized copying of this file, via any medium is strictly prohibited               *
  ~  * Proprietary and confidential                                                           *
  ~  * Written Mandeep Sidhu <mandeep.sidhu@fidelissd.com>, November 2018
  ~  ******************************************************************************************/
  -->

<html xmlns:collapse="http://docbook.org/ns/docbook" xmlns:er="http://www.w3.org/1999/xhtml" xmlns:6.0pt xmlns:6.0pt
      xmlns:o="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <meta property="og:title" content="Fidelis" />
    <title>Fidelis</title>
    <style>
        p img {
            width: 100%!important;
            height: 100%!important;
        }

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
                                    style="background-color:#f6f8fa;padding:5px 16px 13px;border-bottom:1px solid #ececec">
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
                                                <img style="display:block;padding-right:10px;" width="30" height="30" border="0" alt="" role="presentation" src="cid:logoImageUrl2">
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr >
                                <td bgcolor="#fff"
                                    style="background-color:#fff;padding:5px 16px 13px;border-bottom:1px solid #ececec">
                                    <table border="0" cellspacing="0" cellpadding="0"
                                           width="100%" style="width:100%!important;min-width:100%!important">
                                        <tbody>
                                        <tr>
                                            <td valign="top" align="right" width="80" rowspan="1" colspan="1" style="padding-top:10px">
                                                <div style="margin-bottom:10px;margin-right:20px">
                                                    <img style="display:block;border-radius:100%" width="60" height="60" border="0" alt="" role="presentation" src="cid:partyLogo">
                                                </div>
                                            </td>
                                            <td width="auto" align="left" valign="top" rowspan="1" colspan="1" style="padding-top:10px">
                                                <div style="font-family:Helvetica Neue,Helvetica,Arial,Verdana,sans-serif;color:#172b4d;font-size:14px;line-height:24px;text-align:left;margin-bottom:10px">
                                                    <span><a style="text-decoration:none;color:#0078d7;font-weight:bold;opacity:1!important">Dustin Lee</a> <#if isUpdated!?matches("Y")> updated<#else> added</#if> a note to a task requirement for the task <b>${taskName}</b> # ${taskId!}</span>
                                                </div>
                                                <#--<div>Task Requirement Id : ${taskRequirementId!}</div>-->
                                                <div style="font-family:Helvetica Neue,Helvetica,Arial,Verdana,sans-serif;color:#172b4d;font-size:15px;line-height:24px;text-align:left;font-weight:bold;margin-bottom:10px">
                                                    <span>
                                                        <a style="text-decoration:none;color:#172b4d;opacity:1!important">${requirementDescription!}</a>
                                                    </span>
                                                </div>
                                                <div style="color: grey;">
                                                 ${StringUtil.wrapString(noteInfo!)}
                                                </div>
                                                <div style="margin-bottom:30px">
                                                <#assign taskUrl = '${_SERVER_ROOT_URL_!}'/>
                                                    <a href="${taskUrl!}/sellercentral/c/MyTasks?taskId=${taskId!}"  style="font-size:16px;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Roboto','Noto Sans','Ubuntu','Droid Sans','Helvetica Neue',sans-serif;font-weight:none;color:#ffffff;text-decoration:none;background-color:#0078d7;border-top:11px solid #0078d7;border-bottom:11px solid #0078d7;border-left:20px solid #0078d7;border-right:20px solid #0078d7;border-radius:5px;display:inline-block">View Task</a>
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
