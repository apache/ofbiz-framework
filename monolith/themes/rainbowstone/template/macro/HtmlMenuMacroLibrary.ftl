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

<#include "component://common-theme/template/macro/HtmlMenuMacroLibrary.ftl"/>

<#macro renderLink linkUrl parameterList targetWindow uniqueItemName actionUrl confirmation="" linkType="" id="" style="" name="" height="600" width="800" text="" imgStr="">
    <#if linkType?has_content && "hidden-form" == linkType>
    <form method="post" action="${actionUrl}"<#if targetWindow?has_content> target="${targetWindow}"</#if> onsubmit="javascript:submitFormDisableSubmits(this)" name="${uniqueItemName}"><#rt/>
        <#list parameterList as parameter>
            <input name="${parameter.name}" value="${parameter.value?html}" type="hidden"/><#rt/>
        </#list>
    </form><#rt/>
    </#if>
    <#if uniqueItemName?has_content && "layered-modal" == linkType>
      <#local params = "{&quot;presentation&quot;:&quot;layer&quot; ">
      <#if parameterList?has_content>
        <#list parameterList as parameter>
          <#local params += ",&quot;${parameter.name}&quot;: &quot;${parameter.value?html}&quot;">
        </#list>
      </#if>
      <#local params += "}">
    <a href="javascript:void(0);" id="${uniqueItemName}_link"
       data-dialog-params="${params}"
       data-dialog-width="${width}"
       data-dialog-height="${height}"
       data-dialog-url="${linkUrl}"
       <#if text?has_content>data-dialog-title="${text}"</#if>
       <#if style?has_content>class="${style}"</#if>>
        <#if text?has_content>${text}</#if></a>
    <#else>
        <#if (linkType?has_content && "hidden-form" == linkType) || linkUrl?has_content>
        <a<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if><#if name?has_content> name="${name}"</#if><#if confirmation?has_content> onclick="return confirm('${confirmation?js_string}');"</#if><#if targetWindow?has_content && "update-area" != linkType> target="${targetWindow}"</#if> href="<#if "hidden-form"==linkType><#if linkUrl?has_content>javascript:ajaxSubmitFormUpdateAreas('${uniqueItemName}', '${linkUrl}')<#else>javascript:document.${uniqueItemName}.submit()</#if><#else><#if "update-area" == linkType>javascript:ajaxUpdateAreas('${linkUrl}')<#else>${linkUrl}</#if></#if>"><#rt/>
        </#if>
        <#if imgStr?has_content>${imgStr}</#if><#if text?has_content>${text}</#if><#rt/>
        <#if (linkType?has_content && "hidden-form" == linkType) || linkUrl?has_content></a><#rt/></#if>
    </#if>
</#macro>