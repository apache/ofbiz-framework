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
