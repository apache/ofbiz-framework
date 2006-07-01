<#assign currentPage = "Edit" + page.entityName?default("ContentType") >
<#assign unselectedClassName = "tabButton">
<#assign selectedClassMap = {currentPage : "tabButtonSelected"}>

<div class='tabContainer'>
<a href="<@ofbizUrl>EditContentType</@ofbizUrl>" class="${selectedClassMap.EditContentType?default(unselectedClassName)}">Type</a>
<a href="<@ofbizUrl>EditContentAssocType</@ofbizUrl>" class="${selectedClassMap.EditContentAssocType?default(unselectedClassName)}">AssocType</a>
<a href="<@ofbizUrl>EditContentPurposeType</@ofbizUrl>" class="${selectedClassMap.EditContentPurposeType?default(unselectedClassName)}">PurposeType</a>
<a href="<@ofbizUrl>EditContentTypeAttr</@ofbizUrl>" class="${selectedClassMap.EditContentTypeAttr?default(unselectedClassName)}">TypeAttr</a>
<a href="<@ofbizUrl>EditContentAssocPredicate</@ofbizUrl>" class="${selectedClassMap.EditContentAssocPredicate?default(unselectedClassName)}">AssocPredicate</a>
<a href="<@ofbizUrl>EditContentOperation</@ofbizUrl>" class="${selectedClassMap.EditContentOperation?default(unselectedClassName)}">Operation</a>
<a href="<@ofbizUrl>EditContentPurposeOperation</@ofbizUrl>" class="${selectedClassMap.EditContentPurposeOperation?default(unselectedClassName)}">PurposeOperation</a>
</div>
