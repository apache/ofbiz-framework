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
