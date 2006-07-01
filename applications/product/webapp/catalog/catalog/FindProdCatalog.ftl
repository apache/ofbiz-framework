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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Catherine Heintz (catherine.heintz@nereide.biz)
 *@version    $Rev$
 *@since      2.1
-->

<div class="head1">${uiLabelMap.ProductProductCatalogsList}</div>
<div><a href="<@ofbizUrl>EditProdCatalog</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductCreateNewProdCatalog}]</a></div>
<br/>
<table border="1" cellpadding="2" cellspacing="0">
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.ProductCatalogNameId}</b></div></td>    
    <td><div class="tabletext"><b>${uiLabelMap.ProductUseQuickAdd}?</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>
<#list prodCatalogs as prodCatalog>
  <tr valign="middle">
    <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class="buttontext">${prodCatalog.catalogName} [${prodCatalog.prodCatalogId}]</a></div></td>   
    <td><div class="tabletext">&nbsp;${prodCatalog.useQuickAdd?if_exists}</div></td>
    <td>
      <a href="<@ofbizUrl>EditProdCatalog?prodCatalogId=${prodCatalog.prodCatalogId}</@ofbizUrl>" class="buttontext">
      [${uiLabelMap.CommonEdit}]</a>
    </td>
  </tr>
</#list>
</table>
<br/>
