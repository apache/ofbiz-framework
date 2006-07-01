<#assign currentPage =  page.getPageName() >
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {currentPage : "tabButtonSelected"}>
<#assign dataResourceClass="tabButton"/>
<#assign dataResourceUri="EditDataResource"/>
<#if currentPK?exists >
    <#assign valSeq = currentPK.values() />
    <#list valSeq as val>
        <#if !val?exists >
             <#assign dataResourceUri="AddDataResource"/>
             <#break/>
        </#if>
    </#list>
</#if>
<#if currentPage == dataResourceUri>
    <#assign dataResourceClass="tabButtonSelected"/>
</#if>
<#assign currentValue = requestAttributes.currentValue?if_exists>

<div class='tabContainer'>
<a href="<@ofbizUrl>FindDataResource</@ofbizUrl>" class="${selectedClassMap.FindDataResource?default(unselectedClassName)}">Find</a>
<a href="<@ofbizUrl>BrowseDataResource</@ofbizUrl>" class="${selectedClassMap.BrowseDataResource?default(unselectedClassName)}">Browse</a>
<#--
<a href="<@ofbizUrl>EditDataResource</@ofbizUrl>" class="${selectedClassMap.EditDataResource?default(unselectedClassName)}">DataResource</a>
-->
<a href="<@ofbizUrl>EditDataResource</@ofbizUrl>" class="${dataResourceClass}">DataResource</a>
<a href="<@ofbizUrl>EditElectronicText</@ofbizUrl>" class="${selectedClassMap.EditElectronicText?default(unselectedClassName)}">Text</a>
<a href="<@ofbizUrl>EditHtmlText</@ofbizUrl>" class="${selectedClassMap.EditHtmlText?default(unselectedClassName)}">Html</a>
<a href="<@ofbizUrl>UploadImage</@ofbizUrl>" class="${selectedClassMap.UploadImage?default(unselectedClassName)}">Image</a>
<a href="<@ofbizUrl>EditDataResourceAttribute</@ofbizUrl>" class="${selectedClassMap.EditDataResourceAttribute?default(unselectedClassName)}">Attribute</a>
<a href="<@ofbizUrl>EditDataResourceRole</@ofbizUrl>" class="${selectedClassMap.EditDataResourceRole?default(unselectedClassName)}">Role</a>
<a href="<@ofbizUrl>EditDataResourceProductFeatures</@ofbizUrl>" class="${selectedClassMap.EditDataResourceProductFeatures?default(unselectedClassName)}">ProductFeatures</a>
<#if currentValue?has_content>
    <a href="<@ofbizUrl>FindContent?dataResourceId=${(currentValue.dataResourceId)?if_exists}</@ofbizUrl>" class="${selectedClassMap.FindContent?default(unselectedClassName)}">Find Content</a>
</#if>
</div>
