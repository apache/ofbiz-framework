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
      <td nowrap><div class="tableheadtext">${uiLabelMap.AccountingPaymentMethodType}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductServiceType}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.ProductServiceName}</div></td>
      <td nowrap><div class="tableheadtext">${uiLabelMap.AccountingPaymentProps}</div></td>         
      <td nowrap><div class="tableheadtext">${uiLabelMap.ApplyToAll}</div></td>         
      <td nowrap><div class="tabletext">&nbsp;</div></td>
    </tr>
    <#list paymentSettings as setting>
      <#assign payMeth = setting.getRelatedOne("PaymentMethodType")>
      <#assign enum = setting.getRelatedOne("Enumeration")>      
      <tr>                  
        <td><div class="tabletext">${payMeth.get("description",locale)}</div></td>
        <td><div class="tabletext">${enum.get("description",locale)}</div></td>
        <td><div class="tabletext">${setting.paymentService?default("${uiLabelMap.CommonNA}")}</div></td>
        <td><div class="tabletext">${setting.paymentPropertiesPath?default("[${uiLabelMap.ProductGlobal}]")}</div></td>
        <td><div class="tabletext">${setting.applyToAllProducts?if_exists}</div></td>
        <td align="center" nowrap>
          <div class="tabletext"><#if security.hasEntityPermission("CATALOG", "_DELETE", session)><a href="<@ofbizUrl>storeRemovePaySetting?productStoreId=${productStoreId}&amp;paymentMethodTypeId=${setting.paymentMethodTypeId}&amp;paymentServiceTypeEnumId=${setting.paymentServiceTypeEnumId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonDelete}]</a></#if> <a href="<@ofbizUrl>EditProductStorePaySetup?productStoreId=${productStoreId}&amp;paymentMethodTypeId=${setting.paymentMethodTypeId}&amp;paymentServiceTypeEnumId=${setting.paymentServiceTypeEnumId}</@ofbizUrl>" class="buttontext">[${uiLabelMap.CommonEdit}]</a></div>
        </td>        
      </tr>
    </#list>
</table>
  
<br/>
    <#if security.hasEntityPermission("CATALOG", "_CREATE", session)>
      <#if editSetting?has_content>
        <#assign requestName = "/storeUpdatePaySetting">
        <#assign buttonText = "${uiLabelMap.CommonUpdate}">
      <#else>
        <#assign requestName = "/storeCreatePaySetting">
        <#assign buttonText = "${uiLabelMap.CommonCreate}">
      </#if>
      <form method="get" name="addrate" action="<@ofbizUrl>${requestName}</@ofbizUrl>">
      <input type="hidden" name="productStoreId" value="${productStoreId}"/>
      <table>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.AccountingPaymentMethodType}</span></td>
          <td>
            <select name="paymentMethodTypeId" class="selectBox"> 
              <#if editSetting?has_content>
                <#assign paymentMethodType = editSetting.getRelatedOne("PaymentMethodType")>
                <option value="${editSetting.paymentMethodTypeId}">${paymentMethodType.get("description",locale)}</option>
                <option value="${editSetting.paymentMethodTypeId}">---</option>
              </#if>
              <#list paymentMethodTypes as paymentMethodType>
                <option value="${paymentMethodType.paymentMethodTypeId}">${paymentMethodType.get("description",locale)}</option>
              </#list>
            </select>
          </td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductServiceType}</span></td>
          <td>
            <select name="paymentServiceTypeEnumId" class="selectBox"> 
              <#if editSetting?has_content>
                <#assign enum = editSetting.getRelatedOne("Enumeration")>
                <option value="${editSetting.paymentServiceTypeEnumId}">${enum.get("description",locale)}</option>
                <option value="${editSetting.paymentServiceTypeEnumId}">---</option>
              </#if>          
              <#list serviceTypes as type>
                <option value="${type.enumId}">${type.get("description",locale)}</option>
              </#list>
            </select>
          </td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ProductServiceName}</span></td>      
          <td><input type="text" size="30" name="paymentService" class="inputBox" value="${(editSetting.paymentService)?if_exists}"/></td>
        </tr>
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.AccountingPaymentProperties}</span></td>
          <td><input type="text" size="30" name="paymentPropertiesPath" class="inputBox" value="${(editSetting.paymentPropertiesPath)?if_exists}"/></td>      
        </tr>               
        <tr>
          <td><span class="tableheadtext">${uiLabelMap.ApplyToAll} ${uiLabelMap.ProductProducts}</span></td>
          <td>
              <select name="applyToAllProducts" class="smallSelect">
                  <#if (editSetting.applyToAllProducts)?has_content>
                  <option>${editSetting.applyToAllProducts}</option>
                  <option value="${editSetting.applyToAllProducts}">---</option>
                  </#if>
                  <option value="Y">${uiLabelMap.CommonY}</option>
				  <option value="N">${uiLabelMap.CommonN}</option>                  
              </select>
          </td>
        </tr>               
        <tr>
          <td><input type="submit" class="smallSubmit" value="${buttonText}"/></td>
        </tr>
      </table>  
      </form>
    </#if>
