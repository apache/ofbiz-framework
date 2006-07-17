<#--
Copyright 2001-2006 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
-->
<#if (requestAttributes.uiLabelMap)?exists><#assign uiLabelMap = requestAttributes.uiLabelMap></#if>
<#assign currentPage = "Edit" + page.entityName?default("ContentType") >
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {currentPage : "tabButtonSelected"}>

<div class='tabContainer'>
<a href="<@ofbizUrl>EditDataResourceType</@ofbizUrl>" class="${selectedClassMap.EditDataResourceType?default(unselectedClassName)}">${uiLabelMap.CommonType}</a>
<a href="<@ofbizUrl>EditCharacterSet</@ofbizUrl>" class="${selectedClassMap.EditCharacterSet?default(unselectedClassName)}">${uiLabelMap.ContentCharacterSet}</a>
<a href="<@ofbizUrl>EditDataCategory</@ofbizUrl>" class="${selectedClassMap.EditDataCategory?default(unselectedClassName)}">${uiLabelMap.ContentCategory}</a>
<a href="<@ofbizUrl>EditDataResourceTypeAttr</@ofbizUrl>" class="${selectedClassMap.EditDataResourceTypeAttr?default(unselectedClassName)}">${uiLabelMap.ContentTypeAttr}</a>
<a href="<@ofbizUrl>EditFileExtension</@ofbizUrl>" class="${selectedClassMap.EditFileExtension?default(unselectedClassName)}">${uiLabelMap.ContentFileExt}</a>
<a href="<@ofbizUrl>EditMetaDataPredicate</@ofbizUrl>" class="${selectedClassMap.EditMetaDataPredicate?default(unselectedClassName)}">${uiLabelMap.ContentMetaDataPred}</a>
<a href="<@ofbizUrl>EditMimeType</@ofbizUrl>" class="${selectedClassMap.EditMimeType?default(unselectedClassName)}">${uiLabelMap.ContentMimeType}</a>
</div>
