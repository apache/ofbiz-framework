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
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>${title}</title>
    <#-- this needs to be fully qualified to appear in email; the server must also be available -->
    <link rel="stylesheet" href="${baseUrl}/images/maincss.css" type="text/css"/>
</head>
<body>

<#-- custom logo or text can be inserted here -->
<div class="head1">${title}</div>
<#if note?exists><p class="tabletext">${note}</p></#if>

<p class="tabletext">Hello ${partyName.firstName?if_exists} ${partyName.lastName?if_exists} ${partyName.groupName?if_exists}!</p>
<p class="tabletext">We have received a request for subscription to the ${contactList.contactListName} contact list.</p>
<p class="tabletext">To complete your subscription use the verify form in your <a href="${baseEcommerceSecureUrl}viewprofile">online profile</a>, or use the following link:</p>
<#if (contactListPartyStatus.optInVerifyCode)?has_content><p class="tabletext">Your verify code is: ${contactListPartyStatus.optInVerifyCode}</p></#if>

<#assign verifyUrl = baseEcommerceSecureUrl + "updateContactListParty?contactListId=" + contactListParty.contactListId + "&amp;partyId=" + contactListParty.partyId + "&amp;fromDate=" + contactListParty.fromDate/>
<#if (contactListPartyStatus.optInVerifyCode)?has_content><#assign verifyUrl = verifyUrl + "&amp;optInVerifyCode=" + contactListPartyStatus.optInVerifyCode/></#if>
<p class="tabletext"><a href="${verifyUrl}" class="linktext">${verifyUrl}</a></p>
</body>
</html>
