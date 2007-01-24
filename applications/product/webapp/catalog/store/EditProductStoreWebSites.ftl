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
<table border="1" cellpadding="2" cellspacing="0">
    <tr>
      <td><span class="tableheadtext">${uiLabelMap.ProductWebSiteId}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductHost}</span></td>
      <td><span class="tableheadtext">${uiLabelMap.ProductPort}</span></td>
      <td>&nbsp;</td>
    </tr>
    <#if storeWebSites?has_content>
      <#list storeWebSites as webSite>
        <tr> 
          <td><a href="/content/control/EditWebSite?webSiteId=${webSite.webSiteId}&externalLoginKey=${requestAttributes.externalLoginKey}" class="buttontext">${webSite.siteName} [${webSite.webSiteId}]</a></td>
          <td><span class="tabletext">${webSite.httpHost?default('&nbsp;')}</span></td>
          <td><span class="tabletext">${webSite.httpPort?default('&nbsp;')}</span></td>
          <td align="center">
            <a href="<@ofbizUrl>storeUpdateWebSite?viewProductStoreId=${productStoreId}&productStoreId=&webSiteId=${webSite.webSiteId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a>
          </td>
        </tr>
      </#list>
    </#if>
</table>
  
  <br/>
<div class="head2">${uiLabelMap.ProductSetStoreOnWebSite}:</div>
<form name="addWebSite" action="<@ofbizUrl>storeUpdateWebSite</@ofbizUrl>" method="post">
    <input type="hidden" name="viewProductStoreId" value="${productStoreId}">
    <input type="hidden" name="productStoreId" value="${productStoreId}">
    <select class="selectBox" name="webSiteId">
      <#list webSites as webSite>
        <option value="${webSite.webSiteId}">${webSite.siteName} [${webSite.webSiteId}]</option>
      </#list>
    </select>
    <input type="submit" class="smallSubmit" value="${uiLabelMap.CommonUpdate}">
</form>
