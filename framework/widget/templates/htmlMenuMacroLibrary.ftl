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

<#macro renderMenuBegin boundaryComment="" id="" style="" title="">
  <#if boundaryComment?has_content>
<!-- ${boundaryComment} -->
  </#if>
  <#-- FIXME: A menu could have an ID and a style, but some visual themes break if both are used. -->
  <div<#if id?has_content> id="${id}"<#elseif style?has_content> class="${style}"</#if>>
  <#if title?has_content>
  <h2>${title}</h2>
  </#if>
  <ul>
    <li>
      <ul>
</#macro>

<#macro renderMenuEnd boundaryComment="">
      </ul>
    </li>
  </ul>
  <br class="clear"/>
  </div>
<#if boundaryComment?has_content>
<!-- ${boundaryComment} -->
</#if>
</#macro>

<#macro renderImage src id style width height border>
<img src="${src}"<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if><#if width?has_content> width="${width}"</#if><#if height?has_content> height="${height}"</#if><#if border?has_content> border="${border}"</#if> />
</#macro>

<#macro renderLink linkUrl parameterList targetWindow uniqueItemName actionUrl linkType="" id="" style="" name="" height="" width="" text="" imgStr="">
  <#if "hidden-form" == linkType>
<form method="post" action="${actionUrl}"<#if targetWindow?has_content> target="${targetWindow}"</#if> onsubmit="javascript:submitFormDisableSubmits(this)" name="${uniqueItemName}"><#rt/>
    <#list parameterList as parameter>
<input name="${parameter.name}" value="${parameter.value}" type="hidden"/><#rt/>
    </#list>
</form><#rt/>
  </#if>
<a<#if id?has_content> id="${id}"</#if><#if style?has_content> class="${style}"</#if><#if name?has_content> name="${name}"</#if><#if targetWindow?has_content> target="${targetWindow}"</#if> href="<#if "hidden-form"==linkType>javascript:document.${uniqueItemName}.submit()<#else>${linkUrl}</#if>"><#if imgStr?has_content>${imgStr}</#if><#if text?has_content>${text}</#if></a><#rt/>
</#macro>

<#macro renderMenuItemBegin style toolTip linkStr containsNestedMenus>
        <li<#if style?has_content> class="${style}"</#if><#if toolTip?has_content> title="${toolTip}"</#if>><#if linkStr?has_content>${linkStr}</#if><#if containsNestedMenus><ul></#if><#rt/>
</#macro>

<#macro renderMenuItemEnd containsNestedMenus>
<#if containsNestedMenus></ul></#if></li>
</#macro>
