<#--
 *  Copyright (c) 2005-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a 
 *  copy of this software and associated documentation files (the "Software"), 
 *  to deal in the Software without restriction, including without limitation 
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 *  and/or sell copies of the Software, and to permit persons to whom the 
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included 
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT 
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
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
