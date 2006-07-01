<#--
 *  Copyright (c) 2004-2005 The Open For Business Project - www.ofbiz.org
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
 *@author     Jacopo Cappellato
 *@version    $Rev$
 *@since      3.2
-->

<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {page.tabButtonItem?default("void") : "tabButtonSelected"}>

<#if configItemId?has_content>
  <div class='tabContainer'>
    <a href="<@ofbizUrl>EditProductConfigItem?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigItem?default(unselectedClassName)}">${uiLabelMap.ProductConfigItem}</a>
    <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigOptions?default(unselectedClassName)}">${uiLabelMap.ProductConfigOptions}</a>
    <a href="<@ofbizUrl>EditProductConfigItemContent?configItemId=${configItemId}</@ofbizUrl>" class="${selectedClassMap.EditProductConfigItemContent?default(unselectedClassName)}">${uiLabelMap.ProductContent}</a>
  </div>
</#if>
