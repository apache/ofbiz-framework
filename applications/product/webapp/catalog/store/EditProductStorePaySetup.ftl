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
