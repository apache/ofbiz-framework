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
<#assign mapKey=wrapMapKey?if_exists/>
<#assign subDataResourceId=wrapDataResourceId?if_exists/>
<#assign subDataResourceTypeId=wrapDataResourceTypeId?if_exists/>
<#assign contentIdTo=wrapContentIdTo?if_exists/>
<#assign mimeTypeId=wrapMimeTypeId?if_exists/>
<#assign subContentId=wrapSubContentId?if_exists/>

mapKey:${mapKey?if_exists}
<div id="divTwo" class="wrapOuter">
<div id="divOne" class="wrapInner">
<@renderWrappedText />
</div>
<a class="tabButton" href="javascript:lookupSubContent('<@ofbizUrl>LookupSubContent</@ofbizUrl>', '${contentIdTo?if_exists}','${mapKey?if_exists}',  '${subDataResourceTypeId?if_exists}', '${mimeTypeId?if_exists}') " > 
&nbsp;&nbsp;&nbsp;&nbsp;Lookup&nbsp;&nbsp;&nbsp;&nbsp;
</a>
&nbsp;
<#assign ofbizRequest=""/>
<#assign httpParams="contentIdTo=" + contentIdTo?if_exists + "&mapKey=" + mapKey?if_exists />
<#if subDataResourceTypeId == "IMAGE_OBJECT">
    <#assign ofbizRequest="EditLayoutImage" />
<#else>
    <#if subDataResourceTypeId == "URL_RESOURCE">
        <#assign ofbizRequest="EditLayoutUrl" />
    <#else>
        <#assign ofbizRequest="EditLayoutSubContent" />
    </#if>
</#if>
<a class="tabButton" href="<@ofbizUrl>${ofbizRequest}?${httpParams}&mode=add</@ofbizUrl>" >New</a>
<#if subContentId?exists && (0 < subContentId?length)>
&nbsp;
    <a class="tabButton" href="<@ofbizUrl>${ofbizRequest}?${httpParams}&contentId=${subContentId}&drDataResourceId=${subDataResourceId?if_exists}</@ofbizUrl>" >Edit</a>
</#if>
</div>
