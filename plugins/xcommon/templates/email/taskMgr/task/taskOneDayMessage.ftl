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
                                <td align="center"
                                    style="font: 25px arial, verdana, sans-serif; color: #333333; padding: 20px 15px; background-color: #fafafa;">
                                    You have been assigned a task</td>
                            </tr>
                        </table> <!-- end primary module -->
                    </td>
                </tr>
                <tr style="background-color: white">
                    <td style="padding: 20px;font-family: Helvetica, Arial, Verdana, sans-serif;color: #333333; font-size: 15px;" valign="top">
                        <h5>Please find below task details assigned to you.</h5>
                    </td>
                </tr>
                <tr style="background-color: white">
                    <td style="padding-left: 20px;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" valign="top">
                        <table width="100%" style="">
                            <tr>
                                <td align="left" style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" valign="top">
                                    Task Id :
                                </td>
                                <td style="text-align:left;padding:5px;text-align:left;display: block; font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;" >
                                    <span>${taskId!}</span>
                                </td>
                            </tr>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Task Name :
                                </td>
                                <td class="fsd-font-color-primary task-name fsd-font-m" style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif;">
                                    <strong>${taskName!}</strong>
                                </td>
                            </tr>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Task Status :
                                </td>
                                <td class="fsd-font-color-primary task-name fsd-font-m" style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif;">
                                    <span><#if taskStatus?matches("TASK_CREATED")>Created<#else>In Progress</#if></span>
                                </td>
                            </tr>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Type :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif;font-size: 12px;">
                                    <span>${taskType!}</span>
                                </td>
                            </tr>
                        <#if description??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Description :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${description!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if taskCategoryName??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Category :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${taskCategoryName!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if priorityType??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Priority Type :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${priorityType!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if dueDate??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Due Date :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${dueDate!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if timeZoneName??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Time Zone :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${timeZoneName!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if linkSupplier??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    link Supplier :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${linkSupplier!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if linkCustomer??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Link Customer :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${linkCustomer!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if createdByPartyName??>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Created By :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <span>${createdByPartyName!}</span>
                                </td>
                            </tr>
                        </#if>
                        <#if assignees?has_content>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Assigned To :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <#list assignees as assignee>
                                        <span style="background-color:#0078d7;border-radius:5px;-webkit-border-radius:5px;border-top-width:5px;border-top-style:solid;border-top-color:#0078d7;border-bottom-width:5px;border-bottom-style:solid;border-bottom-color:#0078d7;border-left-width:10px;border-left-style:solid;border-left-color:#0078d7;border-right-width:10px;border-right-style:solid;border-right-color:#0078d7;color:#ffffff;display:inline-block;text-decoration:none;margin-bottom: 10px;">${assignee.assigneePartyName!}</span>
                                    </#list>
                                </td>
                            </tr>
                        </#if>
                        <#if requirements?has_content>
                            <tr>
                                <td style="width:100px;padding: 5px 0;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;">
                                    Requirements :
                                </td>
                                <td style="padding: 5px 0;text-align:left;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 12px;">
                                    <#list requirements as requirement>
                                        <li>
                                            <span>${requirement.description}&nbsp;&nbsp;<#if requirement.statusId?matches("TASK_CREATED")><b>(Pending)</b><#else><b>(Completed)</b></#if></span>
                                        </li>
                                    </#list>
                                </td>
                            </tr>
                        </#if>
                            <!--<tr>-->
                            <!--<td style="padding:7.5pt 15.0pt 30.0pt 15.0pt;">-->
                            <!--<div align="center">-->
                            <!--<table class="3DMsoNormalTable" border="0" cellspacing="0" cellpadding="0" style="background:#3b7fc4;border-collapse:collapse;border-radius:5px;-webkit-border-radius:5px;display:inline;">-->
                            <!--<tr>-->
                            <!--<td style="padding:2.25pt 18.75pt 2.25pt 18.75pt;">-->
                            <!--<p class="3DMsoNormal" align="center" style="text-align:center;line-height:15.75pt;">-->
                            <!--<span style="font-family:Arial,sans-serif;">-->
                            <!--<a href="#"><span style="color:white;border:solid #3b7fc4 6.0pt;padding:0;">Collect all December Freebies Now</span>-->
                        <!--</a> <o:p></o:p>-->
                        <!--</span>-->
                        <!--</p>-->
                        <!--</td>-->
                        <!--</tr>-->
                        <!--</table>-->
                        <!--</div>-->
                        <!--</td>-->
                        <#--<tr>-->
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
