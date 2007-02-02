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

<#if (requestAttributes.externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#if (externalLoginKey)?exists><#assign externalKeyParam = "?externalLoginKey=" + requestAttributes.externalLoginKey?if_exists></#if>
<#assign ofbizServerName = application.getAttribute("_serverId")?default("default-server")>
<#assign contextPath = request.getContextPath()>

<#if userLogin?has_content>
<#assign unselectedClass = {"col" : "tabdownblock", "left" : "tabdownleft", "center" : "tabdowncenter", "right" : "tabdownright", "link" : "tablink"}>
<#assign selectedClass = {"col" : "mainblock", "left" : "tabupleft", "center" : "tabupcenter", "right" : "tabupright", "link" : "tablinkselected"}>
<#--Just for now Later fix -->
<#assign class = selectedClass>

<table align="center" width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr bgcolor="#FFFFFF">
    <td><div class="appbarleft"></div></td>
    <td height="15" width="100%">
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr>
          <#if appbarItem?if_exists != "fixedAssets"><#assign class = unselectedClass><#else><#assign class = selectedClass></#if>
          <td height="15" class="${class.col}">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">                
              <tr>
                <td class="${class.left}"><a href="<@ofbizUrl>/findFixedAssets</@ofbizUrl>"  title="${uiLabelMap.AccountingFixedAssets}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
                <td nowrap="nowrap" class="${class.center}"><a href="<@ofbizUrl>/findFixedAssets</@ofbizUrl>" class="${class.link}">${uiLabelMap.AccountingFixedAssets}</a></td>
                <td class="${class.right}"><a href="<@ofbizUrl>/findFixedAssets</@ofbizUrl>" title="${uiLabelMap.AccountingFixedAssets}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
              </tr>
              <#if appbarItem?if_exists != "fixedAssets">            
              <tr><td colspan="3" class="blackarea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>
              <tr><td colspan="3" class="whitearea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>               
              </#if>
            </table>
          </td>
          <#if appbarItem?if_exists != "fixedAssetMaints"><#assign class = unselectedClass><#else><#assign class = selectedClass></#if>
          <td height="15" class="${class.col}">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">                
              <tr>
                <td class="${class.left}"><a href="<@ofbizUrl>/findFixedAssetMaints</@ofbizUrl>"  title="${uiLabelMap.AccountingFixedAssetMaints}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
                <td nowrap="nowrap" class="${class.center}"><a href="<@ofbizUrl>/findFixedAssetMaints</@ofbizUrl>" class="${class.link}">${uiLabelMap.AccountingFixedAssetMaints}</a></td>
                <td class="${class.right}"><a href="<@ofbizUrl>/findFixedAssetMaints</@ofbizUrl>" title="${uiLabelMap.AccountingFixedAssetMaints}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
              </tr>
              <#if appbarItem?if_exists != "fixedAssetMaints">            
              <tr><td colspan="3" class="blackarea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>
              <tr><td colspan="3" class="whitearea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>               
              </#if>
            </table>
          </td>
          <#if appbarItem?if_exists != "facility"><#assign class = unselectedClass><#else><#assign class = selectedClass></#if>
          <td height="15" class="${class.col}">
            <table width="100%" border="0" cellspacing="0" cellpadding="0">                
              <tr>
                <td class="${class.left}"><a href="<@ofbizUrl>/FindFacility?facilityTypeId=WAREHOUSE</@ofbizUrl>"  title="${uiLabelMap.ProductWarehouse}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
                <td nowrap="nowrap" class="${class.center}"><a href="<@ofbizUrl>/FindFacility?facilityTypeId=WAREHOUSE</@ofbizUrl>" class="${class.link}">${uiLabelMap.ProductWarehouse}</a></td>
                <td class="${class.right}"><a href="<@ofbizUrl>/FindFacility?facilityTypeId=WAREHOUSE</@ofbizUrl>" title="${uiLabelMap.ProductWarehouse}" class="${class.link}"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" width="10" height="15" border="0"></a></td>
              </tr>
              <#if appbarItem?if_exists != "facility">            
              <tr><td colspan="3" class="blackarea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>
              <tr><td colspan="3" class="whitearea"><img src="<@ofbizContentUrl>/images/spacer.gif</@ofbizContentUrl>" alt="" height="1"></td></tr>               
              </#if>
            </table>
          </td>
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
