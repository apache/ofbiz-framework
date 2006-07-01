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
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@author     Catherine.Heintz@nereide.biz (migration to UiLabel)
 *@version    $Rev$
 *@since      2.2
-->
  <div class="head1">${uiLabelMap.ProductProductStoreList}</div>
  <div><a href="<@ofbizUrl>EditProductStore</@ofbizUrl>" class="buttontext">[${uiLabelMap.ProductCreateNewProductStore}]</a></div>
  <br/>
  <table border="1" cellpadding="2" cellspacing="0">
    <tr>
      <td><div class="tabletext"><b>${uiLabelMap.ProductStoreNameId}</b></div></td>
      <td><div class="tabletext"><b>${uiLabelMap.ProductTitle}</b></div></td>
      <td><div class="tabletext"><b>${uiLabelMap.ProductSubTitle}</b></div></td>
      <td><div class="tabletext">&nbsp;</div></td>
    </tr>
    <#list productStores as productStore>
      <tr valign="middle">
        <td><div class="tabletext">&nbsp;<a href="<@ofbizUrl>EditProductStore?productStoreId=${productStore.productStoreId}</@ofbizUrl>" class="buttontext">${productStore.storeName?if_exists} [${productStore.productStoreId}]</a></div></td>
        <td><div class="tabletext">&nbsp;${productStore.title?if_exists}</div></td>
        <td><div class="tabletext">&nbsp;${productStore.subtitle?if_exists}</div></td>
        <td>
          <a href="<@ofbizUrl>EditProductStore?productStoreId=${productStore.productStoreId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
        </td>
      </tr>
    </#list>
  </table>
