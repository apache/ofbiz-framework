<#--
 *  Copyright (c) 2003-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Al Byers (byersa@automationgroups.com)
 *@version    $Revision: 5462 $
 *@since      3.1
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
