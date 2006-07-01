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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
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
