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
<#if productId?exists && product?exists>
    <table border="1" cellpadding="2" cellspacing="0">
        <tr>
            <td><div class="tabletext"><b>${uiLabelMap.ProductName}</b></div></td>
            <td><div class="tabletext"><b>${uiLabelMap.ProductValueType}</b></div></td>
        </tr>
        <#list productAttributes as productAttribute>
        <tr valign="middle">
            <td><div class="tabletext">${(productAttribute.attrName)?if_exists}</div></td>
            <td>
                <form method="post" action="<@ofbizUrl>UpdateProductAttribute?UPDATE_MODE=UPDATE</@ofbizUrl>">
                    <input type="hidden" name="productId" value="${productAttribute.productId}"/>
                    <input type="hidden" name="PRODUCT_ID" value="${productAttribute.productId}"/>
                    <input type="hidden" name="ATTRIBUTE_NAME" value="${productAttribute.attrName}"/>
                    <input type="text" class="inputBox" size="50" name="ATTRIBUTE_VALUE" value="${(productAttribute.attrValue)?if_exists}"/>
                    <input type="text" class="inputBox" size="15" name="ATTRIBUTE_TYPE" value="${(productAttribute.attrType)?if_exists}"/>
                    <input type="submit" value="${uiLabelMap.CommonUpdate}"/>
                </form>
            </td>
            <td>
            <a href="<@ofbizUrl>UpdateProductAttribute?UPDATE_MODE=DELETE&productId=${productAttribute.productId}&PRODUCT_ID=${productAttribute.productId}&ATTRIBUTE_NAME=${productAttribute.attrName}</@ofbizUrl>" class="buttontext">
            [${uiLabelMap.CommonDelete}]</a>
            </td>
        </tr>
        </#list>
    </table>
    <br/>
    <form method="post" action="<@ofbizUrl>UpdateProductAttribute</@ofbizUrl>" style="margin: 0;">
        <input type="hidden" name="productId" value="${productId}"/>
        <input type="hidden" name="PRODUCT_ID" value="${productId}"/>
        <input type="hidden" name="UPDATE_MODE" value="CREATE"/>
        <input type="hidden" name="useValues" value="true"/>
        <div class="head2">${uiLabelMap.ProductAddProductAttributeNameValueType}:</div>
        <input type="text" class="inputBox" name="ATTRIBUTE_NAME" size="15"/>&nbsp;
        <input type="text" class="inputBox" name="ATTRIBUTE_VALUE" size="50"/>&nbsp;
        <input type="text" class="inputBox" name="ATTRIBUTE_TYPE" size="15"/>&nbsp;
        <input type="submit" value="${uiLabelMap.CommonAdd}"/>
    </form>
</#if>
