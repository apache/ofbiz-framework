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

<html>
<head>
    <title>Automation Groups - Main</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <script language='javascript' src='<@ofbizContentUrl>/images/calendar1.js</@ofbizContentUrl>' type='text/javascript'></script>
    <script language='javascript' src='<@ofbizContentUrl>/images/fieldlookup.js</@ofbizContentUrl>' type='text/javascript'></script>
    <script language='javascript' src='<@ofbizContentUrl>/images/selectall.js</@ofbizContentUrl>' type='text/javascript'></script>
    <script language="javascript" src="<@ofbizContentUrl>/images/fieldlookup.js</@ofbizContentUrl>" type="text/javascript"></script>
    <link rel='stylesheet' href='<@ofbizContentUrl>/images/maincss.css</@ofbizContentUrl>' type='text/css'>
    <link rel='stylesheet' href='<@ofbizContentUrl>/images/tabstyles.css</@ofbizContentUrl>' type='text/css'>
    <link rel='stylesheet' href='<@ofbizContentUrl>/ecommerce/images/blog.css</@ofbizContentUrl>' type='text/css'>
    <#if layoutSettings.styleSheets?has_content>
        <#--layoutSettings.styleSheets is a list of style sheets. So, you can have a user-specified "main" style sheet, AND a component style sheet.-->
        <#list layoutSettings.styleSheets as styleSheet>
            <link rel="stylesheet" href="<@ofbizContentUrl>${styleSheet}</@ofbizContentUrl>" type="text/css"/>
        </#list>
    <#else>
        <link rel="stylesheet" href="<@ofbizContentUrl>/images/maincss.css</@ofbizContentUrl>" type="text/css"/>
        <link rel="stylesheet" href="<@ofbizContentUrl>/images/tabstyles.css</@ofbizContentUrl>" type="text/css"/>
    </#if>
</head>
<body>
<table border="0" width="100%" cellspacing="0" cellpadding="0" class="headerboxoutside">
  <tr>
    <td width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0" class="headerboxtop">
        <tr>
          <#if layoutSettings.headerImageUrl?exists>
          <td align="left" width="1%"><img alt="${layoutSettings.companyName}" src="<@ofbizContentUrl>${layoutSettings.headerImageUrl}</@ofbizContentUrl>"/></td>
          </#if>   
          <td align="right" width="1%" nowrap="nowrap" <#if layoutSettings.headerRightBackgroundUrl?has_content>background="${layoutSettings.headerRightBackgroundUrl}"</#if>>
            <div class="insideHeaderText">
                <#if userLogin?has_content>
                  Logged in as&nbsp;<a href="#" class="linktext">${userLogin.userLoginId}</a>&nbsp;|&nbsp;<a href="<@ofbizUrl>/logoff</@ofbizUrl>" class="linktext">Logout</a>&nbsp;|&nbsp;<a href="#" class="linktext">Help Center</a>
                <#else>
                  Sign up <a href="#" class="linktext">Now!</a>&nbsp;|&nbsp;<a href="#" class="linktext">Help Center</a>
                </#if>
            </div>
            <div style="padding-top: 10px;" class="insideHeaderText">
                <form action="#">
                    <input type="text" class="inputBox" name="search" size="20">
                    <input type="submit" class="smallSubmit" value="Search">
                </form>
            </div>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
            ${sections.render("header")}
    <div class="centerarea">
    <!--
        <div class="toparea">
            ${sections.render("top")}
        </div>
        -->
        <div class="contentarea">
            <!-- by default will render left-bar only if leftbarScreen value not empty -->
            ${sections.render("leftbar")}
            <div class="columncenter">
              ${sections.render("messages")}
              ${sections.render("body")}
            </div>
            ${sections.render("rightbar")}
        </div>
    </div>
</body>
</html>
