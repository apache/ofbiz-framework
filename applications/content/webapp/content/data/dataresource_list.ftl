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
<div class="apptitle">&nbsp;DataResources&nbsp;</div>
<table border="0" >
<#if entityList?exists>
<#list entityList as dataResource >
<#assign id=dataResource.dataResourceId />
<#if dataResource.dataResourceTypeId == "ELECTRONIC_TEXT" >
    <#assign uriResource="EditDataResource" />
<#else>
    <#if dataResource.dataResourceTypeId == "IMAGE_OBJECT" >
        <#assign uriResource="EditDataResource" />
    <#else>
        <#if dataResource.dataResourceTypeId == "URL_RESOURCE" >
            <#assign uriResource="EditDataResource" />
        <#else>
            <#assign uriResource="EditDataResource" />
        </#if>
    </#if>
</#if>
<tr>
<td valign="top">
<div class="inputBox">
    <a href="<@ofbizUrl>${uriResource}?dataResourceId=${dataResource.dataResourceId}</@ofbizUrl>">[Edit]</a>
</div>
    ${id}
<br/>
    ${dataResource.dataResourceName?if_exists}
</td>
<td valign="top">
<#if dataResource.dataResourceTypeId == "ELECTRONIC_TEXT" >
${electronicTextMap[id]?if_exists}
<#else>
</#if>
</td>
</tr>
</#list>
</#if>
</table>
<div class="inputBox">
<a href="<@ofbizUrl>AddDataResource</@ofbizUrl>">New DataResource</a>
</div>
