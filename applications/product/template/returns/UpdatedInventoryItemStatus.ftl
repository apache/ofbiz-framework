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
<#assign inventoryItemType = parameters.inventoryItemType?default("NON_SERIAL_INV_ITEM")>
<#assign inventoryItemStatus = parameters.inventoryItemStatus?default("INV_RETURNED")>
<#if inventoryItemType == "NON_SERIAL_INV_ITEM"> 
    <option value="INV_RETURNED" <#if inventoryItemStatus == "INV_RETURNED">selected="selected"</#if>>${uiLabelMap.ProductReturned}</option>
    <option value="INV_AVAILABLE" <#if inventoryItemStatus == "INV_AVAILABLE">selected="selected"</#if>>${uiLabelMap.ProductAvailable}</option>
    <option value="INV_NS_DEFECTIVE" <#if inventoryItemStatus == "INV_DEFECTIVE">selected="selected"</#if>>${uiLabelMap.ProductDefective}</option>
<#else>
    <option value="INV_RETURNED" <#if inventoryItemStatus == "INV_RETURNED">selected="selected"</#if>>${uiLabelMap.ProductReturned}</option>
    <option value="INV_AVAILABLE" <#if inventoryItemStatus == "INV_AVAILABLE">selected="selected"</#if>>${uiLabelMap.ProductAvailable}</option>
    <option value="INV_DEFECTIVE" <#if inventoryItemStatus == "INV_NS_DEFECTIVE">selected="selected"</#if>>${uiLabelMap.ProductDefective}</option>
</#if>