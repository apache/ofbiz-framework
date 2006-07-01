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
 *@version    $Rev$
 *@since      3.0
-->

<div class="head1">${uiLabelMap.ProductFeatureGroup}</div>

<br/>
<table border="1" cellpadding='2' cellspacing='0'>
  <tr>
    <td><div class="tabletext"><b>${uiLabelMap.CommonId}</b></div></td>
    <td><div class="tabletext"><b>${uiLabelMap.CommonDescription}</b></div></td>
    <td><div class="tabletext">&nbsp;</div></td>
    <td><div class="tabletext">&nbsp;</div></td>
  </tr>

  <#list productFeatureGroups as productFeatureGroup>
    <tr valign="middle">
      <FORM method='POST' action='<@ofbizUrl>UpdateProductFeatureGroup</@ofbizUrl>'>
        <input type='hidden' name="productFeatureGroupId" value="${productFeatureGroup.productFeatureGroupId}">
        <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">${productFeatureGroup.productFeatureGroupId}</a></td>
        <td><input type='text' class='inputBox' size='30' name="description" value="${productFeatureGroup.description?if_exists}"></td>
        <td><INPUT type="submit" value="${uiLabelMap.CommonUpdate}"></td>
        <td><a href='<@ofbizUrl>EditFeatureGroupAppls?productFeatureGroupId=${productFeatureGroup.productFeatureGroupId}</@ofbizUrl>' class="buttontext">[${uiLabelMap.CommonEdit}]</a></td>
      </FORM>
    </tr>
  </#list>
</table>
<br/>

<form method="post" action="<@ofbizUrl>CreateProductFeatureGroup</@ofbizUrl>" style='margin: 0;'>
  <div class='head2'>${uiLabelMap.ProductCreateProductFeatureGroup}:</div>
  <br/>
  <table>
    <tr>
      <td><div class='tabletext'>${uiLabelMap.CommonDescription}:</div></td>
      <td><input type="text" class='inputBox' size='30' name='description' value=''></td>
    </tr>
    <tr>
      <td colspan='2'><input type="submit" value="${uiLabelMap.CommonCreate}"></td>
    </tr>
  </table>
</form>
<br/>
