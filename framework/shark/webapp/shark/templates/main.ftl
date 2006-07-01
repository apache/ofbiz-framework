<!doctype HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<!-- Copyright 2004-2006 The Apache Software Foundation -->
<#--
$Id: main.ftl 7426 2006-04-26 23:35:58Z jonesde $

Copyright 2004-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#assign layoutSettings = requestAttributes.layoutSettings>
<#assign locale = Static["org.ofbiz.base.util.UtilHttp"].getLocale(session)>

<html>
<head>
    <#assign layoutSettings = requestAttributes.layoutSettings>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>${layoutSettings.companyName}: ${page.title}</title>
    <script language="javascript" src="<@ofbizContentUrl>/images/calendar1.js</@ofbizContentUrl>" type="text/javascript"></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/fieldlookup.js</@ofbizContentUrl>" type="text/javascript"></script>
    <link rel="stylesheet" href="<@ofbizContentUrl>/images/maincss.css</@ofbizContentUrl>" type="text/css">
    <link rel="stylesheet" href="<@ofbizContentUrl>/images/tabstyles.css</@ofbizContentUrl>" type="text/css">    
</head>

<body>
<table border="0" width='100%' cellspacing='0' cellpadding='0' class='headerboxoutside'>
  <tr>
    <td width='100%'>
      <table width='100%' border='0' cellspacing='0' cellpadding='0' class='headerboxtop'>
        <tr>
          <#if layoutSettings.headerImageUrl?exists>
          <td align="left" width='1%'><img alt="${layoutSettings.companyName}" src='<@ofbizContentUrl>${layoutSettings.headerImageUrl}</@ofbizContentUrl>'></td>
          </#if>
          <td align='right' width='1%' nowrap <#if layoutSettings.headerRightBackgroundUrl?has_content>background='${layoutSettings.headerRightBackgroundUrl}'</#if>>
            <#if requestAttributes.person?has_content>
              <div class="insideHeaderText">Welcome&nbsp;${requestAttributes.person.firstName?if_exists}&nbsp;${requestAttributes.person.lastName?if_exists}!</div>
            <#elseif requestAttributes.partyGroup?has_content>
              <div class="insideHeaderText">Welcome&nbsp;${requestAttributes.partyGroup.groupName?if_exists}!</div>
            <#else>
              <div class="insideHeaderText">Welcome!</div>
            </#if>
            <div class="insideHeaderText">&nbsp;${Static["org.ofbiz.base.util.UtilDateTime"].nowTimestamp().toString()}</div>
            <div class="insideHeaderText">
                <form method="post" action="<@ofbizUrl>setSessionLocale</@ofbizUrl>" style="margin: 0;">
                <select name="locale" class="selectBox">
                    <option value="${requestAttributes.locale.toString()}">${locale.getDisplayName(locale)}</option>
                    <option value="${requestAttributes.locale.toString()}">----</option>
                    <#list requestAttributes.availableLocales as availableLocale>
                        <option value="${availableLocale.toString()}">${availableLocale.getDisplayName(locale)}</option>
                    </#list>
                </select>
                <input type="submit" value="Set" class="smallSubmit"/>
                </form>
            </div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>

${pages.get("/includes/appbar.ftl")}

<div class="centerarea">
  ${pages.get("/includes/header.ftl")}
  <div class="contentarea">
    <div style="border: 0; margin: 0; padding: 0; width: 100%;">
      <table style="border: 0; margin: 0; padding: 0; width: 100%;" cellpadding="0" cellspacing="0">
        <tr>
          <#if page.leftbar?exists>${pages.get(page.leftbar)}</#if>
          <td width="100%" valign="top" align="left">
            ${common.get("/includes/messages.ftl")}
            ${pages.get(page.path)}
          </td>
          <#if page.rightbar?exists>${pages.get(page.rightbar)}</#if>
        </tr>
      </table>       
    </div>
    <div class="spacer"></div>
  </div>
</div>

${pages.get("/includes/footer.ftl")}

</body>
</html>
