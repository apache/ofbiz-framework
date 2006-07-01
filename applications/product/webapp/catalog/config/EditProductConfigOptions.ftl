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
 *@author     Andy Zeneski (jaz@ofbiz.org)
 *@author     David E. Jones (jonesde@ofbiz.org)
 *@version    $Rev$
 *@since      3.0
-->

  <table border="1" cellpadding='2' cellspacing='0'>
    <tr>
      <td><div class="tableheadtext">${uiLabelMap.CommonName}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
      <td><div class="tableheadtext">${uiLabelMap.CommonDescription}</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
      <td><div class="tableheadtext">&nbsp;</div></td>
    </tr>
    <#list configOptionList as question>
      <form method="post" action="<@ofbizUrl>updateProductConfigOption</@ofbizUrl>">
        <input type="hidden" name="configItemId" value="${question.configItemId}">
        <input type="hidden" name="configOptionId" value="${question.configOptionId}">
        <tr valign="middle">
          <td><div class="tabletext">${question.configOptionId} - ${question.configOptionName?if_exists}</div></td>
          <td><input type="text" name="sequenceNum" size="3" class="textBox" value="${question.sequenceNum?if_exists}">
          <td><div class="tabletext">${question.description?if_exists}</div></td>
          <td><input type="submit" value="${uiLabelMap.CommonUpdate}">
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${question.configOptionId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          <td><a href="<@ofbizUrl>deleteProductConfigOption?configItemId=${question.configItemId}&configOptionId=${question.configOptionId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
        </tr>
      </form>
    </#list>
  </table>
  <br/>

  <hr class="sepbar">
  <a name="edit"/>
  <#-- new question / category -->

    <#if configOptionId?has_content>
      <div class="head2">${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigOptions}:</div>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigOptions}]</a>
    <#else>
      <div class="head2">${uiLabelMap.CommonCreateNew} ${uiLabelMap.ProductConfigOptions}</div>
    </#if>
    ${sections.render("CreateConfigOptionForm")}

  <#if (configOption?has_content)>
    <br/>
    <hr class="sepbar">
    <div class="head1">${uiLabelMap.ProductComponents} - <span class="head2">${uiLabelMap.CommonId}: ${configOption.configOptionId?if_exists} - ${configOption.description?if_exists}</div>
    <table border="1" cellpadding='2' cellspacing='0'>
      <tr>
        <td><div class="tableheadtext">${uiLabelMap.CommonSequenceNum}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.ProductProduct}</div></td>
        <td><div class="tableheadtext">${uiLabelMap.ProductQuantity}</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
        <td><div class="tableheadtext">&nbsp;</div></td>
      </tr>

      <#list configProducts as component>
        <#assign product = component.getRelatedOne("ProductProduct")>
        <tr valign="middle">
          <td><div class="tabletext">${component.sequenceNum?if_exists}</div></td>
          <td><div class="tabletext">${component.productId?if_exists} - ${product.internalName?if_exists}</div></td>
          <td><div class="tabletext">${component.quantity?if_exists}</div></td>
          <td><a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a>
          <td><a href="<@ofbizUrl>deleteProductConfigProduct?configItemId=${requestParameters.configItemId}&configOptionId=${component.configOptionId}&productId=${component.productId}#edit</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonRemove}]</a>
        </tr>
      </#list>
    </table>

    <#if !productConfigProduct?has_content>
      <div class="head2">${uiLabelMap.CommonAddA} ${uiLabelMap.ProductConfigs}:</div>
    <#else>
      <div class="head2">${uiLabelMap.CommonEdit} ${uiLabelMap.ProductConfigs}:</div>
      <a href="<@ofbizUrl>EditProductConfigOptions?configItemId=${requestParameters.configItemId}&configOptionId=${productConfigProduct.configOptionId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonNew} ${uiLabelMap.ProductConfigs}]</a>
    </#if>
    ${sections.render("CreateConfigProductForm")}
  </#if>
