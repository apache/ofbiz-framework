<#--
 *  Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

<#assign security = requestAttributes.security>
<#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists>
<#assign ofbizServerName = application.getAttribute("_serverId")?default("default-server")>
<#assign contextPath = request.getContextPath()>
<#assign displayApps = Static["org.ofbiz.base.component.ComponentConfig"].getAppBarWebInfos(ofbizServerName)>

<#if requestAttributes.userLogin?has_content>

<#assign unselectedClass = {"col" : "tabdownblock", "left" : "tabdownleft", "center" : "tabdowncenter", "right" : "tabdownright", "link" : "tablink"}>
<#assign selectedClass = {"col" : "mainblock", "left" : "tabupleft", "center" : "tabupcenter", "right" : "tabupright", "link" : "tablinkselected"}>

<table align="center" width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr bgcolor="#FFFFFF">
    <td><div class="appbarleft"></div></td>
    <td height="15" width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <#list displayApps as display>
            <#assign thisApp = display.getContextRoot()>
            <#assign permission = true>
            <#assign permissions = display.getBasePermission()>
            <#list permissions as perm>
                <#if (perm != "NONE" && !security.hasEntityPermission(perm, "_VIEW", session))>
                    <#assign permission = false>
                </#if>
            </#list>
            <#if permission == true>
              <#if thisApp == contextPath>
                <#assign class = selectedClass>
              <#else>
                <#assign class = unselectedClass>
              </#if>
              <td height="15" class="${class.col}">
                <table width="100%" border="0" cellspacing="0" cellpadding="0">
                  <tr>
                    <td class="${class.left}"><a href="${response.encodeURL(thisApp + "/control/main" + externalKeyParam)}" title="" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
                    <td nowrap="nowrap" class="${class.center}"><a href="${response.encodeURL(thisApp + "/control/main" + externalKeyParam)}" title="" class="${class.link}">${display.title}</a></td>
                    <td class="${class.right}"><a href="${response.encodeURL(thisApp + "/control/main" + externalKeyParam)}" title="" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
                  </tr>
                  <#if thisApp != contextPath>
                    <tr><td colspan="3" class="blackarea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>
                    <tr><td colspan="3" class="whitearea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>
                  </#if>
                </table>
              </td>
            </#if>
          </#list>
          <td><div class="appbarright"></div></td>
          <td width="100%" class="appbarresize">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">
              <tr>
                <td class="whitearea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</#if>

