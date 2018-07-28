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
<script type="application/javascript" src="<@ofbizContentUrl>/common/js/plugins/imagemanagement/sizzle.min.js</@ofbizContentUrl>"></script>
<script type="application/javascript">
jQuery.noConflict();
jQuery(document).ready(function(){
    jQuery('input:radio').click(function(){
        var elementVal = jQuery(this).val();
        var elementList = elementVal.split('__');
        var result = elementList[0] + "/" + elementList[1];
        jQuery('input:radio[value=' + result + ']').attr('checked',true);
    });
    jQuery('input:radio[value^="IM_APPROVED"]').each( function() {
        this.checked = true;
    });
});
</script>
        
<table>
    <#if partyRoles?has_content>
        <#assign alt_row = false>
        <#list partyRoles as partyRole>
            <td>
                <table>
                    <#assign userLoginApprovers  = EntityQuery.use(delegator).from("UserLogin").where("partyId", partyRole.partyId!).queryList()!/>
                    <#assign userLoginApprover = userLoginApprovers[0]>
                    <#assign userLoginAndPartyDetails = delegator.findOne("UserLoginAndPartyDetails", Static["org.apache.ofbiz.base.util.UtilMisc"].toMap("partyId", userLoginApprover.partyId, "userLoginId", userLoginApprover.userLoginId), false)!>
                    <#if userLoginAndPartyDetails?has_content>
                        <#assign partyContentDetail  = EntityQuery.use(delegator).from("ContentApproval").where("roleTypeId", "IMAGEAPPROVER", "approvalStatusId", "IM_PENDING", "partyId", userLoginAndPartyDetails.partyId!).queryList()!/>
                        <#assign imageApproveSize = partyContentDetail.size()>
                        <#if userLoginAndPartyDetails.userLoginId == userLogin.userLoginId>
                            <#if userMap.checkUser == userLoginAndPartyDetails.userLoginId>
                                <td>
                                    <div>
                                        <b>${userLoginAndPartyDetails.firstName!} ${userLoginAndPartyDetails.middleName!} ${userLoginAndPartyDetails.lastName!} (${imageApproveSize})</b>&nbsp;&nbsp;|&nbsp;&nbsp;
                                    </div>
                                </td>
                            <#else>
                                <td>
                                    <div>
                                        <b><a href="<@ofbizUrl>ImageApprove</@ofbizUrl>" class="text">${userLoginAndPartyDetails.firstName!} ${userLoginAndPartyDetails.middleName!} ${userLoginAndPartyDetails.lastName!} (${imageApproveSize})</a></b>&nbsp;&nbsp;|&nbsp;&nbsp;
                                    </div>
                                </td>
                            </#if>
                        <#else>
                            <#if userMap.checkUser == userLoginAndPartyDetails.userLoginId>
                                <td>
                                    <div>
                                        <b>${userLoginAndPartyDetails.firstName!} ${userLoginAndPartyDetails.middleName!} ${userLoginAndPartyDetails.lastName!} (${imageApproveSize})</b>&nbsp;&nbsp;|&nbsp;&nbsp;
                                    </div>
                                </td>
                            <#else>
                                <td>
                                    <div>
                                        <b><a href="<@ofbizUrl>ListPeopleApproved?createdByUserLogin=${userLoginAndPartyDetails.userLoginId}</@ofbizUrl>" class="text">${userLoginAndPartyDetails.firstName!} ${userLoginAndPartyDetails.middleName!} ${userLoginAndPartyDetails.lastName!} (${imageApproveSize})</a></b>&nbsp;&nbsp;|&nbsp;&nbsp;
                                    </div>
                                </td>
                            </#if>
                        </#if>
                    </#if>
                </table>
            </td>
        </#list>
        <#if "REJECTED" == userMap.checkUser>
            <td>
                <div><b>Rejected</b></div>
            </td>
        <#else>
            <td>
                <div><b><a href="<@ofbizUrl>ListPeopleRejected</@ofbizUrl>" class="text">Rejected</a></b></div>
            </td>
        </#if>
    </#if>
</table>
<br/>
