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
                                <td align="left"
                                    style="font: 9px arial, helvetica, sans-serif;"><img
                                        src="<@ofbizContentUrl>/ext/sellercentral/img/2462015_logo_151x54.png</@ofbizContentUrl>"
                                        alt="Fidelis Sustainability Distribution" width="151" height="54"/></td>
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
                                    <table border="0" cellpadding="0" cellspacing="0" width="90%">
                                        <tr>
                                        <#assign partyName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, assignee)>
                                            <td align="center"
                                                style="font: 20px arial, verdana, sans-serif; color: #333333; padding: 20px 15px; background-color: #fafafa;">
                                                <label style="color:#3b73af;">${partyName!}</label> <strong>updated Requirement</strong>&nbsp;<label style="color:#3b73af;">&nbsp;${taskRequirementId!}</label></td>
                                        </tr>
                                    </table> <!-- end primary module -->
                                </td>
                            </tr>
                            <tr style="background-color: white">
                                <td style="padding-left: 20px;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" valign="top">
                                    <table width="100%" style="">
                                        <tr>
                                            <td align="left" style="width:30%;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" valign="top">
                                                Task Id :
                                            </td>
                                            <td style="text-align:left;padding:5px;text-align:left;display: block; font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" >
                                                <span>${taskId!}</span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td align="left" style="width:30%;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" valign="top">
                                                Task Name :
                                            </td>
                                            <td style="text-align:left;padding:5px;text-align:left;display: block; font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" >
                                                <span>${taskName!}</span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="width:30%;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                                Requirement Name :
                                            </td>
                                            <td class="fsd-font-color-primary task-name fsd-font-m" style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif;">
                                                <strong>${description!}</strong>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="width:30%;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                                Requirement Id :
                                            </td>
                                            <td class="fsd-font-color-primary task-name fsd-font-m" style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif;">
                                                <strong>${taskRequirementId!}</strong>
                                            </td>
                                        </tr>
                                        <br>
                                        <tr>
                                        <#assign taskUrl = '${_SERVER_ROOT_URL_!}'/>
                                            <td style="padding:5px;text-align:center;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" colspan="2">
                                                <a href="${taskUrl!}/sellercentral/c/MyTasks?taskId=${taskId!}" style="background-color:#0078d7;border-radius:5px;-webkit-border-radius:5px;border-top-width:13px;border-top-style:solid;border-top-color:#0078d7;border-bottom-width:14px;border-bottom-style:solid;border-bottom-color:#0078d7;border-left-width:35px;border-left-style:solid;border-left-color:#0078d7;border-right-width:35px;border-right-style:solid;border-right-color:#0078d7;color:#ffffff;display:inline-block;text-decoration:none;">Open Task</a>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                            <!-- shipment module wrapper -->
                        </table> <!-- primary module wrapper -->
                    </td>
                </tr>
                <tr>
                    <td align="left"
                        style="background: #ffffff; border-left: 1px solid #dadbdd; border-right: 1px solid #dadbdd; border-bottom: 1px solid #dadbdd; padding: 20px 0px 0px 15px;">

                        <!-- secondary module 1 -->
                        <table border="0" cellpadding="0" cellspacing="0" width="100%">
                            <tr>
                                <td align="center">
                                    <table border="0" cellpadding="0" cellspacing="0"
                                           style="font: 12px arial, verdana, sans-serif; line-height: 20px; padding-right: 20px;"
                                           width="100%">

                                        <thead>

                                        </thead>

                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td align="left"
                                    style="font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;line-height: 24px; padding: 15px 0 0 0;">Thanks,</td>
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
                            <tr>
                                <!--legal section -->
                                <td align="left"
                                    style="font: 11px arial, helvetica, sans-serif; color: #b9b9b9; padding: 0px 5px 0px 2px; line-height: 20px;">
                                    <br />
                                    Copyright &copy; 2013-${nowTimestamp?string("yyyy")} Fidelis Sustainability Distribution.<br /> <br />
                                </td>
                                <!-- end Legal -->
                            </tr>
                        </table> <!-- end secondary module -->
                    </td>
                </tr>
            </table> <!-- end Main body table -->
        </td>
    </tr>
</table>
<!-- end outer frame -->
</body>
</html>
