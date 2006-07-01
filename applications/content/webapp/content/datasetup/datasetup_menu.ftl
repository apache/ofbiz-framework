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
