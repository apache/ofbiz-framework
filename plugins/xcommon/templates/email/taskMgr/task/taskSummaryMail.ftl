
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
                                    style="background-color:#f6f8fa;padding:5px 16px 13px;border-bottom:1px solid #ececec">
                                    <table border="0" cellspacing="0" cellpadding="0"
                                           width="100%"
                                           style="width:100%!important;min-width:100%!important">
                                        <tbody>
                                        <tr>
                                            <td align="left" valign="middle" style="padding-top:7px">
                                                <img src="cid:logoImageUrl3" height="30" border="0" style="outline:none;color:#ffffff;text-decoration:none;display:block"/>
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-right: 20px;padding-left: 20px;padding-bottom: 20px;width:100%!important;min-width:100%!important">
                                        <#if quoteName??>
                                            <tr>
                                                <td style="padding-top: 10px;">
                                                    <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                        <tr>
                                                            <th align="left" style="color: grey;padding-top: 10px;width: 50%;">LINKED QUOTE</th>
                                                        </tr>
                                                        <tr>
                                                            <td>
                                                                 <span style="font-size: 11px;padding-left: 10px;">
                                                                   ${quoteName!}
                                                                 </span>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </#if>
                                        <#if opportunityName??>
                                            <tr>
                                                <td style="padding-top: 10px;">
                                                    <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                        <tr>
                                                            <th align="left" style="color: grey;padding-top: 10px;width: 50%;">LINKED OPPORTUNITY</th>
                                                        </tr>
                                                        <tr>
                                                            <td>
                                                                 <span style="font-size: 11px;padding-left: 10px;">
                                                                     ${opportunityName!}
                                                                 </span>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </#if>
                                        <#if callEventList??>
                                            <tr>
                                                <td style="padding-top: 10px;">
                                                    <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                        <tr>
                                                            <th align="left" style="color: grey;padding-top: 10px;width: 50%;">
                                                                    LINKED CALL LOG / EVENT
                                                            </th>
                                                        </tr>
                                                        <#list  callEventList as callEvent>
                                                            <tr>
                                                                <td>
                                                                    <span style="font-size: 11px;padding-left: 10px;">  ${callEvent.eventId!}-${callEvent.eventName!}</span>
                                                                </td>
                                                            </tr>
                                                        </#list>
                                                    </table>
                                                </td>
                                            </tr>
                                        </#if>

                                        <tr>
                                            <td style="padding-top: 10px;">
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                    <tr>
                                                        <th align="left" style="color: grey;padding-top: 10px;width: 50%;">TASK NAME</th>
                                                    </tr>
                                                    <tr>
                                                        <td>
                                                            <div>
                                                           <#if taskName??>${taskName!}</#if>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </table>

                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                    <tr>
                                                        <th align="left" style="color: grey;padding-top: 20px;width: 50%;">ASSIGNEES</th>
                                                        <th align="right" style="color: grey;padding-top: 20px;width: 50%;">SUPPORTING POC</th>
                                                    </tr>
                                                    <tr>
                                                        <#if taskAssignees??>
                                                            <td align="left" style="width: 50%;">
                                                                <#list taskAssignees as assignees>
                                                                    <#assign partyName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, assignees.assigneePartyId) >
                                                                    <span style="color: #3B93DD">${partyName}</span><#if assignees?has_next>,</#if>
                                                                </#list>
                                                            </td>
                                                        </#if>
                                                        <#if secondaryTaskAssignees??>
                                                            <td align="right" style="width: 50%;">
                                                                <#list secondaryTaskAssignees as assignees>
                                                                    <#assign partyName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, assignees.assigneePartyId) >
                                                                    <span style="color: #DD592B">${partyName}</span><#if assignees?has_next>,</#if>
                                                                </#list>
                                                            </td>
                                                        </#if>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                    <tr>
                                                        <td style="padding-top: 20px;">
                                                            <tr>
                                                                <th align="left" style="color: grey;">REQUIREMENTS</th>
                                                            </tr>

                                                            <#if taskRequirementList??>
                                                                <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 5px;">

                                                                    <#list taskRequirementList as requirement>
                                                                        <tr>
                                                                            <td style="padding-left: 5px;vertical-align: top;">
                                                                               <#if requirement??>${requirement?index + 1}</#if>
                                                                            </td>
                                                                            <td style="padding-left: 5px;width: 100%" align="left">
                                                                                <strong>${requirement.description!}</strong>
                                                                            </td>
                                                                        </tr>
                                                                        <#if requirement.requirementNotes??>
                                                                            <#list requirement.requirementNotes as note>
                                                                                <tr>
                                                                                    <td colspan="2" align="left">
                                                                                        <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 5px;">
                                                                                            <tr style="border: 1px solid grey; border-top: none;height: 30px;">
                                                                                                <td style="padding-left: 10px;vertical-align: top;">
                                                                                                    <img width="15px" src="https://cdn.fidelissd.com/img/icons/icon-arrow-01.png">
                                                                                                </td>
                                                                                                <td align="left" style="padding-left: 2px;width: 100%;vertical-align: top;">
                                                                                                   <span style="font-size: 12px;margin: 0">
                                                                                                       ${StringUtil.wrapString(note.noteInfo)!}
                                                                                                   </span>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                            </#list>
                                                                        </#if>
                                                                    </#list>
                                                                </table>
                                                            </#if>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                    <tr style="">
                                                        <th align="left" style="color: grey;padding-top: 20px;width: 50%;">LINKED VENDOR</th>
                                                        <th align="right" style="color: grey;padding-top: 20px;width: 50%;">LINKED CUSTOMER</th>
                                                    </tr>
                                                    <tr>
                                                        <#if taskSupplierPartyId??>
                                                            <td style="width: 50%;">
                                                                <#assign supplierName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, taskSupplierPartyId) >
                                                                <span style="color: #DD592B">
                                                                    <#if supplierName??>
                                                                         ${supplierName!}
                                                                   </#if>
                                                                </span>
                                                            </td>
                                                        <#else>
                                                            <td style="width: 50%;">
                                                                <div>No Link Supplier</div>
                                                            </td>
                                                        </#if>
                                                        <#if taskCustomerPartyId??>
                                                            <td align="right" style="width: 50%;">
                                                               <#assign customerName = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, taskCustomerPartyId) >
                                                                <span style="color: #5C2D8F"><#if customerName??>${customerName!}</#if></span>
                                                            </td>
                                                        <#else>
                                                            <td style="width: 50%;" align="right">
                                                                <div>No Link Customer</div>
                                                            </td>
                                                        </#if>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <#if taskNotesList??>
                                                    <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 15px;">
                                                        <tr>
                                                            <th style="color: grey;" align="left">
                                                               NOTES
                                                            </th>
                                                        </tr>
                                                        <tr>
                                                            <td align="left">
                                                                <ol style="padding-left: 5px;vertical-align: top;">
                                                                    <#list taskNotesList as notes>
                                                                        <li>
                                                                          ${StringUtil.wrapString(notes.noteInfo)!}
                                                                        </li>
                                                                    </#list>
                                                                </ol>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </#if>
                                            </td>
                                        </tr>

                                        <tr>
                                            <td>
                                               <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 10px;">
                                                   <tr>
                                                       <td>
                                                         <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                             <tr>
                                                                   <th style="color: grey;padding-top: 15px;" align="left">SUPPLIER POCS</th>
                                                             </tr>
                                                             <#if taskRoles??>
                                                               <#list taskRoles as role>
                                                                   <#if role.roleTypeId?? && role.roleTypeId?matches("SUPPLIER_POC")>
                                                                       <tr>
                                                                           <td style="background-color: #FAFAFA;">
                                                                               <#assign supplierPocs = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, role.partyId) >
                                                                               <#assign partyDesignation = delegator.findOne("PartyAttribute", {"partyId": role.partyId, "attrName": "Designation"}, true)?if_exists>
                                                                               <#assign partyPhoneNumber = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyLatestPhoneFormatted(delegator,  role.partyId)?if_exists >
                                                                               <#assign partyEmailAddress = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].findPartyLatestEmailAddress(role.partyId, delegator)?if_exists >
                                                                               <div style="padding-left: 10px;padding-top: 5px;padding-bottom: 5px;">
                                                                               <span style="color: #DD592B">${supplierPocs!}</span>
                                                                                   <div style="color: grey;font-size: 10px;">
                                                                                       <#if partyDesignation.attrValue?has_content>${partyDesignation.attrValue!}<#else>Designation Not Set</#if>
                                                                                       &nbsp;|&nbsp;
                                                                                       <span><#if partyPhoneNumber?has_content>${partyPhoneNumber!}<#else>No Phone Available</#if></span>
                                                                                       &nbsp;|&nbsp;
                                                                                       <span><#if partyEmailAddress?has_content>${partyEmailAddress.infoString!}<#else>No Email Available</#if></span>
                                                                                   </div>
                                                                               </div>
                                                                           </td>
                                                                       </tr>

                                                                       <#if role.noteTypeId?? && role.noteTypeId?matches("TASK_SUPP_POC_NOTE")>
                                                                           <tr>
                                                                               <td colspan="2" align="left" style="background-color: #FAFAFA;">
                                                                                   <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                                                       <tr style="border: 1px solid grey; border-top: none;height: 30px; vertical-align: top;">
                                                                                           <td style="padding-left: 10px;">
                                                                                               <img width="15px" src="https://cdn.fidelissd.com/img/icons/icon-arrow-01.png">
                                                                                           </td>
                                                                                           <td align="left" style="width: 100%;">
                                                                                               <ol style="margin: 0;padding-left: 0;">
                                                                                                   <#list role.partyNotes as pocNotes>
                                                                                                       <li >
                                                                                                           <span style="font-size: 12px;">${StringUtil.wrapString(pocNotes)!}</span>
                                                                                                       </li>
                                                                                                   </#list>
                                                                                               </ol>
                                                                                           </td>
                                                                                       </tr>
                                                                                   </table>
                                                                               </td>
                                                                           </tr>
                                                                       </#if>
                                                                   </#if>
                                                               </#list>
                                                             <#else>
                                                                 <div>No Link Supplier Poc</div>
                                                             </#if>
                                                         </table>
                                                       </td>
                                                   </tr>
                                                   <tr>
                                                       <td>
                                                            <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                                <tr>
                                                                    <th style="color: grey;padding-top: 10px;" align="left">COMPANY POCS</th>
                                                                </tr>
                                                                <#if taskRoles??>
                                                                    <#list taskRoles as role>
                                                                        <#if role.roleTypeId?? && role.roleTypeId?matches("ORDER_CLERK")>
                                                                            <tr>
                                                                                <td style="background-color: #FAFAFA;">
                                                                                    <#assign companyPocs = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, role.partyId) >
                                                                                    <#assign partyDesignation = delegator.findOne("PartyAttribute", {"partyId": role.partyId, "attrName": "Designation"}, true)?if_exists>
                                                                                    <#assign partyPhoneNumber = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyLatestPhoneFormatted(delegator,  role.partyId)?if_exists >
                                                                                    <#assign partyEmailAddress = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].findPartyLatestEmailAddress(role.partyId, delegator)?if_exists >
                                                                                    <div style="padding-left: 10px;padding-top: 5px;padding-bottom: 5px;">
                                                                                        <span style="color: #603CB8"> ${companyPocs!}</span>
                                                                                        <div style="color: grey;font-size: 10px;">
                                                                                            <#if partyDesignation.attrValue?has_content>${partyDesignation.attrValue!}<#else>Designation Not Set</#if>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyPhoneNumber?has_content>${partyPhoneNumber!}<#else>No Phone Available</#if></span>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyEmailAddress?has_content>${partyEmailAddress.infoString!}<#else>No Email Available</#if></span>
                                                                                        </div>
                                                                                    </div>

                                                                                </td>
                                                                            </tr>
                                                                        </#if>
                                                                    </#list>
                                                                <#else>
                                                                    <div>No Link Company Poc</div>
                                                                </#if>
                                                            </table>
                                                       </td>
                                                   </tr>
                                                   <tr>
                                                       <td>
                                                            <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                                <tr>
                                                                    <th style="color: grey;padding-top: 10px;" align="left">PERSONAL POCS</th>
                                                                </tr>
                                                                <#if taskRoles??>
                                                                    <#list taskRoles as role>
                                                                        <#if role.roleTypeId?? && role.roleTypeId?matches("PERSONAL_POC")>
                                                                            <tr>
                                                                                <td style="background-color: #FAFAFA;">
                                                                                    <#assign personalPocs = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, role.partyId) >
                                                                                    <#assign partyDesignation = delegator.findOne("PartyAttribute", {"partyId": role.partyId, "attrName": "Designation"}, true)?if_exists>
                                                                                    <#assign partyPhoneNumber = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyLatestPhoneFormatted(delegator,  role.partyId)?if_exists >
                                                                                    <#assign partyEmailAddress = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].findPartyLatestEmailAddress(role.partyId, delegator)?if_exists >
                                                                                    <div style="padding-left: 10px;padding-top: 5px;padding-bottom: 5px;">
                                                                                        <span style="color: #603CB8">${personalPocs!}</span>
                                                                                        <div style="color: grey;font-size: 10px;">
                                                                                            <#if partyDesignation.attrValue?has_content>${partyDesignation.attrValue!}<#else>Designation Not Set</#if>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyPhoneNumber?? && partyPhoneNumber?has_content>${partyPhoneNumber!}<#else>No Phone Available </#if></span>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyEmailAddress?has_content>${partyEmailAddress.infoString!}<#else>No Email Available </#if></span>
                                                                                        </div>
                                                                                    </div>
                                                                                </td>
                                                                            </tr>
                                                                        </#if>
                                                                    </#list>
                                                                <#else>
                                                                    <div>No Link Personal Poc</div>
                                                                </#if>
                                                            </table>
                                                       </td>
                                                   </tr>
                                                   <tr>
                                                       <td>
                                                            <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                                <tr>
                                                                    <th style="color: grey;padding-top: 10px;" align="left">CUSTOMER POCS</th>
                                                                </tr>
                                                                <#if taskRoles??>
                                                                    <#list taskRoles as role>
                                                                        <#if role.roleTypeId?? && role.roleTypeId?matches("CONTRACTING_OFFICER")>
                                                                            <tr>
                                                                                <td style="background-color: #FAFAFA;">
                                                                                    <#assign customerPocs = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyName(delegator, role.partyId) >
                                                                                    <#assign partyDesignation = delegator.findOne("PartyAttribute", {"partyId": role.partyId, "attrName": "Designation"}, true)?if_exists>
                                                                                    <#assign partyPhoneNumber = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].getPartyLatestPhoneFormatted(delegator,  role.partyId)?if_exists >
                                                                                    <#assign partyEmailAddress = Static["com.fidelissd.fsdParty.party.FsdPartyHelper"].findPartyLatestEmailAddress(role.partyId, delegator)?if_exists >
                                                                                    <div style="padding-left: 10px;padding-top: 5px;padding-bottom: 5px;">
                                                                                        <span style="color: #603CB8"> ${customerPocs!}</span>
                                                                                        <div style="color: grey;font-size: 10px;">
                                                                                            <#if partyDesignation.attrValue?has_content>${partyDesignation.attrValue!}<#else>Designation Not Set</#if>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyPhoneNumber?has_content>${partyPhoneNumber!}<#else>No Phone Available </#if></span>
                                                                                            &nbsp;|&nbsp;
                                                                                            <span><#if partyEmailAddress?has_content>${partyEmailAddress.infoString!}<#else>No Email Available </#if></span>
                                                                                        </div>
                                                                                    </div>
                                                                                </td>
                                                                            </tr>

                                                                            <#if role.noteTypeId?? && role.noteTypeId?matches("TASK_CUST_POC_NOTE")>
                                                                                <tr>
                                                                                    <td colspan="2" align="left" style="background-color: #FAFAFA;">
                                                                                        <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                                                            <tr style="border: 1px solid grey; border-top: none;height: 30px; vertical-align: top;">
                                                                                                <td style="padding-left: 10px;">
                                                                                                    <img width="15px" src="https://cdn.fidelissd.com/img/icons/icon-arrow-01.png">
                                                                                                </td>
                                                                                                <td align="left" style="width: 100%;">
                                                                                                    <ol style="margin: 0;padding-left: 0;">
                                                                                                        <#list role.partyNotes as pocNotes>
                                                                                                            <li >
                                                                                                                <span style="font-size: 12px;">${StringUtil.wrapString(pocNotes)!}</span>
                                                                                                            </li>
                                                                                                        </#list>
                                                                                                    </ol>
                                                                                                </td>
                                                                                            </tr>
                                                                                        </table>
                                                                                    </td>
                                                                                </tr>
                                                                            </#if>
                                                                        </#if>
                                                                    </#list>
                                                                <#else>
                                                                    <div>No Link Customer Poc</div>
                                                                </#if>
                                                            </table>
                                                       </td>
                                                   </tr>
                                               </table>
                                            </td>
                                        </tr>

                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                  <#if taskLink??>
                                                      <tr>
                                                         <th style="padding-top: 20px;color: grey;" align="left">LINKS</th>
                                                      </tr>
                                                      <tr>
                                                          <td>
                                                              <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 10px;">
                                                                  <#list taskLink as link>
                                                                      <tr>
                                                                          <td style="padding-left: 5px;vertical-align: top;"  bgcolor="#FAFAFA">
                                                                              <#if link??>${link?index + 1}</#if>)
                                                                          </td>&nbsp;
                                                                          <td align="left"  bgcolor="#FAFAFA" style="width: 100%;">
                                                                              <#assign taskLinkDescription = delegator.findOne("Enumeration", {"enumId": link.taskLinkTypeId}, true)?if_exists>
                                                                              <div> ${link.taskLinkUrl!} <span  align="right" style="color: grey;font-size: 10px;">(${taskLinkDescription.description!})</span></div>
                                                                              <div align="left">${link.taskLinkDescription!}</div>
                                                                          </td>
                                                                      </tr>
                                                                  </#list>
                                                              </table>
                                                          </td>
                                                      </tr>
                                                  </#if>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%">
                                                    <#if taskTag??>
                                                        <tr>
                                                            <th style="padding-top: 20px;color: grey;" align="left">TAGS</th>
                                                        </tr>
                                                        <tr>
                                                            <td  align="left" style="padding-top: 10px;padding-left: 10px;" bgcolor="#FAFAFA">
                                                                <#list taskTag as tag>
                                                                    <div style="background-color: ${tag.tagColorCode!};color:white;margin-bottom: 1%;padding: 4px 4px 4px 4px;
                                                                            font-size: 10px;font-weight: normal;margin-left: 1em;width: fit-content;
                                                                            font-family: 'Segoe UI Semibold WestEuropean', 'Segoe UI Semibold', 'Segoe UI', Tahoma, Arial, sans-serif;">
                                                                     ${tag.tagName!}
                                                                    </div>
                                                                </#list>
                                                            </td>
                                                        </tr>
                                                    </#if>
                                                </table>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td>
                                                <table border="0" cellspacing="0" cellpadding="0" width="100%" style="padding-top: 15px;">
                                                    <tr>
                                                    <#assign taskUrl = '${_SERVER_ROOT_URL_!}'/>
                                                        <td style="text-align:center;font-family: Helvetica, Arial, Verdana, sans-serif; font-size: 15px;background-color: aliceblue;padding: 10px 0 10px 0;" colspan="2">
                                                            <a href="${taskUrl!}/sellercentral/c/MyTasks?taskId=${taskId!}" style="background-color:#0078d7;border-radius:5px;-webkit-border-radius:5px;border-top-width:13px;border-top-style:solid;border-top-color:#0078d7;border-bottom-width:14px;border-bottom-style:solid;border-bottom-color:#0078d7;border-left-width:35px;border-left-style:solid;border-left-color:#0078d7;border-right-width:35px;border-right-style:solid;border-right-color:#0078d7;color:#ffffff;display:inline-block;text-decoration:none;">View Task</a>
                                                        </td>
                                                    </tr>
                                                </table>
                                            </td>
                                        </tr>
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
</body>
</html>
