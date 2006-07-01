<#assign currentPage =  page.getPageName() >
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {currentPage : "tabButtonSelected"}>

<div class='tabContainer'>
<a href="<@ofbizUrl>FindContent</@ofbizUrl>" class="${selectedClassMap.FindContent?default(unselectedClassName)}">Find</a>
<a href="<@ofbizUrl>EditContent</@ofbizUrl>" class="${selectedClassMap.EditContent?default(unselectedClassName)}">Content</a>
<a href="<@ofbizUrl>EditContentAssoc</@ofbizUrl>" class="${selectedClassMap.EditContentAssoc?default(unselectedClassName)}">Association</a>
<a href="<@ofbizUrl>EditContentRole</@ofbizUrl>" class="${selectedClassMap.EditContentRole?default(unselectedClassName)}">Role</a>
<a href="<@ofbizUrl>EditContentPurpose</@ofbizUrl>" class="${selectedClassMap.EditContentPurpose?default(unselectedClassName)}">Purpose</a>
<a href="<@ofbizUrl>EditContentAttribute</@ofbizUrl>" class="${selectedClassMap.EditContentAttribute?default(unselectedClassName)}">Attribute</a>
<a href="<@ofbizUrl>EditContentMetaData</@ofbizUrl>" class="${selectedClassMap.EditContentMetaData?default(unselectedClassName)}">MetaData</a>
</div>
